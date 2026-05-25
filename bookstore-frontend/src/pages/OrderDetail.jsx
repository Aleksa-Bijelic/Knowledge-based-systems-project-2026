import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams, useLocation } from 'react-router-dom';
import { request, authHeader } from '../api';

function OrderDetail({ token, username }) {
  const { orderId } = useParams();
  const location = useLocation();
  const navigate = useNavigate();
  const [order, setOrder] = useState(location.state?.order || null);
  const [loading, setLoading] = useState(!order);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!order) {
      async function fetchOrder() {
        setLoading(true);
        try {
          const data = await request(`/orders/${orderId}`, {
            headers: {
              'Content-Type': 'application/json',
              ...authHeader(token),
            },
          });
          setOrder(data);
        } catch (err) {
          setError(err.message);
        } finally {
          setLoading(false);
        }
      }
      fetchOrder();
    }
  }, [orderId, token, order]);

  if (loading) {
    return <div className="card">Loading order details...</div>;
  }

  if (error) {
    return (
      <div className="card">
        <div className="error">{error}</div>
        <button className="btn btn-secondary" onClick={() => navigate('/books')}>
          Back to library
        </button>
      </div>
    );
  }

  if (!order) {
    return (
      <div className="card">
        <p>Order not found</p>
        <button className="btn btn-secondary" onClick={() => navigate('/books')}>
          Back to library
        </button>
      </div>
    );
  }

  return (
    <div className="card order-detail-card">
      <div className="order-header">
        <h2>Order #{order.id}</h2>
        <span className={`order-status status-${order?.status?.toLowerCase() || 'pending'}`}>
          {order?.status || 'PENDING'}
        </span>
      </div>

      <div className="order-info">
        <div className="info-row">
          <span className="info-label">Customer:</span>
          <span>{order?.customerUsername}</span>
        </div>
        <div className="info-row">
          <span className="info-label">Order Date:</span>
          <span>{order?.createdAt ? new Date(order.createdAt).toLocaleString() : 'N/A'}</span>
        </div>
      </div>

      <div className="order-items-section">
        <h3>Order Items</h3>
        <div className="order-items-list">
          {order?.items?.map((item) => (
            <div key={item.id} className="order-item">
              <div className="order-item-info">
                <div className="order-item-title">{item.book?.title}</div>
                <div className="order-item-author">by {item.book?.author}</div>
              </div>
              <div className="order-item-quantity">Qty: {item.quantity}</div>
              <div className="order-item-price">{item.unitPrice?.toFixed(2) || '0.00'} RSD</div>
              <div className="order-item-subtotal">
                {((item.unitPrice || 0) * item.quantity).toFixed(2)} RSD
              </div>
            </div>
          ))}
        </div>
      </div>

      <div className="order-summary">
        <div className="summary-row total">
          <span>Total Amount:</span>
          <span className="summary-amount">{order?.totalAmount?.toFixed(2) || '0.00'} RSD</span>
        </div>
      </div>

      <div className="order-actions">
        <Link to="/books" className="btn btn-secondary">
          Continue Shopping
        </Link>
      </div>
    </div>
  );
}

export default OrderDetail;
