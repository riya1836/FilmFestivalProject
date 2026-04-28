import { useEffect, useState } from 'react';
import { getTickets, createTicket, updateTicket, deleteTicket, getScreenings, getVenues } from '../api';

function Tickets() {
  const [tickets, setTickets] = useState([]);
  const [screenings, setScreenings] = useState([]);
  const [venues, setVenues] = useState([]);
  const [occupiedSeats, setOccupiedSeats] = useState([]);
  const [selectedSeat, setSelectedSeat] = useState(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [showModal, setShowModal] = useState(false);
  const [showSeatSelection, setShowSeatSelection] = useState(false);
  const [editingTicket, setEditingTicket] = useState(null);
  const [formData, setFormData] = useState({ ticket_id: '', screening_id: '', attendee_id: '', seat_number: '', price: '' });
  const [errors, setErrors] = useState({});
  const [loading, setLoading] = useState(false);
  const [fetchError, setFetchError] = useState('');

  useEffect(() => {
    async function loadData() {
      setLoading(true);
      setFetchError('');
      try {
        const [ticketsData, screeningsData, venuesData] = await Promise.all([
          getTickets(),
          getScreenings(),
          getVenues()
        ]);
        setTickets(Array.isArray(ticketsData) ? ticketsData.map(t => ({ ...t, id: t.ticket_id })) : []);
        setScreenings(Array.isArray(screeningsData) ? screeningsData : []);
        setVenues(Array.isArray(venuesData) ? venuesData : []);
      } catch (error) {
        setFetchError(error.message || 'Unable to load data');
      } finally {
        setLoading(false);
      }
    }

    loadData();
  }, []);

  const loadOccupiedSeats = (screeningId) => {
    const occupied = tickets
      .filter(ticket => ticket.screening_id === Number(screeningId))
      .map(ticket => ticket.seat_number);
    setOccupiedSeats(occupied);
  };

  const filteredTickets = tickets.filter(ticket =>
    ticket.screening_id.toString().includes(searchTerm) ||
    ticket.attendee_id.toString().includes(searchTerm) ||
    ticket.seat_number.includes(searchTerm)
  );

  const handleScreeningChange = (screeningId) => {
    setFormData({ ...formData, screening_id: screeningId, seat_number: '' });
    setSelectedSeat(null);
    if (screeningId) {
      loadOccupiedSeats(screeningId);
      setShowSeatSelection(true); // Automatically show seat selection when screening is selected
    } else {
      setOccupiedSeats([]);
      setShowSeatSelection(false); // Hide seat selection when no screening is selected
    }
  };

  const handleSeatSelect = (seatNumber) => {
    setSelectedSeat(seatNumber);
    setFormData({ ...formData, seat_number: seatNumber });
  };

  const generateSeatMap = () => {
    if (!formData.screening_id) return null;

    const screening = screenings.find(s => s.screening_id === Number(formData.screening_id));
    if (!screening) return null;

    const venue = venues.find(v => v.venue_id === screening.venue_id);
    if (!venue) return null;

    // Create a more realistic cinema layout based on venue capacity
    const totalSeats = venue.capacity;
    const seatsPerRow = 12; // 12 seats per row
    const rows = Math.ceil(totalSeats / seatsPerRow);
    
    const seatMap = [];
    
    // Add screen indicator
    seatMap.push(
      <div key="screen" className="text-center mb-4">
        <div style={{ 
          width: '80%', 
          height: '20px', 
          backgroundColor: '#666', 
          borderRadius: '10px 10px 50px 50px',
          margin: '0 auto',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          color: 'white',
          fontSize: '12px',
          fontWeight: 'bold'
        }}>
          SCREEN
        </div>
      </div>
    );

    for (let row = 0; row < rows; row++) {
      const rowSeats = [];
      const seatsInThisRow = row === rows - 1 ? totalSeats - (row * seatsPerRow) : seatsPerRow;
      
      // Add row label
      rowSeats.push(
        <span key={`label-${row}`} className="me-2 fw-bold" style={{ width: '20px', textAlign: 'center' }}>
          {String.fromCharCode(65 + row)}
        </span>
      );
      
      for (let seat = 1; seat <= seatsInThisRow; seat++) {
        const seatNumber = `${String.fromCharCode(65 + row)}${seat}`;
        const isOccupied = occupiedSeats.includes(seatNumber);
        const isSelected = selectedSeat === seatNumber;
        
        rowSeats.push(
          <button
            key={seatNumber}
            className={`btn m-1 ${
              isOccupied 
                ? 'btn-danger' 
                : isSelected 
                  ? 'btn-success' 
                  : 'btn-outline-secondary'
            }`}
            style={{ 
              width: '30px', 
              height: '30px', 
              fontSize: '9px',
              padding: '1px',
              borderRadius: '3px'
            }}
            disabled={isOccupied}
            onClick={() => handleSeatSelect(seatNumber)}
            title={isOccupied ? 'Occupied' : isSelected ? 'Selected' : `Seat ${seatNumber}`}
          >
            {seat}
          </button>
        );
      }
      
      seatMap.push(
        <div key={row} className="d-flex justify-content-center align-items-center mb-1">
          {rowSeats}
        </div>
      );
    }

    return (
      <div className="mt-3 border rounded p-3 bg-light">
        <h6 className="text-center mb-3">Select Your Seat</h6>
        
        {/* Legend */}
        <div className="d-flex justify-content-center gap-3 mb-3">
          <div className="d-flex align-items-center">
            <div className="btn btn-outline-secondary btn-sm me-1" style={{ width: '20px', height: '20px', padding: '0' }}></div>
            <small>Available</small>
          </div>
          <div className="d-flex align-items-center">
            <div className="btn btn-success btn-sm me-1" style={{ width: '20px', height: '20px', padding: '0' }}></div>
            <small>Selected</small>
          </div>
          <div className="d-flex align-items-center">
            <div className="btn btn-danger btn-sm me-1" style={{ width: '20px', height: '20px', padding: '0' }}></div>
            <small>Occupied</small>
          </div>
        </div>
        
        {/* Seat Map */}
        <div style={{ 
          maxHeight: '400px', 
          overflowY: 'auto',
          backgroundColor: '#f8f9fa',
          padding: '20px',
          borderRadius: '8px'
        }}>
          {seatMap}
        </div>
        
        {selectedSeat && (
          <div className="text-center mt-3">
            <strong>Selected Seat: {selectedSeat}</strong>
          </div>
        )}
      </div>
    );
  };

  const validateForm = () => {
    const newErrors = {};
    if (formData.ticket_id && Number(formData.ticket_id) <= 0) newErrors.ticket_id = 'ID must be a positive number';
    if (!formData.screening_id || Number(formData.screening_id) <= 0) newErrors.screening_id = 'Valid screening ID is required';
    if (!formData.attendee_id || Number(formData.attendee_id) <= 0) newErrors.attendee_id = 'Valid attendee ID is required';
    if (!formData.seat_number.trim()) newErrors.seat_number = 'Seat number is required';
    if (!formData.price || Number(formData.price) < 0) newErrors.price = 'Valid price is required';
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!validateForm()) return;

    const payload = {
      screening_id: Number(formData.screening_id),
      attendee_id: Number(formData.attendee_id),
      seat_number: formData.seat_number,
      price: Number(formData.price),
    };
    if (formData.ticket_id) payload.ticket_id = Number(formData.ticket_id);

    try {
      if (editingTicket) {
        const updated = await updateTicket(editingTicket.id, payload);
        setTickets(tickets.map(t => (t.id === editingTicket.id ? { ...updated, id: updated.ticket_id } : t)));
      } else {
        const created = await createTicket(payload);
        setTickets(prev => [...prev, { ...created, id: created.ticket_id }]);
      }
      handleCloseModal();
    } catch (error) {
      setFetchError(error.message || 'Unable to save ticket');
    }
  };

  const handleEdit = (ticket) => {
    setEditingTicket(ticket);
    setFormData({ ticket_id: ticket.id, screening_id: ticket.screening_id, attendee_id: ticket.attendee_id, seat_number: ticket.seat_number, price: ticket.price });
    setSelectedSeat(ticket.seat_number);
    loadOccupiedSeats(ticket.screening_id);
    setShowModal(true);
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Are you sure you want to delete this ticket?')) return;

    try {
      await deleteTicket(id);
      setTickets(tickets.filter(t => t.id !== id));
    } catch (error) {
      setFetchError(error.message || 'Unable to delete ticket');
    }
  };

  const handleCloseModal = () => {
    setShowModal(false);
    setEditingTicket(null);
    setFormData({ ticket_id: '', screening_id: '', attendee_id: '', seat_number: '', price: '' });
    setErrors({});
    setSelectedSeat(null);
    setOccupiedSeats([]);
    setShowSeatSelection(false);
  };

  const handleAddNew = () => {
    setEditingTicket(null);
    setFormData({ ticket_id: '', screening_id: '', attendee_id: '', seat_number: '', price: '' });
    setShowModal(true);
    setSelectedSeat(null);
    setOccupiedSeats([]);
    setShowSeatSelection(false);
  };

  return (
    <div className="container mt-4">
      <div className="d-flex justify-content-between align-items-center mb-4">
        <h2><i className="fas fa-ticket-alt text-primary me-2"></i>Tickets Management</h2>
        <button className="btn btn-primary" onClick={handleAddNew}>
          <i className="fas fa-plus me-2"></i>Add Ticket
        </button>
      </div>

      <div className="mb-3">
        <input
          type="text"
          className="form-control"
          placeholder="Search tickets by screening ID, attendee ID, or seat number..."
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
        {filteredTickets.map(ticket => (
          <div key={ticket.id} className="col-md-6 col-lg-4 mb-3">
            <div className="card h-100">
              <div className="card-body">
                <h5 className="card-title">Ticket #{ticket.ticket_id}</h5>
                <p className="card-text">
                  <strong>ID:</strong> {ticket.ticket_id}<br/>
                  <strong>Screening ID:</strong> {ticket.screening_id}<br/>
                  <strong>Attendee ID:</strong> {ticket.attendee_id}<br/>
                  <strong>Seat:</strong> {ticket.seat_number}<br/>
                  <strong>Price:</strong> ${ticket.price}
                </p>
                <div className="d-flex gap-2">
                  <button className="btn btn-outline-primary btn-sm" onClick={() => handleEdit(ticket)}>
                    <i className="fas fa-edit me-1"></i>Edit
                  </button>
                  <button className="btn btn-outline-danger btn-sm" onClick={() => handleDelete(ticket.id)}>
                    <i className="fas fa-trash me-1"></i>Delete
                  </button>
                </div>
              </div>
            </div>
          </div>
        ))}
      </div>
      )}

      {filteredTickets.length === 0 && (
        <div className="text-center mt-4">
          <i className="fas fa-ticket-alt fa-3x text-muted mb-3"></i>
          <p className="text-muted">No tickets found. {searchTerm ? 'Try adjusting your search.' : 'Add your first ticket!'}</p>
        </div>
      )}

      {/* Modal */}
      {showModal && (
        <div className="modal show d-block" style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}>
          <div className="modal-dialog">
            <div className="modal-content">
              <div className="modal-header">
                <h5 className="modal-title">{editingTicket ? 'Edit Ticket' : 'Add Ticket'}</h5>
                <button type="button" className="btn-close" onClick={handleCloseModal}></button>
              </div>
              <form onSubmit={handleSubmit}>
                <div className="modal-body">
                  <div className="mb-3">
                    <label className="form-label">Ticket ID (optional)</label>
                    <input
                      type="number"
                      className={`form-control ${errors.ticket_id ? 'is-invalid' : ''}`}
                      value={formData.ticket_id}
                      onChange={(e) => setFormData({...formData, ticket_id: e.target.value})}
                      disabled={Boolean(editingTicket)}
                    />
                    {errors.ticket_id && <div className="invalid-feedback">{errors.ticket_id}</div>}
                  </div>
                  <div className="mb-3">
                    <label className="form-label">Screening *</label>
                    <select
                      className={`form-control ${errors.screening_id ? 'is-invalid' : ''}`}
                      value={formData.screening_id}
                      onChange={(e) => handleScreeningChange(e.target.value)}
                    >
                      <option value="">Select Screening</option>
                      {screenings.map(screening => (
                        <option key={screening.screening_id} value={screening.screening_id}>
                          Film #{screening.film_id} at Venue #{screening.venue_id} - {screening.screening_time}
                        </option>
                      ))}
                    </select>
                    {errors.screening_id && <div className="invalid-feedback">{errors.screening_id}</div>}
                  </div>
                  <div className="mb-3">
                    <label className="form-label">Attendee ID *</label>
                    <input
                      type="number"
                      className={`form-control ${errors.attendee_id ? 'is-invalid' : ''}`}
                      value={formData.attendee_id}
                      onChange={(e) => setFormData({...formData, attendee_id: e.target.value})}
                    />
                    {errors.attendee_id && <div className="invalid-feedback">{errors.attendee_id}</div>}
                  </div>
                  <div className="mb-3">
                    <label className="form-label">Seat Selection *</label>
                    <div className="d-flex gap-2 align-items-center">
                      <input
                        type="text"
                        className={`form-control ${errors.seat_number ? 'is-invalid' : ''}`}
                        value={formData.seat_number}
                        readOnly
                        placeholder="Click on a seat below to select"
                      />
                      <button 
                        type="button" 
                        className="btn btn-outline-primary"
                        onClick={() => setShowSeatSelection(!showSeatSelection)}
                        disabled={!formData.screening_id}
                      >
                        {selectedSeat ? 'Change Seat' : (showSeatSelection ? 'Hide Seats' : 'Select Seat')}
                      </button>
                    </div>
                    {errors.seat_number && <div className="invalid-feedback">{errors.seat_number}</div>}
                    {showSeatSelection && generateSeatMap()}
                  </div>
                  <div className="mb-3">
                    <label className="form-label">Price *</label>
                    <input
                      type="number"
                      step="0.01"
                      className={`form-control ${errors.price ? 'is-invalid' : ''}`}
                      value={formData.price}
                      onChange={(e) => setFormData({...formData, price: e.target.value})}
                    />
                    {errors.price && <div className="invalid-feedback">{errors.price}</div>}
                  </div>
                </div>
                <div className="modal-footer">
                  <button type="button" className="btn btn-secondary" onClick={handleCloseModal}>Cancel</button>
                  <button type="submit" className="btn btn-primary">{editingTicket ? 'Update' : 'Add'} Ticket</button>
                </div>
              </form>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default Tickets;