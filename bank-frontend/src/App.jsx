import { useEffect, useState } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import Login from './pages/Login';
import Register from './pages/Register';
import Dashboard from './pages/Dashboard';
import NavBar from './components/NavBar';

function App() {
  const [token, setToken] = useState(localStorage.getItem('bankAuthToken'));
  const [username, setUsername] = useState(localStorage.getItem('bankAuthUsername'));
  const [role, setRole] = useState(localStorage.getItem('bankAuthRole'));

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

  const handleLogin = (newToken, newUsername, newRole) => {
    setToken(newToken);
    setUsername(newUsername);
    setRole(newRole);
  };

  const handleLogout = () => {
    setToken(null);
    setUsername(null);
    setRole(null);
  };

  return (
    <BrowserRouter>
      <div className="app-shell">
        <NavBar username={username} role={role} onLogout={handleLogout} />
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
            <Route path="/" element={<Navigate to={token ? '/dashboard' : '/login'} replace />} />
            <Route path="*" element={<Navigate to={token ? '/dashboard' : '/login'} replace />} />
          </Routes>
        </main>
      </div>
    </BrowserRouter>
  );
}

export default App;
