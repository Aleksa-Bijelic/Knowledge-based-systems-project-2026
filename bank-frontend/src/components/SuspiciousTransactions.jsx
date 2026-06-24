import { useEffect, useState, useCallback } from 'react';
import { FiAlertTriangle, FiCheck, FiX, FiRefreshCw } from 'react-icons/fi';
import { request, authHeader } from '../api';

function SuspiciousTransactions({ token }) {
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [actionLoading, setActionLoading] = useState(null);

  const loadSuspicious = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await request('/payments/suspicious', {
        headers: { ...authHeader(token) },
      });
      setTransactions(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, [token]);

  useEffect(() => {
    loadSuspicious();
    const interval = setInterval(loadSuspicious, 15000);
    return () => clearInterval(interval);
  }, [loadSuspicious]);

  const handleAction = async (id, action) => {
    setActionLoading(id);
    try {
      await request(`/payments/${id}/${action}`, {
        method: 'POST',
        headers: { ...authHeader(token) },
      });
      setTransactions((prev) => prev.filter((tx) => tx.id !== id));
    } catch (err) {
      setError(err.message);
    } finally {
      setActionLoading(null);
    }
  };

  if (loading && transactions.length === 0) {
    return (
      <section className="card">
        <h3>Suspicious Transactions</h3>
        <div className="loading-copy">Checking for suspicious transactions...</div>
      </section>
    );
  }

  if (transactions.length === 0) {
    return null;
  }

  return (
    <section className="card suspicious-card">
      <div className="section-header">
        <div>
          <div className="section-eyebrow">
            <FiAlertTriangle className="icon" />
            Fraud Alert
          </div>
          <h3>Suspicious Transactions</h3>
          <p className="card-subtitle">
            The following transactions were flagged by our fraud detection system.
            Review and approve or reject each transaction.
          </p>
        </div>
        <button className="btn btn-outline" onClick={loadSuspicious}>
          <FiRefreshCw className="icon" />
          Refresh
        </button>
      </div>

      {error && <div className="error">{error}</div>}

      <div className="suspicious-list">
        {transactions.map((tx) => (
          <div key={tx.id} className="suspicious-item">
            <div className="suspicious-header">
              <span className="suspicious-id">#{tx.id}</span>
              <span className="suspicious-status">{tx.status}</span>
            </div>
            <div className="suspicious-details">
              <div className="suspicious-detail">
                <span className="detail-label">Amount</span>
                <span className="detail-value">{Number(tx.amount).toFixed(2)} {tx.currency}</span>
              </div>
              <div className="suspicious-detail">
                <span className="detail-label">From</span>
                <span className="detail-value mono">{tx.senderAccountNumber}</span>
              </div>
              <div className="suspicious-detail">
                <span className="detail-label">To</span>
                <span className="detail-value mono">{tx.receiverAccountNumber}</span>
              </div>
              <div className="suspicious-detail">
                <span className="detail-label">Date</span>
                <span className="detail-value">{new Date(tx.createdAt).toLocaleString()}</span>
              </div>
            </div>
            {tx.fraudReason && (
              <div className="suspicious-reason">
                <FiAlertTriangle className="icon" />
                {tx.fraudReason}
              </div>
            )}
            <div className="suspicious-actions">
              <button
                className="btn btn-approve"
                disabled={actionLoading === tx.id}
                onClick={() => handleAction(tx.id, 'approve')}
              >
                <FiCheck className="icon" />
                {actionLoading === tx.id ? 'Processing...' : 'Approve'}
              </button>
              <button
                className="btn btn-reject"
                disabled={actionLoading === tx.id}
                onClick={() => handleAction(tx.id, 'reject')}
              >
                <FiX className="icon" />
                {actionLoading === tx.id ? 'Processing...' : 'Reject'}
              </button>
            </div>
          </div>
        ))}
      </div>
    </section>
  );
}

export default SuspiciousTransactions;
