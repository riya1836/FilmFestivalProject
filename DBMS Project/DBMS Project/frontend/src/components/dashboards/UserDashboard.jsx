import React, { useState, useEffect, useContext } from 'react';
import { AuthContext } from '../../contexts/AuthContext';
import './dashboards.css';

export const UserDashboard = () => {
  const { user, token } = useContext(AuthContext);
  const [films, setFilms] = useState([]);
  const [bookings, setBookings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState('films');
  
  // Booking Modal State
  const [showBookingModal, setShowBookingModal] = useState(false);
  const [selectedFilm, setSelectedFilm] = useState(null);
  const [screenings, setScreenings] = useState([]);
  const [selectedScreening, setSelectedScreening] = useState('');
  const [seatNumber, setSeatNumber] = useState('');
  const [bookingStatus, setBookingStatus] = useState({ loading: false, error: '', success: false });

  useEffect(() => {
    loadDashboardData();
  }, [token]);

  const loadDashboardData = async () => {
    setLoading(true);
    try {
      const filmsRes = await fetch('http://localhost:8080/api/films', {
        headers: { 'Authorization': `Bearer ${token}` },
      });
      if (filmsRes.ok) setFilms(await filmsRes.json());

      const bookingsRes = await fetch('http://localhost:8080/api/user/my-bookings', {
        headers: { 'Authorization': `Bearer ${token}` },
      });
      if (bookingsRes.ok) setBookings(await bookingsRes.json());
    } catch (error) {
      console.error('Failed to load dashboard data:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleOpenBooking = async (film) => {
    setSelectedFilm(film);
    setShowBookingModal(true);
    setBookingStatus({ loading: true, error: '', success: false });
    try {
      const res = await fetch(`http://localhost:8080/api/screenings?film_id=${film.film_id}`, {
        headers: { 'Authorization': `Bearer ${token}` },
      });
      if (res.ok) {
        const data = await res.json();
        setScreenings(data);
      }
    } catch (err) {
      setBookingStatus(prev => ({ ...prev, error: 'Failed to load screenings' }));
    } finally {
      setBookingStatus(prev => ({ ...prev, loading: false }));
    }
  };

  const handleBookTicket = async (e) => {
    e.preventDefault();
    if (!selectedScreening || !seatNumber) return;

    setBookingStatus({ loading: true, error: '', success: false });
    try {
      const res = await fetch('http://localhost:8080/api/user/book-ticket', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify({
          screening_id: selectedScreening,
          seat_number: seatNumber
        })
      });

      if (res.ok) {
        setBookingStatus({ loading: false, error: '', success: true });
        loadDashboardData(); // Refresh bookings
        setTimeout(() => {
          setShowBookingModal(false);
          setSelectedScreening('');
          setSeatNumber('');
        }, 2000);
      } else {
        const err = await res.json();
        setBookingStatus({ loading: false, error: err.error || 'Booking failed', success: false });
      }
    } catch (err) {
      setBookingStatus({ loading: false, error: 'Network error', success: false });
    }
  };

  if (loading) {
    return (
      <div className="dashboard-container">
        <div className="text-center py-5">
          <div className="spinner-border text-primary" role="status"></div>
          <p className="mt-2">Loading your festival experience...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="dashboard-page">
      <div className="dashboard-container">
        <div className="dashboard-tabs">
          <button 
            className={`tab-btn ${activeTab === 'films' ? 'active' : ''}`}
            onClick={() => setActiveTab('films')}
          >
            <i className="fas fa-film me-2"></i>Browse Films ({films.length})
          </button>
          <button 
            className={`tab-btn ${activeTab === 'bookings' ? 'active' : ''}`}
            onClick={() => setActiveTab('bookings')}
          >
            <i className="fas fa-ticket-alt me-2"></i>My Bookings ({bookings.length})
          </button>
          <button 
            className={`tab-btn ${activeTab === 'leaderboard' ? 'active' : ''}`}
            onClick={() => setActiveTab('leaderboard')}
          >
            <i className="fas fa-trophy me-2"></i>Leaderboard
          </button>
        </div>

        {activeTab === 'films' && (
          <div className="dashboard-content fade-in">
            <div className="section-header">
              <h2>Available Films</h2>
              <p>Select a film to view screenings and book tickets</p>
            </div>
            
            {films.length === 0 ? (
              <div className="empty-state">
                <i className="fas fa-film fa-3x"></i>
                <p>No films are currently scheduled for the festival.</p>
              </div>
            ) : (
              <div className="films-grid">
                {films.map((film) => (
                  <div key={film.film_id} className="film-card-premium">
                    <div className="film-card-body">
                      <h3>{film.title}</h3>
                      <div className="film-meta">
                        <span><i className="fas fa-globe me-1"></i>{film.language}</span>
                        <span><i className="fas fa-clock me-1"></i>{film.runtime}m</span>
                        <span className="genre-tag">{film.genre}</span>
                      </div>
                      {film.avg_score ? (
                        <div className="film-rating">
                          <span className="stars">★★★★★</span>
                          <span className="score">{film.avg_score.toFixed(1)}</span>
                          <span className="count">({film.evaluation_count})</span>
                        </div>
                      ) : (
                        <div className="film-rating no-rating">Not yet rated</div>
                      )}
                      <button className="book-btn-premium" onClick={() => handleOpenBooking(film)}>
                        <i className="fas fa-calendar-plus me-2"></i>Book Ticket
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {activeTab === 'bookings' && (
          <div className="dashboard-content fade-in">
            <div className="section-header">
              <h2>My Tickets</h2>
              <p>All your upcoming festival bookings</p>
            </div>
            
            {bookings.length === 0 ? (
              <div className="empty-state">
                <i className="fas fa-ticket-alt fa-3x"></i>
                <p>You haven't booked any tickets yet. Browse films to get started!</p>
              </div>
            ) : (
              <div className="bookings-grid">
                {bookings.map((booking) => (
                  <div key={booking.ticket_id} className="ticket-item-premium">
                    <div className="ticket-left">
                      <div className="ticket-circle"></div>
                    </div>
                    <div className="ticket-main">
                      <h4>{booking.film_title}</h4>
                      <div className="ticket-details">
                        <div><i className="fas fa-calendar me-2"></i>{new Date(booking.screening_date).toLocaleDateString()}</div>
                        <div><i className="fas fa-chair me-2"></i>Seat: {booking.seat_number}</div>
                      </div>
                    </div>
                    <div className="ticket-right">
                      <div className="ticket-price">₹{booking.total_price.toFixed(0)}</div>
                      <div className="ticket-id">#{booking.ticket_id}</div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {/* Booking Modal */}
        {showBookingModal && (
          <div className="modal-overlay">
            <div className="modal-content-premium">
              <div className="modal-header">
                <h3>Book Ticket: {selectedFilm?.title}</h3>
                <button className="close-btn" onClick={() => setShowBookingModal(false)}>&times;</button>
              </div>
              <form onSubmit={handleBookTicket} className="booking-form">
                {bookingStatus.error && <div className="alert alert-danger">{bookingStatus.error}</div>}
                {bookingStatus.success && <div className="alert alert-success">Booking Successful! Redirecting...</div>}
                
                <div className="form-group">
                  <label>Select Screening</label>
                  {bookingStatus.loading && !screenings.length ? (
                    <p>Loading screenings...</p>
                  ) : screenings.length === 0 ? (
                    <p className="text-danger">No screenings available for this film.</p>
                  ) : (
                    <div className="screening-selector">
                      {screenings.map(s => (
                        <div 
                          key={s.screening_id} 
                          className={`screening-opt ${selectedScreening === s.screening_id ? 'active' : ''}`}
                          onClick={() => setSelectedScreening(s.screening_id)}
                        >
                          <div className="date">{new Date(s.screening_date).toLocaleDateString('en-GB', { day: 'numeric', month: 'short' })}</div>
                          <div className="time">{s.start_time.substring(0, 5)}</div>
                          <div className="venue">{s.venue_name}</div>
                          <div className="price">₹{s.ticket_price}</div>
                        </div>
                      ))}
                    </div>
                  )}
                </div>

                <div className="form-group">
                  <label>Enter Seat Number (e.g. A1, B5)</label>
                  <input 
                    type="text" 
                    value={seatNumber} 
                    onChange={(e) => setSeatNumber(e.target.value.toUpperCase())}
                    placeholder="A1"
                    maxLength="3"
                    required
                  />
                </div>

                <div className="modal-footer">
                  <button type="button" className="btn-secondary" onClick={() => setShowBookingModal(false)}>Cancel</button>
                  <button 
                    type="submit" 
                    className="btn-primary" 
                    disabled={!selectedScreening || !seatNumber || bookingStatus.loading || bookingStatus.success}
                  >
                    {bookingStatus.loading ? 'Processing...' : 'Confirm Booking'}
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}

        {activeTab === 'leaderboard' && (
          <div className="dashboard-content fade-in">
             <div className="section-header">
              <h2>Film Leaderboard</h2>
              <p>The highest rated films by our jury members</p>
            </div>
            
            {films.filter(f => f.avg_score).length === 0 ? (
              <div className="empty-state">
                <i className="fas fa-trophy fa-3x"></i>
                <p>No ratings have been submitted yet. Come back soon!</p>
              </div>
            ) : (
              <div className="leaderboard-premium">
                {films
                  .filter(f => f.avg_score)
                  .sort((a, b) => b.avg_score - a.avg_score)
                  .slice(0, 10)
                  .map((film, index) => (
                    <div key={film.film_id} className="leaderboard-card">
                      <div className="rank">#{index + 1}</div>
                      <div className="film-info">
                        <h4>{film.title}</h4>
                        <p>{film.genre} • {film.language}</p>
                      </div>
                      <div className="score">
                        <span className="value">{film.avg_score.toFixed(1)}</span>
                        <span className="total">/10</span>
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
