import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { request, authHeader } from '../api';
import RatingModal from '../components/RatingModal';

function Books({ token, username }) {
  const [books, setBooks] = useState([]);
  const [error, setError] = useState(null);
  const [selectedBook, setSelectedBook] = useState(null);
  const [genreFilter, setGenreFilter] = useState('');
  const [minRating, setMinRating] = useState('');
  const [minPrice, setMinPrice] = useState('');
  const [maxPrice, setMaxPrice] = useState('');
  const navigate = useNavigate();

  const availableGenres = Array.from(
    new Set(
      books.flatMap((book) => (book.genre ? book.genre.split(',').map((tag) => tag.trim()) : []))
    )
  ).sort();

  const filteredBooks = books.filter((book) => {
    const bookGenres = book.genre ? book.genre.split(',').map((tag) => tag.trim()) : [];
    const matchesGenre = !genreFilter || bookGenres.includes(genreFilter);
    const rating = Number(book.averageRating || 0);
    const price = Number(book.price || 0);
    const matchesRating = !minRating || rating >= Number(minRating);
    const matchesMinPrice = !minPrice || price >= Number(minPrice);
    const matchesMaxPrice = !maxPrice || price <= Number(maxPrice);
    return matchesGenre && matchesRating && matchesMinPrice && matchesMaxPrice;
  });

  function resetFilters() {
    setGenreFilter('');
    setMinRating('');
    setMinPrice('');
    setMaxPrice('');
  }

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
      <div className="book-filters">
        <div className="filter-grid">
          <div className="filter-row">
            <label>Genre</label>
            <select value={genreFilter} onChange={(e) => setGenreFilter(e.target.value)}>
              <option value="">All</option>
              {availableGenres.map((genre) => (
                <option key={genre} value={genre}>{genre}</option>
              ))}
            </select>
          </div>
          <div className="filter-row">
            <label>Rating</label>
            <select value={minRating} onChange={(e) => setMinRating(e.target.value)}>
              <option value="">Any</option>
              <option value="1">1+ ⭐</option>
              <option value="2">2+ ⭐</option>
              <option value="3">3+ ⭐</option>
              <option value="4">4+ ⭐</option>
              <option value="5">5 ⭐</option>
            </select>
          </div>
          <div className="filter-row price-range-row">
            <label>Price</label>
            <div className="price-inputs">
              <input
                type="number"
                min="0"
                value={minPrice}
                onChange={(e) => setMinPrice(e.target.value)}
                placeholder="Min"
              />
              <span>–</span>
              <input
                type="number"
                min="0"
                value={maxPrice}
                onChange={(e) => setMaxPrice(e.target.value)}
                placeholder="Max"
              />
            </div>
          </div>
          <div className="filter-actions">
            <button type="button" className="btn btn-outline small" onClick={resetFilters}>
              Clear
            </button>
          </div>
        </div>
      </div>
      <div className="book-grid">
        {books.length === 0 && <p className="empty-state">No books are available yet.</p>}
        {books.length > 0 && filteredBooks.length === 0 && (
          <p className="empty-state">No books match the selected filters.</p>
        )}
        {filteredBooks.map((book) => (
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
              <div className="price book-card-price">{book.price.toFixed(2)} RSD</div>
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
