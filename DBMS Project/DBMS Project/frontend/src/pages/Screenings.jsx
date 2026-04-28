import { useEffect, useState } from 'react';
import { getScreenings, createScreening, updateScreening, deleteScreening } from '../api';

function Screenings() {
  const [screenings, setScreenings] = useState([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [showModal, setShowModal] = useState(false);
  const [editingScreening, setEditingScreening] = useState(null);
  const [formData, setFormData] = useState({ screening_id: '', film_id: '', venue_id: '', screening_time: '' });
  const [errors, setErrors] = useState({});
  const [loading, setLoading] = useState(false);
  const [fetchError, setFetchError] = useState('');

  useEffect(() => {
    async function loadScreenings() {
      setLoading(true);
      setFetchError('');
      try {
        const data = await getScreenings();
        setScreenings(Array.isArray(data) ? data.map(s => ({ ...s, id: s.screening_id })) : []);
      } catch (error) {
        setFetchError(error.message || 'Unable to load screenings');
      } finally {
        setLoading(false);
      }
    }

    loadScreenings();
  }, []);

  const filteredScreenings = screenings.filter(screening =>
    screening.film_id.toString().includes(searchTerm) ||
    screening.venue_id.toString().includes(searchTerm) ||
    screening.screening_time.includes(searchTerm)
  );

  const validateForm = () => {
    const newErrors = {};
    if (formData.screening_id && Number(formData.screening_id) <= 0) newErrors.screening_id = 'ID must be a positive number';
    if (!formData.film_id || Number(formData.film_id) <= 0) newErrors.film_id = 'Valid film ID is required';
    if (!formData.venue_id || Number(formData.venue_id) <= 0) newErrors.venue_id = 'Valid venue ID is required';
    if (!formData.screening_time.trim()) newErrors.screening_time = 'Screening time is required';
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!validateForm()) return;

    const payload = {
      film_id: Number(formData.film_id),
      venue_id: Number(formData.venue_id),
      screening_time: formData.screening_time,
    };
    if (formData.screening_id) payload.screening_id = Number(formData.screening_id);

    try {
      if (editingScreening) {
        const updated = await updateScreening(editingScreening.id, payload);
        setScreenings(screenings.map(s => (s.id === editingScreening.id ? { ...updated, id: updated.screening_id } : s)));
      } else {
        const created = await createScreening(payload);
        setScreenings(prev => [...prev, { ...created, id: created.screening_id }]);
      }
      handleCloseModal();
    } catch (error) {
      setFetchError(error.message || 'Unable to save screening');
    }
  };

  const handleEdit = (screening) => {
    setEditingScreening(screening);
    setFormData({ screening_id: screening.id, film_id: screening.film_id, venue_id: screening.venue_id, screening_time: screening.screening_time });
    setShowModal(true);
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Are you sure you want to delete this screening?')) return;

    try {
      await deleteScreening(id);
      setScreenings(screenings.filter(s => s.id !== id));
    } catch (error) {
      setFetchError(error.message || 'Unable to delete screening');
    }
  };

  const handleCloseModal = () => {
    setShowModal(false);
    setEditingScreening(null);
    setFormData({ screening_id: '', film_id: '', venue_id: '', screening_time: '' });
    setErrors({});
  };

  const handleAddNew = () => {
    setEditingScreening(null);
    setFormData({ screening_id: '', film_id: '', venue_id: '', screening_time: '' });
    setShowModal(true);
  };

  return (
    <div className="container mt-4">
      <div className="d-flex justify-content-between align-items-center mb-4">
        <h2><i className="fas fa-calendar-alt text-primary me-2"></i>Screenings Management</h2>
        <button className="btn btn-primary" onClick={handleAddNew}>
          <i className="fas fa-plus me-2"></i>Add Screening
        </button>
      </div>

      <div className="mb-3">
        <input
          type="text"
          className="form-control"
          placeholder="Search screenings by film ID, venue ID, or time..."
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
        />
      </div>

      {fetchError && (
        <div className="alert alert-danger" role="alert">
          {fetchError}
        </div>
      )}

      {loading ? (
        <div className="text-center py-4">
          <div className="spinner-border text-primary" role="status">
            <span className="visually-hidden">Loading...</span>
          </div>
        </div>
      ) : (
        <div className="row">
        {filteredScreenings.map(screening => (
          <div key={screening.id} className="col-md-6 col-lg-4 mb-3">
            <div className="card h-100">
              <div className="card-body">
                <h5 className="card-title">Film #{screening.film_id}</h5>
                <p className="card-text">
                  <strong>ID:</strong> {screening.screening_id}<br/>
                  <strong>Venue ID:</strong> {screening.venue_id}<br/>
                  <strong>Time:</strong> {screening.screening_time}
                </p>
                <div className="d-flex gap-2">
                  <button className="btn btn-outline-primary btn-sm" onClick={() => handleEdit(screening)}>
                    <i className="fas fa-edit me-1"></i>Edit
                  </button>
                  <button className="btn btn-outline-danger btn-sm" onClick={() => handleDelete(screening.id)}>
                    <i className="fas fa-trash me-1"></i>Delete
                  </button>
                </div>
              </div>
            </div>
          </div>
        ))}
      </div>
      )}

      {filteredScreenings.length === 0 && (
        <div className="text-center mt-4">
          <i className="fas fa-calendar-alt fa-3x text-muted mb-3"></i>
          <p className="text-muted">No screenings found. {searchTerm ? 'Try adjusting your search.' : 'Add your first screening!'}</p>
        </div>
      )}

      {/* Modal */}
      {showModal && (
        <div className="modal show d-block" style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}>
          <div className="modal-dialog">
            <div className="modal-content">
              <div className="modal-header">
                <h5 className="modal-title">{editingScreening ? 'Edit Screening' : 'Add Screening'}</h5>
                <button type="button" className="btn-close" onClick={handleCloseModal}></button>
              </div>
              <form onSubmit={handleSubmit}>
                <div className="modal-body">
                  <div className="mb-3">
                    <label className="form-label">Screening ID (optional)</label>
                    <input
                      type="number"
                      className={`form-control ${errors.screening_id ? 'is-invalid' : ''}`}
                      value={formData.screening_id}
                      onChange={(e) => setFormData({...formData, screening_id: e.target.value})}
                      disabled={Boolean(editingScreening)}
                    />
                    {errors.screening_id && <div className="invalid-feedback">{errors.screening_id}</div>}
                  </div>
                  <div className="mb-3">
                    <label className="form-label">Film ID *</label>
                    <input
                      type="number"
                      className={`form-control ${errors.film_id ? 'is-invalid' : ''}`}
                      value={formData.film_id}
                      onChange={(e) => setFormData({...formData, film_id: e.target.value})}
                    />
                    {errors.film_id && <div className="invalid-feedback">{errors.film_id}</div>}
                  </div>
                  <div className="mb-3">
                    <label className="form-label">Venue ID *</label>
                    <input
                      type="number"
                      className={`form-control ${errors.venue_id ? 'is-invalid' : ''}`}
                      value={formData.venue_id}
                      onChange={(e) => setFormData({...formData, venue_id: e.target.value})}
                    />
                    {errors.venue_id && <div className="invalid-feedback">{errors.venue_id}</div>}
                  </div>
                  <div className="mb-3">
                    <label className="form-label">Screening Time *</label>
                    <input
                      type="datetime-local"
                      className={`form-control ${errors.screening_time ? 'is-invalid' : ''}`}
                      value={formData.screening_time}
                      onChange={(e) => setFormData({...formData, screening_time: e.target.value})}
                    />
                    {errors.screening_time && <div className="invalid-feedback">{errors.screening_time}</div>}
                  </div>
                </div>
                <div className="modal-footer">
                  <button type="button" className="btn btn-secondary" onClick={handleCloseModal}>Cancel</button>
                  <button type="submit" className="btn btn-primary">{editingScreening ? 'Update' : 'Add'} Screening</button>
                </div>
              </form>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default Screenings;