import { useState, useContext } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { AuthContext } from '../contexts/AuthContext';
import './Navbar.css';

function Navbar() {
  const [isSearchOpen, setIsSearchOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const navigate = useNavigate();
  const { user } = useContext(AuthContext);

  const handleSearch = (e) => {
    e.preventDefault();
    const q = searchQuery.trim();
    if (!user) {
      navigate('/login');
      return;
    }
    navigate('/films');
    try {
      window.dispatchEvent(new CustomEvent('app-search', { detail: q }));
    } catch (err) {
      const ev = document.createEvent('CustomEvent');
      ev.initCustomEvent('app-search', true, true, q);
      window.dispatchEvent(ev);
    }
  };

  return (
    <nav className="navbar">
      <div className="navbar-content">
        {/* Left Side - Logo (when sidebar is present) */}
        <div className="navbar-left">
          <div className="navbar-spacer"></div>
        </div>

        {/* Center - Search */}
        <div className="navbar-center">
          <form className="search-form" onSubmit={handleSearch}>
            <input
              type="text"
              className="search-input"
              placeholder="Search films, venues, screenings..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
            />
            <button type="submit" className="search-btn">
              🔍
            </button>
          </form>
        </div>

        {/* Right Side - User & Notifications */}
        <div className="navbar-right">
          <div className="navbar-icons">
            <button className="icon-btn" title="Notifications">
              <span className="icon-badge">🔔</span>
              <span className="notification-dot"></span>
            </button>

            {!user && (
              <Link to="/login" className="icon-btn" title="Login">🔐 Login</Link>
            )}
          </div>
        </div>
      </div>
    </nav>
  );
}

export default Navbar;