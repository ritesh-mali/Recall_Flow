import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { topicService } from '../services';
import { Spinner, RiskBadge, PriorityBar, DifficultyBadge } from '../components/common/UIComponents';
import { Play, RotateCcw, Brain, Clock, AlertTriangle } from 'lucide-react';

export default function TodayRevisionPage() {
  const navigate = useNavigate();
  const [queue, setQueue] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    topicService.getTodayQueue()
      .then(r => setQueue(r.data))
      .catch(console.error)
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <Spinner />;

  const highPriority = queue.filter(q => q.priorityScore >= 70);
  const medium = queue.filter(q => q.priorityScore >= 45 && q.priorityScore < 70);
  const low = queue.filter(q => q.priorityScore < 45);

  const Section = ({ title, items, color }) => items.length === 0 ? null : (
    <div style={{ marginBottom: 28 }}>
      <h3 style={{ fontSize: 14, fontWeight: 700, color, textTransform: 'uppercase',
        letterSpacing: '0.5px', marginBottom: 12, display: 'flex', alignItems: 'center', gap: 8 }}>
        <span style={{ width: 8, height: 8, background: color, borderRadius: '50%', display: 'inline-block' }} />
        {title} ({items.length})
      </h3>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
        {items.map((item, i) => (
          <div key={item.topicId}
            className="revision-queue-item"
            style={{ cursor: 'pointer', borderLeft: `3px solid ${color}` }}
            onClick={() => navigate(`/revision/${item.topicId}`)}
          >
            <div className="queue-rank" style={{ background: color }}>{i + 1}</div>

            <div className="queue-info">
              <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                <div className="queue-name">{item.topicName}</div>
                <DifficultyBadge difficulty={item.difficulty} />
              </div>
              <div className="queue-reason" style={{ marginTop: 4 }}>{item.reason}</div>
              <div style={{ display: 'flex', gap: 8, marginTop: 6, alignItems: 'center' }}>
                <RiskBadge risk={item.riskLevel} />
                <span style={{ fontSize: 11, color: 'var(--text-muted)' }}>{item.subject}</span>
                {item.daysSince != null && (
                  <span style={{ fontSize: 11, color: 'var(--text-muted)', display: 'flex', alignItems: 'center', gap: 3 }}>
                    <Clock size={10} /> {item.daysSince}d ago
                  </span>
                )}
                {item.latestQuizScore != null && (
                  <span style={{
                    fontSize: 11, fontWeight: 600,
                    color: item.latestQuizScore >= 80 ? 'var(--success)' :
                           item.latestQuizScore >= 60 ? 'var(--warning)' : 'var(--danger)'
                  }}>
                    Last quiz: {Math.round(item.latestQuizScore)}%
                  </span>
                )}
              </div>
            </div>

            <div className="queue-score">
              <div className={`queue-priority ${item.priorityScore >= 70 ? 'high' : item.priorityScore >= 45 ? 'medium' : 'low'}`}>
                {Math.round(item.priorityScore)}
              </div>
              <div style={{ fontSize: 9, color: 'var(--text-muted)', textAlign: 'center', marginTop: 2 }}>PRIORITY</div>
              <button
                className="btn btn-primary btn-sm"
                style={{ marginTop: 8 }}
                onClick={(e) => { e.stopPropagation(); navigate(`/revision/${item.topicId}`); }}
              >
                <Play size={12} /> Start
              </button>
            </div>
          </div>
        ))}
      </div>
    </div>
  );

  return (
    <div className="animate-fade-in">
      <div className="page-header">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="page-title">📅 Today's Revision Queue</h1>
            <p className="page-subtitle">
              {queue.length} topics ranked by forgetting risk and priority score
            </p>
          </div>
          <button className="btn btn-secondary" onClick={() => window.location.reload()}>
            <RotateCcw size={14} /> Refresh Queue
          </button>
        </div>
      </div>

      {queue.length === 0 ? (
        <div className="card" style={{ textAlign: 'center', padding: 60 }}>
          <div style={{ fontSize: 56, marginBottom: 16 }}>🎉</div>
          <h2 style={{ fontSize: 22, fontWeight: 700, marginBottom: 8 }}>All caught up!</h2>
          <p style={{ color: 'var(--text-muted)', marginBottom: 24 }}>
            No topics are due for revision today. Add more topics or mark existing ones as learned.
          </p>
          <button className="btn btn-primary" onClick={() => navigate('/topics')}>
            <Brain size={16} /> Manage Topics
          </button>
        </div>
      ) : (
        <>
          {/* Summary Banner */}
          <div style={{
            display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 16, marginBottom: 28
          }}>
            {[
              { label: 'High Priority', count: highPriority.length, color: 'var(--danger)', bg: 'var(--danger-bg)', icon: AlertTriangle },
              { label: 'Medium Priority', count: medium.length, color: 'var(--warning)', bg: 'var(--warning-bg)', icon: Clock },
              { label: 'Low Priority', count: low.length, color: 'var(--success)', bg: 'var(--success-bg)', icon: Brain },
            ].map(s => (
              <div key={s.label} className="card" style={{ textAlign: 'center', background: s.bg, borderColor: `${s.color}44` }}>
                <s.icon size={24} color={s.color} style={{ margin: '0 auto 8px' }} />
                <div style={{ fontSize: 28, fontWeight: 800, color: s.color }}>{s.count}</div>
                <div style={{ fontSize: 13, color: s.color }}>{s.label}</div>
              </div>
            ))}
          </div>

          <Section title="🔴 High Priority" items={highPriority} color="var(--danger)" />
          <Section title="🟡 Medium Priority" items={medium} color="var(--warning)" />
          <Section title="🟢 Low Priority" items={low} color="var(--success)" />
        </>
      )}
    </div>
  );
}
