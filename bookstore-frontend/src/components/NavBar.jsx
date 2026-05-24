import { Link } from 'react-router-dom';
import { FiLogOut, FiUserPlus } from 'react-icons/fi';

function NavBar({ username, onLogout }) {
  return (
    <nav className="topbar">
      <div className="brand">Olive Bookstore</div>
      <div className="tabs">
        <Link to="/books" className="tab">Books</Link>
      </div>
      <div className="actions">
        {!username && (
          <Link to="/login" className="btn btn-outline">Login</Link>
        )}
        {!username && (
          <Link to="/register" className="btn btn-primary">
            <FiUserPlus className="icon" />
            Register
          </Link>
        )}
        {username && <div className="user">Signed in as {username}</div>}
        {username && (
          <button className="btn btn-secondary logout-button" onClick={onLogout}>
            <FiLogOut className="icon" />
            Logout
          </button>
        )}
      </div>
    </nav>
  );
}

export default NavBar;
