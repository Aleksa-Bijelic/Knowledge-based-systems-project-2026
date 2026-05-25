import { useNavigate, Navigate } from 'react-router-dom';
import { request, authHeader } from '../api';
import { useState, useEffect } from 'react';

function Cart({ cart, onUpdateQuantity, onRemoveFromCart, onClearCart, token, username }) {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [paymentMethod, setPaymentMethod] = useState('cash');
  const [preview, setPreview] = useState(null);
  const [previewLoading, setPreviewLoading] = useState(false);

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

    setLoading(true);
    setError(null);

    try {
      const orderRequest = {
        customerUsername: username,
        paymentMethod: paymentMethod,
        items: cart.map((item) => ({
          bookId: item.book.id,
          quantity: item.quantity,
        })),
      };

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
                src={item.book.imageUrl || 'https://via.placeholder.com/80x120?text=No+Cover'}
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
