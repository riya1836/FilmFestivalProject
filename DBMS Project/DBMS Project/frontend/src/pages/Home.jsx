import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import './Home.css';
import { Card, CardBody, CardHeader, StatCard } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { getVenues, getFilms, getAttendees, getTickets, getScreenings } from '../api';

function Home() {
  const navigate = useNavigate();
  const [stats, setStats] = useState({
    films: 0,
    venues: 0,
    screenings: 0,
    ticketsSold: 0
  });
  const [loading, setLoading] = useState(true);
  const [screeningsList, setScreeningsList] = useState([]);

  useEffect(() => {
    loadDashboardData();
  }, []);

  const loadDashboardData = async () => {
    try {
      setLoading(true);
      const [venuesData, filmsData, attendeesData, ticketsData, screeningsData] = await Promise.all([
        getVenues(),
        getFilms(),
        getAttendees(),
        getTickets(),
        getScreenings()
      ]);

      setStats({
        films: filmsData.length,
        venues: venuesData.length,
        screenings: 24, // Placeholder
        ticketsSold: ticketsData.length
      });
      setScreeningsList(Array.isArray(screeningsData) ? screeningsData.map(s => ({ ...s, id: s.screening_id })) : []);
    } catch (error) {
      console.error('Failed to load dashboard data:', error);
    } finally {
      setLoading(false);
    }
  };

  const quickActions = [
    { icon: '🎬', label: 'Add Film', color: 'accent', action: () => navigate('/films') },
    { icon: '🎪', label: 'Add Venue', color: 'info', action: () => navigate('/venues') },
    { icon: '🎟️', label: 'Book Ticket', color: 'success', action: () => navigate('/tickets') },
    { icon: '👥', label: 'Add Attendee', color: 'warning', action: () => navigate('/attendees') },
  ];

  const recentActivities = [
    { icon: '🎬', text: 'New film "Inception" added to catalog', time: '2 hours ago' },
    { icon: '🎟️', text: '150 tickets sold for evening screening', time: '4 hours ago' },
    { icon: '👥', text: 'John Doe registered for the festival', time: '6 hours ago' },
    { icon: '🏆', text: 'Award "Best Director" assigned', time: '1 day ago' },
  ];

  return (
    <div className="page-home">
      <div className="page-header">
        <div className="page-header-content">
          <div className="home-header">
            <div>
              <h1 className="page-title">
                <span className="page-icon">🎬</span>
                Festival Dashboard
              </h1>
              <p className="page-subtitle">Welcome to Film Festival Management System</p>
            </div>
            <div className="header-time">
              <div className="time-display">
                {new Date().toLocaleDateString('en-US', { weekday: 'long', month: 'long', day: 'numeric' })}
              </div>
            </div>
          </div>
        </div>
      </div>

      <div className="page-content">
        {/* STATS SECTION */}
        <div className="page-section">
          <div className="stats-grid">
            <StatCard
              icon="🎬"
              label="Total Films"
              value={stats.films}
              trend={12}
              trendLabel="vs last month"
              color="accent"
            />
            <StatCard
              icon="🎪"
              label="Venues"
              value={stats.venues}
              trend={5}
              trendLabel="active"
              color="info"
            />
            <StatCard
              icon="📽️"
              label="Screenings"
              value={stats.screenings}
              trend={8}
              trendLabel="this week"
              color="success"
            />
            <StatCard
              icon="🎟️"
              label="Tickets Sold"
              value={stats.ticketsSold}
              trend={25}
              trendLabel="sold this week"
              color="warning"
            />
          </div>
        </div>

        {/* QUICK ACTIONS SECTION */}
        <div className="page-section">
          <h2 className="section-title">⚡ Quick Actions</h2>
          <div className="quick-actions-grid">
            {quickActions.map((action, idx) => (
              <button
                key={idx}
                className="quick-action-card"
                onClick={action.action}
              >
                <div className="action-icon">{action.icon}</div>
                <div className="action-label">{action.label}</div>
              </button>
            ))}
          </div>
        </div>

        {/* MAIN CONTENT GRID */}
        <div className="page-section">
          <div className="content-grid">
            {/* RECENT ACTIVITIES */}
            <div className="grid-col-2">
              <Card>
                <CardHeader>
                  <h3 className="card-title">📊 Recent Activities</h3>
                </CardHeader>
                <CardBody className="activity-list">
                  {recentActivities.map((activity, idx) => (
                    <div key={idx} className="activity-item">
                      <div className="activity-icon">{activity.icon}</div>
                      <div className="activity-content">
                        <p className="activity-text">{activity.text}</p>
                        <span className="activity-time">{activity.time}</span>
                      </div>
                    </div>
                  ))}
                </CardBody>
              </Card>
            </div>

            {/* FEATURED SECTION */}
            <div className="grid-col-1">
              <Card gradient neon>
                <CardHeader>
                  <h3 className="card-title">✨ Featured</h3>
                </CardHeader>
                <CardBody className="featured-content">
                  <div className="featured-item">
                    <div className="featured-icon">🌟</div>
                    <div className="featured-text">
                      <h4>Film Festival 2024</h4>
                      <p>Largest collection of international films</p>
                    </div>
                  </div>
                  <div className="featured-item">
                    <div className="featured-icon">🎭</div>
                    <div className="featured-text">
                      <h4>World Premiere</h4>
                      <p>Exclusive screening of upcoming blockbuster</p>
                    </div>
                  </div>
                  <Button 
                    variant="primary" 
                    className="explore-btn"
                    onClick={() => navigate('/films')}
                  >
                    Explore Films →
                  </Button>
                </CardBody>
              </Card>

              {/* Quick Stats removed as requested */}
            </div>
          </div>
        </div>

        {/* COMING SOON SECTION */}
        <div className="page-section">
          <Card>
            <CardHeader>
              <h3 className="card-title">🎬 Upcoming Screenings</h3>
            </CardHeader>
            <CardBody>
              <div className="screenings-list">
                {(() => {
                  const now = Date.now();
                  const upcoming = (screeningsList || [])
                    .map(s => ({ ...s, timeMs: s.screening_time ? Date.parse(s.screening_time) : 0 }))
                    .filter(s => s.timeMs && s.timeMs >= now)
                    .sort((a,b) => a.timeMs - b.timeMs)
                    .slice(0,6);

                  if (upcoming.length === 0) return <p className="text-muted">No upcoming screenings. Add some from the Screenings page.</p>;

                  return upcoming.map((s, idx) => (
                    <div key={s.id || idx} className="screening-item">
                      <div className="film-poster">📽️</div>
                      <div className="film-details">
                        <h4>Film #{s.film_id}</h4>
                        <p>{new Date(s.timeMs).toLocaleString()}</p>
                      </div>
                      <button className="book-btn" onClick={() => navigate('/screenings')}>Details</button>
                    </div>
                  ));
                })()}
              </div>
            </CardBody>
          </Card>
        </div>
      </div>
    </div>
  );
}

export default Home;