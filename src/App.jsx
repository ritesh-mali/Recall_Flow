import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { Toaster } from 'react-hot-toast';
import { AuthProvider, useAuth } from './context/AuthContext';
import Sidebar from './components/common/Sidebar';
import { Spinner } from './components/common/UIComponents';

// Pages
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import DashboardPage from './pages/DashboardPage';
import TopicsPage from './pages/TopicsPage';
import TodayRevisionPage from './pages/TodayRevisionPage';
import RevisionSessionPage from './pages/RevisionSessionPage';
import AnalyticsPage from './pages/AnalyticsPage';
import MistakesPage from './pages/MistakesPage';
import RecommendationsPage from './pages/RecommendationsPage';
import ProfilePage from './pages/ProfilePage';

function ProtectedRoute({ children }) {
  const { user, loading } = useAuth();
  if (loading) return <Spinner />;
  if (!user) return <Navigate to="/login" replace />;
  return children;
}

function AppLayout({ children }) {
  return (
    <div className="app-layout">
      <Sidebar />
      <main className="main-content">{children}</main>
    </div>
  );
}

function AppRoutes() {
  const { user, loading } = useAuth();
  if (loading) return <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '100vh' }}><Spinner /></div>;

  return (
    <Routes>
      <Route path="/login" element={!user ? <LoginPage /> : <Navigate to="/dashboard" />} />
      <Route path="/register" element={!user ? <RegisterPage /> : <Navigate to="/dashboard" />} />

      <Route path="/" element={<Navigate to="/dashboard" />} />

      <Route path="/dashboard" element={
        <ProtectedRoute><AppLayout><DashboardPage /></AppLayout></ProtectedRoute>
      } />
      <Route path="/topics" element={
        <ProtectedRoute><AppLayout><TopicsPage /></AppLayout></ProtectedRoute>
      } />
      <Route path="/revision/today" element={
        <ProtectedRoute><AppLayout><TodayRevisionPage /></AppLayout></ProtectedRoute>
      } />
      <Route path="/revision/:topicId" element={
        <ProtectedRoute><AppLayout><RevisionSessionPage /></AppLayout></ProtectedRoute>
      } />
      <Route path="/analytics" element={
        <ProtectedRoute><AppLayout><AnalyticsPage /></AppLayout></ProtectedRoute>
      } />
      <Route path="/mistakes" element={
        <ProtectedRoute><AppLayout><MistakesPage /></AppLayout></ProtectedRoute>
      } />
      <Route path="/recommendations" element={
        <ProtectedRoute><AppLayout><RecommendationsPage /></AppLayout></ProtectedRoute>
      } />
      <Route path="/profile" element={
        <ProtectedRoute><AppLayout><ProfilePage /></AppLayout></ProtectedRoute>
      } />

      <Route path="*" element={<Navigate to="/dashboard" />} />
    </Routes>
  );
}

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Toaster
          position="top-right"
          toastOptions={{
            style: {
              background: 'var(--bg-card)',
              color: 'var(--text-primary)',
              border: '1px solid var(--border)',
              borderRadius: '10px',
            },
          }}
        />
        <AppRoutes />
      </AuthProvider>
    </BrowserRouter>
  );
}
