import React, { useContext } from 'react';
import { Navigate } from 'react-router-dom';
import { AuthContext } from '../../contexts/AuthContext';

/**
 * ProtectedRoute - Requires authentication
 */
export const ProtectedRoute = ({ children }) => {
  const { user, loading } = useContext(AuthContext);

  if (loading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>
        <div>Loading...</div>
      </div>
    );
  }

  if (!user) {
    return <Navigate to="/login" replace />;
  }

  return children;
};

/**
 * RoleBasedRoute - Requires specific role(s)
 */
export const RoleBasedRoute = ({ children, requiredRoles = [] }) => {
  const { user, loading } = useContext(AuthContext);

  if (loading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>
        <div>Loading...</div>
      </div>
    );
  }

  if (!user) {
    return <Navigate to="/login" replace />;
  }

  // Check if user has required role
  if (requiredRoles.length > 0 && !requiredRoles.includes(user.role)) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>
        <div>
          <h2>Access Denied</h2>
          <p>You do not have permission to access this page.</p>
          <p>Required role: {requiredRoles.join(' or ')}</p>
          <p>Your role: {user.role}</p>
        </div>
      </div>
    );
  }

  return children;
};

/**
 * PrivateRoute - Alias for ProtectedRoute
 */
export const PrivateRoute = ProtectedRoute;
