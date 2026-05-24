import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { request, authHeader } from '../api';
import RatingModal from '../components/RatingModal';

function Books({ token, username }) {
  const [books, setBooks] = useState([]);
  const [error, setError] = useState(null);
  const [selectedBook, setSelectedBook] = useState(null);
  const navigate = useNavigate();

  async function loadBooks() {
    try {
      const data = await request('/books', {
        headers: {
          'Content-Type': 'application/json',
          ...authHeader(token),
        },
      });
      setBooks(data);
    } catch (err) {
      setError(err.message);
    }
  }

  useEffect(() => {
    loadBooks();
  }, [token]);

  function handleOpenRatingModal(book) {
    setSelectedBook(book);
  }

  function handleCloseRatingModal() {
    setSelectedBook(null);
  }

  async function handleRatingSuccess() {
    await loadBooks();
  }

  return (
    <div className="card books-card">
      <div className="books-header">
        <div>
          <h2>Books</h2>
          <p className="card-subtitle">Browse the available titles and see what you can order today.</p>
        </div>
        {username === 'admin' && (
          <button className="btn btn-primary" onClick={() => navigate('/admin/add-book')}>
            Add Book
          </button>
        )}
      </div>
      {error && <div className="error">{error}</div>}
      <div className="book-grid">
        {books.length === 0 && <p className="empty-state">No books are available yet.</p>}
        {books.map((book) => (
          <div key={book.id} className="book-card compact">
            <div className="book-card-image click-target" onClick={() => navigate(`/books/${book.id}`)}>
              <img
                src={book.imageUrl || 'https://via.placeholder.com/160x240?text=No+Cover'}
                alt={book.title}
                onError={(e) => { e.currentTarget.src = 'https://via.placeholder.com/160x240?text=No+Cover'; }}
              />
            </div>
            <div className="book-card-details">
              <h3 className="book-card-title" onClick={() => navigate(`/books/${book.id}`)}>{book.title}</h3>
              <span className="book-author">by {book.author}</span>
              <div className="price book-card-price">${book.price.toFixed(2)}</div>
            </div>
          </div>
        ))}
      </div>
      
      <RatingModal
        isOpen={selectedBook !== null}
        book={selectedBook}
        username={username}
        token={token}
        onClose={handleCloseRatingModal}
        onSuccess={handleRatingSuccess}
      />
    </div>
  );
}

export default Books;
