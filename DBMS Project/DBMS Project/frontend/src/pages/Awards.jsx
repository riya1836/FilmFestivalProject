import { useEffect, useState } from 'react';
import { getAwards, createAward, deleteAward, getFilms, getFilmCrew } from '../api';
import { Button } from '../components/ui/Button';
import { Card, CardBody } from '../components/ui/Card';
import { Input, Select } from '../components/ui/Input';
import { useModal, Modal } from '../components/ui/Modal';
import { showToast } from '../components/ui/Toast';
import './Awards.css';

function Awards() {
  const [awards, setAwards] = useState([]);
  const [films, setFilms] = useState([]);
  const [loading, setLoading] = useState(false);
  const [errors, setErrors] = useState({});
  
  const emptyForm = {
    award_id: '',
    award_name: '',
    film_id: '',
    crew_id: '',
    award_type: 'film',
    year: '',
  };
  
  
  const [crewList, setCrewList] = useState([]);
  const [formData, setFormData] = useState(emptyForm);
  const { isOpen, open, close } = useModal();

  useEffect(() => {
    loadData();
    const onSearch = (e) => {
      const q = (e && e.detail) || '';
      // simple filter: if query matches award name or year, highlight via filtering
      if (q) setAwards(prev => prev.filter(a => (a.award_name || '').toLowerCase().includes(String(q).toLowerCase()) || String(a.year).includes(q)));
      else loadData();
    };
    window.addEventListener('app-search', onSearch);
    return () => window.removeEventListener('app-search', onSearch);
  }, []);

  const loadData = async () => {
    setLoading(true);
    try {
      const [awardsData, filmsData, crewData] = await Promise.all([
        getAwards(),
        getFilms(),
        getFilmCrew()
      ]);

      setAwards(Array.isArray(awardsData) ? awardsData.map(a => ({ ...a, id: a.award_id })) : []);
      setFilms(Array.isArray(filmsData) ? filmsData : []);
      setCrewList(Array.isArray(crewData) ? crewData : []);
    } catch (error) {
      showToast('Failed to load data: ' + (error.message || 'Unknown error'), 'error');
    } finally {
      setLoading(false);
    }
  };

  const validateForm = () => {
    const newErrors = {};
    if (!formData.award_name || !formData.award_name.trim()) newErrors.award_name = 'Award name is required';
    const yr = Number(formData.year);
    if (!formData.year || isNaN(yr) || yr < 1900 || yr > 2100) newErrors.year = 'Enter a valid year';
    if (formData.award_type === 'film') {
      if (!formData.film_id) newErrors.film_id = 'Select a film';
    } else {
      if (!formData.crew_id) newErrors.crew_id = 'Select a crew member';
    }
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleAdd = async () => {
    if (!validateForm()) {
      showToast('Please fix the errors in the form', 'error');
      return;
    }

    try {
      const payload = {
        award_name: formData.award_name.trim(),
        year: Number(formData.year),
      };

      if (formData.award_id && String(formData.award_id).trim()) {
        payload.award_id = Number(formData.award_id);
      }

      if (formData.award_type === 'film') {
        payload.film_id = Number(formData.film_id);
      } else {
        payload.crew_id = Number(formData.crew_id);
      }

      console.log('[Awards] Submitting payload:', payload);
      const created = await createAward(payload);
      console.log('[Awards] Created award:', created);
      
      setAwards(prev => [...prev, { ...created, id: created.award_id }]);
      setFormData(emptyForm);
      setErrors({});
      close();
      showToast('🏆 Award added successfully!', 'success');
    } catch (error) {
      console.error('[Awards] Error adding award:', error);
      showToast('Failed to add award: ' + (error.message || 'Unknown error'), 'error');
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Are you sure you want to delete this award?')) return;
    
    try {
      await deleteAward(id);
      setAwards(prev => prev.filter(a => a.id !== id));
      showToast('Award deleted successfully!', 'success');
    } catch (error) {
      showToast('Failed to delete award: ' + (error.message || 'Unknown error'), 'error');
    }
  };

  const handleOpenModal = () => {
    setFormData(emptyForm);
    setErrors({});
    open();
  };

  return (
    <div className="awards-container">
      <div className="awards-header">
        <div>
          <h1 className="awards-title">🏆 Awards Management</h1>
          <p className="awards-subtitle">Manage film festival awards and recognitions</p>
        </div>
        <Button variant="primary" size="lg" onClick={handleOpenModal}>
          ➕ Add New Award
        </Button>
      </div>

      {loading ? (
        <Card className="awards-loading">
          <CardBody>Loading awards...</CardBody>
        </Card>
      ) : awards.length === 0 ? (
        <Card className="awards-empty">
          <CardBody>
            <p>No awards found. Start by adding your first award!</p>
          </CardBody>
        </Card>
      ) : (
        <div className="awards-grid">
          {awards.map(award => {
            const film = films.find(f => Number(f.film_id) === Number(award.film_id));
            const crew = crewList.find(c => Number(c.crew_id) === Number(award.crew_id));
            const recipientLabel = film ? `🎬 ${film.title}` : crew ? `🎭 ${crew.name}` : null;
            return (
            <Card key={award.id} className="award-card">
              <CardBody>
                <div className="award-content">
                  <div className="award-info">
                    <h3 className="award-name">{award.award_name}</h3>
                    <div className="award-details">
                      <span className="award-year">🗓️ {award.year}</span>
                      {recipientLabel && (
                        <span className="award-recipient">{recipientLabel}</span>
                      )}
                    </div>
                  </div>
                  <Button 
                    variant="danger" 
                    size="sm"
                    onClick={() => handleDelete(award.id)}
                  >
                    🗑️ Delete
                  </Button>
                </div>
              </CardBody>
            </Card>
          );
          })}
        </div>
      )}

      <Modal isOpen={isOpen} onClose={close} title="Add New Award" size="md">
        <div className="award-form">
          <Input
            type="text"
            label="Award Name *"
            placeholder="e.g., Best Director"
            value={formData.award_name}
            onChange={e => setFormData({ ...formData, award_name: e.target.value })}
            error={errors.award_name}
          />

          <div className="award-type-row">
            <label className="radio-inline">
              <input
                type="radio"
                name="award_type"
                value="film"
                checked={formData.award_type === 'film'}
                onChange={e => setFormData({ ...formData, award_type: 'film', crew_id: '' })}
              />
              Film
            </label>
            <label className="radio-inline">
              <input
                type="radio"
                name="award_type"
                value="crew"
                checked={formData.award_type === 'crew'}
                onChange={e => setFormData({ ...formData, award_type: 'crew', film_id: '' })}
              />
              Crew
            </label>
          </div>

          {formData.award_type === 'film' ? (
            <Select
              label="Select Film *"
              value={formData.film_id}
              onChange={e => {
                setFormData({ ...formData, film_id: e.target.value });
                setErrors({ ...errors, film_id: '' });
              }}
              error={errors.film_id}
              options={[
                { value: '', label: '-- Select a Film --' },
                ...films.map(film => ({ 
                  value: film.film_id, 
                  label: `${film.title}` 
                }))
              ]}
            />
          ) : (
            <Select
              label="Select Crew *"
              value={formData.crew_id}
              onChange={e => {
                setFormData({ ...formData, crew_id: e.target.value });
                setErrors({ ...errors, crew_id: '' });
              }}
              error={errors.crew_id}
              options={[
                { value: '', label: '-- Select a Crew Member --' },
                ...crewList.map(c => ({ value: c.crew_id, label: `${c.name}` }))
              ]}
            />
          )}

          <Input
            type="number"
            label="Year *"
            placeholder="e.g., 2024"
            value={formData.year}
            onChange={e => {
              setFormData({ ...formData, year: e.target.value });
              setErrors({ ...errors, year: '' });
            }}
            error={errors.year}
            min="1900"
            max="2100"
          />

          <Input
            type="number"
            label="Award ID (Optional)"
            placeholder="Auto-generated if left empty"
            value={formData.award_id}
            onChange={e => setFormData({ ...formData, award_id: e.target.value })}
          />

          <div className="form-actions">
            <Button variant="secondary" onClick={close}>
              Cancel
            </Button>
            <Button variant="primary" onClick={handleAdd}>
              Add Award
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  );
}

export default Awards;
