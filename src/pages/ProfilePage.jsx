import { useAuth } from '../context/AuthContext';
import { Card } from '../components/common/UIComponents';
import { User, Mail, Calendar, Flame, Award } from 'lucide-react';

export default function ProfilePage() {
  const { user } = useAuth();

  return (
    <div className="animate-fade-in">
      <div className="page-header">
        <h1 className="page-title">👤 Profile</h1>
      </div>

      <div style={{ maxWidth: 560 }}>
        <Card>
          <div style={{ display: 'flex', alignItems: 'center', gap: 20, marginBottom: 28 }}>
            <div style={{
              width: 72, height: 72, borderRadius: '50%',
              background: 'linear-gradient(135deg, var(--accent), var(--accent-2))',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              fontSize: 28, fontWeight: 800, color: 'white'
            }}>
              {(user?.fullName || user?.username || 'RF').slice(0, 2).toUpperCase()}
            </div>
            <div>
              <h2 style={{ fontSize: 22, fontWeight: 800 }}>{user?.fullName || user?.username}</h2>
              <p style={{ color: 'var(--text-muted)', fontSize: 14 }}>@{user?.username}</p>
            </div>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
            {[
              { icon: User,     label: 'Username', value: user?.username },
              { icon: Mail,     label: 'Email',    value: user?.email },
              { icon: Flame,    label: 'Current Streak', value: `${user?.currentStreak || 0} days` },
              { icon: Award,    label: 'Longest Streak', value: `${user?.longestStreak || 0} days` },
            ].map(item => (
              <div key={item.label} style={{
                display: 'flex', alignItems: 'center', gap: 14,
                padding: '14px 16px', background: 'var(--bg-secondary)',
                borderRadius: 10, border: '1px solid var(--border)'
              }}>
                <item.icon size={18} color="var(--accent)" />
                <div>
                  <div style={{ fontSize: 11, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
                    {item.label}
                  </div>
                  <div style={{ fontWeight: 600, fontSize: 15 }}>{item.value}</div>
                </div>
              </div>
            ))}
          </div>

          <div style={{
            marginTop: 24, padding: 16, background: 'rgba(108,99,255,0.08)',
            borderRadius: 12, border: '1px solid rgba(108,99,255,0.2)',
            fontSize: 13, color: 'var(--text-secondary)', lineHeight: 1.6
          }}>
            <strong style={{ color: 'var(--accent)' }}>🧠 RecallFlow</strong> uses your quiz scores,
            confidence ratings, and revision history to calculate personalized forgetting risk scores
            and schedule your next revision at exactly the right time.
          </div>
        </Card>
      </div>
    </div>
  );
}
