import React, { useState, useEffect, useRef } from 'react';
import { MessageSquare, Send, CreditCard as Edit2, Trash2, X, Paperclip, Download, FileText, Image as ImageIcon, File, Archive, Mail, MessageCircle, Smartphone, Check } from 'lucide-react';
import { WorkflowComment, WorkflowStep } from '../../types';
import { TicketService } from '../../services/ticketService';
import { useAuth } from '../../context/AuthContext';

interface WorkflowStepCommentsProps {
  stepId: string;
  step?: WorkflowStep;
  ticketAssignedTo?: string;
  onRefresh?: () => void;
  onChannelInvoke?: (channel: string, message: string) => void;
}

const ALLOWED_FILE_TYPES = [
  'application/pdf',
  'image/jpeg',
  'image/jpg',
  'image/png',
  'image/gif',
  'application/msword',
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
  'application/vnd.ms-excel',
  'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  'application/zip',
  'application/x-zip-compressed',
];

const MAX_FILE_SIZE = 5 * 1024 * 1024;

const CHANNELS = [
  { id: 'in-app', label: 'In-App', icon: MessageSquare },
  { id: 'email', label: 'Email', icon: Mail },
  { id: 'sms', label: 'SMS', icon: Smartphone },
  { id: 'whatsapp', label: 'WhatsApp', icon: MessageCircle },
];

