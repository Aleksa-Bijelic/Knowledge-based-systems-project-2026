import { useEffect, useState } from 'react';
import { FiPlus, FiCreditCard, FiRefreshCw, FiFileText } from 'react-icons/fi';
import { request, authHeader } from '../api';
import LoanRequestForm from '../components/LoanRequestForm';
import LoanAssessmentResult from '../components/LoanAssessmentResult';

const CURRENCIES = ['RSD', 'EUR', 'USD'];

function CreatePackageAccountForm({ token, onCreated }) {
  const [name, setName] = useState('');
  const [currency, setCurrency] = useState('RSD');
  const [initialBalance, setInitialBalance] = useState('0');
  const [cardholderName, setCardholderName] = useState('');
  const [expirationYears, setExpirationYears] = useState('3');
  const [error, setError] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  const reset = () => {
    setName('');
    setCurrency('RSD');
    setInitialBalance('0');
    setCardholderName('');
    setExpirationYears('3');
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      const payload = {
        name: name.trim(),
        currency,
        initialBalance: parseFloat(initialBalance || '0'),
        cardholderName: cardholderName.trim(),
        cardExpirationYears: parseInt(expirationYears, 10),
      };
      const created = await request('/package-accounts', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          ...authHeader(token),
        },
        body: JSON.stringify(payload),
      });
      onCreated(created);
      reset();
    } catch (err) {
      setError(err.message);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <form className="package-form" onSubmit={handleSubmit}>
      <div className="form-row">
        <label>Package account name</label>
        <input
          value={name}
          onChange={(e) => setName(e.target.value)}
          placeholder="e.g. Personal, Business, Family"
          required
        />
      </div>
      <div className="form-grid">
        <div className="form-row">
          <label>Currency</label>
          <select value={currency} onChange={(e) => setCurrency(e.target.value)}>
            {CURRENCIES.map((c) => (
              <option key={c} value={c}>{c}</option>
            ))}
          </select>
        </div>
        <div className="form-row">
          <label>Initial balance</label>
          <input
            type="number"
            min="0"
            step="0.01"
            value={initialBalance}
            onChange={(e) => setInitialBalance(e.target.value)}
            required
          />
        </div>
      </div>
      <div className="form-row">
        <label>Cardholder name</label>
        <input
          value={cardholderName}
          onChange={(e) => setCardholderName(e.target.value)}
          placeholder="Name printed on the card"
          required
        />
      </div>
      <div className="form-row">
        <label>Card validity (years)</label>
        <input
          type="number"
          min="1"
          max="10"
          value={expirationYears}
          onChange={(e) => setExpirationYears(e.target.value)}
          required
        />
      </div>
      <div className="form-actions">
        <button type="submit" className="btn btn-primary" disabled={submitting}>
          <FiPlus className="icon" />
          {submitting ? 'Creating...' : 'Create package account'}
        </button>
      </div>
      {error && <div className="error">{error}</div>}
    </form>
  );
}

function PackageAccountCard({ pkg }) {
  const { account, card } = pkg;
  return (
    <div className="package-card">
      <div className="package-card-header">
        <div>
          <div className="package-card-eyebrow">Package account</div>
          <h3>{pkg.name}</h3>
        </div>
        <FiCreditCard className="package-card-icon" />
      </div>
      {account && (
        <div className="package-card-section">
          <div className="package-card-label">Bank account</div>
          <div className="package-card-value">{account.accountNumber}</div>
          <div className="package-card-meta">
            <span>Balance: <strong>{Number(account.balance).toFixed(2)} {account.currency}</strong></span>
          </div>
        </div>
      )}
      {card && (
        <div className="package-card-section card-section">
          <div className="package-card-label">Payment card</div>
          <div className="card-number">{card.maskedCardNumber || card.cardNumber}</div>
          <div className="package-card-meta">
            <span>Cardholder: <strong>{card.cardholderName}</strong></span>
            <span>Expires: {card.expirationDate}</span>
            <span>CVV: {card.cvv}</span>
          </div>
        </div>
      )}
      <div className="package-card-footer">
        Created on {new Date(pkg.createdAt).toLocaleDateString()}
      </div>
    </div>
  );
}

