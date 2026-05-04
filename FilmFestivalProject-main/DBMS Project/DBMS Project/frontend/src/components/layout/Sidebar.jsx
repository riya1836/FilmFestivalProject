import { useState, useContext } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { AuthContext } from '../../contexts/AuthContext';
import './Sidebar.css';

export const Sidebar = () => {
  const [isOpen, setIsOpen] = useState(false);
  const location = useLocation();
  const navigate = useNavigate();
  const { user, logout } = useContext(AuthContext);

  const adminItems = [
    { path: '/admin-dashboard', icon: '📊', label: 'Admin Panel' },
    { path: '/films', icon: '🎬', label: 'Films' },
    { path: '/venues', icon: '🎪', label: 'Venues' },
    { path: '/screenings', icon: '📽️', label: 'Screenings' },
    { path: '/attendees', icon: '👥', label: 'Attendees' },
    { path: '/filmcrew', icon: '🎭', label: 'Film Crew' },
    { path: '/awards', icon: '🏆', label: 'Awards' },
  ];

  const userItems = [
    { path: '/user-dashboard', icon: '📱', label: 'My Dashboard' },
    { path: '/films', icon: '🎬', label: 'Browse Films' },
  ];

  const juryItems = [
    { path: '/jury-dashboard', icon: '⚖️', label: 'Jury Panel' },
    { path: '/films', icon: '🎬', label: 'Films' },
  ];

  const getNavItems = () => {
    if (!user) return [];
    if (user.role === 'ADMIN') return adminItems;
    if (user.role === 'JURY') return juryItems;
    return userItems;
  };

  const navItems = getNavItems();
  const isActive = (path) => location.pathname === path;

  return (
    <>
      <button 
        className="sidebar-toggle"
        onClick={() => setIsOpen(!isOpen)}
        aria-label="Toggle sidebar"
      >
        ☰
      </button>

      <aside className={`sidebar ${isOpen ? 'open' : ''}`}>
        <div className="sidebar-header">
          <div className="sidebar-logo">
            <span className="logo-icon">🎬</span>
            <span className="logo-text">FilmFest</span>
          </div>
        </div>

        <nav className="sidebar-nav">
          {navItems.map(item => (
            <Link
              key={item.path}
              to={item.path}
              className={`nav-item ${isActive(item.path) ? 'active' : ''}`}
              onClick={() => setIsOpen(false)}
            >
              <span className="nav-icon">{item.icon}</span>
              <span className="nav-label">{item.label}</span>
              {isActive(item.path) && <span className="nav-indicator"></span>}
            </Link>
          ))}
        </nav>

        <div className="sidebar-footer">
          <Link to="/settings" className="footer-item" onClick={() => setIsOpen(false)}>
            <span className="footer-icon">⚙️</span>
            <span>Settings</span>
          </Link>
          <div className="footer-item logout" onClick={() => { logout(); navigate('/login'); }}>
            <span className="footer-icon">🚪</span>
            <span>Logout</span>
          </div>
        </div>
      </aside>

      {isOpen && <div className="sidebar-backdrop" onClick={() => setIsOpen(false)} />}
    </>
  );
};
