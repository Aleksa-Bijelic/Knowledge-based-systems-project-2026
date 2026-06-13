import { FiCheckCircle, FiXCircle, FiAlertTriangle, FiInfo } from 'react-icons/fi';

function RiskBadge({ level }) {
  const config = {
    LOW: { className: 'risk-low', icon: <FiCheckCircle /> },
    MEDIUM: { className: 'risk-medium', icon: <FiAlertTriangle /> },
    HIGH: { className: 'risk-high', icon: <FiXCircle /> },
  };
  const { className, icon } = config[level] || config.LOW;
  return (
    <span className={`risk-badge ${className}`}>
      {icon} {level}
    </span>
  );
}

function LoanAssessmentResult({ result }) {
  if (!result) return null;

  const { approved, reasons, riskScore, riskLevel, monthlyPayment, debtToIncomeRatio } = result;

  return (
    <div className={`assessment-result ${approved ? 'approved' : 'denied'}`}>
      <div className="assessment-header">
        <div className={`assessment-status-icon ${approved ? 'status-approved' : 'status-denied'}`}>
          {approved ? <FiCheckCircle size={32} /> : <FiXCircle size={32} />}
        </div>
        <div>
          <h3>{approved ? 'Loan Approved' : 'Loan Denied'}</h3>
          <p className="assessment-subtitle">
            {approved
              ? 'This loan request meets the bank requirements.'
              : 'This loan request does not meet the bank requirements.'}
          </p>
        </div>
      </div>

      <div className="assessment-metrics">
        <div className="metric-card">
          <div className="metric-label">Risk Level</div>
          <RiskBadge level={riskLevel} />
        </div>
        <div className="metric-card">
          <div className="metric-label">Risk Score</div>
          <div className="metric-value">{riskScore?.toFixed(1)}</div>
        </div>
        <div className="metric-card">
          <div className="metric-label">Monthly Payment</div>
          <div className="metric-value">{monthlyPayment?.toFixed(2)} RSD</div>
        </div>
        <div className="metric-card">
          <div className="metric-label">DTI Ratio</div>
          <div className="metric-value">{(debtToIncomeRatio * 100)?.toFixed(1)}%</div>
        </div>
      </div>

      {reasons && reasons.length > 0 && (
        <div className="assessment-reasons">
          <div className="reasons-header">
            <FiInfo className="icon" />
            <span>Assessment Details</span>
          </div>
          <ul>
            {reasons.map((reason, index) => (
              <li key={index} className={approved ? 'reason-positive' : 'reason-negative'}>
                {reason}
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}

export default LoanAssessmentResult;
