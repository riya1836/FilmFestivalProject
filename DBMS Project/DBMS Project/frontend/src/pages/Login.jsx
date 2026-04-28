import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { login, isAuthenticated } from '../auth';
import './Login.css';
import { Button } from '../components/ui/Button';

function Login() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const navigate = useNavigate();

  if (isAuthenticated()) {
    navigate('/');
  }

  const handleSubmit = (e) => {
    e.preventDefault();
    if (login(username.trim(), password)) {
      navigate('/');
      window.location.reload();
    } else {
      setError('Invalid credentials');
    }
  };

  return (
    <div className="login-page">
      <div className="login-card">
        <h2>Welcome to Film Festival</h2>
        <p>Sign in to manage the festival</p>
        <form onSubmit={handleSubmit} className="login-form">
          <input placeholder="Username" value={username} onChange={e => setUsername(e.target.value)} />
          <input placeholder="Password" type="password" value={password} onChange={e => setPassword(e.target.value)} />
          {error && <div className="error">{error}</div>}
          <Button type="submit" variant="primary">Sign in</Button>
        </form>
        <div className="demo-creds">Try username <b>master</b> and password <b>film</b></div>
      </div>
    </div>
  );
}

export default Login;
