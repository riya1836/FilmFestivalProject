import React, { useState, useEffect, useContext } from 'react';
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
      // Get jury assignments
      const assignRes = await fetch('http://localhost:8080/api/admin/jury-assignments', {
        headers: { 'Authorization': `Bearer ${token}` },
      });
      if (assignRes.ok) {
        setJuryAssignments(await assignRes.json());
      }

      // Get award eligible films
      const awardRes = await fetch('http://localhost:8080/api/admin/award-eligible', {
        headers: { 'Authorization': `Bearer ${token}` },
      });
      if (awardRes.ok) {
        const films = await awardRes.json();
        setAwardEligibleFilms(films);
      }

      // Get all evaluations
      const evalRes = await fetch('http://localhost:8080/api/admin/evaluations', {
        headers: { 'Authorization': `Bearer ${token}` },
      });
      if (evalRes.ok) {
        const evaluations = await evalRes.json();
        setStats((prev) => ({
          ...prev,
          evaluationsCount: evaluations.length,
        }));
      }

      // TODO: Get counts from database
      setStats((prev) => ({
        ...prev,
        juryMembersCount: juryAssignments.length,
        awardEligibleCount: awardEligibleFilms.length,
      }));
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
      <nav className="dashboard-navbar">
        <div className="navbar-brand">🎬 Film Festival Admin</div>
        <div className="navbar-user">
          <span>{user?.name}</span>
          <button onClick={logout} className="logout-btn">Logout</button>
        </div>
      </nav>

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
            <h2>Admin Dashboard</h2>
            
            <div className="stats-grid">
              <div className="stat-card">
                <div className="stat-value">{stats.evaluationsCount}</div>
                <div className="stat-label">Total Evaluations</div>
              </div>
              <div className="stat-card">
                <div className="stat-value">{juryAssignments.length}</div>
                <div className="stat-label">Jury Assignments</div>
              </div>
              <div className="stat-card">
                <div className="stat-value">{awardEligibleFilms.length}</div>
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
                    {juryAssignments.slice(0, 10).map((assign) => (
                      <tr key={assign.id}>
                        <td>{assign.jury_id}</td>
                        <td>{assign.film_title}</td>
                        <td>{new Date(assign.assigned_at).toLocaleDateString()}</td>
                      </tr>
                    ))}
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
