import './Layout.css';
import { Sidebar } from './Sidebar';
import { useLocation } from 'react-router-dom';
import { isAuthenticated } from '../../auth';

export const Layout = ({ children }) => {
  const loc = useLocation();
  const showSidebar = isAuthenticated() && loc.pathname !== '/login';

  return (
    <div className="app-layout">
      {showSidebar && <Sidebar />}
      <div className={`app-container ${showSidebar ? '' : 'no-sidebar'}`}>
        {children}
      </div>
    </div>
  );
};
