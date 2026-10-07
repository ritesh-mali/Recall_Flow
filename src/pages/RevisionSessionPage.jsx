import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { revisionService, quizService } from '../services';
import {
  Spinner, RiskBadge, DifficultyBadge, MemoryStrengthBar,
  ConfidenceStars, Card
} from '../components/common/UIComponents';
import { Brain, Play, CheckCircle, Clock, AlertTriangle, BarChart2 } from 'lucide-react';

export default function RevisionSessionPage() {
  const { topicId } = useParams();
  const navigate = useNavigate();
  const [session, setSession] = useState(null);
  const [loading, setLoading] = useState(true);
  const [phase, setPhase] = useState('info'); // info | quiz | complete
  const [confidence, setConfidence] = useState(3);
  const [notes, setNotes] = useState('');
  const [quizResult, setQuizResult] = useState(null);
  const [completing, setCompleting] = useState(false);

  useEffect(() => {
    revisionService.getSession(topicId)
      .then(r => {
        setSession(r.data);
        setConfidence(r.data.previousConfidence || 3);
      })
      .catch(console.error)
      .finally(() => setLoading(false));
  }, [topicId]);

  const handleCompleteRevision = async () => {
    setCompleting(true);
    try {
      await revisionService.complete(topicId, {
        confidenceAfter: confidence,
        notes,
      }, quizResult?.scorePercentage);
      setPhase('done');
    } catch (err) {
      alert('Failed to complete revision');
    } finally { setCompleting(false); }
  };

  if (loading) return <Spinner />;
  if (!session) return <div className="card">Session not found.</div>;

  const riskColor = {
    LOW: 'var(--success)', MEDIUM: 'var(--warning)',
    HIGH: 'var(--danger)', VERY_HIGH: 'var(--risk-very-high)'
  }[session.currentRisk] || 'var(--text-muted)';

  if (phase === 'done') {
    return (
      <div className="animate-fade-in" style={{ maxWidth: 580, margin: '0 auto' }}>
        <div className="card" style={{ textAlign: 'center', padding: 48 }}>
          <div style={{ fontSize: 56, marginBottom: 20 }}>✅</div>
          <h2 style={{ fontSize: 24, fontWeight: 700, marginBottom: 8 }}>Revision Complete!</h2>
          <p style={{ color: 'var(--text-muted)', marginBottom: 28 }}>
            Great work on <strong>{session.topicName}</strong>. Your progress has been saved.
          </p>
          {quizResult && (
            <div style={{
              background: 'var(--bg-secondary)', borderRadius: 12,
              padding: 20, marginBottom: 28, border: '1px solid var(--border)'
            }}>
              <div style={{ fontSize: 40, fontWeight: 800,
                color: quizResult.scorePercentage >= 70 ? 'var(--success)' : 'var(--danger)'
              }}>
                {Math.round(quizResult.scorePercentage)}%
              </div>
              <div style={{ fontSize: 13, color: 'var(--text-muted)', marginTop: 4 }}>
                {quizResult.correctAnswers}/{quizResult.totalQuestions} correct
              </div>
              <div style={{ fontSize: 13, marginTop: 8, color: 'var(--text-secondary)' }}>
                Next revision in <strong>{quizResult.nextRevisionDays} days</strong>
              </div>
            </div>
          )}
          <div style={{ display: 'flex', gap: 10, justifyContent: 'center' }}>
            <button className="btn btn-secondary" onClick={() => navigate('/topics')}>Back to Topics</button>
            <button className="btn btn-primary" onClick={() => navigate('/revision/today')}>Today's Queue</button>
          </div>
        </div>
      </div>
    );
  }

  if (phase === 'quiz') {
    return <QuizPhase
      topicId={topicId}
      session={session}
      onComplete={(result) => { setQuizResult(result); setPhase('rate'); }}
      onSkip={() => setPhase('rate')}
    />;
  }

  if (phase === 'rate') {
    return (
      <div className="animate-fade-in" style={{ maxWidth: 560, margin: '0 auto' }}>
        <Card title="Rate Your Confidence" subtitle="After revising, how well do you know this topic?">
          {quizResult && (
            <div style={{
              background: quizResult.scorePercentage >= 70 ? 'var(--success-bg)' : 'var(--danger-bg)',
              borderRadius: 10, padding: '12px 16px', marginBottom: 20,
              border: `1px solid ${quizResult.scorePercentage >= 70 ? 'rgba(16,185,129,0.3)' : 'rgba(239,68,68,0.3)'}`
            }}>
              <div style={{ fontWeight: 700, fontSize: 16 }}>Quiz Score: {Math.round(quizResult.scorePercentage)}%</div>
              <div style={{ fontSize: 13, marginTop: 4, color: 'var(--text-muted)' }}>
                {quizResult.correctAnswers}/{quizResult.totalQuestions} correct · {quizResult.performanceMessage}
              </div>
            </div>
          )}
          <div style={{ marginBottom: 24 }}>
            <div style={{ fontSize: 14, marginBottom: 12, color: 'var(--text-secondary)' }}>
              How confident are you now with <strong>{session.topicName}</strong>?
            </div>
            <ConfidenceStars value={confidence} onChange={setConfidence} />
            <div style={{ marginTop: 8, fontSize: 12, color: 'var(--text-muted)' }}>
              {['', 'Very low — need a lot more practice',
                'Low — still struggling', 'Moderate — getting there',
                'Good — mostly confident', 'Excellent — fully confident'][confidence]}
            </div>
          </div>
          <div className="form-group">
            <label className="form-label">Notes (optional)</label>
            <textarea className="form-textarea" placeholder="Any observations or things to remember..."
              value={notes} onChange={e => setNotes(e.target.value)} style={{ minHeight: 80 }} />
          </div>
          <button className="btn btn-primary w-full btn-lg" disabled={completing} onClick={handleCompleteRevision}>
            {completing ? 'Saving...' : '✅ Complete Revision'}
          </button>
        </Card>
      </div>
    );
  }

  // Phase: info
  return (
    <div className="animate-fade-in" style={{ maxWidth: 700, margin: '0 auto' }}>
      <div className="page-header">
        <h1 className="page-title">🔄 Revision Session</h1>
        <p className="page-subtitle">{session.topicName} · {session.subject}</p>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 240px', gap: 20, marginBottom: 24 }}>
        <Card>
          <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 20 }}>
            <div>
              <h2 style={{ fontSize: 22, fontWeight: 800 }}>{session.topicName}</h2>
              <p style={{ color: 'var(--text-muted)', fontSize: 14 }}>{session.subject}</p>
            </div>
            <DifficultyBadge difficulty={session.difficulty} />
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12, marginBottom: 20 }}>
            {[
              { label: 'Last Revised', value: session.lastRevisedDate ? `${session.daysSinceRevision} days ago` : 'Never', icon: Clock },
              { label: 'Revisions Done', value: session.revisionCount || 0, icon: RotateCcw },
              { label: 'Previous Score', value: session.previousQuizScore != null ? `${Math.round(session.previousQuizScore)}%` : '—', icon: BarChart2 },
              { label: 'Total Mistakes', value: session.totalMistakes || 0, icon: AlertTriangle },
            ].map(s => (
              <div key={s.label} style={{
                background: 'var(--bg-secondary)', borderRadius: 10, padding: '12px 14px',
                border: '1px solid var(--border)'
              }}>
                <div style={{ fontSize: 11, color: 'var(--text-muted)', marginBottom: 4 }}>{s.label}</div>
                <div style={{ fontSize: 18, fontWeight: 700 }}>{s.value}</div>
              </div>
            ))}
          </div>

          <div style={{
            background: `${riskColor}15`, border: `1px solid ${riskColor}44`,
            borderRadius: 10, padding: '12px 16px', marginBottom: 20,
            display: 'flex', alignItems: 'center', gap: 10
          }}>
            <AlertTriangle size={18} color={riskColor} />
            <div>
              <div style={{ fontWeight: 700, color: riskColor }}>Forgetting Risk: {session.currentRisk?.replace('_', ' ')}</div>
              <div style={{ fontSize: 12, color: 'var(--text-muted)', marginTop: 2 }}>
                Priority Score: {Math.round(session.priorityScore || 0)}/100
              </div>
            </div>
          </div>

          <MemoryStrengthBar strength={session.memoryStrength || 0} />
        </Card>

        <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
          <Card title="Previous Confidence">
            <ConfidenceStars value={session.previousConfidence || 0} readonly />
            <div style={{ fontSize: 12, color: 'var(--text-muted)', marginTop: 8 }}>
              From last revision
            </div>
          </Card>
          <Card title="Avg Quiz Score">
            <div style={{ fontSize: 28, fontWeight: 800,
              color: session.averageQuizScore >= 80 ? 'var(--success)' :
                     session.averageQuizScore >= 60 ? 'var(--warning)' : 'var(--danger)'
            }}>
              {session.averageQuizScore != null ? `${Math.round(session.averageQuizScore)}%` : 'No data'}
            </div>
          </Card>
        </div>
      </div>

      <Card>
        <h3 style={{ fontSize: 16, fontWeight: 700, marginBottom: 16 }}>📖 Study this topic, then take the quiz</h3>
        <p style={{ color: 'var(--text-muted)', fontSize: 14, marginBottom: 24 }}>
          Take some time to review <strong>{session.topicName}</strong>. When you're ready,
          click "Take Quiz" to test your knowledge and update your revision schedule.
        </p>
        <div style={{ display: 'flex', gap: 12 }}>
          <button className="btn btn-primary btn-lg" onClick={() => setPhase('quiz')}>
            <Play size={18} /> Take Quiz
          </button>
          <button className="btn btn-secondary btn-lg" onClick={() => setPhase('rate')}>
            <CheckCircle size={18} /> Skip Quiz & Rate Confidence
          </button>
        </div>
      </Card>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────────────────