const WorkflowStepComments: React.FC<WorkflowStepCommentsProps> = ({
  stepId,
  step,
  ticketAssignedTo,
  onRefresh,
  onChannelInvoke,
}) => {
  const { user } = useAuth();
  const [comments, setComments] = useState<WorkflowComment[]>([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [newComment, setNewComment] = useState('');
  const [editingCommentId, setEditingCommentId] = useState<string | null>(null);
  const [editContent, setEditContent] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [selectedChannel, setSelectedChannel] = useState('in-app');
  const [channelInfo, setChannelInfo] = useState<string | null>(null);
  const [attachmentFile, setAttachmentFile] = useState<File | null>(null);
  const [attachmentError, setAttachmentError] = useState<string | null>(null);
  const [downloadingAttachment, setDownloadingAttachment] = useState<string | null>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  const stepLevel = step ? (step.level_3 && step.level_3 > 0 ? 3 : step.level_2 && step.level_2 > 0 ? 2 : 1) : 1;
  const stepAssignedTo = step?.assignedTo;

  const canParticipate = React.useMemo(() => {
    if (!user) return false;
    if (stepLevel === 1) {
      return user.role === 'EO' || user.id === ticketAssignedTo;
    }
    return user.id === ticketAssignedTo || user.id === stepAssignedTo;
  }, [user, stepLevel, ticketAssignedTo, stepAssignedTo]);

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

  useEffect(() => {
    if (!loading && messagesEndRef.current) {
      messagesEndRef.current.scrollIntoView({ behavior: 'smooth' });
    }
  }, [comments, loading]);

  const handleSubmitComment = async () => {
    if (!user || !newComment.trim()) return;

    if (selectedChannel !== 'in-app') {
      onChannelInvoke?.(selectedChannel, newComment.trim());
      setChannelInfo(`Message queued for ${selectedChannel} channel. Configure your webhook in Settings to enable delivery.`);
    }

    try {
      setSubmitting(true);
      setError(null);
      await TicketService.addStepComment(stepId, newComment, user.id, {
        attachmentFile: attachmentFile || undefined,
        channel: selectedChannel,
      });
      setNewComment('');
      setAttachmentFile(null);
      if (fileInputRef.current) fileInputRef.current.value = '';
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

  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    setAttachmentError(null);

    if (file.size > MAX_FILE_SIZE) {
      setAttachmentError(`File size exceeds 5MB limit. Your file is ${(file.size / (1024 * 1024)).toFixed(2)}MB`);
      return;
    }

    if (!ALLOWED_FILE_TYPES.includes(file.type)) {
      setAttachmentError('File type not supported. Allowed: PDF, Images, Word, Excel, ZIP');
      return;
    }

    setAttachmentFile(file);
  };

  const removeAttachment = () => {
    setAttachmentFile(null);
    setAttachmentError(null);
    if (fileInputRef.current) fileInputRef.current.value = '';
  };

  const handleDownloadAttachment = async (attachmentPath: string, attachmentName: string) => {
    try {
      setDownloadingAttachment(attachmentPath);
      const url = await TicketService.getChatAttachmentUrl(attachmentPath);
      const link = document.createElement('a');
      link.href = url;
      link.download = attachmentName;
      link.target = '_blank';
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
    } catch (err) {
      console.error('Failed to download attachment:', err);
      setError('Failed to download attachment. Please try again.');
    } finally {
      setDownloadingAttachment(null);
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      if (newComment.trim() && !submitting) {
        handleSubmitComment();
      }
    }
  };

  const formatTimestamp = (date: Date) => {
    const now = new Date();
    const diffInSeconds = Math.floor((now.getTime() - date.getTime()) / 1000);

    if (diffInSeconds < 60) return 'Just now';
    if (diffInSeconds < 3600) return `${Math.floor(diffInSeconds / 60)} min ago`;
    if (diffInSeconds < 86400) return `${Math.floor(diffInSeconds / 3600)} hr ago`;
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

  const getAvatarColor = (role?: string) => {
    switch (role?.toUpperCase()) {
      case 'EO':
        return 'from-blue-500 to-blue-600';
      case 'DO':
      case 'DEPT_OFFICER':
        return 'from-green-500 to-green-600';
      case 'FINANCE':
        return 'from-amber-500 to-amber-600';
      case 'VENDOR':
        return 'from-orange-500 to-orange-600';
      default:
        return 'from-gray-500 to-gray-600';
    }
  };

  const getInitials = (name?: string) => {
    if (!name) return 'U';
    return name.split(' ').map(n => n[0]).join('').toUpperCase().slice(0, 2);
  };

  const getFileIcon = (type?: string) => {
    if (!type) return <File className="w-4 h-4" />;
    if (type.startsWith('image/')) return <ImageIcon className="w-4 h-4" />;
    if (type === 'application/zip' || type === 'application/x-zip-compressed') return <Archive className="w-4 h-4" />;
    return <FileText className="w-4 h-4" />;
  };

  const formatFileSize = (bytes: number) => {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  };

  if (loading && comments.length === 0) {
    return (
      <div className="flex items-center justify-center py-12">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-green-600"></div>
      </div>
    );
  }

  return (
    <div className="flex flex-col h-full">
      {/* Channel Selector Bar */}
      <div className="flex items-center gap-1.5 px-3 py-2 bg-gray-50 border-b border-gray-200 flex-shrink-0">
        <span className="text-xs text-gray-500 font-medium mr-1">via</span>
        {CHANNELS.map((ch) => {
          const Icon = ch.icon;
          const isActive = selectedChannel === ch.id;
          return (
            <button
              key={ch.id}
              onClick={() => {
                setSelectedChannel(ch.id);
                setChannelInfo(null);
              }}
              className={`flex items-center gap-1.5 px-2.5 py-1 text-xs font-medium rounded-full border transition-all ${
                isActive
                  ? 'bg-green-600 text-white border-green-600 shadow-sm'
                  : 'bg-white text-gray-600 border-gray-300 hover:border-gray-400 hover:bg-gray-50'
              }`}
              title={ch.label}
            >
              <Icon className="w-3 h-3" />
              <span>{ch.label}</span>
            </button>
          );
        })}
      </div>

      {/* Channel Info Banner */}
      {channelInfo && (
        <div className="px-3 py-1.5 bg-blue-50 border-b border-blue-100 text-xs text-blue-700 flex items-center justify-between flex-shrink-0">
          <span>{channelInfo}</span>
          <button onClick={() => setChannelInfo(null)} className="ml-2 shrink-0">
            <X className="w-3 h-3" />
          </button>
        </div>
      )}

      {/* Error Banner */}
      {error && (
        <div className="px-3 py-2 bg-red-50 border-b border-red-100 text-xs text-red-700 flex items-start space-x-2 flex-shrink-0">
          <X className="w-3.5 h-3.5 mt-0.5 flex-shrink-0" />
          <span>{error}</span>
          <button onClick={() => setError(null)} className="ml-auto shrink-0">
            <X className="w-3.5 h-3.5" />
          </button>
        </div>
      )}

      {/* Messages Area */}
      <div className="flex-1 overflow-y-auto px-3 py-3 space-y-3 min-h-0">
        {comments.length === 0 ? (
          <div className="flex flex-col items-center justify-center h-full text-center py-12">
            <div className="w-16 h-16 rounded-full bg-green-50 flex items-center justify-center mb-3">
              <Send className="w-7 h-7 text-green-500" />
            </div>
            <p className="text-gray-700 text-sm font-semibold">No messages yet</p>
            <p className="text-gray-400 text-xs mt-1">Start the conversation below</p>
          </div>
        ) : (
          comments.map((comment) => {
            const isOwn = user && user.id === comment.createdBy;
            const isEdited = comment.updatedAt && comment.updatedAt.getTime() !== comment.createdAt.getTime();

            return (
              <div
                key={comment.id}
                className={`flex ${isOwn ? 'justify-end' : 'justify-start'}`}
              >
                <div className={`flex ${isOwn ? 'flex-row-reverse' : 'flex-row'} items-start gap-2 max-w-[85%]`}>
                  {/* Avatar */}
                  <div className={`w-8 h-8 rounded-full bg-gradient-to-br ${getAvatarColor(comment.createdByRole)} flex items-center justify-center text-white font-semibold text-xs flex-shrink-0 shadow-sm`}>
                    {getInitials(comment.createdByName)}
                  </div>

                  {/* Message Bubble */}
                  <div className={`flex flex-col ${isOwn ? 'items-end' : 'items-start'}`}>
                    {/* Meta row */}
                    <div className={`flex items-center gap-1.5 mb-1 ${isOwn ? 'flex-row-reverse' : 'flex-row'}`}>
                      <span className="text-xs font-semibold text-gray-700">
                        {comment.createdByName || 'Unknown'}
                      </span>
                      {comment.createdByRole && (
                        <span className={`text-[10px] px-1.5 py-0.5 rounded-full border ${getRoleBadgeColor(comment.createdByRole)}`}>
                          {comment.createdByRole.toUpperCase()}
                        </span>
                      )}
                      <span className="text-[10px] text-gray-400">
                        {formatTimestamp(comment.createdAt)}
                      </span>
                      {isEdited && (
                        <span className="text-[10px] text-gray-400 italic">edited</span>
                      )}
                    </div>

                    {/* Bubble */}
                    <div
                      className={`relative px-3 py-2 rounded-2xl text-sm break-words ${
                        isOwn
                          ? 'bg-gradient-to-br from-green-500 to-green-600 text-white rounded-br-md'
                          : 'bg-white border border-gray-200 text-gray-800 rounded-bl-md shadow-sm'
                      }`}
                    >
                      {editingCommentId === comment.id ? (
                        <div className="space-y-2">
                          <textarea
                            value={editContent}
                            onChange={(e) => setEditContent(e.target.value)}
                            className="w-full px-2 py-1.5 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 text-sm text-gray-800"
                            rows={3}
                            placeholder="Edit your comment..."
                            autoFocus
                          />
                          <div className="flex gap-2">
                            <button
                              onClick={() => handleEditComment(comment.id)}
                              className="px-2.5 py-1 text-xs bg-blue-600 text-white rounded-md hover:bg-blue-700 transition-colors"
                              disabled={!editContent.trim()}
                            >
                              Save
                            </button>
                            <button
                              onClick={cancelEdit}
                              className="px-2.5 py-1 text-xs bg-gray-200 text-gray-700 rounded-md hover:bg-gray-300 transition-colors"
                            >
                              Cancel
                            </button>
                          </div>
                        </div>
                      ) : (
                        <>
                          <p className="whitespace-pre-wrap">{comment.content}</p>

                          {/* Attachment Card */}
                          {comment.attachmentPath && comment.attachmentName && (
                            <div
                              className={`mt-2 flex items-center gap-2 px-2.5 py-1.5 rounded-lg border ${
                                isOwn
                                  ? 'bg-green-600/20 border-white/20'
                                  : 'bg-gray-50 border-gray-200'
                              }`}
                            >
                              {getFileIcon(comment.attachmentType)}
                              <div className="flex-1 min-w-0">
                                <p className={`text-xs font-medium truncate ${isOwn ? 'text-white' : 'text-gray-700'}`}>
                                  {comment.attachmentName}
                                </p>
                              </div>
                              <button
                                onClick={() => handleDownloadAttachment(comment.attachmentPath!, comment.attachmentName!)}
                                disabled={downloadingAttachment === comment.attachmentPath}
                                className={`p-1 rounded transition-colors ${
                                  isOwn
                                    ? 'text-white hover:bg-white/20'
                                    : 'text-gray-500 hover:bg-gray-200 hover:text-gray-700'
                                } disabled:opacity-50`}
                                title="Download"
                              >
                                {downloadingAttachment === comment.attachmentPath ? (
                                  <div className="animate-spin rounded-full h-3.5 w-3.5 border-b-2 border-current"></div>
                                ) : (
                                  <Download className="w-3.5 h-3.5" />
                                )}
                              </button>
                            </div>
                          )}
                        </>
                      )}
                    </div>

                    {/* Edit/Delete Actions */}
                    {isOwn && editingCommentId !== comment.id && (
                      <div className={`flex gap-1 mt-1 ${isOwn ? 'justify-end' : 'justify-start'}`}>
                        <button
                          onClick={() => startEdit(comment)}
                          className="p-1 text-gray-400 hover:text-blue-600 hover:bg-blue-50 rounded transition-colors"
                          title="Edit"
                        >
                          <Edit2 className="w-3 h-3" />
                        </button>
                        <button
                          onClick={() => handleDeleteComment(comment.id)}
                          className="p-1 text-gray-400 hover:text-red-600 hover:bg-red-50 rounded transition-colors"
                          title="Delete"
                        >
                          <Trash2 className="w-3 h-3" />
                        </button>
                      </div>
                    )}
                  </div>
                </div>
              </div>
            );
          })
        )}
        <div ref={messagesEndRef} />
      </div>

      {/* Input Toolbar */}
      {canParticipate ? (
        <div className="flex-shrink-0 border-t border-gray-200 bg-white">
          {/* Attachment Preview */}
          {attachmentFile && (
            <div className="flex items-center gap-2 px-3 py-1.5 bg-blue-50 border-b border-blue-100">
              {getFileIcon(attachmentFile.type)}
              <span className="text-xs text-gray-700 flex-1 truncate">{attachmentFile.name}</span>
              <span className="text-xs text-gray-400">{formatFileSize(attachmentFile.size)}</span>
              <button onClick={removeAttachment} className="p-0.5 text-gray-400 hover:text-red-600 rounded">
                <X className="w-3.5 h-3.5" />
              </button>
            </div>
          )}

          {/* Attachment Error */}
          {attachmentError && (
            <div className="px-3 py-1.5 bg-red-50 border-b border-red-100 text-xs text-red-600 flex items-center justify-between">
              <span>{attachmentError}</span>
              <button onClick={() => setAttachmentError(null)} className="shrink-0">
                <X className="w-3 h-3" />
              </button>
            </div>
          )}

          {/* Input Row */}
          <div className="flex items-end gap-2 px-3 py-2">
            {/* Paperclip */}
            <button
              onClick={() => fileInputRef.current?.click()}
              className="p-2 text-gray-500 hover:text-green-600 hover:bg-green-50 rounded-full transition-colors flex-shrink-0"
              title="Attach file"
              disabled={submitting}
            >
              <Paperclip className="w-5 h-5" />
            </button>
            <input
              ref={fileInputRef}
              type="file"
              className="hidden"
              onChange={handleFileSelect}
              accept=".pdf,.jpg,.jpeg,.png,.gif,.doc,.docx,.xls,.xlsx,.zip"
            />

            {/* Text Input */}
            <textarea
              ref={textareaRef}
              value={newComment}
              onChange={(e) => setNewComment(e.target.value)}
              onKeyDown={handleKeyDown}
              className="flex-1 px-3 py-2 border border-gray-300 rounded-full focus:outline-none focus:ring-2 focus:ring-green-500 focus:border-green-500 text-sm resize-none"
              rows={1}
              placeholder="Type a message... (Enter to send, Shift+Enter for new line)"
              disabled={submitting}
              style={{ maxHeight: '120px' }}
            />

            {/* Send Button */}
            <button
              onClick={handleSubmitComment}
              disabled={!newComment.trim() || submitting}
              className="w-9 h-9 flex items-center justify-center bg-gradient-to-br from-green-500 to-green-600 text-white rounded-full hover:from-green-600 hover:to-green-700 transition-all disabled:from-gray-300 disabled:to-gray-400 disabled:cursor-not-allowed flex-shrink-0 shadow-sm"
              title="Send"
            >
              {submitting ? (
                <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white"></div>
              ) : (
                <Send className="w-4 h-4" />
              )}
            </button>
          </div>
        </div>
      ) : (
        <div className="flex-shrink-0 border-t border-gray-200 bg-gray-50 px-3 py-3 text-center">
          <p className="text-xs text-gray-500">You are not part of this conversation.</p>
        </div>
      )}
    </div>
  );
};

export default WorkflowStepComments;
