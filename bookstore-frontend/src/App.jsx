import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { useEffect, useState } from 'react';
import Login from './pages/Login';
import Register from './pages/Register';
import Books from './pages/Books';
import BookDetail from './pages/BookDetail';
import AddBook from './pages/AddBook';
import Cart from './pages/Cart';
import OrderDetail from './pages/OrderDetail';
import NavBar from './components/NavBar';

const getCartKey = (username) => (username && username !== 'admin' ? `cart_${username}` : null);
const loadCartForUser = (username) => {
  const key = getCartKey(username);
  if (!key) {
    return [];
  }
  return JSON.parse(localStorage.getItem(key) || '[]');
};

function App() {
  const [token, setToken] = useState(localStorage.getItem('authToken'));
  const [username, setUsername] = useState(localStorage.getItem('authUsername'));
  const [cart, setCart] = useState(loadCartForUser(localStorage.getItem('authUsername')));

  useEffect(() => {
    localStorage.removeItem('cart');
  }, []);

  useEffect(() => {
    localStorage.setItem('authToken', token || '');
    localStorage.setItem('authUsername', username || '');
    setCart(loadCartForUser(username));
  }, [token, username]);

  useEffect(() => {
    const key = getCartKey(username);
    if (key) {
      localStorage.setItem(key, JSON.stringify(cart));
    }
  }, [cart, username]);

  const handleLogout = () => {
    setToken(null);
    setUsername(null);
    setCart([]);
    localStorage.removeItem('authToken');
    localStorage.removeItem('authUsername');
  };

  const addToCart = (book, quantity) => {
    if (!username || username === 'admin') {
      return;
    }

    setCart((prevCart) => {
      const existingItem = prevCart.find((item) => item.book.id === book.id);
      if (existingItem) {
        return prevCart.map((item) =>
          item.book.id === book.id ? { ...item, quantity: item.quantity + quantity } : item
        );
      }
      return [...prevCart, { book, quantity }];
    });
  };

  const removeFromCart = (bookId) => {
    setCart((prevCart) => prevCart.filter((item) => item.book.id !== bookId));
  };

  const updateCartQuantity = (bookId, quantity) => {
    if (quantity <= 0) {
      removeFromCart(bookId);
    } else {
      setCart((prevCart) =>
        prevCart.map((item) =>
          item.book.id === bookId ? { ...item, quantity } : item
        )
      );
    }
  };

  const clearCart = () => {
    setCart([]);
  };

  return (
    <BrowserRouter>
      <div className="app-shell">
        <NavBar username={username} onLogout={handleLogout} cartCount={cart.length} />
        <main className="page-container">
          <Routes>
            <Route path="/login" element={<Login onLogin={(token, username) => { setToken(token); setUsername(username); }} />} />
            <Route path="/register" element={<Register />} />
            <Route path="/books/:bookId" element={<BookDetail token={token} username={username} onAddToCart={addToCart} />} />
            <Route path="/books" element={<Books token={token} username={username} />} />
            <Route path="/cart" element={<Cart cart={cart} onUpdateQuantity={updateCartQuantity} onRemoveFromCart={removeFromCart} onClearCart={clearCart} token={token} username={username} />} />
            <Route path="/orders/:orderId" element={<OrderDetail token={token} username={username} />} />
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
