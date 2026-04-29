import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './contexts/AuthContext';
import { ProtectedRoute, RoleBasedRoute } from './components/shared/ProtectedRoute';
import { LoginForm } from './components/auth/LoginForm';
import { RegisterForm } from './components/auth/RegisterForm';
import { AdminDashboard } from './components/dashboards/AdminDashboard';
import { UserDashboard } from './components/dashboards/UserDashboard';
import { JuryDashboard } from './components/dashboards/JuryDashboard';
import { Layout } from './components/layout/Layout';
import { ToastContainer } from './components/ui/Toast';
import Home from './pages/Home';
import Films from './pages/Films';
import Attendees from './pages/Attendees';
import Awards from './pages/Awards';
import FilmCrew from './pages/FilmCrew';
import Screenings from './pages/Screenings';
import Tickets from './pages/Tickets';
import Venues from './pages/Venues';
import Settings from './pages/Settings';

function App() {
  return (
    <Router>
      <AuthProvider>
        <Routes>
          {/* Public Auth Routes */}
          <Route path="/login" element={<LoginForm />} />
          <Route path="/register" element={<RegisterForm />} />

          {/* Role-Based Dashboards */}
          <Route
            path="/admin-dashboard"
            element={
              <RoleBasedRoute requiredRoles={['ADMIN']}>
                <Layout>
                  <AdminDashboard />
                </Layout>
              </RoleBasedRoute>
            }
          />

          <Route
            path="/user-dashboard"
            element={
              <RoleBasedRoute requiredRoles={['USER']}>
                <Layout>
                  <UserDashboard />
                </Layout>
              </RoleBasedRoute>
            }
          />

          <Route
            path="/jury-dashboard"
            element={
              <RoleBasedRoute requiredRoles={['JURY']}>
                <Layout>
                  <JuryDashboard />
                </Layout>
              </RoleBasedRoute>
            }
          />

          {/* Protected Legacy Routes */}
          <Route
            path="/*"
            element={
              <ProtectedRoute>
                <Layout>
                  <Routes>
                    <Route path="/settings" element={<Settings />} />
                    <Route path="/" element={<Home />} />
                    <Route path="/films" element={<Films />} />
                    
                    {/* Admin Only Management Routes */}
                    <Route path="/attendees" element={<RoleBasedRoute requiredRoles={['ADMIN']}><Attendees /></RoleBasedRoute>} />
                    <Route path="/awards" element={<RoleBasedRoute requiredRoles={['ADMIN']}><Awards /></RoleBasedRoute>} />
                    <Route path="/filmcrew" element={<RoleBasedRoute requiredRoles={['ADMIN']}><FilmCrew /></RoleBasedRoute>} />
                    <Route path="/screenings" element={<RoleBasedRoute requiredRoles={['ADMIN']}><Screenings /></RoleBasedRoute>} />
                    <Route path="/tickets" element={<RoleBasedRoute requiredRoles={['ADMIN']}><Tickets /></RoleBasedRoute>} />
                    <Route path="/venues" element={<RoleBasedRoute requiredRoles={['ADMIN']}><Venues /></RoleBasedRoute>} />
                    
                    <Route path="*" element={<Navigate to="/" replace />} />
                  </Routes>
                </Layout>
              </ProtectedRoute>
            }
          />

          {/* Fallback */}
          <Route path="*" element={<Navigate to="/login" replace />} />
        </Routes>

        <ToastContainer />
      </AuthProvider>
    </Router>
  );
}

export default App;