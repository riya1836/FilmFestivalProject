import React, { useState, useEffect, useContext } from 'react';
import { AuthContext } from '../../contexts/AuthContext';
import './dashboards.css';

export const UserDashboard = () => {
  const { user, logout, token } = useContext(AuthContext);
  const [films, setFilms] = useState([]);
  const [bookings, setBookings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState('films');

  useEffect(() => {
    loadDashboardData();
  }, [token]);

  const loadDashboardData = async () => {
    setLoading(true);
    try {
      // Get all films
      const filmsRes = await fetch('http://localhost:8080/api/films', {
        headers: { 'Authorization': `Bearer ${token}` },
      });
      if (filmsRes.ok) {
        setFilms(await filmsRes.json());
      }

      // Get my bookings
      const bookingsRes = await fetch('http://localhost:8080/api/user/my-bookings', {
        headers: { 'Authorization': `Bearer ${token}` },
      });
      if (bookingsRes.ok) {
        setBookings(await bookingsRes.json());
      }
    } catch (error) {
      console.error('Failed to load dashboard data:', error);
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return <div className="dashboard-container">Loading...</div>;
  }

  return (
    <div className="dashboard-page">
      <nav className="dashboard-navbar">
        <div className="navbar-brand">🎬 Film Festival</div>
        <div className="navbar-user">
          <span>{user?.name}</span>
          <button onClick={logout} className="logout-btn">Logout</button>
        </div>
      </nav>

      <div className="dashboard-container">
        <div className="dashboard-tabs">
          <button 
            className={`tab-btn ${activeTab === 'films' ? 'active' : ''}`}
            onClick={() => setActiveTab('films')}
          >
            Browse Films ({films.length})
          </button>
          <button 
            className={`tab-btn ${activeTab === 'bookings' ? 'active' : ''}`}
            onClick={() => setActiveTab('bookings')}
          >
            My Bookings ({bookings.length})
          </button>
          <button 
            className={`tab-btn ${activeTab === 'leaderboard' ? 'active' : ''}`}
            onClick={() => setActiveTab('leaderboard')}
          >
            Leaderboard
          </button>
        </div>

        {activeTab === 'films' && (
          <div className="dashboard-content">
            <h2>Browse Films</h2>
            
            {films.length === 0 ? (
              <p className="no-data">No films available.</p>
            ) : (
              <div className="films-grid">
                {films.map((film) => (
                  <div key={film.film_id} className="film-card-compact">
                    <h3>{film.title}</h3>
                    <p><strong>Genre:</strong> {film.genre}</p>
                    <p><strong>Language:</strong> {film.language}</p>
                    <p><strong>Runtime:</strong> {film.runtime} mins</p>
                    {film.avg_score && (
                      <p className="rating">
                        <span className="stars">★</span> {film.avg_score.toFixed(1)}/10
                        <span className="eval-count">({film.evaluation_count} reviews)</span>
                      </p>
                    )}
                    <button className="book-btn">Book Ticket</button>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {activeTab === 'bookings' && (
          <div className="dashboard-content">
            <h2>My Bookings</h2>
            
            {bookings.length === 0 ? (
              <p className="no-data">No bookings yet.</p>
            ) : (
              <div className="table-container">
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Film Title</th>
                      <th>Screening Date</th>
                      <th>Seats</th>
                      <th>Total Price</th>
                    </tr>
                  </thead>
                  <tbody>
                    {bookings.map((booking) => (
                      <tr key={booking.ticket_id}>
                        <td>{booking.film_title}</td>
                        <td>{new Date(booking.screening_date).toLocaleDateString()}</td>
                        <td>{booking.seats_count}</td>
                        <td>₹{booking.total_price.toFixed(2)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        )}

        {activeTab === 'leaderboard' && (
          <div className="dashboard-content">
            <h2>Film Leaderboard</h2>
            <p className="subtitle">Top-rated films of the festival</p>
            
            {films.filter(f => f.avg_score).length === 0 ? (
              <p className="no-data">No rated films yet.</p>
            ) : (
              <div className="leaderboard">
                {films
                  .filter(f => f.avg_score)
                  .sort((a, b) => b.avg_score - a.avg_score)
                  .slice(0, 10)
                  .map((film, index) => (
                    <div key={film.film_id} className="leaderboard-item">
                      <div className="rank">#{index + 1}</div>
                      <div className="film-details">
                        <h4>{film.title}</h4>
                        <p>{film.genre} • {film.language}</p>
                      </div>
                      <div className="film-score">
                        <span className="score">{film.avg_score.toFixed(1)}</span>
                        <span className="max">/10</span>
                      </div>
                    </div>
                  ))}
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
};
