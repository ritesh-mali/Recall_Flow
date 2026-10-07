export function Spinner() {
  return (
    <div className="loading-spinner">
      <div className="spinner" />
    </div>
  );
}

export function LoadingDots() {
  return (
    <div className="loading-dots">
      <div className="loading-dot" />
      <div className="loading-dot" />
      <div className="loading-dot" />
    </div>
  );
}

export function RiskBadge({ risk }) {
  const labels = { LOW: 'Low', MEDIUM: 'Medium', HIGH: 'High', VERY_HIGH: 'Very High' };
  return (
    <span className={`badge risk-badge-${risk}`}>
      {labels[risk] || risk}
    </span>
  );
}

export function DifficultyBadge({ difficulty }) {
  const cls = { EASY: 'badge-success', MEDIUM: 'badge-warning', HARD: 'badge-danger' };
  return (
    <span className={`badge ${cls[difficulty] || 'badge-info'}`}>{difficulty}</span>
  );
}

export function StatusBadge({ status }) {
  const map = {
    NOT_STARTED: ['badge-info', 'Not Started'],
    LEARNING:    ['badge-warning', 'Learning'],
    LEARNED:     ['badge-success', 'Learned'],
    UNDER_REVISION: ['badge-accent', 'Revising'],
  };
  const [cls, label] = map[status] || ['badge-info', status];
  return <span className={`badge ${cls}`}>{label}</span>;
}

export function PriorityBar({ score }) {
  const color = score >= 70 ? 'var(--danger)'
    : score >= 45 ? 'var(--warning)'
    : 'var(--success)';
  return (
    <div className="priority-score">
      <div className="priority-bar">
        <div
          className="priority-bar-fill"
          style={{ width: `${score}%`, background: color }}
        />
      </div>
      <span className="priority-number" style={{ color }}>{Math.round(score)}</span>
    </div>
  );
}

export function MemoryStrengthBar({ strength }) {
  const percent = Math.round(strength);
  const color = percent >= 70 ? 'var(--success)'
    : percent >= 40 ? 'var(--warning)'
    : 'var(--danger)';
  return (
    <div>
      <div className="flex items-center justify-between mb-4" style={{ marginBottom: 6 }}>
        <span className="text-sm text-muted">Memory Strength</span>
        <span style={{ fontWeight: 700, color }}>{percent}%</span>
      </div>
      <div className="progress-bar">
        <div className="progress-fill" style={{ width: `${percent}%`, background: color }} />
      </div>
    </div>
  );
}

export function ConfidenceStars({ value, onChange, readonly }) {
  return (
    <div className="confidence-stars">
      {[1,2,3,4,5].map(n => (
        <button
          key={n}
          className="star-btn"
          onClick={() => !readonly && onChange?.(n)}
          disabled={readonly}
          style={{ opacity: n <= value ? 1 : 0.25 }}
          type="button"
        >
          ⭐
        </button>
      ))}
    </div>
  );
}

export function EmptyState({ icon = '📭', title, text, action }) {
  return (
    <div className="empty-state">
      <div className="empty-state-icon">{icon}</div>
      <div className="empty-state-title">{title}</div>
      <div className="empty-state-text">{text}</div>
      {action}
    </div>
  );
}

export function Modal({ open, onClose, title, children }) {
  if (!open) return null;
  return (
    <div className="modal-overlay" onClick={e => e.target === e.currentTarget && onClose()}>
      <div className="modal animate-slide-up">
        <div className="modal-header">
          <h2 className="modal-title">{title}</h2>
          <button className="modal-close" onClick={onClose}>×</button>
        </div>
        {children}
      </div>
    </div>
  );
}

export function Card({ title, subtitle, action, children, className = '' }) {
  return (
    <div className={`card ${className}`}>
      {(title || action) && (
        <div className="card-header">
          <div>
            {title && <div className="card-title">{title}</div>}
            {subtitle && <div className="card-subtitle">{subtitle}</div>}
          </div>
          {action}
        </div>
      )}
      {children}
    </div>
  );
}
