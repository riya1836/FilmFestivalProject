import './Layout.css';
import { Sidebar } from './Sidebar';
import { useLocation } from 'react-router-dom';
import { useContext } from 'react';
import { AuthContext } from '../../contexts/AuthContext';

export const Layout = ({ children }) => {
  const loc = useLocation();
  const { user } = useContext(AuthContext);
  const showSidebar = !!user && loc.pathname !== '/login';

  return (
    <div className="app-layout">
      {showSidebar && <Sidebar />}
      <div className={`app-container ${showSidebar ? '' : 'no-sidebar'}`}>
        {children}
      </div>
    </div>
  );
};
