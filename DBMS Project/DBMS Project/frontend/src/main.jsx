import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './styles/global.css'
import './styles/pages.css'
import App from './App.jsx'

function renderErrorOverlay(err) {
  try {
    const root = document.getElementById('root');
    if (!root) return;
    root.innerHTML = '';
    const pre = document.createElement('pre');
    pre.style.whiteSpace = 'pre-wrap';
    pre.style.background = 'rgba(0,0,0,0.85)';
    pre.style.color = '#ff6b6b';
    pre.style.padding = '20px';
    pre.style.borderRadius = '8px';
    pre.style.margin = '24px';
    pre.textContent = 'Application error:\n' + (err && err.stack ? err.stack : String(err));
    root.appendChild(pre);
  } catch (e) {
    // ignore
  }
}

window.addEventListener('error', (ev) => {
  console.error('Global error captured:', ev.error || ev.message || ev);
  renderErrorOverlay(ev.error || ev.message || ev);
});

window.addEventListener('unhandledrejection', (ev) => {
  console.error('Unhandled rejection:', ev.reason || ev);
  renderErrorOverlay(ev.reason || ev);
});

try {
  createRoot(document.getElementById('root')).render(
    <StrictMode>
      <App />
    </StrictMode>
  );
} catch (err) {
  console.error('Render error:', err);
  renderErrorOverlay(err);
}
