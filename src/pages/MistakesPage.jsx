import { useState, useEffect } from 'react';
import { analyticsService } from '../services';
import { Spinner, Card, RiskBadge } from '../components/common/UIComponents';
import { AlertTriangle, RotateCcw } from 'lucide-react';

export default function MistakesPage() {
  const [summary, setSummary] = useState([]);
  const [mistakes, setMistakes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selected, setSelected] = useState(null);

  useEffect(() => {
    Promise.all([
      analyticsService.getMistakeSummary(),
      analyticsService.getMistakes(),
    ]).then(([s, m]) => {
      setSummary(s.data);
      setMistakes(m.data);
    }).catch(console.error)
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <Spinner />;

  const filteredMistakes = selected
    ? mistakes.filter(m => m.topicId === selected)
    : mistakes;

  return (
    <div className="animate-fade-in">
      <div className="page-header">
        <h1 className="page-title">⚠️ Mistake Tracker</h1>
        <p className="page-subtitle">{mistakes.length} total mistakes recorded across all topics</p>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '280px 1fr', gap: 20 }}>
        {/* Left: Mistake Summary */}
        <div>
          <Card title="Repeated Mistakes" subtitle="Topics with most errors">
            {summary.length === 0 ? (
              <div className="empty-state" style={{ padding: '24px 0' }}>
                <div className="empty-state-icon" style={{ fontSize: 32 }}>🎉</div>
                <div className="empty-state-title">No mistakes yet!</div>
              </div>
            ) : (
              summary.map((s, i) => (
                <div
                  key={s.topicId}
                  onClick={() => setSelected(selected === s.topicId ? null : s.topicId)}
                  style={{
                    display: 'flex', alignItems: 'center', justifyContent: 'space-between',
                    padding: '12px 0', borderBottom: '1px solid var(--border)',
                    cursor: 'pointer',
                    background: selected === s.topicId ? 'var(--danger-bg)' : 'transparent',
                    borderRadius: 8, paddingLeft: selected === s.topicId ? 8 : 0,
                    transition: 'all 0.2s'
                  }}
                >
                  <div>
                    <div style={{ fontWeight: 600, fontSize: 14, display: 'flex', alignItems: 'center', gap: 6 }}>
                      <span style={{
                        width: 20, height: 20, background: 'var(--danger-bg)', borderRadius: '50%',
                        display: 'flex', alignItems: 'center', justifyContent: 'center',
                        fontSize: 10, fontWeight: 800, color: 'var(--danger)', flexShrink: 0
                      }}>{i + 1}</span>
                      {s.topicName}
                    </div>
                    <div style={{ fontSize: 11, color: 'var(--text-muted)', marginTop: 2, paddingLeft: 26 }}>
                      {s.subject} · Accuracy: {Math.round(s.accuracyRate || 0)}%
                    </div>
                  </div>
                  <span style={{
                    background: 'var(--danger-bg)', color: 'var(--danger)',
                    fontWeight: 800, fontSize: 14, padding: '2px 10px', borderRadius: 20,
                    border: '1px solid rgba(239,68,68,0.3)'
                  }}>
                    {s.mistakeCount}
                  </span>
                </div>
              ))
            )}
          </Card>
        </div>

        {/* Right: Mistake Details */}
        <Card
          title={selected ? `Mistakes for: ${summary.find(s => s.topicId === selected)?.topicName}` : 'All Mistakes'}
          action={selected && (
            <button className="btn btn-secondary btn-sm" onClick={() => setSelected(null)}>
              Clear Filter
            </button>
          )}
        >
          {filteredMistakes.length === 0 ? (
            <div className="empty-state">
              <div className="empty-state-icon">✅</div>
              <div className="empty-state-title">No mistakes here!</div>
              <div className="empty-state-text">Keep practicing to see your mistakes tracked here.</div>
            </div>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
              {filteredMistakes.map(m => (
                <div key={m.id} style={{
                  background: 'var(--bg-secondary)', borderRadius: 12,
                  padding: 16, border: '1px solid var(--border)'
                }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
                    <div>
                      <span style={{
                        fontSize: 11, fontWeight: 700, color: 'var(--accent)',
                        background: 'rgba(108,99,255,0.12)', padding: '2px 8px', borderRadius: 20
                      }}>{m.subject}</span>
                      <span style={{ fontSize: 11, color: 'var(--text-muted)', marginLeft: 8 }}>{m.topicName}</span>
                    </div>
                    <span style={{ fontSize: 11, color: 'var(--text-muted)' }}>
                      {new Date(m.mistakeDate).toLocaleDateString()}
                    </span>
                  </div>

                  <p style={{ fontSize: 14, fontWeight: 500, marginBottom: 12, lineHeight: 1.5 }}>
                    {m.questionText}
                  </p>

                  <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8, marginBottom: 10 }}>
                    <div style={{
                      background: 'var(--danger-bg)', borderRadius: 8, padding: '8px 12px',
                      border: '1px solid rgba(239,68,68,0.3)'
                    }}>
                      <div style={{ fontSize: 10, color: 'var(--danger)', fontWeight: 700, textTransform: 'uppercase', marginBottom: 2 }}>
                        Your Answer
                      </div>
                      <div style={{ fontWeight: 700 }}>{m.userAnswer}</div>
                    </div>
                    <div style={{
                      background: 'var(--success-bg)', borderRadius: 8, padding: '8px 12px',
                      border: '1px solid rgba(16,185,129,0.3)'
                    }}>
                      <div style={{ fontSize: 10, color: 'var(--success)', fontWeight: 700, textTransform: 'uppercase', marginBottom: 2 }}>
                        Correct Answer
                      </div>
                      <div style={{ fontWeight: 700 }}>{m.correctAnswer}</div>
                    </div>
                  </div>

                  {m.explanation && (
                    <div style={{
                      background: 'rgba(108,99,255,0.08)', borderRadius: 8, padding: '10px 12px',
                      border: '1px solid rgba(108,99,255,0.2)', fontSize: 13, color: 'var(--text-secondary)'
                    }}>
                      💡 <strong>Explanation:</strong> {m.explanation}
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </Card>
      </div>
    </div>
  );
}
