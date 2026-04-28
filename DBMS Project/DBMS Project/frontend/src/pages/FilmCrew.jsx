import { useEffect, useState } from 'react';
import { getFilmCrew, createFilmCrew, deleteFilmCrew } from '../api';

function FilmCrew() {
  const [crew, setCrew] = useState([]);
  const [formData, setFormData] = useState({ crew_id: '', film_id: '', name: '', phone_no: '', role: '' });
  const [loading, setLoading] = useState(false);
  const [fetchError, setFetchError] = useState('');

  useEffect(() => {
    async function loadCrew() {
      setLoading(true);
      setFetchError('');
      try {
        const data = await getFilmCrew();
        setCrew(Array.isArray(data) ? data.map(item => ({ ...item, id: item.crew_id })) : []);
      } catch (error) {
        setFetchError(error.message || 'Unable to load film crew');
      } finally {
        setLoading(false);
      }
    }

    loadCrew();
  }, []);

  const handleAdd = async () => {
    try {
      const created = await createFilmCrew({
        film_id: Number(formData.film_id),
        name: formData.name,
        phone_no: formData.phone_no,
        role: formData.role,
      });
      setCrew(prev => [...prev, { ...created, id: created.crew_id }]);
      setFormData({ crew_id: '', film_id: '', name: '', phone_no: '', role: '' });
    } catch (error) {
      setFetchError(error.message || 'Unable to add crew member');
    }
  };

  const handleDelete = async (crewId) => {
    try {
      await deleteFilmCrew(crewId);
      setCrew(prev => prev.filter(member => member.id !== crewId));
    } catch (error) {
      setFetchError(error.message || 'Unable to delete crew member');
    }
  };

  return (
    <div className="container mt-4">
      <h2>Film Crew</h2>
      <div className="mb-3">
        <input type="number" className="form-control mb-2" placeholder="Crew ID (optional)" value={formData.crew_id} onChange={e => setFormData({ ...formData, crew_id: e.target.value })} />
        <input type="text" className="form-control mb-2" placeholder="Name" value={formData.name} onChange={e => setFormData({ ...formData, name: e.target.value })} />
        <input type="text" className="form-control mb-2" placeholder="Role" value={formData.role} onChange={e => setFormData({ ...formData, role: e.target.value })} />
        <input type="number" className="form-control mb-2" placeholder="Film ID" value={formData.film_id} onChange={e => setFormData({ ...formData, film_id: e.target.value })} />
        <input type="text" className="form-control mb-2" placeholder="Phone No" value={formData.phone_no} onChange={e => setFormData({ ...formData, phone_no: e.target.value })} />
        <button className="btn btn-primary" onClick={handleAdd}>Add Crew Member</button>
      </div>
      {fetchError && (
        <div className="alert alert-danger" role="alert">
          {fetchError}
        </div>
      )}
      {loading ? (
        <div className="text-center py-4">
          <div className="spinner-border text-info" role="status">
            <span className="visually-hidden">Loading...</span>
          </div>
        </div>
      ) : (
        <ul className="list-group">
          {crew.map(member => (
            <li key={member.id} className="list-group-item d-flex justify-content-between align-items-center">
              <div>
                <strong>ID:</strong> {member.crew_id} - <strong>{member.name}</strong> ({member.role})<br />
                Film ID: {member.film_id}, Phone: {member.phone_no}
              </div>
              <button className="btn btn-sm btn-outline-danger" onClick={() => handleDelete(member.id)}>
                <i className="fas fa-trash"></i>
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

export default FilmCrew;