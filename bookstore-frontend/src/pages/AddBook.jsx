import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { FiBook } from 'react-icons/fi';
import { request, authHeader } from '../api';

const DEFAULT_IMAGE_URL = 'https://www.klett-cotta.de/assets/default-image.jpg';

const PREDEFINED_GENRES = [
  'Mystery',
  'Thriller and Suspense',
  'Horror',
  'Science Fiction',
  'Fantasy',
  'High Fantasy',
  'Urban Fantasy',
  'Grimdark Fantasy',
  'Dystopian Fiction',
  "Romance",
  "Romantasy",
  "Historical Fiction",
  "Contemporary Fiction",
  "Literary Fiction",
  "Young Adult",
  "New Adult",
  "Xenofiction",
  "Children’s Fiction",
  "Graphic Novel",
  "Manga",
  "Short Story",
  "Novella",
  "Autobiography",
  "Memoir",
  "Biography",
  "Self-Help",
  "Parenting",
  "Food and Drink",
  "Photography",
  "History",
  "Business",
  "Humor",
  "True Crime",
  "Religion and Spirituality",
  "Philosophy",
  "Health and Fitness",
  "Science",
  "Technology",
  "Crafts and DIY",
  "Learning and Education",
  "Essays",
  "Gardening and Homesteading",
  "Music",
  "Children’s"
];

function AddBook({ token }) {
  const [title, setTitle] = useState('');
  const [author, setAuthor] = useState('');
  const [selectedGenres, setSelectedGenres] = useState([]);
  const [genreInput, setGenreInput] = useState('');
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
      genre: selectedGenres.join(', '),
      imageUrl: imageUrl.trim() || DEFAULT_IMAGE_URL,
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
      setSelectedGenres([]);
      setGenreInput('');
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
          <label>Genre tags</label>
          <div className="genre-input-row">
            <input
              list="genre-options"
              value={genreInput}
              onChange={(e) => setGenreInput(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === 'Enter') {
                  e.preventDefault();
                  const normalized = genreInput.trim();
                  if (normalized && !selectedGenres.includes(normalized)) {
                    setSelectedGenres([...selectedGenres, normalized]);
                    setGenreInput('');
                  }
                }
              }}
              placeholder="Type a genre or select one"
            />
            <button
              type="button"
              className="btn btn-secondary small"
              onClick={() => {
                const normalized = genreInput.trim();
                if (normalized && !selectedGenres.includes(normalized)) {
                  setSelectedGenres([...selectedGenres, normalized]);
                  setGenreInput('');
                }
              }}
            >
              Add
            </button>
          </div>
          <datalist id="genre-options">
            {PREDEFINED_GENRES.map((genreOption) => (
              <option key={genreOption} value={genreOption} />
            ))}
          </datalist>
          <div className="book-tags">
            {selectedGenres.map((tag) => (
              <span key={tag} className="book-tag">
                {tag}
                <button
                  type="button"
                  className="tag-remove"
                  onClick={() => setSelectedGenres(selectedGenres.filter((g) => g !== tag))}
                >
                  ×
                </button>
              </span>
            ))}
          </div>
          <input type="hidden" value={selectedGenres.join(', ')} />
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
