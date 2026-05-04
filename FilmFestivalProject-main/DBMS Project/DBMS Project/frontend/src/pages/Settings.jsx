import { useState, useContext } from 'react';
import { useNavigate } from 'react-router-dom';
import { AuthContext } from '../contexts/AuthContext';
import './Settings.css';
import { Button } from '../components/ui/Button';

function Settings() {
  const [dark, setDark] = useState(document.body.classList.contains('dark'));
  const navigate = useNavigate();
  const { logout } = useContext(AuthContext);

  const toggleTheme = () => {
    document.body.classList.toggle('dark');
    setDark(document.body.classList.contains('dark'));
  };

  const handleLogout = () => {
    logout();
    navigate('/login');
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
