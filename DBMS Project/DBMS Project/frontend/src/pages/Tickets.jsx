import React, { useEffect, useState } from 'react';
import { getTickets, createTicket, updateTicket, deleteTicket, getScreenings, getVenues, getAttendees } from '../api';
import { Card, CardBody } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { Modal, useModal } from '../components/ui/Modal';
import { Input, Select } from '../components/ui/Input';
import { showToast } from '../components/ui/Toast';

function Tickets() {
  const [tickets, setTickets] = useState([]);
  const [screenings, setScreenings] = useState([]);
  const [venues, setVenues] = useState([]);
  const [attendees, setAttendees] = useState([]);
  const [loading, setLoading] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const [showSeatSelection, setShowSeatSelection] = useState(false);
  const [occupiedSeats, setOccupiedSeats] = useState([]);
  const [selectedSeat, setSelectedSeat] = useState(null);
  
  const formModal = useModal();
  const [editingTicket, setEditingTicket] = useState(null);
  const [formData, setFormData] = useState({ 
    ticket_id: '', 
    screening_id: '', 
    attendee_id: '', 
    seat_number: '' 
  });
  const [errors, setErrors] = useState({});

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    setLoading(true);
    try {
      const [ticketsData, screeningsData, venuesData, attendeesData] = await Promise.all([
        getTickets(),
        getScreenings(),
        getVenues(),
        getAttendees()
      ]);
      setTickets(Array.isArray(ticketsData) ? ticketsData.map(t => ({ ...t, id: t.ticket_id })) : []);
      setScreenings(Array.isArray(screeningsData) ? screeningsData : []);
      setVenues(Array.isArray(venuesData) ? venuesData : []);
      setAttendees(Array.isArray(attendeesData) ? attendeesData : []);
    } catch (error) {
      showToast('Failed to load data', 'error');
    } finally {
      setLoading(false);
    }
  };

  const loadOccupiedSeats = (screeningId) => {
    const occupied = tickets
      .filter(ticket => ticket.screening_id === Number(screeningId))
      .map(ticket => ticket.seat_number);
    setOccupiedSeats(occupied);
  };

  const filteredTickets = tickets.filter(ticket =>
    (ticket.ticket_id || '').toString().includes(searchTerm) ||
    (ticket.seat_number || '').toLowerCase().includes(searchTerm.toLowerCase())
  );

  const handleScreeningChange = (screeningId) => {
    setFormData({ ...formData, screening_id: screeningId, seat_number: '' });
    setSelectedSeat(null);
    if (screeningId) {
      loadOccupiedSeats(screeningId);
      setShowSeatSelection(true);
    } else {
      setOccupiedSeats([]);
      setShowSeatSelection(false);
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

    const totalSeats = venue.capacity;
    const seatsPerRow = 10;
    const rows = Math.ceil(totalSeats / seatsPerRow);
    
    return (
      <div style={{ marginTop: '20px', padding: '15px', background: 'rgba(255,255,255,0.05)', borderRadius: '12px' }}>
        <p style={{ textAlign: 'center', fontSize: '12px', color: 'var(--text-muted)', marginBottom: '10px' }}>SCREEN</p>
        <div style={{ height: '4px', background: 'var(--accent)', width: '80%', margin: '0 auto 20px', borderRadius: '4px', boxShadow: '0 0 10px var(--accent)' }}></div>
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px', justifyContent: 'center', maxHeight: '300px', overflowY: 'auto', padding: '10px' }}>
          {Array.from({ length: totalSeats }).map((_, idx) => {
            const row = String.fromCharCode(65 + Math.floor(idx / seatsPerRow));
            const num = (idx % seatsPerRow) + 1;
            const seatNumber = `${row}${num}`;
            const isOccupied = occupiedSeats.includes(seatNumber) && (!editingTicket || editingTicket.seat_number !== seatNumber);
            const isSelected = selectedSeat === seatNumber;

            return (
              <button
                key={seatNumber}
                type="button"
                onClick={() => handleSeatSelect(seatNumber)}
                disabled={isOccupied}
                style={{
                  width: '32px',
                  height: '32px',
                  borderRadius: '6px',
                  fontSize: '10px',
                  border: '1px solid var(--border-light)',
                  background: isOccupied ? 'rgba(239, 68, 68, 0.2)' : isSelected ? 'var(--accent)' : 'transparent',
                  color: isSelected ? 'black' : 'var(--text-primary)',
                  cursor: isOccupied ? 'not-allowed' : 'pointer',
                  transition: 'all 0.2s',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center'
                }}
              >
                {seatNumber}
              </button>
            );
          })}
        </div>
      </div>
    );
  };

  const validateForm = () => {
    const newErrors = {};
    if (!formData.screening_id) newErrors.screening_id = 'Select a screening';
    if (!formData.attendee_id) newErrors.attendee_id = 'Select an attendee';
    if (!formData.seat_number) newErrors.seat_number = 'Select a seat';
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async () => {
    if (!validateForm()) return;

    try {
      if (editingTicket) {
        await updateTicket(editingTicket.id, formData);
        showToast('Ticket updated successfully', 'success');
      } else {
        await createTicket(formData);
        showToast('Ticket created successfully', 'success');
      }
      await loadData();
      handleCloseModal();
    } catch (error) {
      showToast(error.message || 'Failed to save ticket', 'error');
    }
  };

  const handleEdit = (ticket) => {
    setEditingTicket(ticket);
    setFormData({ 
      ticket_id: ticket.id, 
      screening_id: ticket.screening_id, 
      attendee_id: ticket.attendee_id, 
      seat_number: ticket.seat_number 
    });
    setSelectedSeat(ticket.seat_number);
    loadOccupiedSeats(ticket.screening_id);
    setShowSeatSelection(true);
    formModal.open();
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Delete this ticket?')) return;
    try {
      await deleteTicket(id);
      showToast('Ticket deleted successfully', 'success');
      await loadData();
    } catch (error) {
      showToast('Failed to delete ticket', 'error');
    }
  };

  const handleCloseModal = () => {
    formModal.close();
    setEditingTicket(null);
    setFormData({ ticket_id: '', screening_id: '', attendee_id: '', seat_number: '' });
    setSelectedSeat(null);
    setShowSeatSelection(false);
    setErrors({});
  };

  return (
    <div className="page-films">
      <div className="page-header">
        <div className="page-header-content">
          <h1 className="page-title">
            <span className="page-icon">🎫</span>
            Tickets
          </h1>
          <p className="page-subtitle">Manage film screening bookings</p>
        </div>
      </div>

      <div className="page-content">
        <div className="page-toolbar">
          <div className="toolbar-left">
            <div className="search-box">
              <input
                type="text"
                className="search-input"
                placeholder="🔍 Search tickets..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
              />
            </div>
          </div>
          <div className="toolbar-right">
            <Button variant="primary" onClick={() => { setEditingTicket(null); formModal.open(); }} icon="➕">
              Add Ticket
            </Button>
          </div>
        </div>

        {loading ? (
          <div className="loading-skeleton">
            {[...Array(6)].map((_, i) => (
              <div key={i} className="skeleton-card"></div>
            ))}
          </div>
        ) : filteredTickets.length === 0 ? (
          <div className="empty-state">
            <div className="empty-icon">🎫</div>
            <h3>No tickets found</h3>
            <Button variant="primary" onClick={() => formModal.open()}>Add Ticket</Button>
          </div>
        ) : (
          <div className="films-grid">
            {filteredTickets.map(ticket => {
              const screening = screenings.find(s => s.screening_id === Number(ticket.screening_id));
              const attendee = attendees.find(a => a.attendee_id === Number(ticket.attendee_id));
              return (
                <Card key={ticket.id} hoverable className="film-card">
                  <div className="film-poster">🎫</div>
                  <CardBody>
                    <h3 className="film-title">Ticket #{ticket.ticket_id}</h3>
                    <div className="film-meta">
                      <div className="meta-item">
                        <span className="label">Seat</span>
                        <span className="badge">{ticket.seat_number}</span>
                      </div>
                      <div className="meta-item">
                        <span className="label">Attendee</span>
                        <span className="value" style={{fontSize: '12px'}}>{attendee?.name || `ID: ${ticket.attendee_id}`}</span>
                      </div>
                    </div>
                    <div className="film-meta">
                      <div className="meta-item">
                        <span className="label">Film</span>
                        <span className="value" style={{fontSize: '12px'}}>{screening?.film_title || `ID: ${screening?.film_id}`}</span>
                      </div>
                      <div className="meta-item">
                        <span className="label">Price</span>
                        <span className="value">${ticket.price}</span>
                      </div>
                    </div>
                    <div className="film-actions">
                      <Button variant="secondary" size="sm" onClick={() => handleEdit(ticket)} icon="✏️">Edit</Button>
                      <Button variant="danger" size="sm" onClick={() => handleDelete(ticket.id)} icon="🗑️">Delete</Button>
                    </div>
                  </CardBody>
                </Card>
              );
            })}
          </div>
        )}
      </div>

      <Modal
        isOpen={formModal.isOpen}
        onClose={handleCloseModal}
        title={editingTicket ? '✏️ Edit Ticket' : '➕ Add Ticket'}
        footer={
          <div style={{ display: 'flex', gap: '8px' }}>
            <Button variant="ghost" onClick={handleCloseModal}>Cancel</Button>
            <Button variant="primary" onClick={handleSubmit}>
              {editingTicket ? 'Update' : 'Create'}
            </Button>
          </div>
        }
      >
        <div className="form-grid">
          <Select
            label="Screening"
            value={formData.screening_id}
            onChange={(e) => handleScreeningChange(e.target.value)}
            error={errors.screening_id}
            required
            options={[
              { value: '', label: '-- Select Screening --' },
              ...screenings.map(s => ({ 
                value: s.screening_id, 
                label: `${s.film_title || 'Film '+s.film_id} - ${s.screening_date} ${s.start_time.substring(0,5)}` 
              }))
            ]}
          />
          <Select
            label="Attendee"
            value={formData.attendee_id}
            onChange={(e) => setFormData({...formData, attendee_id: e.target.value})}
            error={errors.attendee_id}
            required
            options={[
              { value: '', label: '-- Select Attendee --' },
              ...attendees.map(a => ({ value: a.attendee_id, label: a.name }))
            ]}
          />
          <div className="form-group">
            <label>Seat Number: <span style={{ color: 'var(--accent)', fontWeight: 'bold' }}>{formData.seat_number || 'None selected'}</span></label>
            {errors.seat_number && <p style={{ color: 'var(--danger)', fontSize: '12px', marginTop: '4px' }}>{errors.seat_number}</p>}
            {showSeatSelection && generateSeatMap()}
          </div>
        </div>
      </Modal>
    </div>
  );
}

export default Tickets;