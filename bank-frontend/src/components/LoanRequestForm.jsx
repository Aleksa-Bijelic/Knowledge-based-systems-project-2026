import { useState, useEffect } from 'react';
import { FiSend, FiLoader } from 'react-icons/fi';
import { request, authHeader } from '../api';

const EMPLOYMENT_STATUSES = [
  { value: 'UNEMPLOYED', label: 'Unemployed' },
  { value: 'FIXED_TERM', label: 'Fixed-term contract' },
  { value: 'INDEFINITE', label: 'Indefinite employment' },
];

function LoanRequestForm({ token, onAssessmentResult }) {
  const [clients, setClients] = useState([]);
  const [selectedClientId, setSelectedClientId] = useState('');
  const [loanAmount, setLoanAmount] = useState('');
  const [numberOfInstallments, setNumberOfInstallments] = useState('');
  const [employmentStatus, setEmploymentStatus] = useState('INDEFINITE');
  const [contractStartDate, setContractStartDate] = useState('');
  const [contractEndDate, setContractEndDate] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [clientsLoading, setClientsLoading] = useState(true);

  useEffect(() => {
    const loadClients = async () => {
      try {
        const data = await request('/loans/clients', {
          headers: { ...authHeader(token) },
        });
        setClients(Array.isArray(data) ? data : []);
      } catch (err) {
        setError(err.message);
      } finally {
        setClientsLoading(false);
      }
    };
    loadClients();
  }, [token]);

  const reset = () => {
    setSelectedClientId('');
    setLoanAmount('');
    setNumberOfInstallments('');
    setEmploymentStatus('INDEFINITE');
    setContractStartDate('');
    setContractEndDate('');
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError(null);
    setLoading(true);
    try {
      const payload = {
        clientId: parseInt(selectedClientId, 10),
        loanAmount: parseFloat(loanAmount),
        numberOfInstallments: parseInt(numberOfInstallments, 10),
        employmentStatus,
        contractStartDate: contractStartDate || null,
        contractEndDate: contractEndDate || null,
      };
      const result = await request('/loans/assess', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          ...authHeader(token),
        },
        body: JSON.stringify(payload),
      });
      onAssessmentResult(result);
      reset();
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const showEndDate = employmentStatus === 'FIXED_TERM';
  const isEmployed = employmentStatus !== 'UNEMPLOYED';

  return (
    <form className="loan-form" onSubmit={handleSubmit}>
      <div className="form-row">
        <label>Client</label>
        <select
          value={selectedClientId}
          onChange={(e) => setSelectedClientId(e.target.value)}
          required
          disabled={clientsLoading}
        >
          <option value="">
            {clientsLoading ? 'Loading clients...' : 'Select a client'}
          </option>
          {clients.map((c) => (
            <option key={c.id} value={c.id}>
              {c.firstName} {c.lastName} ({c.username})
            </option>
          ))}
        </select>
      </div>

      <div className="form-grid">
        <div className="form-row">
          <label>Loan amount (RSD)</label>
          <input
            type="number"
            min="1"
            step="1"
            value={loanAmount}
            onChange={(e) => setLoanAmount(e.target.value)}
            placeholder="e.g. 500000"
            required
          />
        </div>
        <div className="form-row">
          <label>Number of installments (months)</label>
          <input
            type="number"
            min="3"
            max="360"
            value={numberOfInstallments}
            onChange={(e) => setNumberOfInstallments(e.target.value)}
            placeholder="e.g. 60"
            required
          />
        </div>
      </div>

      <div className="form-row">
        <label>Employment status</label>
        <select
          value={employmentStatus}
          onChange={(e) => setEmploymentStatus(e.target.value)}
          required
        >
          {EMPLOYMENT_STATUSES.map((s) => (
            <option key={s.value} value={s.value}>{s.label}</option>
          ))}
        </select>
      </div>

      {isEmployed && (
        <div className="form-grid">
          <div className="form-row">
            <label>Contract start date</label>
            <input
              type="date"
              value={contractStartDate}
              onChange={(e) => setContractStartDate(e.target.value)}
              required
            />
          </div>
          {showEndDate && (
            <div className="form-row">
              <label>Contract end date</label>
              <input
                type="date"
                value={contractEndDate}
                onChange={(e) => setContractEndDate(e.target.value)}
                required
              />
            </div>
          )}
        </div>
      )}

      <div className="form-actions">
        <button type="submit" className="btn btn-primary" disabled={loading}>
          {loading ? (
            <>
              <FiLoader className="icon spin" />
              Processing...
            </>
          ) : (
            <>
              <FiSend className="icon" />
              Assess loan
            </>
          )}
        </button>
      </div>
      {error && <div className="error">{error}</div>}
    </form>
  );
}

export default LoanRequestForm;
