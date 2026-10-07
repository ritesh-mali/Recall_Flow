import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import {
  LayoutDashboard, BookOpen, RotateCcw, Brain, BarChart3,
  AlertTriangle, Lightbulb, LogOut, Zap, User
} from 'lucide-react';

const NAV_ITEMS = [
  { label: 'Overview', items: [
    { to: '/dashboard',      icon: LayoutDashboard, label: 'Dashboard' },
    { to: '/topics',         icon: BookOpen,         label: 'Topics' },
    { to: '/revision/today', icon: RotateCcw,        label: "Today's Revision" },
  ]},
  { label: 'Insights', items: [
    { to: '/analytics',     icon: BarChart3,   label: 'Analytics' },
    { to: '/mistakes',      icon: AlertTriangle, label: 'Mistakes' },
    { to: '/recommendations', icon: Lightbulb, label: 'Recommendations' },
  ]},
  { label: 'Account', items: [
    { to: '/profile', icon: User, label: 'Profile' },
  ]},
];

export default function Sidebar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const initials = user?.fullName
    ? user.fullName.split(' ').map(n => n[0]).join('').slice(0,2).toUpperCase()
    : user?.username?.slice(0,2).toUpperCase() || 'RF';

  return (
    <aside className="sidebar">
      <div className="sidebar-logo">
        <div className="sidebar-logo-icon">
          <Zap size={20} color="white" />
        </div>
        <span className="sidebar-logo-text">RecallFlow</span>
      </div>

      <nav className="sidebar-nav">
        {NAV_ITEMS.map(section => (
          <div key={section.label}>
            <div className="nav-section-label">{section.label}</div>
            {section.items.map(item => (
              <NavLink
                key={item.to}
                to={item.to}
                className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}
              >
                <item.icon size={18} className="nav-icon" />
                {item.label}
              </NavLink>
            ))}
          </div>
        ))}
      </nav>

      <div className="sidebar-footer">
        <div className="user-info" style={{ marginBottom: 8 }}>
          <div className="user-avatar">{initials}</div>
          <div style={{ flex: 1, minWidth: 0 }}>
            <div className="user-name truncate">{user?.fullName || user?.username}</div>
            <div className="user-email truncate">{user?.email}</div>
          </div>
        </div>
        <button className="btn btn-secondary w-full btn-sm" onClick={handleLogout}>
          <LogOut size={14} />
          Logout
        </button>
      </div>
    </aside>
  );
}
