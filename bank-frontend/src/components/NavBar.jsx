import { Link } from 'react-router-dom';
import { FiLogOut, FiUserPlus, FiCreditCard, FiFileText } from 'react-icons/fi';

function NavBar({ username, role, onLogout }) {
  const isOfficer = role === 'ROLE_OFFICER';

  return (
    <nav className="topbar">
      <div className="brand">Ocean Bank</div>
      <div className="tabs">
        {username && <Link to="/dashboard" className="tab">Dashboard</Link>}
        {username && isOfficer && (
          <Link to="/dashboard" className="tab">
            <FiFileText className="icon" />
            Loans
          </Link>
        )}
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
        {username && (
          <div className="user">
            <FiCreditCard className="icon" />
            Signed in as {username}
            {isOfficer && <span className="role-badge">Officer</span>}
          </div>
        )}
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
