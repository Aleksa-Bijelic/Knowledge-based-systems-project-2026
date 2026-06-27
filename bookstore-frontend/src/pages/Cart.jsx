import { useNavigate, Navigate } from 'react-router-dom';
import { request, authHeader } from '../api';
import { useState, useEffect } from 'react';

const DEFAULT_IMAGE_URL = 'https://www.klett-cotta.de/assets/default-image.jpg';

function CardPaymentForm({ cardData, setCardData, error }) {
  return (
    <div className="card-payment-form">
      <h4>Card Payment Details</h4>
      <div className="form-row">
        <label>Card Number</label>
        <input
          type="text"
          placeholder="16-digit card number"
          value={cardData.cardNumber}
          onChange={(e) => setCardData({ ...cardData, cardNumber: e.target.value })}
          maxLength={16}
          required
        />
      </div>
      <div className="form-row">
        <label>Cardholder Name</label>
        <input
          type="text"
          placeholder="Name on card"
          value={cardData.cardholderName}
          onChange={(e) => setCardData({ ...cardData, cardholderName: e.target.value })}
          required
        />
      </div>
      <div className="form-grid">
        <div className="form-row">
          <label>Expiration Date</label>
          <input
            type="date"
            value={cardData.cardExpirationDate}
            onChange={(e) => setCardData({ ...cardData, cardExpirationDate: e.target.value })}
            required
          />
        </div>
        <div className="form-row">
          <label>CVV</label>
          <input
            type="text"
            placeholder="3-digit CVV"
            value={cardData.cardCvv}
            onChange={(e) => setCardData({ ...cardData, cardCvv: e.target.value })}
            maxLength={3}
            required
          />
        </div>
      </div>
    </div>
  );
}

