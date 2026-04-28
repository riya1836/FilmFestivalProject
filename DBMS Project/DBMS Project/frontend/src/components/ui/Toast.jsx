import { useState, useEffect, useCallback } from 'react';
import './Toast.css';

const toastContainer = {
  toasts: [],
  listeners: []
};

export const showToast = (message, type = 'info', duration = 4000) => {
  const id = Date.now();
  const toast = { id, message, type, duration };
  
  toastContainer.toasts.push(toast);
  toastContainer.listeners.forEach(listener => listener([...toastContainer.toasts]));

  if (duration > 0) {
    setTimeout(() => {
      removeToast(id);
    }, duration);
  }

  return id;
};

export const removeToast = (id) => {
  toastContainer.toasts = toastContainer.toasts.filter(t => t.id !== id);
  toastContainer.listeners.forEach(listener => listener([...toastContainer.toasts]));
};

export const ToastContainer = () => {
  const [toasts, setToasts] = useState([]);

  useEffect(() => {
    const listener = (updatedToasts) => {
      setToasts(updatedToasts);
    };

    toastContainer.listeners.push(listener);

    return () => {
      toastContainer.listeners = toastContainer.listeners.filter(l => l !== listener);
    };
  }, []);

  return (
    <div className="toast-container">
      {toasts.map(toast => (
        <Toast
          key={toast.id}
          {...toast}
          onClose={() => removeToast(toast.id)}
        />
      ))}
    </div>
  );
};

const Toast = ({ id, message, type, onClose }) => {
  useEffect(() => {
    const timer = setTimeout(onClose, 4000);
    return () => clearTimeout(timer);
  }, [id, onClose]);

  const icons = {
    success: '✓',
    error: '✕',
    info: 'ℹ',
    warning: '⚠'
  };

  return (
    <div className={`toast toast-${type}`}>
      <span className="toast-icon">{icons[type]}</span>
      <span className="toast-message">{message}</span>
      <button className="toast-close" onClick={onClose}>✕</button>
    </div>
  );
};
