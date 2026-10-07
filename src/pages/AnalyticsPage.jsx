import { useState, useEffect } from 'react';
import { analyticsService } from '../services';
import { Spinner, Card, RiskBadge } from '../components/common/UIComponents';
import {
  AreaChart, Area, BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip,
  ResponsiveContainer, RadarChart, PolarGrid, PolarAngleAxis, Radar,
  ScatterChart, Scatter, ZAxis, PieChart, Pie, Cell, Legend
} from 'recharts';
import { TrendingUp, Target, AlertTriangle, BookOpen, RotateCcw, Brain } from 'lucide-react';

const COLORS = ['#6c63ff', '#00d4ff', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#ff6b9d'];

export default function AnalyticsPage() {
  const [stats, setStats] = useState(null);
  const [subjects, setSubjects] = useState([]);
  const [quizPerf, setQuizPerf] = useState([]);
  const [revActivity, setRevActivity] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([
      analyticsService.getDashboardStats(),
      analyticsService.getSubjectPerformance(),
      analyticsService.getQuizPerformance(30),
      analyticsService.getRevisionActivity(30),
    ]).then(([s, sub, qp, ra]) => {
      setStats(s.data);
      setSubjects(sub.data);
      setQuizPerf(qp.data);
      setRevActivity(ra.data);
    }).catch(console.error)
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <Spinner />;

  const riskData = [
    { name: 'High Risk', value: stats?.highRiskTopics || 0 },
    { name: 'Medium Risk', value: stats?.mediumRiskTopics || 0 },
    { name: 'Low Risk', value: stats?.lowRiskTopics || 0 },
  ];

  return (
    <div className="animate-fade-in">
      <div className="page-header">
        <h1 className="page-title">📊 Analytics</h1>
        <p className="page-subtitle">Comprehensive performance overview for the last 30 days</p>
      </div>

      {/* Overview Stats */}
      <div className="stat-grid" style={{ gridTemplateColumns: 'repeat(auto-fit, minmax(160px, 1fr))' }}>
        {[
          { label: 'Total Topics', value: stats?.totalTopics || 0, icon: BookOpen, color: 'var(--accent)' },
          { label: 'Avg Quiz Score', value: `${stats?.averageQuizScore || 0}%`, icon: Target, color: 'var(--success)' },
          { label: 'Total Revisions', value: stats?.totalRevisions || 0, icon: RotateCcw, color: '#8b5cf6' },
          { label: 'Total Mistakes', value: stats?.totalMistakes || 0, icon: AlertTriangle, color: 'var(--danger)' },
          { label: 'Quiz Attempts', value: stats?.totalQuizAttempts || 0, icon: Brain, color: 'var(--accent-2)' },
          { label: 'Current Streak', value: `${stats?.currentStreak || 0}d`, icon: TrendingUp, color: 'var(--warning)' },
        ].map(s => (
          <div key={s.label} className="stat-card">
            <div className="stat-icon" style={{ background: `${s.color}20` }}>
              <s.icon size={18} color={s.color} />
            </div>
            <div className="stat-value" style={{ fontSize: 24, color: s.color }}>{s.value}</div>
            <div className="stat-label">{s.label}</div>
          </div>
        ))}
      </div>

      {/* Charts Row 1 */}
      <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: 20, marginBottom: 20 }}>
        <Card title="Quiz Performance Over Time" subtitle="Average score per day">
          <ResponsiveContainer width="100%" height={240}>
            <AreaChart data={quizPerf}>
              <defs>
                <linearGradient id="quizGrad" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor="#6c63ff" stopOpacity={0.4} />
                  <stop offset="95%" stopColor="#6c63ff" stopOpacity={0} />
                </linearGradient>
              </defs>
              <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" />
              <XAxis dataKey="date" tick={{ fill: 'var(--text-muted)', fontSize: 11 }} />
              <YAxis domain={[0, 100]} tick={{ fill: 'var(--text-muted)', fontSize: 11 }} unit="%" />
              <Tooltip contentStyle={{ background: 'var(--bg-card)', border: '1px solid var(--border)', borderRadius: 8 }}
                formatter={v => [`${v}%`, 'Avg Score']} />
              <Area type="monotone" dataKey="averageScore" stroke="#6c63ff" strokeWidth={2}
                fill="url(#quizGrad)" dot={{ fill: '#6c63ff', r: 4 }} />
            </AreaChart>
          </ResponsiveContainer>
        </Card>

        <Card title="Risk Distribution">
          <ResponsiveContainer width="100%" height={240}>
            <PieChart>
              <Pie data={riskData} cx="50%" cy="50%" innerRadius={50} outerRadius={85} paddingAngle={3} dataKey="value"
                label={({ name, value }) => value > 0 ? `${value}` : ''}>
                {riskData.map((e, i) => (
                  <Cell key={i} fill={['#ef4444', '#f59e0b', '#10b981'][i]} />
                ))}
              </Pie>
              <Tooltip contentStyle={{ background: 'var(--bg-card)', border: '1px solid var(--border)', borderRadius: 8 }} />
              <Legend iconType="circle" iconSize={8} />
            </PieChart>
          </ResponsiveContainer>
        </Card>
      </div>

      {/* Charts Row 2 */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 20, marginBottom: 20 }}>
        <Card title="Subject-Wise Performance">
          <ResponsiveContainer width="100%" height={220}>
            <BarChart data={subjects} layout="vertical">
              <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" />
              <XAxis type="number" domain={[0, 100]} tick={{ fill: 'var(--text-muted)', fontSize: 11 }} unit="%" />
              <YAxis dataKey="subject" type="category" tick={{ fill: 'var(--text-muted)', fontSize: 11 }} width={80} />
              <Tooltip contentStyle={{ background: 'var(--bg-card)', border: '1px solid var(--border)', borderRadius: 8 }}
                formatter={v => [`${v}%`]} />
              <Bar dataKey="averageScore" radius={[0, 6, 6, 0]}>
                {subjects.map((_, i) => <Cell key={i} fill={COLORS[i % COLORS.length]} />)}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        </Card>

        <Card title="Revision Activity (Last 30 Days)">
          <ResponsiveContainer width="100%" height={220}>
            <BarChart data={revActivity}>
              <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" />
              <XAxis dataKey="date" tick={{ fill: 'var(--text-muted)', fontSize: 10 }}
                tickFormatter={d => d?.slice(5)} />
              <YAxis tick={{ fill: 'var(--text-muted)', fontSize: 11 }} allowDecimals={false} />
              <Tooltip contentStyle={{ background: 'var(--bg-card)', border: '1px solid var(--border)', borderRadius: 8 }}
                formatter={v => [v, 'Revisions']} />
              <Bar dataKey="revisionCount" fill="#6c63ff" radius={[4, 4, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </Card>
      </div>

      {/* Subject Table */}
      {subjects.length > 0 && (
        <Card title="Subject Performance Details">
          <div className="table-container">
            <table>
              <thead>
                <tr>
                  <th>Subject</th>
                  <th>Topics</th>
                  <th>Revised</th>
                  <th>Avg Score</th>
                  <th>Mistakes</th>
                  <th>Performance</th>
                </tr>
              </thead>
              <tbody>
                {subjects.map(s => (
                  <tr key={s.subject}>
                    <td><strong>{s.subject}</strong></td>
                    <td>{s.topicCount}</td>
                    <td>{s.revisedCount}</td>
                    <td>
                      <span style={{
                        fontWeight: 700,
                        color: s.averageScore >= 80 ? 'var(--success)' :
                               s.averageScore >= 60 ? 'var(--warning)' : 'var(--danger)'
                      }}>
                        {s.averageScore > 0 ? `${s.averageScore}%` : 'No data'}
                      </span>
                    </td>
                    <td>{s.mistakeCount}</td>
                    <td style={{ width: 150 }}>
                      <div className="progress-bar">
                        <div className="progress-fill" style={{
                          width: `${s.averageScore}%`,
                          background: s.averageScore >= 80 ? 'var(--success)' :
                                      s.averageScore >= 60 ? 'var(--warning)' : 'var(--danger)'
                        }} />
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Card>
      )}
    </div>
  );
}
