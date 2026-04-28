import { useEffect, useState } from 'react';
import { getAttendees, createAttendee, updateAttendee, deleteAttendee } from '../api';

function Attendees() {
  const [attendees, setAttendees] = useState([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [showModal, setShowModal] = useState(false);
  const [editingAttendee, setEditingAttendee] = useState(null);
  const [formData, setFormData] = useState({ attendee_id: '', first_name: '', last_name: '', email: '', phone: '' });
  const [errors, setErrors] = useState({});
  const [loading, setLoading] = useState(false);
  const [fetchError, setFetchError] = useState('');

  useEffect(() => {
    async function loadAttendees() {
      setLoading(true);
      setFetchError('');
      try {
        const data = await getAttendees();
        setAttendees(Array.isArray(data) ? data.map(a => ({ ...a, id: a.attendee_id })) : []);
      } catch (error) {
        setFetchError(error.message || 'Unable to load attendees');
      } finally {
        setLoading(false);
      }
    }

    loadAttendees();
  }, []);

  const filteredAttendees = attendees.filter(attendee =>
    `${attendee.first_name} ${attendee.last_name}`.toLowerCase().includes(searchTerm.toLowerCase()) ||
    attendee.email.toLowerCase().includes(searchTerm.toLowerCase())
  );

  const validateForm = () => {
    const newErrors = {};
    if (formData.attendee_id && Number(formData.attendee_id) <= 0) newErrors.attendee_id = 'ID must be a positive number';
    if (!formData.first_name.trim()) newErrors.first_name = 'First name is required';
    if (!formData.last_name.trim()) newErrors.last_name = 'Last name is required';
    if (!formData.email.trim()) newErrors.email = 'Email is required';
    else if (!/\S+@\S+\.\S+/.test(formData.email)) newErrors.email = 'Email is invalid';
    if (!formData.phone.trim()) newErrors.phone = 'Phone is required';
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!validateForm()) return;

    try {
      if (editingAttendee) {
        const updated = await updateAttendee(editingAttendee.id, formData);
        setAttendees(attendees.map(a => (a.id === editingAttendee.id ? { ...updated, id: updated.attendee_id } : a)));
      } else {
        const payload = { ...formData };
        if (!payload.attendee_id) delete payload.attendee_id;
        const created = await createAttendee(payload);
        setAttendees(prev => [...prev, { ...created, id: created.attendee_id }]);
      }
      handleCloseModal();
    } catch (error) {
      setFetchError(error.message || 'Unable to save attendee');
    }
  };

  const handleEdit = (attendee) => {
    setEditingAttendee(attendee);
    setFormData({
      attendee_id: attendee.id,
      first_name: attendee.first_name || '',
      last_name: attendee.last_name || '',
      email: attendee.email || '',
      phone: attendee.phone || '',
    });
    setShowModal(true);
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Are you sure you want to delete this attendee?')) return;

    try {
      await deleteAttendee(id);
      setAttendees(attendees.filter(a => a.id !== id));
    } catch (error) {
      setFetchError(error.message || 'Unable to delete attendee');
    }
  };

  const handleCloseModal = () => {
    setShowModal(false);
    setEditingAttendee(null);
    setFormData({ attendee_id: '', first_name: '', last_name: '', email: '', phone: '' });
    setErrors({});
  };

  const handleAddNew = () => {
    setEditingAttendee(null);
    setFormData({ attendee_id: '', first_name: '', last_name: '', email: '', phone: '' });
    setShowModal(true);
  };

  return (
    <div className="container mt-4">
      <div className="d-flex justify-content-between align-items-center mb-4">
        <h2><i className="fas fa-users text-success me-2"></i>Attendees Management</h2>
        <button className="btn btn-success" onClick={handleAddNew}>
          <i className="fas fa-plus me-2"></i>Add Attendee
        </button>
      </div>

      <div className="mb-3">
        <input
          type="text"
          className="form-control"
          placeholder="Search attendees by name or email..."
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
          <div className="spinner-border text-success" role="status">
            <span className="visually-hidden">Loading...</span>
          </div>
        </div>
      ) : (
        <div className="row">
          {filteredAttendees.map(attendee => (
          <div key={attendee.id} className="col-md-6 col-lg-4 mb-3">
            <div className="card h-100">
              <div className="card-body">
                <h5 className="card-title">{attendee.first_name} {attendee.last_name}</h5>
                <p className="card-text">
                  <strong>ID:</strong> {attendee.attendee_id}<br/>
                  <strong>Email:</strong> {attendee.email}<br/>
                  <strong>Phone:</strong> {attendee.phone}
                </p>
                <div className="d-flex gap-2">
                  <button className="btn btn-outline-success btn-sm" onClick={() => handleEdit(attendee)}>
                    <i className="fas fa-edit me-1"></i>Edit
                  </button>
                  <button className="btn btn-outline-danger btn-sm" onClick={() => handleDelete(attendee.id)}>
                    <i className="fas fa-trash me-1"></i>Delete
                  </button>
                </div>
              </div>
            </div>
          </div>
        ))}
        </div>
      )}

      {filteredAttendees.length === 0 && !loading && (
        <div className="text-center mt-4">
          <i className="fas fa-users fa-3x text-muted mb-3"></i>
          <p className="text-muted">No attendees found. {searchTerm ? 'Try adjusting your search.' : 'Add your first attendee!'}</p>
        </div>
      )}

      {/* Modal */}
      {showModal && (
        <div className="modal show d-block" style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}>
          <div className="modal-dialog">
            <div className="modal-content">
              <div className="modal-header">
                <h5 className="modal-title">{editingAttendee ? 'Edit Attendee' : 'Add Attendee'}</h5>
                <button type="button" className="btn-close" onClick={handleCloseModal}></button>
              </div>
              <form onSubmit={handleSubmit}>
                <div className="modal-body">
                  <div className="mb-3">
                    <label className="form-label">Attendee ID (optional)</label>
                    <input
                      type="number"
                      className={`form-control ${errors.attendee_id ? 'is-invalid' : ''}`}
                      value={formData.attendee_id}
                      onChange={(e) => setFormData({ ...formData, attendee_id: e.target.value })}
                      disabled={Boolean(editingAttendee)}
                    />
                    {errors.attendee_id && <div className="invalid-feedback">{errors.attendee_id}</div>}
                  </div>
                  <div className="mb-3">
                    <label className="form-label">First Name *</label>
                    <input
                      type="text"
                      className={`form-control ${errors.first_name ? 'is-invalid' : ''}`}
                      value={formData.first_name}
                      onChange={(e) => setFormData({ ...formData, first_name: e.target.value })}
                    />
                    {errors.first_name && <div className="invalid-feedback">{errors.first_name}</div>}
                  </div>
                  <div className="mb-3">
                    <label className="form-label">Last Name *</label>
                    <input
                      type="text"
                      className={`form-control ${errors.last_name ? 'is-invalid' : ''}`}
                      value={formData.last_name}
                      onChange={(e) => setFormData({ ...formData, last_name: e.target.value })}
                    />
                    {errors.last_name && <div className="invalid-feedback">{errors.last_name}</div>}
                  </div>
                  <div className="mb-3">
                    <label className="form-label">Email *</label>
                    <input
                      type="email"
                      className={`form-control ${errors.email ? 'is-invalid' : ''}`}
                      value={formData.email}
                      onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                    />
                    {errors.email && <div className="invalid-feedback">{errors.email}</div>}
                  </div>
                  <div className="mb-3">
                    <label className="form-label">Phone *</label>
                    <input
                      type="text"
                      className={`form-control ${errors.phone ? 'is-invalid' : ''}`}
                      value={formData.phone}
                      onChange={(e) => setFormData({...formData, phone: e.target.value})}
                    />
                    {errors.phone && <div className="invalid-feedback">{errors.phone}</div>}
                  </div>
                </div>
                <div className="modal-footer">
                  <button type="button" className="btn btn-secondary" onClick={handleCloseModal}>Cancel</button>
                  <button type="submit" className="btn btn-success">{editingAttendee ? 'Update' : 'Add'} Attendee</button>
                </div>
              </form>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default Attendees;