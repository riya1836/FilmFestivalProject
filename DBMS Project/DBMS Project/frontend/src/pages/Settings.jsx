import { useState } from 'react';
import { logout } from '../auth';
import './Settings.css';
import { Button } from '../components/ui/Button';

function Settings() {
  const [dark, setDark] = useState(document.body.classList.contains('dark'));

  const toggleTheme = () => {
    document.body.classList.toggle('dark');
    setDark(document.body.classList.contains('dark'));
  };

  const handleLogout = () => {
    logout();
    window.location.href = '/login';
  };

  return (
    <div className="settings-page">
      <h2>Settings</h2>
      <div className="settings-item">
        <label>Dark Theme</label>
        <input type="checkbox" checked={dark} onChange={toggleTheme} />
      </div>
      <div className="settings-actions">
        <Button onClick={handleLogout} variant="danger">Log out</Button>
      </div>
    </div>
  );
}

export default Settings;
