import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { request, authHeader } from '../api';
import RatingModal from '../components/RatingModal';
import '../styles/book-card.css';

const DEFAULT_IMAGE_URL = 'https://www.klett-cotta.de/assets/default-image.jpg';

function Books({ token, username }) {
  const [books, setBooks] = useState([]);
  const [recommendations, setRecommendations] = useState([]);
  const [error, setError] = useState(null);
  const [recommendationsError, setRecommendationsError] = useState(null);
  const [selectedBook, setSelectedBook] = useState(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [genreFilter, setGenreFilter] = useState('');
  const [minRating, setMinRating] = useState('');
  const [minPrice, setMinPrice] = useState('');
  const [maxPrice, setMaxPrice] = useState('');
  const [loadingBooks, setLoadingBooks] = useState(true);
  const [loadingRecommendations, setLoadingRecommendations] = useState(true);
  const navigate = useNavigate();

  const isAdmin = username === 'admin';

  const availableGenres = Array.from(
    new Set(
      books.flatMap((book) => (book.genre ? book.genre.split(',').map((tag) => tag.trim()) : []))
    )
  ).sort();

  const recommendedBookIds = new Set(recommendations.map((book) => book.id));

  const filteredBooks = books.filter((book) => {
    const bookGenres = book.genre ? book.genre.split(',').map((tag) => tag.trim()) : [];
    const matchesGenre = !genreFilter || bookGenres.includes(genreFilter);
    const rating = Number(book.averageRating || 0);
    const price = Number(book.price || 0);
    const matchesRating = !minRating || rating >= Number(minRating);
    const matchesMinPrice = !minPrice || price >= Number(minPrice);
    const matchesMaxPrice = !maxPrice || price <= Number(maxPrice);
    
    // Search term matching for title and author
    const searchLower = searchTerm.toLowerCase();
    const matchesSearch = !searchTerm || 
      book.title.toLowerCase().includes(searchLower) ||
      book.author.toLowerCase().includes(searchLower);
    
    return matchesGenre && matchesRating && matchesMinPrice && matchesMaxPrice && matchesSearch;
  });

  function resetFilters() {
    setSearchTerm('');
    setGenreFilter('');
    setMinRating('');
    setMinPrice('');
    setMaxPrice('');
  }

  async function loadBooks() {
    try {
      setLoadingBooks(true);
      setError(null);
      const data = await request('/books', {
        headers: {
          'Content-Type': 'application/json',
          ...authHeader(token),
        },
      });
      setBooks(data);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoadingBooks(false);
    }
  }

  async function loadRecommendations() {
    try {
      setLoadingRecommendations(true);
      setRecommendationsError(null);
      const data = await request('/books/recommendations', {
        headers: {
          ...authHeader(token),
        },
      });
      setRecommendations(data);
    } catch (err) {
      setRecommendationsError(err.message);
    } finally {
      setLoadingRecommendations(false);
    }
  }

  useEffect(() => {
    loadBooks();
    loadRecommendations();
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
    <div className="books-page">
      <section className="card recommendations-card">
          <div className="section-header">
            <div>
              <div className="section-eyebrow">Recommended books</div>
              <h3>Books you may like</h3>
              <p className="card-subtitle">
                {username
                  ? 'Personalized recommendations based on your activity.'
                  : 'A quick selection of books currently recommended for guests.'}
              </p>
            </div>
          </div>

          {recommendationsError && (
            <div className="error inline-error">Could not load recommendations: {recommendationsError}</div>
          )}

          {loadingRecommendations && !recommendationsError && <div className="loading-copy">Loading recommendations...</div>}

          {!loadingRecommendations && !recommendationsError && recommendations.length === 0 && (
            <p className="empty-state">No recommendations are available right now.</p>
          )}

          {!loadingRecommendations && !recommendationsError && recommendations.length > 0 && (
            <div className="book-grid recommendation-books-grid">
              {recommendations.map((book) => (
                <div key={book.id} className="book-card book-card-recommended">
                  <div className="book-card-image" onClick={() => navigate(`/books/${book.id}`)}>
                    <div className="recommendation-badge">Recommended</div>
                    <div className="genre-badge-container">
                      {(book.genre || '').split(',').map((g, i) => (
                        <span key={i} className="genre-tag">{g.trim()}</span>
                      ))}
                    </div>
                    <img
                      src={book.imageUrl || DEFAULT_IMAGE_URL}
                      alt={book.title}
                      onError={(e) => { e.currentTarget.src = DEFAULT_IMAGE_URL; }}
                    />
                  </div>
                  <div className="book-card-details">
                    <h3 className="book-card-title" onClick={() => navigate(`/books/${book.id}`)}>{book.title}</h3>
                    <span className="book-author">{book.author}</span>
                    <div className="book-card-footer">
                      <div className="book-card-metadata">
                        <span className="rating-badge">{Number(book.averageRating || 0).toFixed(1)} ★</span>
                        <span>({book.ratingCount})</span>
                      </div>
                      <div className="book-card-price">{Number(book.price || 0).toFixed(0)} RSD</div>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </section>

      <section className="card books-card">
        <div className="books-header">
          <div>
            <div className="section-eyebrow">Library</div>
            <h3>Browse the catalog</h3>
            <p className="card-subtitle">Search across the full collection and narrow it down by genre, rating or price.</p>
          </div>
        </div>

        <div className="search-section">
          <div className="search-bar-container">
            <input
              type="text"
              className="search-bar"
              placeholder="Search by title or author..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />
            {searchTerm && (
              <button 
                className="search-clear-btn"
                onClick={() => setSearchTerm('')}
                title="Clear search"
              >
                ✕
              </button>
            )}
          </div>
        </div>

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
        {error && <div className="error inline-error">{error}</div>}
        {loadingBooks && !error && <div className="loading-copy">Loading books...</div>}
        {!loadingBooks && !error && (
          <div className="book-grid">
            {books.length === 0 && <p className="empty-state">No books are available yet.</p>}
            {books.length > 0 && filteredBooks.length === 0 && (
              <p className="empty-state">No books match the selected filters.</p>
            )}
            {filteredBooks.map((book) => (
              <div key={book.id} className={`book-card ${recommendedBookIds.has(book.id) ? 'book-card-recommended' : ''}`}>
                <div className="book-card-image" onClick={() => navigate(`/books/${book.id}`)}>
                  {recommendedBookIds.has(book.id) && <div className="recommendation-badge">Recommended</div>}
                  <div className="genre-badge-container">
                    {(book.genre || '').split(',').map((g, i) => (
                      <span key={i} className="genre-tag">{g.trim()}</span>
                    ))}
                  </div>
                  <img
                    src={book.imageUrl || DEFAULT_IMAGE_URL}
                    alt={book.title}
                    onError={(e) => { e.currentTarget.src = DEFAULT_IMAGE_URL; }}
                  />
                </div>
                <div className="book-card-details">
                  <h3 className="book-card-title" onClick={() => navigate(`/books/${book.id}`)}>{book.title}</h3>
                  <span className="book-author">{book.author}</span>
                  <div className="book-card-footer">
                    <div className="book-card-metadata">
                      <span className="rating-badge">{Number(book.averageRating || 0).toFixed(1)} ★</span>
                      <span>({book.ratingCount})</span>
                    </div>
                    <div className="book-card-price">{Number(book.price || 0).toFixed(0)} RSD</div>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </section>
      
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
