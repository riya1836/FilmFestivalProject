import React, { useState, useEffect, useContext } from 'react';
import { Link } from 'react-router-dom';
import { AuthContext } from '../../contexts/AuthContext';
import './dashboards.css';

export const AdminDashboard = () => {
  const { user, logout, token } = useContext(AuthContext);
  const [stats, setStats] = useState({
    filmsCount: 0,
    evaluationsCount: 0,
    juryMembersCount: 0,
    awardEligibleCount: 0,
  });
  const [juryAssignments, setJuryAssignments] = useState([]);
  const [awardEligibleFilms, setAwardEligibleFilms] = useState([]);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState('overview');
  const [assignForm, setAssignForm] = useState({
    juryId: '',
    filmIds: '',
  });

  useEffect(() => {
    loadDashboardData();
  }, [token]);

  const loadDashboardData = async () => {
    setLoading(true);
    try {
      const headers = { 'Authorization': `Bearer ${token}` };

      const [filmsRes, assignRes, awardRes, evalRes] = await Promise.all([
        fetch('http://localhost:8080/api/films', { headers }),
        fetch('http://localhost:8080/api/admin/jury-assignments', { headers }),
        fetch('http://localhost:8080/api/admin/award-eligible', { headers }),
        fetch('http://localhost:8080/api/admin/evaluations', { headers }),
      ]);

      const films = filmsRes.ok ? await filmsRes.json() : [];
      const assignments = assignRes.ok ? await assignRes.json() : [];
      const awards = awardRes.ok ? await awardRes.json() : [];
      const evaluations = evalRes.ok ? await evalRes.json() : [];

      setJuryAssignments(assignments);
      setAwardEligibleFilms(awards);
      setStats({
        filmsCount: films.length,
        evaluationsCount: evaluations.length,
        juryMembersCount: assignments.length,
        awardEligibleCount: awards.length,
      });
    } catch (error) {
      console.error('Failed to load dashboard data:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleAssignJury = async (e) => {
    e.preventDefault();
    try {
      const response = await fetch('http://localhost:8080/api/admin/assign-jury', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`,
        },
        body: JSON.stringify({
          jury_id: parseInt(assignForm.juryId),
          film_ids: assignForm.filmIds.split(',').map(id => parseInt(id.trim())),
        }),
      });

      if (response.ok) {
        alert('Jury assigned successfully!');
        setAssignForm({ juryId: '', filmIds: '' });
        loadDashboardData();
      } else {
        const error = await response.json();
        alert(`Error: ${error.error}`);
      }
    } catch (error) {
      alert('Failed to assign jury: ' + error.message);
    }
  };

  if (loading) {
    return <div className="dashboard-container">Loading...</div>;
  }

  return (
    <div className="dashboard-page">
      <div className="dashboard-container">
        <div className="dashboard-tabs">
          <button 
            className={`tab-btn ${activeTab === 'overview' ? 'active' : ''}`}
            onClick={() => setActiveTab('overview')}
          >
            Overview
          </button>
          <button 
            className={`tab-btn ${activeTab === 'assign' ? 'active' : ''}`}
            onClick={() => setActiveTab('assign')}
          >
            Assign Jury
          </button>
          <button 
            className={`tab-btn ${activeTab === 'awards' ? 'active' : ''}`}
            onClick={() => setActiveTab('awards')}
          >
            Award Eligible
          </button>
        </div>

        {activeTab === 'overview' && (
          <div className="dashboard-content">
            <div className="dashboard-header">
              <div>
                <span className="dashboard-badge">Admin Overview</span>
                <h2>Festival Control Center</h2>
                <p className="dashboard-subtitle">
                  Manage films, venues, awards and jury assignments from one polished admin workspace.
                </p>
              </div>
              <div className="dashboard-summary-card">
                <p className="summary-label">Welcome back, {user?.name || 'Administrator'}</p>
                <p className="summary-value">Quick actions are ready below.</p>
              </div>
            </div>

            <div className="admin-actions-grid">
              <Link to="/films" className="action-card">
                <div className="action-title">Manage Films</div>
                <div className="action-subtitle">Create, edit and delete festival films.</div>
              </Link>
              <Link to="/venues" className="action-card">
                <div className="action-title">Manage Venues</div>
                <div className="action-subtitle">Add venues and update capacities.</div>
              </Link>
              <Link to="/screenings" className="action-card">
                <div className="action-title">Manage Screenings</div>
                <div className="action-subtitle">Schedule films for every hall.</div>
              </Link>
              <Link to="/awards" className="action-card">
                <div className="action-title">Manage Awards</div>
                <div className="action-subtitle">Review award nominees and winners.</div>
              </Link>
            </div>

            <div className="stats-grid">
              <div className="stat-card">
                <div className="stat-value">{stats.filmsCount}</div>
                <div className="stat-label">Registered Films</div>
              </div>
              <div className="stat-card">
                <div className="stat-value">{stats.evaluationsCount}</div>
                <div className="stat-label">Total Evaluations</div>
              </div>
              <div className="stat-card">
                <div className="stat-value">{stats.juryMembersCount}</div>
                <div className="stat-label">Jury Assignments</div>
              </div>
              <div className="stat-card">
                <div className="stat-value">{stats.awardEligibleCount}</div>
                <div className="stat-label">Award Eligible Films</div>
              </div>
            </div>

            <div className="section">
              <h3>Recent Jury Assignments</h3>
              <div className="table-container">
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Jury ID</th>
                      <th>Film Title</th>
                      <th>Assigned Date</th>
                    </tr>
                  </thead>
                  <tbody>
                    {juryAssignments.length > 0 ? (
                      juryAssignments.slice(0, 10).map((assign) => (
                        <tr key={assign.id ?? `${assign.jury_id}-${assign.film_id}`}>
                          <td>{assign.jury_id}</td>
                          <td>{assign.film_title || 'Untitled Film'}</td>
                          <td>{assign.assigned_at ? new Date(assign.assigned_at).toLocaleDateString() : 'TBD'}</td>
                        </tr>
                      ))
                    ) : (
                      <tr>
                        <td colSpan="3" className="no-data">
                          No jury assignments available yet.
                        </td>
                      </tr>
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        )}

        {activeTab === 'assign' && (
          <div className="dashboard-content">
            <h2>Assign Jury to Films</h2>
            
            <form onSubmit={handleAssignJury} className="assign-form">
              <div className="form-group">
                <label>Jury Member ID:</label>
                <input
                  type="number"
                  value={assignForm.juryId}
                  onChange={(e) => setAssignForm({ ...assignForm, juryId: e.target.value })}
                  placeholder="e.g., 1"
                  required
                />
              </div>

              <div className="form-group">
                <label>Film IDs (comma-separated):</label>
                <input
                  type="text"
                  value={assignForm.filmIds}
                  onChange={(e) => setAssignForm({ ...assignForm, filmIds: e.target.value })}
                  placeholder="e.g., 1,2,3,4"
                  required
                />
              </div>

              <button type="submit" className="submit-btn">Assign Jury</button>
            </form>
          </div>
        )}

        {activeTab === 'awards' && (
          <div className="dashboard-content">
            <h2>Award-Eligible Films</h2>
            
            <div className="table-container">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Film Title</th>
                    <th>Genre</th>
                    <th>Avg Score</th>
                    <th>Evaluations</th>
                  </tr>
                </thead>
                <tbody>
                  {awardEligibleFilms.map((film) => (
                    <tr key={film.film_id}>
                      <td>{film.title}</td>
                      <td>{film.genre}</td>
                      <td>{film.avg_score.toFixed(2)}</td>
                      <td>{film.evaluation_count}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
