import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { request, authHeader } from '../api';
import RatingModal from '../components/RatingModal';

function BookDetail({ token, username, onAddToCart }) {
  const { bookId } = useParams();
  const [book, setBook] = useState(null);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(true);
  const [showRatingModal, setShowRatingModal] = useState(false);
  const [hasReviewed, setHasReviewed] = useState(false);
  const [successMessage, setSuccessMessage] = useState('');
  const navigate = useNavigate();

  useEffect(() => {
    async function fetchBook() {
      setLoading(true);
      setError(null);
      try {
        const data = await request(`/books/${bookId}`, {
          headers: {
            'Content-Type': 'application/json',
            ...authHeader(token),
          },
        });
        setBook(data);
        if (username && data.ratings) {
          setHasReviewed(data.ratings.some((rating) => rating.username === username));
        }
      } catch (err) {
        setError(err.message);
      } finally {
        setLoading(false);
      }
    }

    fetchBook();
  }, [bookId, token, username]);

  function handleOpenRatingModal() {
    setShowRatingModal(true);
  }

  function handleCloseRatingModal() {
    setShowRatingModal(false);
  }

  async function handleRatingSuccess() {
    setShowRatingModal(false);
    const data = await request(`/books/${bookId}`, {
      headers: {
        'Content-Type': 'application/json',
        ...authHeader(token),
      },
    });
    setBook(data);
    if (username && data.ratings) {
      setHasReviewed(data.ratings.some((rating) => rating.username === username));
    }
  }

  function handleAddToCart() {
    if (!username) {
      navigate('/login');
      return;
    }
    onAddToCart(book, 1);
    setSuccessMessage(`Added 1 copy to cart`);
    setTimeout(() => setSuccessMessage(''), 3000);
  }

  if (loading) {
    return <div className="card">Loading book details...</div>;
  }

  if (error) {
    return (
      <div className="card">
        <div className="error">{error}</div>
        <button className="btn btn-secondary" onClick={() => navigate('/books')}>
          Back to library
        </button>
      </div>
    );
  }

  return (
    <div className="card book-detail-card">
      <div className="detail-topbar">
        <div>
          <Link to="/books" className="btn btn-outline small">
            ← Back to books
          </Link>
        </div>
        <div className="detail-actions">
          {username && username !== 'admin' && (
            <button
              className="btn btn-primary"
              onClick={handleOpenRatingModal}
              disabled={hasReviewed}
            >
              {hasReviewed ? 'Already Reviewed' : 'Review'}
            </button>
          )}
          {username && username !== 'admin' && (
            <button
              className="btn btn-primary"
              onClick={handleAddToCart}
            >
              Add to Cart
            </button>
          )}
        </div>
      </div>
      {successMessage && (
        <div className="success-message">{successMessage}</div>
      )}
      
      <div className="book-detail-grid">
        <div className="book-detail-image">
          <img
            src={book.imageUrl || 'https://via.placeholder.com/240x360?text=No+Cover'}
            alt={book.title}
            onError={(e) => { e.currentTarget.src = 'https://via.placeholder.com/240x360?text=No+Cover'; }}
          />
        </div>

        <div className="book-detail-content">
          <h2>{book.title}</h2>
          <p className="book-detail-meta">by {book.author}</p>
          <div className="book-detail-tags">
            {book.genre ? book.genre.split(',').map((genreTag) => (
              <span key={genreTag.trim()} className="book-tag">{genreTag.trim()}</span>
            )) : null}
            <span className="book-tag">{book.publishedDate || 'Unknown'}</span>
            <span className="book-tag rating-pill">
              {book.ratingCount > 0 ? `${book.averageRating.toFixed(1)} ★ (${book.ratingCount})` : 'No ratings yet'}
            </span>
          </div>
          <div className="detail-price">{book.price.toFixed(2)} RSD</div>
          <div className="detail-meta-line">Added on: {book.addedAt || 'N/A'}</div>
          <p className="detail-description">
            This title is a great choice for readers who enjoy modern classics with rich storytelling and 
            evocative world building. Explore reviews below to see what others think.
          </p>
        </div>
      </div>

      <div className="reviews-section">
        <h3>Reader Reviews</h3>
        {book.ratings.length === 0 ? (
          <p className="empty-state">No reviews have been posted for this book yet.</p>
        ) : (
          <div className="review-list">
            {book.ratings.map((rating) => (
              <div key={rating.id} className="review-card">
                <div className="review-header">
                  <div>
                    <strong>{rating.username}</strong>
                    <div className="review-score">{rating.score} ★</div>
                  </div>
                  <div className="review-date">{new Date(rating.ratedAt).toLocaleString()}</div>
                </div>
                <p className="review-comment">{rating.comment || 'No comment provided.'}</p>
              </div>
            ))}
          </div>
        )}
      </div>

      <RatingModal
        isOpen={showRatingModal}
        book={book}
        username={username}
        token={token}
        onClose={handleCloseRatingModal}
        onSuccess={handleRatingSuccess}
      />
    </div>
  );
}

export default BookDetail;
