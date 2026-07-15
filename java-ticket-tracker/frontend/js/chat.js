/**
 * Chat component for workflow step comments.
 * Provides a chat panel UI for viewing, posting, editing, and deleting comments.
 */
const ChatPanel = (function () {
  let activeStepId = null;
  let comments = [];
  let loading = false;
  let submitting = false;
  let editingCommentId = null;
  let error = null;

  function getInitials(name) {
    if (!name) return 'U';
    return name.split(' ').map(function (n) { return n[0]; }).join('').toUpperCase().slice(0, 2);
  }

  function formatTimestamp(dateStr) {
    var date = new Date(dateStr);
    var now = new Date();
    var diffSec = Math.floor((now.getTime() - date.getTime()) / 1000);
    if (diffSec < 60) return 'Just now';
    if (diffSec < 3600) return Math.floor(diffSec / 60) + ' minutes ago';
    if (diffSec < 86400) return Math.floor(diffSec / 3600) + ' hours ago';
    if (diffSec < 604800) return Math.floor(diffSec / 86400) + ' days ago';
    return date.toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });
  }

  function getRoleBadgeClass(role) {
    switch ((role || '').toUpperCase()) {
      case 'EO': return 'role-badge-eo';
      case 'DO':
      case 'DEPT_OFFICER': return 'role-badge-do';
      case 'FINANCE': return 'role-badge-finance';
      case 'VENDOR': return 'role-badge-vendor';
      default: return 'role-badge-default';
    }
  }

  function getCurrentUser() {
    var userJson = localStorage.getItem('currentUser');
    if (!userJson) return null;
    try { return JSON.parse(userJson); } catch (e) { return null; }
  }

  async function loadComments() {
    if (!activeStepId) return;
    loading = true;
    error = null;
    render();
    try {
      comments = await API.getStepComments(activeStepId);
    } catch (err) {
      error = 'Failed to load comments. Please try again.';
      console.error('Failed to load comments:', err);
    } finally {
      loading = false;
      render();
    }
  }

  async function handleSubmitComment() {
    var textarea = document.getElementById('chat-new-comment-input');
    if (!textarea) return;
    var content = textarea.value.trim();
    if (!content) return;

    submitting = true;
    error = null;
    render();
    try {
      await API.addStepComment(activeStepId, content);
      textarea.value = '';
      await loadComments();
    } catch (err) {
      error = 'Failed to add comment. Please try again.';
      console.error('Failed to add comment:', err);
      render();
    } finally {
      submitting = false;
      render();
    }
  }

  async function handleEditComment(commentId) {
    var textarea = document.getElementById('chat-edit-input-' + commentId);
    if (!textarea) return;
    var content = textarea.value.trim();
    if (!content) return;

    try {
      await API.updateStepComment(commentId, content);
      editingCommentId = null;
      await loadComments();
    } catch (err) {
      error = 'Failed to update comment. Please try again.';
      console.error('Failed to update comment:', err);
      render();
    }
  }

  async function handleDeleteComment(commentId) {
    if (!confirm('Are you sure you want to delete this comment?')) return;
    try {
      await API.deleteStepComment(commentId);
      await loadComments();
    } catch (err) {
      error = 'Failed to delete comment. Please try again.';
      console.error('Failed to delete comment:', err);
      render();
    }
  }

  function startEdit(commentId, content) {
    editingCommentId = commentId;
    render();
    var textarea = document.getElementById('chat-edit-input-' + commentId);
    if (textarea) textarea.value = content;
  }

  function cancelEdit() {
    editingCommentId = null;
    render();
  }

  function render() {
    var container = document.getElementById('chat-panel-container');
    if (!container) return;

    var user = getCurrentUser();
    var html = '';

    // Header
    html += '<div class="chat-header">';
    html += '  <div class="chat-header-info">';
    html += '    <div class="chat-header-title">';
    html += '      <svg class="chat-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>';
    html += '      <span>Task Chat</span>';
    html += '    </div>';
    html += '    <p class="chat-header-subtitle">' + (window._chatStepTitle || '') + '</p>';
    html += '  </div>';
    html += '  <button class="chat-close-btn" onclick="ChatPanel.close()" title="Close">';
    html += '    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>';
    html += '  </button>';
    html += '</div>';

    // Error banner
    if (error) {
      html += '<div class="chat-error">';
      html += '  <span>' + error + '</span>';
      html += '  <button onclick="ChatPanel._dismissError()" class="chat-error-dismiss">&times;</button>';
      html += '</div>';
    }

    // Comments list
    html += '<div class="chat-messages" id="chat-messages-list">';
    if (loading) {
      html += '<div class="chat-loading"><div class="chat-spinner"></div></div>';
    } else if (comments.length === 0) {
      html += '<div class="chat-empty">';
      html += '  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" class="chat-empty-icon"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>';
      html += '  <p class="chat-empty-title">No comments yet</p>';
      html += '  <p class="chat-empty-subtitle">Be the first to add a comment</p>';
      html += '</div>';
    } else {
      comments.forEach(function (comment) {
        var isOwn = user && user.id === comment.createdBy;
        html += '<div class="chat-message">';
        html += '  <div class="chat-avatar chat-avatar-' + (isOwn ? 'own' : 'other') + '">' + getInitials(comment.createdByName) + '</div>';
        html += '  <div class="chat-message-body">';
        html += '    <div class="chat-message-meta">';
        html += '      <span class="chat-message-author">' + (comment.createdByName || 'Unknown User') + '</span>';
        if (comment.createdByRole) {
          html += '      <span class="chat-role-badge ' + getRoleBadgeClass(comment.createdByRole) + '">' + comment.createdByRole.toUpperCase() + '</span>';
        }
        html += '      <span class="chat-message-time">' + formatTimestamp(comment.createdAt) + '</span>';
        html += '    </div>';
        if (editingCommentId === comment.id) {
          html += '    <div class="chat-edit-form">';
          html += '      <textarea id="chat-edit-input-' + comment.id + '" class="chat-edit-textarea" rows="3">' + (comment.content || '') + '</textarea>';
          html += '      <div class="chat-edit-actions">';
          html += '        <button onclick="ChatPanel._saveEdit(\'' + comment.id + '\')" class="chat-btn chat-btn-primary">Save</button>';
          html += '        <button onclick="ChatPanel._cancelEdit()" class="chat-btn chat-btn-secondary">Cancel</button>';
          html += '      </div>';
          html += '    </div>';
        } else {
          html += '    <p class="chat-message-content">' + (comment.content || '').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/\n/g, '<br>') + '</p>';
        }
        html += '  </div>';
        if (isOwn && editingCommentId !== comment.id) {
          html += '  <div class="chat-message-actions">';
          html += '    <button onclick="ChatPanel._startEdit(\'' + comment.id + '\', ' + JSON.stringify(comment.content || '').replace(/'/g, '&#39;') + ')" class="chat-action-btn" title="Edit">';
          html += '      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>';
          html += '    </button>';
          html += '    <button onclick="ChatPanel._deleteComment(\'' + comment.id + '\')" class="chat-action-btn chat-action-btn-danger" title="Delete">';
          html += '      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>';
          html += '    </button>';
          html += '  </div>';
        }
        html += '</div>';
      });
    }
    html += '</div>';

    // Comment input form
    if (user) {
      html += '<div class="chat-input-area">';
      html += '  <div class="chat-avatar chat-avatar-own">' + getInitials(user.name) + '</div>';
      html += '  <div class="chat-input-form">';
      html += '    <textarea id="chat-new-comment-input" class="chat-input-textarea" rows="3" placeholder="Add a comment..." ' + (submitting ? 'disabled' : '') + '></textarea>';
      html += '    <div class="chat-input-actions">';
      html += '      <button onclick="ChatPanel._submitComment()" class="chat-btn chat-btn-primary" ' + (submitting ? 'disabled' : '') + '>';
      if (submitting) {
        html += '        <div class="chat-btn-spinner"></div><span>Posting...</span>';
      } else {
        html += '        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="chat-send-icon"><line x1="22" y1="2" x2="11" y2="13"/><polygon points="22 2 15 22 11 13 2 9 22 2"/></svg><span>Post Comment</span>';
      }
      html += '      </button>';
      html += '    </div>';
      html += '  </div>';
      html += '</div>';
    }

    // Refresh button
    if (comments.length > 0 && !loading) {
      html += '<div class="chat-refresh-area">';
      html += '  <button onclick="ChatPanel._refresh()" class="chat-refresh-btn">Refresh Comments</button>';
      html += '</div>';
    }

    container.innerHTML = html;

    // Auto-scroll to bottom
    var messagesList = document.getElementById('chat-messages-list');
    if (messagesList) messagesList.scrollTop = messagesList.scrollHeight;
  }

  return {
    open: function (stepId, stepTitle) {
      activeStepId = stepId;
      window._chatStepTitle = stepTitle || '';
      editingCommentId = null;
      error = null;

      // Create overlay and panel if they don't exist
      var overlay = document.getElementById('chat-overlay');
      if (!overlay) {
        overlay = document.createElement('div');
        overlay.id = 'chat-overlay';
        overlay.className = 'chat-overlay';
        overlay.onclick = function () { ChatPanel.close(); };
        document.body.appendChild(overlay);
      }

      var panel = document.getElementById('chat-panel');
      if (!panel) {
        panel = document.createElement('div');
        panel.id = 'chat-panel';
        panel.className = 'chat-panel';
        var container = document.createElement('div');
        container.id = 'chat-panel-container';
        container.style.display = 'flex';
        container.style.flexDirection = 'column';
        container.style.flex = '1';
        container.style.overflow = 'hidden';
        panel.appendChild(container);
        document.body.appendChild(panel);
      }

      overlay.style.display = 'block';
      panel.style.display = 'flex';
      loadComments();
    },
    close: function () {
      activeStepId = null;
      comments = [];
      editingCommentId = null;
      error = null;
      var container = document.getElementById('chat-panel-container');
      if (container) container.innerHTML = '';
      var overlay = document.getElementById('chat-overlay');
      if (overlay) overlay.style.display = 'none';
      var panel = document.getElementById('chat-panel');
      if (panel) panel.style.display = 'none';
    },
    _submitComment: handleSubmitComment,
    _saveEdit: handleEditComment,
    _deleteComment: handleDeleteComment,
    _startEdit: startEdit,
    _cancelEdit: cancelEdit,
    _refresh: loadComments,
    _dismissError: function () { error = null; render(); }
  };
})();
