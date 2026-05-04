import React, { useState, useEffect, useContext } from 'react';
import { AuthContext } from '../../contexts/AuthContext';
import './dashboards.css';

export const JuryDashboard = () => {
  const { user, logout, token } = useContext(AuthContext);
  const [assignedFilms, setAssignedFilms] = useState([]);
  const [evaluations, setEvaluations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState('films');
  const [selectedFilm, setSelectedFilm] = useState(null);
  const [evaluationForm, setEvaluationForm] = useState({
    filmId: null,
    score: 5,
    remarks: '',
  });

  useEffect(() => {
    loadDashboardData();
  }, [token]);

  const loadDashboardData = async () => {
    setLoading(true);
    try {
      // Get assigned films
      const filmsRes = await fetch('http://localhost:8080/api/jury/assigned-films', {
        headers: { 'Authorization': `Bearer ${token}` },
      });
      if (filmsRes.ok) {
        setAssignedFilms(await filmsRes.json());
      }

      // Get my evaluations
      const evalRes = await fetch('http://localhost:8080/api/jury/evaluations', {
        headers: { 'Authorization': `Bearer ${token}` },
      });
      if (evalRes.ok) {
        setEvaluations(await evalRes.json());
      }
    } catch (error) {
      console.error('Failed to load dashboard data:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleEvaluationSubmit = async (e) => {
    e.preventDefault();
    if (!evaluationForm.filmId) {
      alert('Please select a film');
      return;
    }

    try {
      const response = await fetch('http://localhost:8080/api/jury/evaluate', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`,
        },
        body: JSON.stringify({
          film_id: evaluationForm.filmId,
          score: parseInt(evaluationForm.score),
          remarks: evaluationForm.remarks,
        }),
      });

      if (response.ok) {
        alert('Evaluation submitted successfully!');
        setEvaluationForm({ filmId: null, score: 5, remarks: '' });
        setSelectedFilm(null);
        loadDashboardData();
      } else {
        const error = await response.json();
        alert(`Error: ${error.error}`);
      }
    } catch (error) {
      alert('Failed to submit evaluation: ' + error.message);
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
            className={`tab-btn ${activeTab === 'films' ? 'active' : ''}`}
            onClick={() => setActiveTab('films')}
          >
            Assigned Films ({assignedFilms.length})
          </button>
          <button 
            className={`tab-btn ${activeTab === 'evaluations' ? 'active' : ''}`}
            onClick={() => setActiveTab('evaluations')}
          >
            My Evaluations ({evaluations.length})
          </button>
        </div>

        {activeTab === 'films' && (
          <div className="dashboard-content">
            <h2>Films Assigned to You</h2>
            
            {assignedFilms.length === 0 ? (
              <p className="no-data">No films assigned yet.</p>
            ) : (
              <div className="films-grid">
                {assignedFilms.map((film) => {
                  const isEvaluated = evaluations.some(e => e.film_id === film.film_id);
                  return (
                    <div key={film.film_id} className="film-card">
                      <div className="film-info">
                        <h3>{film.title}</h3>
                        <p className="film-assigned">
                          Assigned: {new Date(film.assigned_at).toLocaleDateString()}
                        </p>
                      </div>
                      <div className="film-status">
                        {isEvaluated ? (
                          <span className="badge-evaluated">✓ Evaluated</span>
                        ) : (
                          <button 
                            className="evaluate-btn"
                            onClick={() => {
                              setSelectedFilm(film);
                              setEvaluationForm({ filmId: film.film_id, score: 5, remarks: '' });
                              setActiveTab('evaluate');
                            }}
                          >
                            Evaluate
                          </button>
                        )}
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        )}

        {activeTab === 'evaluations' && (
          <div className="dashboard-content">
            <h2>My Evaluations</h2>
            
            {evaluations.length === 0 ? (
              <p className="no-data">No evaluations submitted yet.</p>
            ) : (
              <div className="table-container">
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Film Title</th>
                      <th>Score</th>
                      <th>Remarks</th>
                      <th>Submitted</th>
                    </tr>
                  </thead>
                  <tbody>
                    {evaluations.map((evaluation) => (
                      <tr key={evaluation.evaluation_id}>
                        <td>{evaluation.film_title}</td>
                        <td>
                          <strong className="score-badge">{evaluation.score}/10</strong>
                        </td>
                        <td>{evaluation.remarks || '-'}</td>
                        <td>{new Date(evaluation.created_at).toLocaleDateString()}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        )}

        {activeTab === 'evaluate' && selectedFilm && (
          <div className="dashboard-content">
            <h2>Evaluate: {selectedFilm.title}</h2>
            
            <form onSubmit={handleEvaluationSubmit} className="evaluation-form">
              <div className="form-group">
                <label>Film: {selectedFilm.title}</label>
              </div>

              <div className="form-group">
                <label>Score (1-10): <strong>{evaluationForm.score}</strong></label>
                <input
                  type="range"
                  min="1"
                  max="10"
                  value={evaluationForm.score}
                  onChange={(e) => setEvaluationForm({ ...evaluationForm, score: e.target.value })}
                  className="score-slider"
                />
                <div className="score-labels">
                  <span>Poor</span>
                  <span>Average</span>
                  <span>Excellent</span>
                </div>
              </div>

              <div className="form-group">
                <label>Remarks (optional)</label>
                <textarea
                  value={evaluationForm.remarks}
                  onChange={(e) => setEvaluationForm({ ...evaluationForm, remarks: e.target.value })}
                  placeholder="Share your detailed feedback about this film..."
                  rows="5"
                  maxLength="1000"
                />
                <small>{evaluationForm.remarks.length}/1000</small>
              </div>

              <div className="form-actions">
                <button type="submit" className="submit-btn">Submit Evaluation</button>
                <button 
                  type="button" 
                  className="cancel-btn"
                  onClick={() => {
                    setActiveTab('films');
                    setSelectedFilm(null);
                  }}
                >
                  Cancel
                </button>
              </div>
            </form>
          </div>
        )}
      </div>
    </div>
  );
};
