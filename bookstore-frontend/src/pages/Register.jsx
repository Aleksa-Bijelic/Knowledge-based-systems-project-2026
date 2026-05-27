import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { FiUserPlus } from 'react-icons/fi';
import { request } from '../api';
import '../styles/genre-selection.css';

const GENRES = [
  "Fantasy", "Science Fiction", "Mystery", "Thriller", "Crime", "Romance", 
  "Historical Fiction", "Contemporary Fiction", "Literary Fiction", "Young Adult", 
  "Children’s", "Horror", "Adventure", "Biography", "Memoir", "Self-Help", 
  "Personal Development", "Business", "Psychology", "Philosophy", 
  "Religion & Spirituality", "Health & Wellness", "Travel", "Cooking", 
  "Education", "Science", "History"
];

function Register() {
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [selectedGenres, setSelectedGenres] = useState([]);
  const [message, setMessage] = useState(null);
  const [error, setError] = useState(null);
  const navigate = useNavigate();

  const toggleGenre = (genre) => {
    setSelectedGenres(prev => 
      prev.includes(genre) ? prev.filter(g => g !== genre) : [...prev, genre]
    );
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError(null);
    setMessage(null);
    try {
      await request('/auth/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ 
          username, 
          email, 
          password,
          favoriteGenres: selectedGenres 
        }),
      });
      setMessage('Registration successful. Please login.');
      setTimeout(() => navigate('/login'), 1200);
    } catch (err) {
      setError(err.message);
    }
  };

  return (
    <div className="card auth-card">
      <h2>Create Account</h2>
      <p className="card-subtitle">Join the bookstore to rate books and place orders.</p>
      <form onSubmit={handleSubmit}>
        <div className="form-row">
          <label>Username</label>
          <input value={username} onChange={(e) => setUsername(e.target.value)} required />
        </div>
        <div className="form-row">
          <label>Email</label>
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
        </div>
        <div className="form-row">
          <label>Password</label>
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
        </div>

        <div className="form-row genre-selection">
          <label className="genre-selection-label">Favorite Genres (Optional)</label>
          <div className="genre-grid">
            {GENRES.map(genre => (
              <div 
                key={genre}
                className={`genre-chip ${selectedGenres.includes(genre) ? 'selected' : ''}`}
                onClick={() => toggleGenre(genre)}
              >
                {genre}
              </div>
            ))}
          </div>
        </div>

        <div className="form-actions">
          <button type="submit" className="btn btn-primary">
            <FiUserPlus className="icon" />
            Register
          </button>
        </div>
        {message && <div className="success">{message}</div>}
        {error && <div className="error">{error}</div>}
      </form>
    </div>
  );
}

export default Register;
