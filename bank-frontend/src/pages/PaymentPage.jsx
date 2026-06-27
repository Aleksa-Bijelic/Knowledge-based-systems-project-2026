import { useEffect, useState } from 'react';
import { FiSend, FiRefreshCw, FiCheckCircle, FiAlertTriangle, FiXCircle } from 'react-icons/fi';
import { request, authHeader } from '../api';

function PaymentPage({ token }) {
  const [packages, setPackages] = useState([]);
  const [selectedPkgId, setSelectedPkgId] = useState('');
  const [receiverAccountNumber, setReceiverAccountNumber] = useState('');
  const [amount, setAmount] = useState('');
  const [description, setDescription] = useState('');
  const [latitude, setLatitude] = useState('');
  const [longitude, setLongitude] = useState('');
  const [city, setCity] = useState('');
  const [country, setCountry] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);
  const [detectingLocation, setDetectingLocation] = useState(false);
  const [clientIp, setClientIp] = useState('');

  // Auto-detect public IP and location on mount
  useEffect(() => {
    setDetectingLocation(true);
    // Detect public IP first
    fetch('https://api.ipify.org?format=json')
      .then(r => r.json())
      .then(data => { if (data.ip) setClientIp(data.ip); })
      .catch(() => {});

    // Try browser geolocation first for precise lat/lon
    if ('geolocation' in navigator) {
      navigator.geolocation.getCurrentPosition(
        (pos) => {
          setLatitude(pos.coords.latitude.toFixed(4));
          setLongitude(pos.coords.longitude.toFixed(4));
          setDetectingLocation(false);
          // Reverse geocode using free API
          fetch(`https://api.bigdatacloud.net/data/reverse-geocode-client?latitude=${pos.coords.latitude}&longitude=${pos.coords.longitude}&localityLanguage=en`)
            .then(r => r.json())
            .then(data => {
              if (data.city) setCity(data.city);
              if (data.countryName) setCountry(data.countryName);
            })
            .catch(() => {});
        },
        () => {
          setDetectingLocation(false);
          // Geolocation denied/failed - try IP-based geolocation
          fetch('https://ipapi.co/json/')
            .then(r => r.json())
            .then(data => {
              if (data.city) setCity(data.city);
              if (data.country_name) setCountry(data.country_name);
              if (data.latitude) setLatitude(data.latitude.toString());
              if (data.longitude) setLongitude(data.longitude.toString());
            })
            .catch(() => {});
        },
        { timeout: 5000 }
      );
    } else {
      // No geolocation API - try IP-based
      fetch('https://ipapi.co/json/')
        .then(r => r.json())
        .then(data => {
          if (data.city) setCity(data.city);
          if (data.country_name) setCountry(data.country_name);
          if (data.latitude) setLatitude(data.latitude.toString());
          if (data.longitude) setLongitude(data.longitude.toString());
        })
        .catch(() => setDetectingLocation(false));
    }
  }, []);

  useEffect(() => {
    request('/package-accounts', {
      headers: { ...authHeader(token) },
    }).then((data) => {
      const list = Array.isArray(data) ? data : [];
      setPackages(list);
      if (list.length > 0 && !selectedPkgId) {
        setSelectedPkgId(list[0].id.toString());
      }
    }).catch(() => {});
  }, [token]);

  const selectedPkg = packages.find((p) => p.id.toString() === selectedPkgId);

  const handleSubmit = async (event) => {
    event.preventDefault();
    setSubmitting(true);
    setResult(null);
    setError(null);

    try {
      const payload = {
        senderAccountNumber: selectedPkg?.account?.accountNumber || '',
        receiverAccountNumber,
        amount: parseFloat(amount),
        cardNumber: selectedPkg?.card?.cardNumber || '',
        cardCvv: selectedPkg?.card?.cvv || '',
        cardExpirationDate: selectedPkg?.card?.expirationDate || '',
        cardholderName: selectedPkg?.card?.cardholderName || '',
        description: description || '',
        latitude: latitude ? parseFloat(latitude) : 0,
        longitude: longitude ? parseFloat(longitude) : 0,
        city: city || '',
        country: country || '',
        clientIp: clientIp || '',
      };

      const data = await request('/payments/process', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          ...authHeader(token),
        },
        body: JSON.stringify(payload),
      });

      setResult(data);
    } catch (err) {
      setError(err.message);
    } finally {
      setSubmitting(false);
    }
  };

  const resetForm = () => {
    setReceiverAccountNumber('');
    setAmount('');
    setDescription('');
    setLatitude('');
    setLongitude('');
    setCity('');
    setCountry('');
    setResult(null);
    setError(null);
  };

  return (
    <div className="dashboard-page">
      <section className="card">
        <div className="section-header">
          <div>
            <div className="section-eyebrow"><FiSend className="icon" />Payments</div>
            <h2>Make a payment</h2>
            <p className="card-subtitle">
              Send money from your package account. Fraud detection will automatically check each transaction.
            </p>
          </div>
        </div>

        <form className="package-form" onSubmit={handleSubmit}>
          <div className="form-row">
            <label>From (package account)</label>
            <select value={selectedPkgId} onChange={(e) => setSelectedPkgId(e.target.value)} required>
              {packages.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.name} — {p.account?.accountNumber} ({Number(p.account?.balance || 0).toFixed(2)} {p.account?.currency})
                </option>
              ))}
            </select>
          </div>

          <div className="form-row">
            <label>Receiver account number</label>
            <input
              value={receiverAccountNumber}
              onChange={(e) => setReceiverAccountNumber(e.target.value)}
              placeholder="e.g. RSD123456789"
              required
            />
          </div>

          <div className="form-grid">
            <div className="form-row">
              <label>Amount</label>
              <input
                type="number"
                min="0.01"
                step="0.01"
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
                placeholder="0.00"
                required
              />
            </div>
            <div className="form-row">
              <label>Description</label>
              <input
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="Optional note"
              />
            </div>
          </div>

          <fieldset className="location-fieldset">
            <legend>
              Transaction Location (auto-detected)
              {detectingLocation && <span className="detecting-location"> detecting...</span>}
            </legend>
            <p className="form-hint">
              Used by fraud detection. Auto-detected from your browser. Click "Re-detect" if incorrect.
            </p>
            <div className="form-grid">
              <div className="form-row">
                <label>Latitude</label>
                <input
                  type="number"
                  step="any"
                  value={latitude}
                  onChange={(e) => setLatitude(e.target.value)}
                  placeholder="e.g. 45.8150"
                />
              </div>
              <div className="form-row">
                <label>Longitude</label>
                <input
                  type="number"
                  step="any"
                  value={longitude}
                  onChange={(e) => setLongitude(e.target.value)}
                  placeholder="e.g. 15.9819"
                />
              </div>
            </div>

            <div className="form-grid">
              <div className="form-row">
                <label>City</label>
                <input
                  value={city}
                  onChange={(e) => setCity(e.target.value)}
                  placeholder="e.g. Zagreb"
                />
              </div>
              <div className="form-row">
                <label>Country</label>
                <input
                  value={country}
                  onChange={(e) => setCountry(e.target.value)}
                  placeholder="e.g. Croatia"
                />
              </div>
            </div>
          </fieldset>

          <div className="form-actions">
            <button type="button" className="btn btn-secondary" onClick={resetForm}>
              <FiRefreshCw className="icon" />Reset
            </button>
            <button type="submit" className="btn btn-primary" disabled={submitting}>
              <FiSend className="icon" />
              {submitting ? 'Processing...' : 'Process payment'}
            </button>
          </div>
        </form>

        {error && <div className="error">{error}</div>}

        {result && (
          <div className={`payment-result ${result.success ? (result.suspicious ? 'result-suspicious' : 'result-success') : 'result-fail'}`}>
            <div className="payment-result-header">
              {result.success ? (
                result.suspicious ? (
                  <><FiAlertTriangle className="icon" />Suspicious Transaction</>
                ) : (
                  <><FiCheckCircle className="icon" />Payment Successful</>
                )
              ) : (
                <><FiXCircle className="icon" />Payment Failed</>
              )}
            </div>
            <p className="payment-result-message">{result.message}</p>
            {result.suspicious && result.fraudReason && (
              <p className="payment-result-reason">{result.fraudReason}</p>
            )}
            {result.transactionId && (
              <p className="payment-result-id">Transaction ID: #{result.transactionId}</p>
            )}
          </div>
        )}
      </section>
    </div>
  );
}

export default PaymentPage;
