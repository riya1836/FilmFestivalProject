const API_BASE = import.meta.env.DEV ? 'http://localhost:8080/api' : '/api';

// Get auth token from localStorage
function getAuthToken() {
  return localStorage.getItem('authToken');
}

export async function request(path, options = {}) {
  console.log('[API Request]', {
    method: options.method || 'GET',
    path,
    body: options.body ? JSON.parse(options.body) : null
  });

  const headers = {
    'Content-Type': 'application/json',
    ...options.headers,
  };

  // Add auth token if available
  const token = getAuthToken();
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const response = await fetch(`${API_BASE}${path}`, {
    headers,
    ...options,
  });

  if (!response.ok) {
    const text = await response.text();
    let errorMessage = `${response.status} ${response.statusText}`;

    if (text) {
      try {
        const parsed = JSON.parse(text);
        errorMessage = parsed.error || parsed.message || text;
      } catch {
        errorMessage = text;
      }
    }

    console.error('[API Error Response]', {
      status: response.status,
      statusText: response.statusText,
      body: text
    });
    throw new Error(errorMessage);
  }

  if (response.status === 204 || response.status === 205) {
    return null;
  }

  return response.json();
}

export const fetchApi = request;

// ==================== AUTH ENDPOINTS ====================
export const register = (name, email, password, role = 'USER') =>
  request('/auth/register', {
    method: 'POST',
    body: JSON.stringify({ name, email, password, role }),
  });

export const login = (email, password) =>
  request('/auth/login', {
    method: 'POST',
    body: JSON.stringify({ email, password }),
  });

export const getCurrentUser = () => request('/auth/me');

// ==================== FILM ENDPOINTS ====================
export const getFilms = (genre, language, sortBy) => {
  let path = '/public/films';
  const params = [];
  if (genre) params.push(`genre=${genre}`);
  if (language) params.push(`language=${language}`);
  if (sortBy) params.push(`sort=${sortBy}`);
  if (params.length > 0) path += '?' + params.join('&');
  return request(path);
};

export const getFilmById = (id) => request(`/public/films/${id}`);
export const createFilm = (film) => request('/admin/films', { method: 'POST', body: JSON.stringify(film) });
export const updateFilm = (id, film) => request(`/admin/films/${id}`, { method: 'PUT', body: JSON.stringify(film) });
export const deleteFilm = (id) => request(`/admin/films/${id}`, { method: 'DELETE' });
export const getTopRatedFilms = () => request('/public/films/leaderboard');

// ==================== ADMIN ENDPOINTS ====================
export const assignJury = (juryId, filmIds) =>
  request('/admin/assign-jury', {
    method: 'POST',
    body: JSON.stringify({ jury_id: juryId, film_ids: filmIds }),
  });

export const getJuryAssignments = () => request('/admin/jury-assignments');
export const getAllEvaluations = () => request('/admin/evaluations');
export const getAwardEligibleFilms = () => request('/admin/award-eligible');

// ==================== JURY ENDPOINTS ====================
export const getAssignedFilms = () => request('/jury/assigned-films');
export const submitEvaluation = (filmId, score, remarks) =>
  request('/jury/evaluate', {
    method: 'POST',
    body: JSON.stringify({ film_id: filmId, score, remarks }),
  });

export const getMyEvaluations = () => request('/jury/evaluations');

// ==================== USER ENDPOINTS ====================
export const getMyBookings = () => request('/user/my-bookings');
export const bookTicket = (screeningId, seatCount) =>
  request('/user/book-ticket', {
    method: 'POST',
    body: JSON.stringify({ screening_id: screeningId, seat_count: seatCount }),
  });

// ==================== LEGACY ENDPOINTS (for backwards compatibility) ====================
export const getAttendees = () => request('/admin/attendees');
export const createAttendee = (attendee) => request('/admin/attendees', { method: 'POST', body: JSON.stringify(attendee) });
export const updateAttendee = (id, attendee) => request(`/admin/attendees/${id}`, { method: 'PUT', body: JSON.stringify(attendee) });
export const deleteAttendee = (id) => request(`/admin/attendees/${id}`, { method: 'DELETE' });

export const getAwards = () => request('/admin/awards');
export const createAward = (award) => request('/admin/awards', { method: 'POST', body: JSON.stringify(award) });
export const updateAward = (id, award) => request(`/admin/awards/${id}`, { method: 'PUT', body: JSON.stringify(award) });
export const deleteAward = (id) => request(`/admin/awards/${id}`, { method: 'DELETE' });

export const getVenues = () => request('/admin/venues');
export const createVenue = (venue) => request('/admin/venues', { method: 'POST', body: JSON.stringify(venue) });
export const updateVenue = (id, venue) => request(`/admin/venues/${id}`, { method: 'PUT', body: JSON.stringify(venue) });
export const deleteVenue = (id) => request(`/admin/venues/${id}`, { method: 'DELETE' });

export const getFilmCrew = () => request('/admin/filmcrew');
export const createFilmCrew = (crew) => request('/admin/filmcrew', { method: 'POST', body: JSON.stringify(crew) });
export const updateFilmCrew = (id, crew) => request(`/admin/filmcrew/${id}`, { method: 'PUT', body: JSON.stringify(crew) });
export const deleteFilmCrew = (id) => request(`/admin/filmcrew/${id}`, { method: 'DELETE' });

export const getScreenings = () => request('/admin/screenings');
export const createScreening = (screening) => request('/admin/screenings', { method: 'POST', body: JSON.stringify(screening) });
export const updateScreening = (id, screening) => request(`/admin/screenings/${id}`, { method: 'PUT', body: JSON.stringify(screening) });
export const deleteScreening = (id) => request(`/admin/screenings/${id}`, { method: 'DELETE' });

export const getTickets = () => request('/admin/tickets');
export const createTicket = (ticket) => request('/admin/tickets', { method: 'POST', body: JSON.stringify(ticket) });
export const updateTicket = (id, ticket) => request(`/admin/tickets/${id}`, { method: 'PUT', body: JSON.stringify(ticket) });
export const deleteTicket = (id) => request(`/admin/tickets/${id}`, { method: 'DELETE' });
