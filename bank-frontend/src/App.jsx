import { useEffect, useState, useCallback } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import Login from './pages/Login';
import Register from './pages/Register';
import Dashboard from './pages/Dashboard';
import PaymentPage from './pages/PaymentPage';
import SuspiciousPage from './pages/SuspiciousPage';
import NavBar from './components/NavBar';
import { request, authHeader } from './api';

function App() {
  const [token, setToken] = useState(localStorage.getItem('bankAuthToken'));
  const [username, setUsername] = useState(localStorage.getItem('bankAuthUsername'));
  const [role, setRole] = useState(localStorage.getItem('bankAuthRole'));
  const [suspiciousCount, setSuspiciousCount] = useState(0);

  useEffect(() => {
    if (token) {
      localStorage.setItem('bankAuthToken', token);
    } else {
      localStorage.removeItem('bankAuthToken');
    }
    if (username) {
      localStorage.setItem('bankAuthUsername', username);
    } else {
      localStorage.removeItem('bankAuthUsername');
    }
    if (role) {
      localStorage.setItem('bankAuthRole', role);
    } else {
      localStorage.removeItem('bankAuthRole');
    }
  }, [token, username, role]);

  const fetchSuspiciousCount = useCallback(async () => {
    if (!token) return;
    try {
      const data = await request('/payments/suspicious', {
        headers: { ...authHeader(token) },
      });
      setSuspiciousCount(Array.isArray(data) ? data.length : 0);
    } catch {
      // ignore
    }
  }, [token]);

  useEffect(() => {
    fetchSuspiciousCount();
    const interval = setInterval(fetchSuspiciousCount, 15000);
    return () => clearInterval(interval);
  }, [fetchSuspiciousCount]);

  const handleLogin = (newToken, newUsername, newRole) => {
    setToken(newToken);
    setUsername(newUsername);
    setRole(newRole);
  };

  const handleLogout = () => {
    setToken(null);
    setUsername(null);
    setRole(null);
    setSuspiciousCount(0);
  };

  return (
    <BrowserRouter>
      <div className="app-shell">
        <NavBar username={username} role={role} onLogout={handleLogout} suspiciousCount={suspiciousCount} />
        <main className="page-container">
          <Routes>
            <Route
              path="/login"
              element={
                token
                  ? <Navigate to="/dashboard" replace />
                  : <Login onLogin={handleLogin} />
              }
            />
            <Route
              path="/register"
              element={
                token
                  ? <Navigate to="/dashboard" replace />
                  : <Register />
              }
            />
            <Route
              path="/dashboard"
              element={
                token
                  ? <Dashboard token={token} username={username} role={role} />
                  : <Navigate to="/login" replace />
              }
            />
            <Route
              path="/payments"
              element={
                token
                  ? <PaymentPage token={token} />
                  : <Navigate to="/login" replace />
              }
            />
            <Route
              path="/suspicious"
              element={
                token
                  ? <SuspiciousPage token={token} onCountChange={setSuspiciousCount} />
                  : <Navigate to="/login" replace />
              }
            />
            <Route path="/" element={<Navigate to={token ? '/dashboard' : '/login'} replace />} />
            <Route path="*" element={<Navigate to={token ? '/dashboard' : '/login'} replace />} />
          </Routes>
        </main>
      </div>
    </BrowserRouter>
  );
}

export default App;
