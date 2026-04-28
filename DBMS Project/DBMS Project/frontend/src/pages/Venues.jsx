import { useEffect, useState } from 'react';
import { getVenues, createVenue, updateVenue, deleteVenue } from '../api';

function Venues() {
  const [venues, setVenues] = useState([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [showModal, setShowModal] = useState(false);
  const [editingVenue, setEditingVenue] = useState(null);
  const [formData, setFormData] = useState({ venue_id: '', name: '', location: '', capacity: '' });
  const [errors, setErrors] = useState({});
  const [loading, setLoading] = useState(false);
  const [fetchError, setFetchError] = useState('');

  useEffect(() => {
    async function loadVenues() {
      setLoading(true);
      setFetchError('');
      try {
        const data = await getVenues();
        setVenues(Array.isArray(data) ? data.map(v => ({ ...v, id: v.venue_id })) : []);
      } catch (error) {
        setFetchError(error.message || 'Unable to load venues');
      } finally {
        setLoading(false);
      }
    }

    loadVenues();
  }, []);

  const filteredVenues = venues.filter(venue =>
    venue.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
    venue.location.toLowerCase().includes(searchTerm.toLowerCase())
  );

  const validateForm = () => {
    const newErrors = {};
    if (formData.venue_id && Number(formData.venue_id) <= 0) newErrors.venue_id = 'ID must be a positive number';
    if (!formData.name.trim()) newErrors.name = 'Name is required';
    if (!formData.location.trim()) newErrors.location = 'Location is required';
    if (!formData.capacity || Number(formData.capacity) <= 0) newErrors.capacity = 'Valid capacity is required';
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!validateForm()) return;

    const payload = {
      venue_name: formData.name,
      address: formData.location,
      capacity: Number(formData.capacity),
    };
    if (formData.venue_id) payload.venue_id = Number(formData.venue_id);

    try {
      if (editingVenue) {
        const updated = await updateVenue(editingVenue.id, payload);
        setVenues(venues.map(v => (v.id === editingVenue.id ? { ...updated, id: updated.venue_id } : v)));
      } else {
        const created = await createVenue(payload);
        setVenues(prev => [...prev, { ...created, id: created.venue_id }]);
      }
      handleCloseModal();
    } catch (error) {
      setFetchError(error.message || 'Unable to save venue');
    }
  };

  const handleEdit = (venue) => {
    setEditingVenue(venue);
    setFormData({ venue_id: venue.id, name: venue.name, location: venue.location, capacity: venue.capacity });
    setShowModal(true);
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Are you sure you want to delete this venue?')) return;

    try {
      await deleteVenue(id);
      setVenues(venues.filter(v => v.id !== id));
    } catch (error) {
      setFetchError(error.message || 'Unable to delete venue');
    }
  };

  const handleCloseModal = () => {
    setShowModal(false);
    setEditingVenue(null);
    setFormData({ venue_id: '', name: '', location: '', capacity: '' });
    setErrors({});
  };

  const handleAddNew = () => {
    setEditingVenue(null);
    setFormData({ venue_id: '', name: '', location: '', capacity: '' });
    setShowModal(true);
  };

  return (
    <div className="container mt-4">
      <div className="d-flex justify-content-between align-items-center mb-4">
        <h2><i className="fas fa-map-marker-alt text-primary me-2"></i>Venues Management</h2>
        <button className="btn btn-primary" onClick={handleAddNew}>
          <i className="fas fa-plus me-2"></i>Add Venue
        </button>
      </div>

      <div className="mb-3">
        <input
          type="text"
          className="form-control"
          placeholder="Search venues by name or location..."
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
        {filteredVenues.map(venue => (
          <div key={venue.id} className="col-md-6 col-lg-4 mb-3">
            <div className="card h-100">
              <div className="card-body">
                <h5 className="card-title">{venue.name}</h5>
                <p className="card-text">
                  <strong>ID:</strong> {venue.venue_id}<br/>
                  <strong>Location:</strong> {venue.location}<br/>
                  <strong>Capacity:</strong> {venue.capacity} seats
                </p>
                <div className="d-flex gap-2">
                  <button className="btn btn-outline-primary btn-sm" onClick={() => handleEdit(venue)}>
                    <i className="fas fa-edit me-1"></i>Edit
                  </button>
                  <button className="btn btn-outline-danger btn-sm" onClick={() => handleDelete(venue.id)}>
                    <i className="fas fa-trash me-1"></i>Delete
                  </button>
                </div>
              </div>
            </div>
          </div>
        ))}
      </div>
      )}

      {filteredVenues.length === 0 && (
        <div className="text-center mt-4">
          <i className="fas fa-map-marker-alt fa-3x text-muted mb-3"></i>
          <p className="text-muted">No venues found. {searchTerm ? 'Try adjusting your search.' : 'Add your first venue!'}</p>
        </div>
      )}

      {/* Modal */}
      {showModal && (
        <div className="modal show d-block" style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}>
          <div className="modal-dialog">
            <div className="modal-content">
              <div className="modal-header">
                <h5 className="modal-title">{editingVenue ? 'Edit Venue' : 'Add Venue'}</h5>
                <button type="button" className="btn-close" onClick={handleCloseModal}></button>
              </div>
              <form onSubmit={handleSubmit}>
                <div className="modal-body">
                  <div className="mb-3">
                    <label className="form-label">Venue ID (optional)</label>
                    <input
                      type="number"
                      className={`form-control ${errors.venue_id ? 'is-invalid' : ''}`}
                      value={formData.venue_id}
                      onChange={(e) => setFormData({...formData, venue_id: e.target.value})}
                      disabled={Boolean(editingVenue)}
                    />
                    {errors.venue_id && <div className="invalid-feedback">{errors.venue_id}</div>}
                  </div>
                  <div className="mb-3">
                    <label className="form-label">Name *</label>
                    <input
                      type="text"
                      className={`form-control ${errors.name ? 'is-invalid' : ''}`}
                      value={formData.name}
                      onChange={(e) => setFormData({...formData, name: e.target.value})}
                    />
                    {errors.name && <div className="invalid-feedback">{errors.name}</div>}
                  </div>
                  <div className="mb-3">
                    <label className="form-label">Location *</label>
                    <input
                      type="text"
                      className={`form-control ${errors.location ? 'is-invalid' : ''}`}
                      value={formData.location}
                      onChange={(e) => setFormData({...formData, location: e.target.value})}
                    />
                    {errors.location && <div className="invalid-feedback">{errors.location}</div>}
                  </div>
                  <div className="mb-3">
                    <label className="form-label">Capacity *</label>
                    <input
                      type="number"
                      className={`form-control ${errors.capacity ? 'is-invalid' : ''}`}
                      value={formData.capacity}
                      onChange={(e) => setFormData({...formData, capacity: e.target.value})}
                    />
                    {errors.capacity && <div className="invalid-feedback">{errors.capacity}</div>}
                  </div>
                </div>
                <div className="modal-footer">
                  <button type="button" className="btn btn-secondary" onClick={handleCloseModal}>Cancel</button>
                  <button type="submit" className="btn btn-primary">{editingVenue ? 'Update' : 'Add'} Venue</button>
                </div>
              </form>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default Venues;