function OfficerDashboard({ token, username }) {
  const [assessmentResult, setAssessmentResult] = useState(null);
  const [showForm, setShowForm] = useState(false);

  return (
    <div className="dashboard-page">
      <section className="card welcome-card">
        <div>
          <div className="section-eyebrow">Officer Portal</div>
          <h2>Hello, {username}</h2>
          <p className="card-subtitle">
            Process loan requests for bank clients. Submit an assessment to evaluate loan eligibility.
          </p>
        </div>
        <div className="welcome-actions">
          <button
            className="btn btn-primary"
            onClick={() => { setShowForm((prev) => !prev); setAssessmentResult(null); }}
          >
            <FiFileText className="icon" />
            {showForm ? 'Close form' : 'New loan assessment'}
          </button>
        </div>
      </section>

      {showForm && (
        <section className="card">
          <h3>Loan Assessment</h3>
          <p className="card-subtitle">
            Fill in the loan details and the rule-based system will evaluate whether the client qualifies.
          </p>
          <LoanRequestForm
            token={token}
            onAssessmentResult={(result) => {
              setAssessmentResult(result);
              setShowForm(false);
            }}
          />
        </section>
      )}

      {assessmentResult && (
        <section className="card">
          <h3>Assessment Result</h3>
          <LoanAssessmentResult result={assessmentResult} />
        </section>
      )}
    </div>
  );
}

function ClientDashboard({ token, username }) {
  const [packageAccounts, setPackageAccounts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [showForm, setShowForm] = useState(false);

  const loadPackageAccounts = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await request('/package-accounts', {
        headers: { ...authHeader(token) },
      });
      setPackageAccounts(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadPackageAccounts();
  }, [token]);

  const handleCreated = (created) => {
    setPackageAccounts((prev) => [...prev, created]);
    setShowForm(false);
  };

  return (
    <div className="dashboard-page">
      <section className="card welcome-card">
        <div>
          <div className="section-eyebrow">Welcome</div>
          <h2>Hello, {username}</h2>
          <p className="card-subtitle">
            From here you can manage your package accounts. Each package account bundles a bank account with a payment card.
          </p>
        </div>
        <div className="welcome-actions">
          <button
            className="btn btn-primary"
            onClick={() => setShowForm((prev) => !prev)}
          >
            <FiPlus className="icon" />
            {showForm ? 'Close form' : 'New package account'}
          </button>
          <button className="btn btn-outline" onClick={loadPackageAccounts}>
            <FiRefreshCw className="icon" />
            Refresh
          </button>
        </div>
      </section>

      {showForm && (
        <section className="card">
          <h3>Create a new package account</h3>
          <p className="card-subtitle">
            A new bank account and a payment card will be issued automatically. You can name this package for easier tracking.
          </p>
          <CreatePackageAccountForm token={token} onCreated={handleCreated} />
        </section>
      )}

      <section className="card">
        <div className="section-header">
          <div>
            <div className="section-eyebrow">My package accounts</div>
            <h3>Active packages</h3>
          </div>
        </div>
        {error && <div className="error">{error}</div>}
        {loading && !error && <div className="loading-copy">Loading package accounts...</div>}
        {!loading && !error && packageAccounts.length === 0 && (
          <p className="empty-state">
            You don't have any package accounts yet. Click "New package account" to create your first one.
          </p>
        )}
        {!loading && !error && packageAccounts.length > 0 && (
          <div className="package-grid">
            {packageAccounts.map((pkg) => (
              <PackageAccountCard key={pkg.id} pkg={pkg} />
            ))}
          </div>
        )}
      </section>
    </div>
  );
}

function Dashboard({ token, username, role }) {
  const isOfficer = role === 'ROLE_OFFICER';

  return isOfficer ? (
    <OfficerDashboard token={token} username={username} />
  ) : (
    <ClientDashboard token={token} username={username} />
  );
}

export default Dashboard;
