import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { analyticsService, topicService } from '../services';
import { Spinner, RiskBadge, PriorityBar, Card } from '../components/common/UIComponents';
import {
  BookOpen, RotateCcw, Target, AlertTriangle, TrendingUp,
  Flame, Brain, ChevronRight, Play, Star
} from 'lucide-react';
import {
  AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip,
  ResponsiveContainer, PieChart, Pie, Cell, Legend
} from 'recharts';

const RISK_COLORS = { LOW: '#10b981', MEDIUM: '#f59e0b', HIGH: '#ef4444', VERY_HIGH: '#dc2626' };

export default function DashboardPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [stats, setStats] = useState(null);
  const [todayQueue, setTodayQueue] = useState([]);
  const [quizPerf, setQuizPerf] = useState([]);
  const [weakTopics, setWeakTopics] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([
      analyticsService.getDashboardStats(),
      topicService.getTodayQueue(),
      analyticsService.getQuizPerformance(14),
      analyticsService.getWeakTopics(),
    ]).then(([s, q, qp, wt]) => {
      setStats(s.data);
      setTodayQueue(q.data);
      setQuizPerf(qp.data);
      setWeakTopics(wt.data);
    }).catch(console.error)
      .finally(() => setLoading(false));
  }, []);

  const hour = new Date().getHours();
  const greeting = hour < 12 ? 'Good Morning' : hour < 17 ? 'Good Afternoon' : 'Good Evening';

  if (loading) return <Spinner />;

  const riskData = [
    { name: 'High Risk', value: stats?.highRiskTopics || 0, color: '#ef4444' },
    { name: 'Medium Risk', value: stats?.mediumRiskTopics || 0, color: '#f59e0b' },
    { name: 'Low Risk', value: stats?.lowRiskTopics || 0, color: '#10b981' },
  ].filter(d => d.value > 0);

  return (
    <div className="animate-fade-in">
      {/* Hero Banner */}
      <div className="dashboard-hero">
        <div>
          <div className="dashboard-greeting">
            {greeting}, {user?.fullName?.split(' ')[0] || user?.username}! 👋
          </div>
          <div className="dashboard-date">
            {new Date().toLocaleDateString('en-US', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' })}
          </div>
          {stats?.currentStreak > 0 && (
            <div style={{ marginTop: 10, display: 'flex', alignItems: 'center', gap: 6 }}>
              <Flame size={16} color="#f59e0b" />
              <span style={{ fontSize: 13, color: 'var(--warning)', fontWeight: 600 }}>
                {stats.currentStreak}-day streak!
              </span>
            </div>
          )}
        </div>
        {todayQueue.length > 0 && (
          <button
            className="btn btn-primary btn-lg"
            onClick={() => navigate('/revision/today')}
          >
            <Play size={18} />
            Start Today's Revision ({todayQueue.length} topics)
          </button>
        )}
      </div>

      {/* Stats Grid */}
      <div className="stat-grid">
        {[
          { label: 'Total Topics', value: stats?.totalTopics || 0, icon: BookOpen, color: 'var(--accent)', bg: 'rgba(108,99,255,0.12)' },
          { label: 'Learned', value: stats?.learnedTopics || 0, icon: Target, color: 'var(--success)', bg: 'var(--success-bg)' },
          { label: 'At Risk', value: stats?.atRiskTopics || 0, icon: AlertTriangle, color: 'var(--danger)', bg: 'var(--danger-bg)' },
          { label: 'Avg Quiz Score', value: `${stats?.averageQuizScore || 0}%`, icon: TrendingUp, color: 'var(--accent-2)', bg: 'rgba(0,212,255,0.1)' },
          { label: 'Total Revisions', value: stats?.totalRevisions || 0, icon: RotateCcw, color: '#8b5cf6', bg: 'rgba(139,92,246,0.12)' },
          { label: 'Streak (Days)', value: stats?.currentStreak || 0, icon: Flame, color: 'var(--warning)', bg: 'var(--warning-bg)' },
        ].map(s => (
          <div key={s.label} className="stat-card">
            <div className="stat-icon" style={{ background: s.bg }}>
              <s.icon size={20} color={s.color} />
            </div>
            <div className="stat-value" style={{ color: s.color }}>{s.value}</div>
            <div className="stat-label">{s.label}</div>
          </div>
        ))}
      </div>

      {/* Main Content Grid */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 360px', gap: 20 }}>
        {/* Today's Revision Queue */}
        <Card
          title="📚 Today's Revision Queue"
          subtitle="Topics ranked by forgetting risk and priority score"
          action={
            <button className="btn btn-primary btn-sm" onClick={() => navigate('/revision/today')}>
              View All <ChevronRight size={14} />
            </button>
          }
        >
          {todayQueue.length === 0 ? (
            <div className="empty-state">
              <div className="empty-state-icon">🎉</div>
              <div className="empty-state-title">All caught up!</div>
              <div className="empty-state-text">No topics due for revision today. Add more topics to get started.</div>
            </div>
          ) : (
            todayQueue.slice(0, 5).map((item, i) => (
              <div
                key={item.topicId}
                className="revision-queue-item"
                onClick={() => navigate(`/revision/${item.topicId}`)}
                style={{ cursor: 'pointer' }}
              >
                <div className="queue-rank">{i + 1}</div>
                <div className="queue-info">
                  <div className="queue-name">{item.topicName}</div>
                  <div className="queue-reason">{item.reason}</div>
                  <div style={{ display: 'flex', gap: 6, marginTop: 4 }}>
                    <RiskBadge risk={item.riskLevel} />
                    <span style={{ fontSize: 11, color: 'var(--text-muted)' }}>{item.subject}</span>
                  </div>
                </div>
                <div className="queue-score">
                  <div className={`queue-priority ${item.riskLevel === 'HIGH' || item.riskLevel === 'VERY_HIGH' ? 'high' : item.riskLevel === 'MEDIUM' ? 'medium' : 'low'}`}>
                    {Math.round(item.priorityScore)}
                  </div>
                  <div style={{ fontSize: 10, color: 'var(--text-muted)', marginTop: 2 }}>PRIORITY</div>
                </div>
              </div>
            ))
          )}
        </Card>

        {/* Right Column */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          {/* Risk Distribution */}
          {riskData.length > 0 && (
            <Card title="Memory Risk">
              <ResponsiveContainer width="100%" height={160}>
                <PieChart>
                  <Pie data={riskData} cx="50%" cy="50%" innerRadius={45} outerRadius={70} paddingAngle={3} dataKey="value">
                    {riskData.map((entry, i) => (
                      <Cell key={i} fill={entry.color} />
                    ))}
                  </Pie>
                  <Tooltip
                    contentStyle={{ background: 'var(--bg-card)', border: '1px solid var(--border)', borderRadius: 8 }}
                  />
                  <Legend iconType="circle" iconSize={8} />
                </PieChart>
              </ResponsiveContainer>
            </Card>
          )}

          {/* Weak Topics */}
          {weakTopics.length > 0 && (
            <Card title="⚠️ Weak Areas" action={
              <button className="btn btn-sm btn-secondary" onClick={() => navigate('/analytics')}>
                View all
              </button>
            }>
              {weakTopics.slice(0, 3).map(wt => (
                <div key={wt.topicId} style={{
                  display: 'flex', alignItems: 'center', justifyContent: 'space-between',
                  padding: '10px 0', borderBottom: '1px solid var(--border)'
                }}>
                  <div>
                    <div style={{ fontWeight: 600, fontSize: 13 }}>{wt.topicName}</div>
                    <div style={{ fontSize: 11, color: 'var(--text-muted)' }}>{wt.subject} · {wt.mistakeCount} mistakes</div>
                  </div>
                  <span style={{ color: 'var(--danger)', fontWeight: 700, fontSize: 14 }}>
                    {Math.round(wt.averageAccuracy || 0)}%
                  </span>
                </div>
              ))}
            </Card>
          )}
        </div>
      </div>

      {/* Quiz Performance Chart */}
      {quizPerf.length > 0 && (
        <div style={{ marginTop: 20 }}>
          <Card title="📈 Quiz Performance (Last 14 Days)">
            <ResponsiveContainer width="100%" height={220}>
              <AreaChart data={quizPerf} margin={{ top: 5, right: 10, left: -10, bottom: 0 }}>
                <defs>
                  <linearGradient id="perfGrad" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#6c63ff" stopOpacity={0.4} />
                    <stop offset="95%" stopColor="#6c63ff" stopOpacity={0} />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" />
                <XAxis dataKey="date" tick={{ fill: 'var(--text-muted)', fontSize: 11 }} />
                <YAxis domain={[0, 100]} tick={{ fill: 'var(--text-muted)', fontSize: 11 }} unit="%" />
                <Tooltip
                  contentStyle={{ background: 'var(--bg-card)', border: '1px solid var(--border)', borderRadius: 8 }}
                  formatter={(v) => [`${v}%`, 'Avg Score']}
                />
                <Area type="monotone" dataKey="averageScore" stroke="var(--accent)" strokeWidth={2}
                  fill="url(#perfGrad)" dot={{ fill: 'var(--accent)', r: 4 }} />
              </AreaChart>
            </ResponsiveContainer>
          </Card>
        </div>
      )}
    </div>
  );
}
