import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { FiBook } from 'react-icons/fi';
import { request, authHeader } from '../api';

function AddBook({ token }) {
  const [title, setTitle] = useState('');
  const [author, setAuthor] = useState('');
  const [genre, setGenre] = useState('');
  const [imageUrl, setImageUrl] = useState('');
  const [price, setPrice] = useState('');
  const [publishedDate, setPublishedDate] = useState('');
  const [message, setMessage] = useState(null);
  const [error, setError] = useState(null);
  const navigate = useNavigate();

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError(null);
    setMessage(null);

    if (!token) {
      setError('You must be logged in as admin to add books.');
      return;
    }

    const book = {
      title,
      author,
      genre,
      imageUrl: imageUrl || null,
      price: parseFloat(price),
      publishedDate: publishedDate || null,
      addedAt: new Date().toISOString().substring(0, 10)
    };

    try {
      await request('/books', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          ...authHeader(token),
        },
        body: JSON.stringify(book),
      });
      setMessage('Book added successfully.');
      setTitle('');
      setAuthor('');
      setGenre('');
      setImageUrl('');
      setPrice('');
      setPublishedDate('');
      setTimeout(() => navigate('/books'), 1200);
    } catch (err) {
      setError(err.message);
    }
  };

  return (
    <div className="card auth-card">
      <h2>Add New Book</h2>
      <p className="card-subtitle">Admin can add a new title to the bookstore catalog.</p>
      <form onSubmit={handleSubmit}>
        <div className="form-row">
          <label>Title</label>
          <input value={title} onChange={(e) => setTitle(e.target.value)} required />
        </div>
        <div className="form-row">
          <label>Author</label>
          <input value={author} onChange={(e) => setAuthor(e.target.value)} required />
        </div>
        <div className="form-row">
          <label>Genre</label>
          <input value={genre} onChange={(e) => setGenre(e.target.value)} required />
        </div>
        <div className="form-row">
          <label>Cover Image URL</label>
          <input type="url" value={imageUrl} onChange={(e) => setImageUrl(e.target.value)} placeholder="https://example.com/cover.jpg" />
        </div>
        {imageUrl && (
          <div className="form-row image-preview-row">
            <label>Preview</label>
            <div className="image-preview">
              <img src={imageUrl} alt="Cover preview" onError={(e) => { e.currentTarget.src = 'https://via.placeholder.com/160x240?text=No+Cover'; }} />
            </div>
          </div>
        )}
        <div className="form-row">
          <label>Price</label>
          <input type="number" step="0.01" value={price} onChange={(e) => setPrice(e.target.value)} required />
        </div>
        <div className="form-row">
          <label>Published Date</label>
          <input type="date" value={publishedDate} onChange={(e) => setPublishedDate(e.target.value)} />
        </div>
        <div className="form-actions">
          <button type="submit" className="btn btn-primary">
            <FiBook className="icon" />
            Add book
          </button>
        </div>
        {message && <div className="success">{message}</div>}
        {error && <div className="error">{error}</div>}
      </form>
    </div>
  );
}

export default AddBook;
