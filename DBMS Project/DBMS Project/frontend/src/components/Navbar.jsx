import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { isAuthenticated } from '../auth';
import './Navbar.css';

function Navbar() {
  const [isSearchOpen, setIsSearchOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');

  const handleSearch = (e) => {
    e.preventDefault();
    // Navigate to Films and broadcast a global search event so pages can react
    const q = searchQuery.trim();
    if (!isAuthenticated()) {
      window.location.href = '/login';
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

  const navigate = useNavigate();

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

            {!isAuthenticated() && (
              <a href="/login" className="icon-btn" title="Login">🔐 Login</a>
            )}
          </div>
        </div>
      </div>
    </nav>
  );
}

export default Navbar;