import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { analyticsService } from '../services';
import { Spinner, Card, RiskBadge } from '../components/common/UIComponents';
import { Lightbulb, Play, TrendingUp, TrendingDown, AlertTriangle, Clock } from 'lucide-react';

const TYPE_CONFIG = {
  REVISE_NOW:       { color: 'var(--danger)',  bg: 'var(--danger-bg)',  icon: AlertTriangle, label: 'Revise Now' },
  PERFORMING_WELL:  { color: 'var(--success)', bg: 'var(--success-bg)', icon: TrendingUp,    label: 'Performing Well' },
  NEEDS_ATTENTION:  { color: 'var(--warning)', bg: 'var(--warning-bg)', icon: TrendingDown,  label: 'Needs Attention' },
  INTERVAL_EXTENDED:{ color: 'var(--accent-2)',bg: 'rgba(0,212,255,0.1)', icon: Clock,       label: 'Interval Extended' },
  SCHEDULED:        { color: 'var(--text-muted)', bg: 'var(--bg-secondary)', icon: Clock,    label: 'Scheduled' },
};

export default function RecommendationsPage() {
  const navigate = useNavigate();
  const [recs, setRecs] = useState([]);
  const [weakTopics, setWeakTopics] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([
      analyticsService.getRecommendations(),
      analyticsService.getWeakTopics(),
    ]).then(([r, w]) => {
      setRecs(r.data);
      setWeakTopics(w.data);
    }).catch(console.error)
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <Spinner />;

  return (
    <div className="animate-fade-in">
      <div className="page-header">
        <h1 className="page-title">💡 Smart Recommendations</h1>
        <p className="page-subtitle">AI-powered insights based on your quiz performance, confidence, and revision history</p>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 320px', gap: 20 }}>
        {/* Recommendations */}
        <div>
          <Card title="Personalized Recommendations" subtitle={`${recs.length} recommendations based on your data`}>
            {recs.length === 0 ? (
              <div className="empty-state">
                <div className="empty-state-icon">🎯</div>
                <div className="empty-state-title">No recommendations yet</div>
                <div className="empty-state-text">Complete some quizzes and revisions to get personalized recommendations.</div>
              </div>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
                {recs.map(rec => {
                  const cfg = TYPE_CONFIG[rec.type] || TYPE_CONFIG.SCHEDULED;
                  return (
                    <div key={rec.topicId} style={{
                      background: cfg.bg,
                      border: `1px solid ${cfg.color}44`,
                      borderRadius: 14,
                      padding: 18,
                      display: 'flex', alignItems: 'flex-start', gap: 14
                    }}>
                      <div style={{
                        width: 40, height: 40, borderRadius: 10, background: `${cfg.color}25`,
                        display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0
                      }}>
                        <cfg.icon size={20} color={cfg.color} />
                      </div>
                      <div style={{ flex: 1 }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 6 }}>
                          <span style={{ fontWeight: 700, fontSize: 15 }}>{rec.topicName}</span>
                          <span style={{
                            fontSize: 10, fontWeight: 700, padding: '2px 8px', borderRadius: 20,
                            background: `${cfg.color}20`, color: cfg.color, textTransform: 'uppercase'
                          }}>{cfg.label}</span>
                        </div>
                        <p style={{ fontSize: 13, color: 'var(--text-secondary)', lineHeight: 1.6, marginBottom: 10 }}>
                          {rec.message}
                        </p>
                        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                          <RiskBadge risk={rec.riskLevel} />
                          <span style={{ fontSize: 11, color: 'var(--text-muted)' }}>
                            Priority: {Math.round(rec.priorityScore || 0)}
                          </span>
                          {(rec.type === 'REVISE_NOW' || rec.type === 'NEEDS_ATTENTION') && (
                            <button
                              className="btn btn-sm"
                              style={{ marginLeft: 'auto', background: cfg.color, color: 'white', border: 'none' }}
                              onClick={() => navigate(`/revision/${rec.topicId}`)}
                            >
                              <Play size={12} /> Revise Now
                            </button>
                          )}
                        </div>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </Card>
        </div>

        {/* Weak Areas */}
        <div>
          <Card title="🔴 Your Weak Areas" subtitle="Topics needing the most attention">
            {weakTopics.length === 0 ? (
              <div className="empty-state" style={{ padding: '24px 0' }}>
                <div style={{ fontSize: 32 }}>💪</div>
                <div className="empty-state-title" style={{ fontSize: 15 }}>No weak areas!</div>
                <div className="empty-state-text" style={{ fontSize: 12 }}>Keep up the great work!</div>
              </div>
            ) : (
              weakTopics.map((wt, i) => (
                <div key={wt.topicId} style={{
                  padding: '14px 0',
                  borderBottom: i < weakTopics.length - 1 ? '1px solid var(--border)' : 'none',
                  cursor: 'pointer'
                }} onClick={() => navigate(`/revision/${wt.topicId}`)}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 6 }}>
                    <div>
                      <div style={{ fontWeight: 700, fontSize: 14 }}>{wt.topicName}</div>
                      <div style={{ fontSize: 11, color: 'var(--text-muted)' }}>{wt.subject}</div>
                    </div>
                    <div style={{ textAlign: 'right' }}>
                      <div style={{
                        fontSize: 20, fontWeight: 800,
                        color: wt.averageAccuracy >= 60 ? 'var(--warning)' : 'var(--danger)'
                      }}>
                        {Math.round(wt.averageAccuracy || 0)}%
                      </div>
                      <div style={{ fontSize: 10, color: 'var(--text-muted)' }}>accuracy</div>
                    </div>
                  </div>
                  <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
                    <RiskBadge risk={wt.riskLevel} />
                    <span style={{ fontSize: 11, color: 'var(--danger)' }}>
                      {wt.mistakeCount} mistakes
                    </span>
                  </div>
                  <div style={{ fontSize: 11, color: 'var(--text-muted)', marginTop: 4 }}>
                    {wt.weaknessReason}
                  </div>
                </div>
              ))
            )}
          </Card>
        </div>
      </div>
    </div>
  );
}
