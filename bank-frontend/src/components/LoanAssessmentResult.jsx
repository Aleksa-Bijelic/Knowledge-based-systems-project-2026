import { useState } from 'react';
import { FiCheckCircle, FiXCircle, FiAlertTriangle, FiInfo, FiThumbsUp, FiThumbsDown } from 'react-icons/fi';
import { request, authHeader } from '../api';

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

function LoanAssessmentResult({ result, token, onDecisionMade }) {
  const [decisionLoading, setDecisionLoading] = useState(false);
  const [decisionError, setDecisionError] = useState(null);
  const [finalDecision, setFinalDecision] = useState(null);

  if (!result) return null;

  const { requestId, clientId, approved, reasons, riskScore, riskLevel, monthlyPayment, debtToIncomeRatio } = result;

  const handleDecision = async (decision) => {
    setDecisionLoading(true);
    setDecisionError(null);
    try {
      await request('/loans/decision', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          ...authHeader(token),
        },
        body: JSON.stringify({
          requestId: requestId,
          officerDecision: decision,
          officerUsername: 'officer',
        }),
      });
      setFinalDecision(decision);
      if (onDecisionMade) onDecisionMade(decision);
    } catch (err) {
      setDecisionError(err.message);
    } finally {
      setDecisionLoading(false);
    }
  };

  return (
    <div className="assessment-result">
      {/* System Recommendation Banner */}
      <div className={`assessment-header ${approved ? 'recommended' : 'not-recommended'}`}>
        <div className={`assessment-status-icon ${approved ? 'status-approved' : 'status-denied'}`}>
          {approved ? <FiCheckCircle size={32} /> : <FiXCircle size={32} />}
        </div>
        <div>
          <h3>System Recommendation: {approved ? 'Approve' : 'Reject'}</h3>
          <p className="assessment-subtitle">
            {approved
              ? 'Based on the rule-based analysis, this loan request meets the bank criteria.'
              : 'Based on the rule-based analysis, this loan request does not meet the bank criteria.'}
          </p>
        </div>
      </div>

      {/* Financial Metrics */}
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

      {/* Assessment Reasons */}
      {reasons && reasons.length > 0 && (
        <div className="assessment-reasons">
          <div className="reasons-header">
            <FiInfo className="icon" />
            <span>Assessment Details</span>
          </div>
          <ul>
            {reasons.map((reason, index) => (
              <li key={index} className={reason.startsWith('Recommendation') ? 'reason-positive' : 'reason-negative'}>
                {reason}
              </li>
            ))}
          </ul>
        </div>
      )}

      {/* Officer Final Decision */}
      {!finalDecision && (
        <div className="officer-decision-section">
          <h4>Officer Final Decision</h4>
          <p className="card-subtitle">
            The rule-based system provides a recommendation above. As a bank officer, you make the final decision.
          </p>
          <div className="officer-decision-actions">
            <button
              className="btn btn-success"
              onClick={() => handleDecision('APPROVED')}
              disabled={decisionLoading}
            >
              <FiThumbsUp className="icon" />
              {decisionLoading ? 'Processing...' : 'Approve Loan'}
            </button>
            <button
              className="btn btn-danger"
              onClick={() => handleDecision('REJECTED')}
              disabled={decisionLoading}
            >
              <FiThumbsDown className="icon" />
              {decisionLoading ? 'Processing...' : 'Reject Loan'}
            </button>
          </div>
          {decisionError && <div className="error">{decisionError}</div>}
        </div>
      )}

      {finalDecision && (
        <div className={`final-decision-banner ${finalDecision === 'APPROVED' ? 'final-approved' : 'final-rejected'}`}>
          {finalDecision === 'APPROVED' ? <FiCheckCircle size={24} /> : <FiXCircle size={24} />}
          <span>
            <strong>Officer decision: {finalDecision === 'APPROVED' ? 'Loan Approved' : 'Loan Rejected'}</strong>
            {' — This final decision has been recorded.'}
          </span>
        </div>
      )}
    </div>
  );
}

export default LoanAssessmentResult;
