import React, { useState, useEffect } from 'react';
import { MessageSquare, Send, Edit2, Trash2, X, User } from 'lucide-react';
import { WorkflowComment } from '../../types';
import { TicketService } from '../../services/ticketService';
import { useAuth } from '../../context/AuthContext';

interface WorkflowStepCommentsProps {
  stepId: string;
  onRefresh?: () => void;
}

const WorkflowStepComments: React.FC<WorkflowStepCommentsProps> = ({ stepId, onRefresh }) => {
  const { user } = useAuth();
  const [comments, setComments] = useState<WorkflowComment[]>([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [newComment, setNewComment] = useState('');
  const [editingCommentId, setEditingCommentId] = useState<string | null>(null);
  const [editContent, setEditContent] = useState('');
  const [error, setError] = useState<string | null>(null);

  const loadComments = async () => {
    try {
      setLoading(true);
      setError(null);
      const fetchedComments = await TicketService.getStepComments(stepId);
      setComments(fetchedComments);
    } catch (err) {
      console.error('Failed to load comments:', err);
      setError('Failed to load comments. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadComments();
  }, [stepId]);

  const handleSubmitComment = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user || !newComment.trim()) return;

    try {
      setSubmitting(true);
      setError(null);
      await TicketService.addStepComment(stepId, newComment, user.id);
      setNewComment('');
      await loadComments();
      if (onRefresh) onRefresh();
    } catch (err) {
      console.error('Failed to add comment:', err);
      setError('Failed to add comment. Please try again.');
    } finally {
      setSubmitting(false);
    }
  };

  const handleEditComment = async (commentId: string) => {
    if (!user || !editContent.trim()) return;

    try {
      setError(null);
      await TicketService.updateStepComment(commentId, editContent, user.id);
      setEditingCommentId(null);
      setEditContent('');
      await loadComments();
      if (onRefresh) onRefresh();
    } catch (err) {
      console.error('Failed to update comment:', err);
      setError('Failed to update comment. Please try again.');
    }
  };

  const handleDeleteComment = async (commentId: string) => {
    if (!user) return;

    if (!confirm('Are you sure you want to delete this comment?')) return;

    try {
      setError(null);
      await TicketService.deleteStepComment(commentId, user.id);
      await loadComments();
      if (onRefresh) onRefresh();
    } catch (err) {
      console.error('Failed to delete comment:', err);
      setError('Failed to delete comment. Please try again.');
    }
  };

  const startEdit = (comment: WorkflowComment) => {
    setEditingCommentId(comment.id);
    setEditContent(comment.content);
  };

  const cancelEdit = () => {
    setEditingCommentId(null);
    setEditContent('');
  };

  const formatTimestamp = (date: Date) => {
    const now = new Date();
    const diffInSeconds = Math.floor((now.getTime() - date.getTime()) / 1000);

    if (diffInSeconds < 60) return 'Just now';
    if (diffInSeconds < 3600) return `${Math.floor(diffInSeconds / 60)} minutes ago`;
    if (diffInSeconds < 86400) return `${Math.floor(diffInSeconds / 3600)} hours ago`;
    if (diffInSeconds < 604800) return `${Math.floor(diffInSeconds / 86400)} days ago`;

    return date.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const getRoleBadgeColor = (role?: string) => {
    switch (role?.toUpperCase()) {
      case 'EO':
        return 'bg-blue-100 text-blue-800 border-blue-300';
      case 'DO':
      case 'DEPT_OFFICER':
        return 'bg-green-100 text-green-800 border-green-300';
      case 'FINANCE':
        return 'bg-amber-100 text-amber-800 border-amber-300';
      case 'VENDOR':
        return 'bg-orange-100 text-orange-800 border-orange-300';
      default:
        return 'bg-gray-100 text-gray-800 border-gray-300';
    }
  };

  const getInitials = (name?: string) => {
    if (!name) return 'U';
    return name
      .split(' ')
      .map(n => n[0])
      .join('')
      .toUpperCase()
      .slice(0, 2);
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center py-8">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      {error && (
        <div className="bg-red-50 border border-red-200 rounded-lg p-3 text-red-700 text-sm flex items-start space-x-2">
          <X className="w-4 h-4 mt-0.5 flex-shrink-0" />
          <span>{error}</span>
          <button onClick={() => setError(null)} className="ml-auto">
            <X className="w-4 h-4" />
          </button>
        </div>
      )}

      <div className="space-y-3">
        {comments.length === 0 ? (
          <div className="text-center py-8 bg-gray-50 border-2 border-dashed border-gray-300 rounded-lg">
            <MessageSquare className="w-12 h-12 text-gray-400 mx-auto mb-2" />
            <p className="text-gray-600 text-sm font-medium">No comments yet</p>
            <p className="text-gray-500 text-xs mt-1">Be the first to add a comment</p>
          </div>
        ) : (
          comments.map((comment) => (
            <div
              key={comment.id}
              className="bg-white border border-gray-200 rounded-lg p-4 hover:shadow-md transition-shadow"
            >
              <div className="flex items-start space-x-3">
                <div className="flex-shrink-0">
                  <div className="w-10 h-10 rounded-full bg-gradient-to-br from-blue-500 to-sky-600 flex items-center justify-center text-white font-semibold text-sm shadow-md">
                    {getInitials(comment.createdByName)}
                  </div>
                </div>

                <div className="flex-1 min-w-0">
                  <div className="flex items-center space-x-2 mb-1">
                    <span className="text-sm font-semibold text-gray-900">
                      {comment.createdByName || 'Unknown User'}
                    </span>
                    {comment.createdByRole && (
                      <span className={`text-xs px-2 py-0.5 rounded-full border ${getRoleBadgeColor(comment.createdByRole)}`}>
                        {comment.createdByRole.toUpperCase()}
                      </span>
                    )}
                    <span className="text-xs text-gray-500">
                      {formatTimestamp(comment.createdAt)}
                    </span>
                  </div>

                  {editingCommentId === comment.id ? (
                    <div className="space-y-2">
                      <textarea
                        value={editContent}
                        onChange={(e) => setEditContent(e.target.value)}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 text-sm"
                        rows={3}
                        placeholder="Edit your comment..."
                      />
                      <div className="flex space-x-2">
                        <button
                          onClick={() => handleEditComment(comment.id)}
                          className="px-3 py-1 text-sm bg-blue-600 text-white rounded-md hover:bg-blue-700 transition-colors"
                          disabled={!editContent.trim()}
                        >
                          Save
                        </button>
                        <button
                          onClick={cancelEdit}
                          className="px-3 py-1 text-sm bg-gray-200 text-gray-700 rounded-md hover:bg-gray-300 transition-colors"
                        >
                          Cancel
                        </button>
                      </div>
                    </div>
                  ) : (
                    <p className="text-sm text-gray-700 whitespace-pre-wrap break-words">
                      {comment.content}
                    </p>
                  )}
                </div>

                {user && user.id === comment.createdBy && editingCommentId !== comment.id && (
                  <div className="flex items-center space-x-1">
                    <button
                      onClick={() => startEdit(comment)}
                      className="p-1.5 text-gray-500 hover:text-blue-600 hover:bg-blue-50 rounded transition-colors"
                      title="Edit comment"
                    >
                      <Edit2 className="w-4 h-4" />
                    </button>
                    <button
                      onClick={() => handleDeleteComment(comment.id)}
                      className="p-1.5 text-gray-500 hover:text-red-600 hover:bg-red-50 rounded transition-colors"
                      title="Delete comment"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>
                )}
              </div>
            </div>
          ))
        )}
      </div>

      {user && (
        <form onSubmit={handleSubmitComment} className="bg-gray-50 border-2 border-gray-300 rounded-lg p-4">
          <div className="flex items-start space-x-3">
            <div className="flex-shrink-0">
              <div className="w-10 h-10 rounded-full bg-gradient-to-br from-green-500 to-emerald-600 flex items-center justify-center text-white font-semibold text-sm shadow-md">
                {getInitials(user.name)}
              </div>
            </div>
            <div className="flex-1">
              <textarea
                value={newComment}
                onChange={(e) => setNewComment(e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 text-sm resize-none"
                rows={3}
                placeholder="Add a comment..."
                disabled={submitting}
              />
              <div className="flex justify-end mt-2">
                <button
                  type="submit"
                  disabled={!newComment.trim() || submitting}
                  className="flex items-center space-x-2 px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 transition-colors disabled:bg-gray-400 disabled:cursor-not-allowed text-sm font-medium"
                >
                  {submitting ? (
                    <>
                      <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white"></div>
                      <span>Posting...</span>
                    </>
                  ) : (
                    <>
                      <Send className="w-4 h-4" />
                      <span>Post Comment</span>
                    </>
                  )}
                </button>
              </div>
            </div>
          </div>
        </form>
      )}

      {comments.length > 0 && (
        <div className="text-center">
          <button
            onClick={loadComments}
            className="text-sm text-blue-600 hover:text-blue-700 font-medium"
          >
            Refresh Comments
          </button>
        </div>
      )}
    </div>
  );
};

export default WorkflowStepComments;
