import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { request } from '../api';

function Login({ onLogin }) {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState(null);
  const navigate = useNavigate();

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError(null);
    try {
      const response = await request('/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password }),
      });
      onLogin(response.token, response.username);
      navigate('/books');
    } catch (err) {
      setError(err.message);
    }
  };

  return (
    <div className="card auth-card">
      <h2>Welcome Back</h2>
      <p className="card-subtitle">Sign in to continue shopping and save your favorites.</p>
      <form onSubmit={handleSubmit}>
        <div className="form-row">
          <label>Username</label>
          <input value={username} onChange={(e) => setUsername(e.target.value)} required />
        </div>
        <div className="form-row">
          <label>Password</label>
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
        </div>
        <div className="form-actions">
          <button type="submit" className="btn btn-primary">Sign in</button>
        </div>
        {error && <div className="error">{error}</div>}
      </form>
    </div>
  );
}

export default Login;
