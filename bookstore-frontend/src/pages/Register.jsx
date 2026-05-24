import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { FiUserPlus } from 'react-icons/fi';
import { request } from '../api';

function Register() {
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [message, setMessage] = useState(null);
  const [error, setError] = useState(null);
  const navigate = useNavigate();

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError(null);
    setMessage(null);
    try {
      await request('/auth/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, email, password }),
      });
      setMessage('Registration successful. Please login.');
      setTimeout(() => navigate('/login'), 1200);
    } catch (err) {
      setError(err.message);
    }
  };

  return (
    <div className="card auth-card">
      <h2>Create Account</h2>
      <p className="card-subtitle">Join the bookstore to rate books and place orders.</p>
      <form onSubmit={handleSubmit}>
        <div className="form-row">
          <label>Username</label>
          <input value={username} onChange={(e) => setUsername(e.target.value)} required />
        </div>
        <div className="form-row">
          <label>Email</label>
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
        </div>
        <div className="form-row">
          <label>Password</label>
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
        </div>
        <div className="form-actions">
          <button type="submit" className="btn btn-primary">
            <FiUserPlus className="icon" />
            Register
          </button>
        </div>
        {message && <div className="success">{message}</div>}
        {error && <div className="error">{error}</div>}
      </form>
    </div>
  );
}

export default Register;
