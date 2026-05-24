import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { useEffect, useState } from 'react';
import Login from './pages/Login';
import Register from './pages/Register';
import Books from './pages/Books';
import BookDetail from './pages/BookDetail';
import AddBook from './pages/AddBook';
import NavBar from './components/NavBar';

function App() {
  const [token, setToken] = useState(localStorage.getItem('authToken'));
  const [username, setUsername] = useState(localStorage.getItem('authUsername'));

  useEffect(() => {
    localStorage.setItem('authToken', token || '');
    localStorage.setItem('authUsername', username || '');
  }, [token, username]);

  const handleLogout = () => {
    setToken(null);
    setUsername(null);
    localStorage.removeItem('authToken');
    localStorage.removeItem('authUsername');
  };

  return (
    <BrowserRouter>
      <div className="app-shell">
        <NavBar username={username} onLogout={handleLogout} />
        <main className="page-container">
          <Routes>
            <Route path="/login" element={<Login onLogin={(token, username) => { setToken(token); setUsername(username); }} />} />
            <Route path="/register" element={<Register />} />
            <Route path="/books/:bookId" element={<BookDetail token={token} username={username} />} />
            <Route path="/books" element={<Books token={token} username={username} />} />
            <Route path="/admin/add-book" element={<AddBook token={token} />} />
            <Route path="/" element={<Navigate to="/books" replace />} />
            <Route path="*" element={<Navigate to="/books" replace />} />
          </Routes>
        </main>
      </div>
    </BrowserRouter>
  );
}

export default App;
