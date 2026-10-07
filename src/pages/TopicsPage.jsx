import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { topicService } from '../services';
import {
  Spinner, RiskBadge, DifficultyBadge, StatusBadge,
  PriorityBar, Modal, EmptyState, MemoryStrengthBar, ConfidenceStars
} from '../components/common/UIComponents';
import { Plus, Search, Filter, Edit2, Trash2, Play, BookOpen, ChevronRight } from 'lucide-react';

const DIFFICULTIES = ['ALL', 'EASY', 'MEDIUM', 'HARD'];
const STATUSES = ['ALL', 'NOT_STARTED', 'LEARNED', 'UNDER_REVISION'];

export default function TopicsPage() {
  const navigate = useNavigate();
  const [topics, setTopics] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [diffFilter, setDiffFilter] = useState('ALL');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [showLearnModal, setShowLearnModal] = useState(null);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(null);
  const [form, setForm] = useState({ name: '', subject: '', difficulty: 'MEDIUM', description: '', tags: '' });
  const [learnForm, setLearnForm] = useState({ confidenceLevel: 3 });
  const [saving, setSaving] = useState(false);

  const load = () => {
    topicService.getAll()
      .then(r => setTopics(r.data))
      .catch(console.error)
      .finally(() => setLoading(false));
  };

  useEffect(load, []);

  const filtered = topics.filter(t => {
    const matchSearch = t.name.toLowerCase().includes(search.toLowerCase()) ||
                        t.subject.toLowerCase().includes(search.toLowerCase());
    const matchDiff = diffFilter === 'ALL' || t.difficulty === diffFilter;
    const matchStatus = statusFilter === 'ALL' || t.status === statusFilter;
    return matchSearch && matchDiff && matchStatus;
  });

  const handleCreate = async (e) => {
    e.preventDefault();
    setSaving(true);
    try {
      await topicService.create(form);
      setShowCreateModal(false);
      setForm({ name: '', subject: '', difficulty: 'MEDIUM', description: '', tags: '' });
      load();
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to create topic');
    } finally { setSaving(false); }
  };

  const handleLearn = async (e) => {
    e.preventDefault();
    setSaving(true);
    try {
      await topicService.markAsLearned(showLearnModal.id, learnForm);
      setShowLearnModal(null);
      load();
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to mark as learned');
    } finally { setSaving(false); }
  };

  const handleDelete = async (id) => {
    try {
      await topicService.delete(id);
      setShowDeleteConfirm(null);
      load();
    } catch (err) {
      alert('Failed to delete topic');
    }
  };

  if (loading) return <Spinner />;

  return (
    <div className="animate-fade-in">
      <div className="page-header flex items-center justify-between">
        <div>
          <h1 className="page-title">📚 Topics</h1>
          <p className="page-subtitle">{topics.length} total topics across {new Set(topics.map(t => t.subject)).size} subjects</p>
        </div>
        <button className="btn btn-primary" onClick={() => setShowCreateModal(true)}>
          <Plus size={16} /> Add Topic
        </button>
      </div>

      {/* Filters */}
      <div style={{ display: 'flex', gap: 12, marginBottom: 24, flexWrap: 'wrap' }}>
        <div className="search-bar" style={{ flex: 1, minWidth: 200 }}>
          <Search size={16} className="search-icon" />
          <input
            className="form-input search-input"
            placeholder="Search topics..."
            value={search}
            onChange={e => setSearch(e.target.value)}
          />
        </div>
        <select className="form-select" style={{ width: 130 }}
          value={diffFilter} onChange={e => setDiffFilter(e.target.value)}>
          {DIFFICULTIES.map(d => <option key={d}>{d}</option>)}
        </select>
        <select className="form-select" style={{ width: 160 }}
          value={statusFilter} onChange={e => setStatusFilter(e.target.value)}>
          {STATUSES.map(s => <option key={s} value={s}>{s.replace(/_/g, ' ')}</option>)}
        </select>
      </div>

      {/* Topics Grid */}
      {filtered.length === 0 ? (
        <EmptyState
          icon="📭"
          title="No topics found"
          text={topics.length === 0 ? 'Create your first topic to get started with RecallFlow.' : 'No topics match your filters.'}
          action={topics.length === 0 && (
            <button className="btn btn-primary" onClick={() => setShowCreateModal(true)}>
              <Plus size={16} /> Add First Topic
            </button>
          )}
        />
      ) : (
        <div className="topics-grid">
          {filtered.map(topic => (
            <div key={topic.id} className="topic-card" onClick={() => navigate(`/topics/${topic.id}`)}>
              <div className="topic-card-header">
                <div>
                  <div className="topic-name">{topic.name}</div>
                  <div className="topic-subject">{topic.subject}</div>
                </div>
                <div style={{ display: 'flex', gap: 6, flexDirection: 'column', alignItems: 'flex-end' }}>
                  <DifficultyBadge difficulty={topic.difficulty} />
                  <StatusBadge status={topic.status} />
                </div>
              </div>

              {topic.status !== 'NOT_STARTED' && (
                <>
                  <div style={{ margin: '12px 0' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 11, color: 'var(--text-muted)', marginBottom: 4 }}>
                      <span>Priority</span>
                      <RiskBadge risk={topic.forgettingRisk} />
                    </div>
                    <PriorityBar score={topic.revisionPriorityScore || 0} />
                  </div>

                  <div className="topic-stats">
                    <div className="topic-stat-item">
                      <div className="topic-stat-label">Quiz Score</div>
                      <div className="topic-stat-value" style={{
                        color: topic.latestQuizScore >= 80 ? 'var(--success)' :
                               topic.latestQuizScore >= 60 ? 'var(--warning)' : 'var(--danger)'
                      }}>
                        {topic.latestQuizScore != null ? `${Math.round(topic.latestQuizScore)}%` : '—'}
                      </div>
                    </div>
                    <div className="topic-stat-item">
                      <div className="topic-stat-label">Confidence</div>
                      <div className="topic-stat-value">
                        {'⭐'.repeat(topic.confidenceLevel || 0)}{topic.confidenceLevel ? '' : '—'}
                      </div>
                    </div>
                    <div className="topic-stat-item">
                      <div className="topic-stat-label">Revisions</div>
                      <div className="topic-stat-value">{topic.revisionCount || 0}</div>
                    </div>
                    <div className="topic-stat-item">
                      <div className="topic-stat-label">Mistakes</div>
                      <div className="topic-stat-value" style={{ color: topic.totalMistakes > 3 ? 'var(--danger)' : 'inherit' }}>
                        {topic.totalMistakes || 0}
                      </div>
                    </div>
                  </div>
                </>
              )}

              <div style={{ display: 'flex', gap: 8, marginTop: 16 }} onClick={e => e.stopPropagation()}>
                {topic.status === 'NOT_STARTED' ? (
                  <button className="btn btn-success btn-sm" style={{ flex: 1 }}
                    onClick={() => setShowLearnModal(topic)}>
                    <BookOpen size={14} /> Mark as Learned
                  </button>
                ) : (
                  <button className="btn btn-primary btn-sm" style={{ flex: 1 }}
                    onClick={() => navigate(`/revision/${topic.id}`)}>
                    <Play size={14} /> Revise
                  </button>
                )}
                <button className="btn btn-secondary btn-icon btn-sm"
                  onClick={() => setShowDeleteConfirm(topic)}>
                  <Trash2 size={14} />
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Create Modal */}
      <Modal open={showCreateModal} onClose={() => setShowCreateModal(false)} title="Create New Topic">
        <form onSubmit={handleCreate}>
          <div className="form-row">
            <div className="form-group">
              <label className="form-label">Topic Name *</label>
              <input className="form-input" placeholder="e.g. Java Collections" required
                value={form.name} onChange={e => setForm(f => ({ ...f, name: e.target.value }))} />
            </div>
            <div className="form-group">
              <label className="form-label">Subject *</label>
              <input className="form-input" placeholder="e.g. Java, SQL, DSA" required
                value={form.subject} onChange={e => setForm(f => ({ ...f, subject: e.target.value }))} />
            </div>
          </div>
          <div className="form-group">
            <label className="form-label">Difficulty *</label>
            <select className="form-select" value={form.difficulty}
              onChange={e => setForm(f => ({ ...f, difficulty: e.target.value }))}>
              <option value="EASY">Easy</option>
              <option value="MEDIUM">Medium</option>
              <option value="HARD">Hard</option>
            </select>
          </div>
          <div className="form-group">
            <label className="form-label">Description</label>
            <textarea className="form-textarea" placeholder="Optional description..."
              value={form.description} onChange={e => setForm(f => ({ ...f, description: e.target.value }))} />
          </div>
          <div style={{ display: 'flex', gap: 10, justifyContent: 'flex-end' }}>
            <button type="button" className="btn btn-secondary" onClick={() => setShowCreateModal(false)}>Cancel</button>
            <button type="submit" className="btn btn-primary" disabled={saving}>
              {saving ? 'Creating...' : 'Create Topic'}
            </button>
          </div>
        </form>
      </Modal>

      {/* Learn Modal */}
      <Modal open={!!showLearnModal} onClose={() => setShowLearnModal(null)}
        title={`Start Learning: ${showLearnModal?.name}`}>
        <form onSubmit={handleLearn}>
          <div style={{
            background: 'var(--bg-secondary)', borderRadius: 12, padding: 16, marginBottom: 20,
            border: '1px solid var(--border)'
          }}>
            <div style={{ fontSize: 13, color: 'var(--text-muted)', marginBottom: 4 }}>Topic</div>
            <div style={{ fontWeight: 700, fontSize: 16 }}>{showLearnModal?.name}</div>
            <div style={{ fontSize: 13, color: 'var(--text-muted)', marginTop: 4 }}>
              {showLearnModal?.subject} · <DifficultyBadge difficulty={showLearnModal?.difficulty} />
            </div>
          </div>
          <div className="form-group">
            <label className="form-label">How confident are you right now?</label>
            <ConfidenceStars
              value={learnForm.confidenceLevel}
              onChange={v => setLearnForm(f => ({ ...f, confidenceLevel: v }))}
            />
            <div style={{ fontSize: 12, color: 'var(--text-muted)', marginTop: 8 }}>
              This helps schedule your first revision at the right time.
            </div>
          </div>
          <div style={{ display: 'flex', gap: 10, justifyContent: 'flex-end' }}>
            <button type="button" className="btn btn-secondary" onClick={() => setShowLearnModal(null)}>Cancel</button>
            <button type="submit" className="btn btn-primary" disabled={saving}>
              {saving ? 'Marking...' : '✅ Mark as Learned'}
            </button>
          </div>
        </form>
      </Modal>

      {/* Delete Confirm */}
      <Modal open={!!showDeleteConfirm} onClose={() => setShowDeleteConfirm(null)} title="Delete Topic">
        <p style={{ color: 'var(--text-secondary)', marginBottom: 20 }}>
          Are you sure you want to delete <strong>{showDeleteConfirm?.name}</strong>?
          This will permanently remove all revisions, quiz attempts, and mistakes for this topic.
        </p>
        <div style={{ display: 'flex', gap: 10, justifyContent: 'flex-end' }}>
          <button className="btn btn-secondary" onClick={() => setShowDeleteConfirm(null)}>Cancel</button>
          <button className="btn btn-danger" onClick={() => handleDelete(showDeleteConfirm?.id)}>
            Delete Permanently
          </button>
        </div>
      </Modal>
    </div>
  );
}