function Cart({ cart, onUpdateQuantity, onRemoveFromCart, onClearCart, token, username }) {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [paymentMethod, setPaymentMethod] = useState('cash');
  const [preview, setPreview] = useState(null);
  const [previewLoading, setPreviewLoading] = useState(false);
  const [cardData, setCardData] = useState({
    cardNumber: '',
    cardCvv: '',
    cardExpirationDate: '',
    cardholderName: '',
  });
  const [publicIp, setPublicIp] = useState('');

  useEffect(() => {
    fetch('https://api.ipify.org?format=json')
      .then(r => r.json())
      .then(d => setPublicIp(d.ip))
      .catch(() => setPublicIp(''));
  }, []);

  const cartTotal = cart.reduce((sum, item) => sum + item.book.price * item.quantity, 0);

  async function handleCheckout() {
    if (!username) {
      navigate('/login');
      return;
    }

    if (cart.length === 0) {
      setError('Cart is empty');
      return;
    }

    if (paymentMethod === 'card') {
      if (!cardData.cardNumber || cardData.cardNumber.length < 16) {
        setError('Please enter a valid 16-digit card number');
        return;
      }
      if (!cardData.cardCvv || cardData.cardCvv.length < 3) {
        setError('Please enter a valid 3-digit CVV');
        return;
      }
      if (!cardData.cardExpirationDate) {
        setError('Please enter card expiration date');
        return;
      }
      if (!cardData.cardholderName || cardData.cardholderName.trim() === '') {
        setError('Please enter cardholder name');
        return;
      }
    }

    setLoading(true);
    setError(null);

    try {
      const orderRequest = {
        customerUsername: username,
        paymentMethod: paymentMethod,
        clientIp: publicIp || undefined,
        items: cart.map((item) => ({
          bookId: item.book.id,
          quantity: item.quantity,
        })),
      };

      if (paymentMethod === 'card') {
        orderRequest.cardNumber = cardData.cardNumber;
        orderRequest.cardCvv = cardData.cardCvv;
        orderRequest.cardExpirationDate = cardData.cardExpirationDate;
        orderRequest.cardholderName = cardData.cardholderName;
      }

      const response = await request('/orders', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          ...authHeader(token),
        },
        body: JSON.stringify(orderRequest),
      });

      localStorage.setItem('lastOrderId', response.id);
      localStorage.setItem('lastPaymentMethod', paymentMethod);
      onClearCart();
      navigate('/books');
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  async function fetchPreview() {
    if (cart.length === 0) {
      setPreview(null);
      return;
    }

    setPreviewLoading(true);
    setError(null);

    try {
      const orderRequest = {
        customerUsername: username || 'guest',
        paymentMethod,
        items: cart.map((item) => ({
          bookId: item.book.id,
          quantity: item.quantity,
        })),
      };

      const response = await request('/orders/preview', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          ...authHeader(token),
        },
        body: JSON.stringify(orderRequest),
      });

      setPreview(response);
    } catch (err) {
      setPreview(null);
      setError(err.message);
    } finally {
      setPreviewLoading(false);
    }
  }

  useEffect(() => {
    fetchPreview();
  }, [cart, paymentMethod, username, token]);

  if (!username || username === 'admin') {
    return <Navigate to="/books" replace />;
  }

  if (cart.length === 0) {
    return (
      <div className="card">
        <h2>Your Cart</h2>
        <p className="empty-state">Your cart is empty</p>
        <button className="btn btn-primary" onClick={() => navigate('/books')}>
          Continue Shopping
        </button>
      </div>
    );
  }

  return (
    <div className="card">
      <h2>Your Cart</h2>
      {error && <div className="error">{error}</div>}

      <div className="cart-items">
        {cart.map((item) => (
          <div key={item.book.id} className="cart-item">
            <div className="cart-item-image">
              <img
                src={item.book.imageUrl || DEFAULT_IMAGE_URL}
                alt={item.book.title}
              />
            </div>
            <div className="cart-item-details">
              <h3>{item.book.title}</h3>
              <p className="cart-item-author">by {item.book.author}</p>
              <p className="cart-item-price">{item.book.price.toFixed(2)} RSD</p>
              {preview && preview.selectedDiscountType === 'ITEM_DISCOUNT' && (() => {
                const previewItem = preview.items.find((previewItem) => previewItem.bookId === item.book.id);
                const itemDiscount = previewItem?.itemDiscount || 0;
                const itemTotal = item.book.price * item.quantity;
                const discountPercent = itemDiscount > 0 ? (itemDiscount / itemTotal) * 100 : 0;

                return itemDiscount > 0 ? (
                  <p className="cart-item-discount">
                    Discount: {discountPercent.toFixed(1)}%
                  </p>
                ) : null;
              })()}
            </div>
            <div className="cart-item-controls">
              <input
                type="number"
                min="1"
                value={item.quantity}
                onChange={(e) => onUpdateQuantity(item.book.id, parseInt(e.target.value))}
                className="quantity-input"
              />
              <p className="cart-item-subtotal">
                {(item.book.price * item.quantity).toFixed(2)} RSD
              </p>
            </div>
            <button
              className="btn btn-danger small"
              onClick={() => onRemoveFromCart(item.book.id)}
            >
              Remove
            </button>
          </div>
        ))}
      </div>

      <div className="cart-summary">
        <div className="summary-row">
          <span>Total:</span>
          <span className="summary-amount">{cartTotal.toFixed(2)} RSD</span>
        </div>
        {preview && (
          <>
            <div className="summary-row">
              <span>Discount:</span>
              <span className="summary-amount">-{preview.discountAmount.toFixed(2)} RSD</span>
            </div>
            <div className="summary-row">
              <span>Final total:</span>
              <span className="summary-amount">{preview.finalAmount.toFixed(2)} RSD</span>
            </div>
            <div className="summary-note">
              {preview.selectedDiscountType === 'ORDER_DISCOUNT'
                ? 'Best discount applied at order level.'
                : 'Best discount applied at item level.'}
            </div>
          </>
        )}
        {previewLoading && <div className="summary-note">Calculating discount preview...</div>}
      </div>

      <div className="payment-method-selector">
        <label>Payment Method:</label>
        <div className="payment-options">
          <label>
            <input
              type="radio"
              name="paymentMethod"
              value="cash"
              checked={paymentMethod === 'cash'}
              onChange={(e) => setPaymentMethod(e.target.value)}
            />
            <span>Cash on Delivery</span>
          </label>
          <label>
            <input
              type="radio"
              name="paymentMethod"
              value="card"
              checked={paymentMethod === 'card'}
              onChange={(e) => setPaymentMethod(e.target.value)}
            />
            <span>Credit Card Payment</span>
          </label>
        </div>
      </div>

      {paymentMethod === 'card' && (
        <CardPaymentForm cardData={cardData} setCardData={setCardData} error={error} />
      )}

      <div className="cart-actions">
        <button className="btn btn-secondary" onClick={() => navigate('/books')}>
          Continue Shopping
        </button>
        <button
          className="btn btn-primary"
          onClick={handleCheckout}
          disabled={loading || cart.length === 0}
        >
          {loading ? 'Processing...' : 'Checkout'}
        </button>
      </div>
    </div>
  );
}

export default Cart;
