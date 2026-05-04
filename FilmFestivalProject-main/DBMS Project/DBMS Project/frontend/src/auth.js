const AUTH_KEY = 'ff_logged_in';

export function isAuthenticated() {
  return !!localStorage.getItem(AUTH_KEY);
}

export function login(username, password) {
  if (username === 'master' && password === 'film') {
    localStorage.setItem(AUTH_KEY, '1');
    return true;
  }
  return false;
}

export function logout() {
  localStorage.removeItem(AUTH_KEY);
}

export default { isAuthenticated, login, logout };
