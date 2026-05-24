import { useState, useEffect } from 'react';
import { request, authHeader } from '../api';
import '../styles/rating-modal.css';

function RatingModal({ isOpen, book, username, token, onClose, onSuccess }) {
  const [score, setScore] = useState(5);
  const [hoverScore, setHoverScore] = useState(0);
  const [comment, setComment] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [message, setMessage] = useState(null);

  useEffect(() => {
    if (isOpen) {
      document.body.style.overflow = 'hidden';
      setScore(5);
      setComment('');
      setError(null);
      setMessage(null);
      setHoverScore(0);
    } else {
      document.body.style.overflow = 'auto';
    }

    return () => {
      document.body.style.overflow = 'auto';
    };
  }, [isOpen]);

  async function handleSubmit(e) {
    e.preventDefault();

    if (!username || !token) {
      setError('You must be logged in to submit a review.');
      return;
    }

    setLoading(true);
    setError(null);
    setMessage(null);

    try {
      await request(`/books/${book.id}/ratings`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          ...authHeader(token),
        },
        body: JSON.stringify({
          username,
          score,
          comment,
        }),
      });
      setMessage('Your review has been submitted successfully!');
      setScore(5);
      setComment('');
      setTimeout(() => {
        onSuccess();
        onClose();
      }, 1500);
    } catch (err) {
      setError(err.message || 'Failed to submit your review.');
    } finally {
      setLoading(false);
    }
  }

  const displayScore = hoverScore || score;

  if (!isOpen || !book) return null;

  return (
    <div className="rating-modal-overlay">
      <div className="rating-modal-container">
        <button className="modal-close-btn" onClick={onClose} title="Close">
          ✕
        </button>

        <div className="modal-header">
          <h2>Leave a Review</h2>
        </div>

        <div className="modal-book-preview">
          <img
            src={book.imageUrl || 'https://via.placeholder.com/80x120?text=No+Cover'}
            alt={book.title}
            className="modal-book-image"
            onError={(e) => { e.currentTarget.src = 'https://via.placeholder.com/80x120?text=No+Cover'; }}
          />
          <div className="modal-book-info">
            <p className="preview-title">{book.title}</p>
            <p className="preview-author">{book.author}</p>
            <p className="preview-genre">{book.genre}</p>
          </div>
        </div>

        <form onSubmit={handleSubmit} className="rating-form">
          <div className="form-group">
            <label>Your Rating</label>
            <div className="interactive-stars" 
              onMouseLeave={() => setHoverScore(0)}>
              {[1, 2, 3, 4, 5].map((star) => (
                <button
                  key={star}
                  type="button"
                  className={`star-button ${star <= displayScore ? 'active' : ''}`}
                  onMouseEnter={() => setHoverScore(star)}
                  onClick={() => setScore(star)}
                >
                  ★
                </button>
              ))}
            </div>
          </div>

          <div className="form-group">
            <label htmlFor="rating-comment">Comment</label>
            <textarea
              id="rating-comment"
              className="rating-textarea"
              rows="5"
              value={comment}
              onChange={(e) => setComment(e.target.value)}
              placeholder="What do you think about this book? Share your thoughts..."
              maxLength="500"
              disabled={loading}
            />
            <div className="char-count">
              {comment.length}/500
            </div>
          </div>

          {error && (
            <div className="form-error">
              <span className="error-icon">⚠️</span>
              {error}
            </div>
          )}

          {message && (
            <div className="form-success">
              <span className="success-icon">✓</span>
              {message}
            </div>
          )}

          <div className="form-actions">
            <button
              type="button"
              className="btn btn-outline"
              onClick={onClose}
              disabled={loading}
            >
              Cancel
            </button>
            <button
              type="submit"
              className="btn btn-primary"
              disabled={loading}
            >
              {loading ? 'Sending...' : 'Submit Review'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default RatingModal;
