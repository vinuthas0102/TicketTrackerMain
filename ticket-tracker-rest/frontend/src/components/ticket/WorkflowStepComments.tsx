import React, { useState, useEffect, useRef } from 'react';
import { MessageSquare, Send, CreditCard as Edit2, Trash2, X, Paperclip, Download, FileText, Image as ImageIcon, File, Archive, Mail, MessageCircle, Smartphone } from 'lucide-react';
import { WorkflowComment, WorkflowStep } from '../../types';
import { TicketService } from '../../services/ticketService';
import { useAuth } from '../../context/AuthContext';

interface WorkflowStepCommentsProps {
  stepId: string;
  step?: WorkflowStep;
  ticketAssignedTo?: string;
  parentStepAssignedTo?: string;
  recipientName?: string;
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
  parentStepAssignedTo,
  recipientName,
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
    return user.id === ticketAssignedTo || user.id === stepAssignedTo || user.id === parentStepAssignedTo;
  }, [user, stepLevel, ticketAssignedTo, stepAssignedTo, parentStepAssignedTo]);

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
      setChannelInfo(`Message queued for ${selectedChannel}. Configure your webhook in Settings to enable delivery.`);
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
      setError('Failed to send message. Please try again.');
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
      setError('Failed to update message. Please try again.');
    }
  };

  const handleDeleteComment = async (commentId: string) => {
    if (!user) return;
    if (!confirm('Delete this message?')) return;
    try {
      setError(null);
      await TicketService.deleteStepComment(commentId, user.id);
      await loadComments();
      if (onRefresh) onRefresh();
    } catch (err) {
      console.error('Failed to delete comment:', err);
      setError('Failed to delete message. Please try again.');
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
      setAttachmentError(`File exceeds 5 MB limit (${(file.size / 1048576).toFixed(1)} MB)`);
      return;
    }
    if (!ALLOWED_FILE_TYPES.includes(file.type)) {
      setAttachmentError('Unsupported file type. Allowed: PDF, images, Word, Excel, ZIP');
      return;
    }
    setAttachmentFile(file);
  };

  const removeAttachment = () => {
    setAttachmentFile(null);
    setAttachmentError(null);
    if (fileInputRef.current) fileInputRef.current.value = '';
  };

  const handleDownloadAttachment = async (commentId: string, attachmentName: string) => {
    try {
      setDownloadingAttachment(commentId);
      const url = await TicketService.getChatAttachmentUrl(commentId);
      const link = document.createElement('a');
      link.href = url;
      link.download = attachmentName;
      link.target = '_blank';
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
    } catch (err) {
      setError('Failed to download attachment.');
    } finally {
      setDownloadingAttachment(null);
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      if (newComment.trim() && !submitting) handleSubmitComment();
    }
  };

  const formatTimestamp = (date: Date) => {
    const diff = Math.floor((Date.now() - date.getTime()) / 1000);
    if (diff < 60) return 'Just now';
    if (diff < 3600) return `${Math.floor(diff / 60)}m ago`;
    if (diff < 86400) return `${Math.floor(diff / 3600)}h ago`;
    if (diff < 604800) return `${Math.floor(diff / 86400)}d ago`;
    return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });
  };

  const getRoleBadgeColor = (role?: string) => {
    switch (role?.toUpperCase()) {
      case 'EO': return 'bg-blue-50 text-blue-700 border-blue-200';
      case 'DO': case 'DEPT_OFFICER': return 'bg-emerald-50 text-emerald-700 border-emerald-200';
      case 'FINANCE': return 'bg-amber-50 text-amber-700 border-amber-200';
      case 'VENDOR': return 'bg-orange-50 text-orange-700 border-orange-200';
      default: return 'bg-gray-50 text-gray-600 border-gray-200';
    }
  };

  const getAvatarGradient = (role?: string) => {
    switch (role?.toUpperCase()) {
      case 'EO': return 'from-blue-500 to-blue-600';
      case 'DO': case 'DEPT_OFFICER': return 'from-emerald-500 to-emerald-600';
      case 'FINANCE': return 'from-amber-500 to-amber-600';
      case 'VENDOR': return 'from-orange-500 to-orange-600';
      default: return 'from-slate-500 to-slate-600';
    }
  };

  const getInitials = (name?: string) =>
    name ? name.split(' ').map(n => n[0]).join('').toUpperCase().slice(0, 2) : 'U';

  const getFileIcon = (type?: string) => {
    if (!type) return <File className="w-3.5 h-3.5" />;
    if (type.startsWith('image/')) return <ImageIcon className="w-3.5 h-3.5" />;
    if (type.includes('zip')) return <Archive className="w-3.5 h-3.5" />;
    return <FileText className="w-3.5 h-3.5" />;
  };

  const formatFileSize = (bytes: number) => {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1048576) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / 1048576).toFixed(1)} MB`;
  };

  if (loading && comments.length === 0) {
    return (
      <div className="flex items-center justify-center h-full py-16">
        <div className="animate-spin rounded-full h-7 w-7 border-2 border-gray-200 border-b-green-600" />
      </div>
    );
  }

  return (
    <div className="flex flex-col h-full bg-gray-50">

      {/* Error Banner */}
      {error && (
        <div className="flex items-center gap-2 px-4 py-2 bg-red-50 border-b border-red-100 text-xs text-red-700 flex-shrink-0">
          <span className="flex-1">{error}</span>
          <button onClick={() => setError(null)} className="shrink-0 text-red-400 hover:text-red-600">
            <X className="w-3.5 h-3.5" />
          </button>
        </div>
      )}

      {/* Messages Area */}
      <div className="flex-1 overflow-y-auto px-4 py-4 space-y-4 min-h-0">
        {comments.length === 0 ? (
          <div className="flex flex-col items-center justify-center h-full text-center py-16">
            <div className="w-14 h-14 rounded-full bg-white border border-gray-200 shadow-sm flex items-center justify-center mb-4">
              <Send className="w-6 h-6 text-green-500" />
            </div>
            <p className="text-sm font-semibold text-gray-800">No messages yet</p>
            <p className="text-xs text-gray-400 mt-1">Start the conversation below</p>
          </div>
        ) : (
          comments.map((comment) => {
            const isOwn = user?.id === comment.createdBy;
            const isEdited = comment.updatedAt && comment.updatedAt.getTime() !== comment.createdAt.getTime();

            return (
              <div key={comment.id} className={`flex ${isOwn ? 'justify-end' : 'justify-start'}`}>
                <div className={`flex ${isOwn ? 'flex-row-reverse' : 'flex-row'} items-end gap-2 max-w-[82%]`}>

                  {/* Avatar */}
                  <div className={`w-7 h-7 rounded-full bg-gradient-to-br ${getAvatarGradient(comment.createdByRole)} flex items-center justify-center text-white font-semibold text-[10px] flex-shrink-0 mb-0.5 shadow-sm`}>
                    {getInitials(comment.createdByName)}
                  </div>

                  <div className={`flex flex-col gap-0.5 ${isOwn ? 'items-end' : 'items-start'}`}>
                    {/* Meta */}
                    <div className={`flex items-center gap-1.5 px-0.5 ${isOwn ? 'flex-row-reverse' : 'flex-row'}`}>
                      <span className="text-[11px] font-semibold text-gray-700 leading-none">
                        {comment.createdByName || 'Unknown'}
                      </span>
                      {comment.createdByRole && (
                        <span className={`text-[9px] px-1.5 py-0.5 rounded-full border font-medium ${getRoleBadgeColor(comment.createdByRole)}`}>
                          {comment.createdByRole.toUpperCase()}
                        </span>
                      )}
                      <span className="text-[10px] text-gray-400 leading-none">{formatTimestamp(comment.createdAt)}</span>
                      {isEdited && <span className="text-[9px] text-gray-400 italic">edited</span>}
                    </div>

                    {/* Bubble */}
                    <div className={`px-3.5 py-2.5 rounded-2xl text-sm leading-relaxed shadow-sm ${
                      isOwn
                        ? 'bg-green-600 text-white rounded-br-sm'
                        : 'bg-white border border-gray-200 text-gray-800 rounded-bl-sm'
                    }`}>
                      {editingCommentId === comment.id ? (
                        <div className="space-y-2 min-w-[200px]">
                          <textarea
                            value={editContent}
                            onChange={(e) => setEditContent(e.target.value)}
                            className="w-full px-2 py-1.5 border border-gray-300 rounded-lg text-xs text-gray-800 focus:outline-none focus:ring-2 focus:ring-blue-500 resize-none"
                            rows={3}
                            autoFocus
                          />
                          <div className="flex gap-1.5">
                            <button
                              onClick={() => handleEditComment(comment.id)}
                              disabled={!editContent.trim()}
                              className="px-3 py-1 text-xs bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:opacity-50 transition-colors"
                            >
                              Save
                            </button>
                            <button
                              onClick={cancelEdit}
                              className="px-3 py-1 text-xs bg-gray-100 text-gray-700 rounded-md hover:bg-gray-200 transition-colors"
                            >
                              Cancel
                            </button>
                          </div>
                        </div>
                      ) : (
                        <>
                          <p className="whitespace-pre-wrap break-words">{comment.content}</p>
                          {comment.attachmentName && (
                            <div className={`mt-2 flex items-center gap-2 px-2.5 py-1.5 rounded-lg border ${
                              isOwn ? 'bg-white/15 border-white/20' : 'bg-gray-50 border-gray-200'
                            }`}>
                              <span className={isOwn ? 'text-white/80' : 'text-gray-400'}>
                                {getFileIcon(comment.attachmentType)}
                              </span>
                              <p className={`text-xs font-medium flex-1 truncate ${isOwn ? 'text-white' : 'text-gray-700'}`}>
                                {comment.attachmentName}
                              </p>
                              <button
                                onClick={() => handleDownloadAttachment(comment.id, comment.attachmentName!)}
                                disabled={downloadingAttachment === comment.id}
                                className={`shrink-0 p-1 rounded transition-colors ${isOwn ? 'text-white/80 hover:bg-white/20' : 'text-gray-400 hover:text-gray-600 hover:bg-gray-200'} disabled:opacity-40`}
                                title="Download"
                              >
                                {downloadingAttachment === comment.id
                                  ? <div className="w-3.5 h-3.5 border-2 border-current border-b-transparent rounded-full animate-spin" />
                                  : <Download className="w-3.5 h-3.5" />}
                              </button>
                            </div>
                          )}
                        </>
                      )}
                    </div>

                    {/* Edit / Delete */}
                    {isOwn && editingCommentId !== comment.id && (
                      <div className="flex gap-1 px-0.5">
                        <button
                          onClick={() => startEdit(comment)}
                          className="p-1 text-gray-300 hover:text-blue-500 hover:bg-blue-50 rounded transition-colors"
                          title="Edit"
                        >
                          <Edit2 className="w-3 h-3" />
                        </button>
                        <button
                          onClick={() => handleDeleteComment(comment.id)}
                          className="p-1 text-gray-300 hover:text-red-500 hover:bg-red-50 rounded transition-colors"
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

      {/* Bottom input section */}
      {canParticipate ? (
        <div className="flex-shrink-0 bg-white border-t border-gray-200">

          {/* Channel Info Banner */}
          {channelInfo && (
            <div className="flex items-center gap-2 px-4 py-1.5 bg-blue-50 border-b border-blue-100 text-xs text-blue-700">
              <span className="flex-1">{channelInfo}</span>
              <button onClick={() => setChannelInfo(null)} className="shrink-0 text-blue-400 hover:text-blue-600">
                <X className="w-3 h-3" />
              </button>
            </div>
          )}

          {/* Attachment preview */}
          {attachmentFile && (
            <div className="flex items-center gap-2 px-4 py-1.5 bg-slate-50 border-b border-gray-200">
              <span className="text-gray-400">{getFileIcon(attachmentFile.type)}</span>
              <span className="text-xs text-gray-700 flex-1 truncate font-medium">{attachmentFile.name}</span>
              <span className="text-[10px] text-gray-400 shrink-0">{formatFileSize(attachmentFile.size)}</span>
              <button onClick={removeAttachment} className="shrink-0 p-0.5 text-gray-400 hover:text-red-500 transition-colors">
                <X className="w-3.5 h-3.5" />
              </button>
            </div>
          )}

          {/* Attachment error */}
          {attachmentError && (
            <div className="flex items-center gap-2 px-4 py-1.5 bg-red-50 border-b border-red-100 text-xs text-red-600">
              <span className="flex-1">{attachmentError}</span>
              <button onClick={() => setAttachmentError(null)} className="shrink-0 text-red-400 hover:text-red-600">
                <X className="w-3 h-3" />
              </button>
            </div>
          )}

          {/* Channel selector — just above the input row */}
          <div className="flex items-center gap-1.5 px-4 py-2 border-b border-gray-100">
            <span className="text-[11px] text-gray-400 font-medium mr-0.5">via</span>
            {CHANNELS.map((ch) => {
              const Icon = ch.icon;
              const active = selectedChannel === ch.id;
              return (
                <button
                  key={ch.id}
                  onClick={() => { setSelectedChannel(ch.id); setChannelInfo(null); }}
                  className={`inline-flex items-center gap-1 px-2.5 py-1 text-[11px] font-medium rounded-full border transition-all ${
                    active
                      ? 'bg-green-600 text-white border-green-600'
                      : 'bg-white text-gray-500 border-gray-200 hover:border-gray-400 hover:text-gray-700'
                  }`}
                >
                  <Icon className="w-3 h-3" />
                  {ch.label}
                </button>
              );
            })}
          </div>

          {/* Message input row */}
          <div className="flex items-end gap-2 px-3 py-2.5">
            <button
              onClick={() => fileInputRef.current?.click()}
              disabled={submitting}
              className="shrink-0 p-2 text-gray-400 hover:text-green-600 hover:bg-green-50 rounded-full transition-colors"
              title="Attach file"
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
            <textarea
              ref={textareaRef}
              value={newComment}
              onChange={(e) => setNewComment(e.target.value)}
              onKeyDown={handleKeyDown}
              disabled={submitting}
              rows={1}
              placeholder="Type a message…  (Enter to send)"
              className="flex-1 px-3.5 py-2 border border-gray-200 rounded-2xl text-sm resize-none focus:outline-none focus:ring-2 focus:ring-green-500 focus:border-transparent bg-gray-50 placeholder:text-gray-400"
              style={{ maxHeight: '100px' }}
            />
            <button
              onClick={handleSubmitComment}
              disabled={!newComment.trim() || submitting}
              className="shrink-0 w-9 h-9 flex items-center justify-center rounded-full bg-green-600 hover:bg-green-700 disabled:bg-gray-200 text-white disabled:text-gray-400 transition-all shadow-sm"
              title="Send"
            >
              {submitting
                ? <div className="w-4 h-4 border-2 border-white border-b-transparent rounded-full animate-spin" />
                : <Send className="w-4 h-4" />}
            </button>
          </div>
        </div>
      ) : (
        <div className="flex-shrink-0 border-t border-gray-200 bg-gray-50 px-4 py-3 text-center">
          <p className="text-xs text-gray-400">You are not a participant in this conversation.</p>
        </div>
      )}
    </div>
  );
};

export default WorkflowStepComments;