// Quiz sub-component
// ─────────────────────────────────────────────────────────────────────────
function QuizPhase({ topicId, session, onComplete, onSkip }) {
  const [questions, setQuestions] = useState([]);
  const [current, setCurrent] = useState(0);
  const [answers, setAnswers] = useState({});
  const [submitted, setSubmitted] = useState(false);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [showExpl, setShowExpl] = useState(false);
  const startTime = useState(() => Date.now())[0];

  useEffect(() => {
    quizService.getQuestions(topicId, 10)
      .then(r => setQuestions(r.data))
      .catch(() => setQuestions([]))
      .finally(() => setLoading(false));
  }, [topicId]);

  if (loading) return <Spinner />;

  if (questions.length === 0) {
    return (
      <div className="card" style={{ textAlign: 'center', padding: 40, maxWidth: 560, margin: '0 auto' }}>
        <div style={{ fontSize: 40, marginBottom: 12 }}>📝</div>
        <h3 style={{ fontSize: 18, fontWeight: 700, marginBottom: 8 }}>No questions available</h3>
        <p style={{ color: 'var(--text-muted)', marginBottom: 20 }}>
          There are no quiz questions for <strong>{session.topicName}</strong> yet.
          You can add questions from the topic detail page.
        </p>
        <button className="btn btn-primary" onClick={onSkip}>Continue without quiz</button>
      </div>
    );
  }

  const q = questions[current];
  const OPTIONS = ['A', 'B', 'C', 'D'];
  const optionText = { A: q.optionA, B: q.optionB, C: q.optionC, D: q.optionD };
  const selected = answers[q.id];

  const handleSelect = (opt) => {
    if (submitted) return;
    setAnswers(a => ({ ...a, [q.id]: opt }));
    setShowExpl(false);
  };

  const handleSubmitFinal = async () => {
    const answerList = questions.map(qu => ({
      questionId: qu.id,
      selectedAnswer: answers[qu.id] || 'A',
    }));
    setSubmitting(true);
    try {
      const res = await quizService.submit({
        topicId: Number(topicId),
        answers: answerList,
        durationSeconds: Math.round((Date.now() - startTime) / 1000),
      });
      onComplete(res.data);
    } catch (err) {
      alert('Failed to submit quiz');
    } finally { setSubmitting(false); }
  };

  const progress = Math.round(((current + 1) / questions.length) * 100);

  return (
    <div className="quiz-container animate-fade-in">
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 20 }}>
        <h2 style={{ fontWeight: 700, fontSize: 18 }}>Quiz: {session.topicName}</h2>
        <button className="btn btn-secondary btn-sm" onClick={onSkip}>Skip Quiz</button>
      </div>

      <div className="quiz-progress">
        <div className="quiz-progress-bar">
          <div className="quiz-progress-fill" style={{ width: `${progress}%` }} />
        </div>
        <span style={{ fontSize: 13, fontWeight: 600, color: 'var(--text-secondary)', whiteSpace: 'nowrap' }}>
          {current + 1} / {questions.length}
        </span>
      </div>

      <div className="question-card">
        <div className="question-number">Question {current + 1}</div>
        <div className="question-text">{q.questionText}</div>
        <div className="options-grid">
          {OPTIONS.map(opt => {
            let cls = '';
            if (selected === opt) cls = 'selected';
            return (
              <button key={opt} className={`option-btn ${cls}`} onClick={() => handleSelect(opt)}>
                <span className="option-letter">{opt}</span>
                {optionText[opt]}
              </button>
            );
          })}
        </div>
      </div>

      <div style={{ display: 'flex', gap: 12, justifyContent: 'flex-end' }}>
        {current > 0 && (
          <button className="btn btn-secondary" onClick={() => setCurrent(c => c - 1)}>← Prev</button>
        )}
        {current < questions.length - 1 ? (
          <button
            className="btn btn-primary"
            disabled={!selected}
            onClick={() => setCurrent(c => c + 1)}
          >
            Next →
          </button>
        ) : (
          <button
            className="btn btn-success btn-lg"
            disabled={Object.keys(answers).length < questions.length || submitting}
            onClick={handleSubmitFinal}
          >
            {submitting ? 'Submitting...' : '✅ Submit Quiz'}
          </button>
        )}
      </div>

      <div style={{ marginTop: 12, fontSize: 12, color: 'var(--text-muted)', textAlign: 'center' }}>
        {Object.keys(answers).length}/{questions.length} questions answered
      </div>
    </div>
  );
}
