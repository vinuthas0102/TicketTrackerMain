/**
 * Chat component for workflow step comments.
 * Redesigned with bubble-style messages, channel tabs, attachment support,
 * and role-based access control.
 */
const ChatPanel = (function () {
  var activeStepId = null;
  var activeStepTitle = '';
  var stepLevel = 1;
  var ticketAssignedTo = null;
  var stepAssignedTo = null;
  var comments = [];
  var loading = false;
  var submitting = false;
  var editingCommentId = null;
  var error = null;
  var selectedChannel = 'in-app';
  var channelInfo = null;
  var attachmentFile = null;
  var attachmentError = null;
  var downloadingAttachment = null;

  var ALLOWED_FILE_TYPES = [
    'application/pdf', 'image/jpeg', 'image/jpg', 'image/png', 'image/gif',
    'application/msword', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    'application/vnd.ms-excel', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    'application/zip', 'application/x-zip-compressed'
  ];
  var MAX_FILE_SIZE = 5 * 1024 * 1024;

  var CHANNELS = [
    { id: 'in-app', label: 'In-App' },
    { id: 'email', label: 'Email' },
    { id: 'sms', label: 'SMS' },
    { id: 'whatsapp', label: 'WhatsApp' }
  ];

  function getInitials(name) {
    if (!name) return 'U';
    return name.split(' ').map(function (n) { return n[0]; }).join('').toUpperCase().slice(0, 2);
  }

  function formatTimestamp(dateStr) {
    var date = new Date(dateStr);
    var now = new Date();
    var diffSec = Math.floor((now.getTime() - date.getTime()) / 1000);
    if (diffSec < 60) return 'Just now';
    if (diffSec < 3600) return Math.floor(diffSec / 60) + ' min ago';
    if (diffSec < 86400) return Math.floor(diffSec / 3600) + ' hr ago';
    if (diffSec < 604800) return Math.floor(diffSec / 86400) + ' days ago';
    return date.toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });
  }

  function getRoleBadgeClass(role) {
    switch ((role || '').toUpperCase()) {
      case 'EO': return 'chat-role-eo';
      case 'DO': case 'DEPT_OFFICER': return 'chat-role-do';
      case 'FINANCE': return 'chat-role-finance';
      case 'VENDOR': return 'chat-role-vendor';
      default: return 'chat-role-default';
    }
  }

  function getAvatarClass(role) {
    switch ((role || '').toUpperCase()) {
      case 'EO': return 'chat-avatar-eo';
      case 'DO': case 'DEPT_OFFICER': return 'chat-avatar-do';
      case 'FINANCE': return 'chat-avatar-finance';
      case 'VENDOR': return 'chat-avatar-vendor';
      default: return 'chat-avatar-default';
    }
  }

  function getCurrentUser() {
    var userJson = localStorage.getItem('currentUser');
    if (!userJson) return null;
    try { return JSON.parse(userJson); } catch (e) { return null; }
  }

  function canParticipate() {
    var user = getCurrentUser();
    if (!user) return false;
    if (stepLevel === 1) {
      return (user.role || '').toUpperCase() === 'EO' || user.id === ticketAssignedTo;
    }
    return user.id === ticketAssignedTo || user.id === stepAssignedTo;
  }

  function escapeHtml(text) {
    if (!text) return '';
    return text.replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/&/g, '&amp;');
  }

  function formatFileSize(bytes) {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1048576) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / 1048576).toFixed(1) + ' MB';
  }

  function getFileIcon(type) {
    if (!type) return '&#128196;';
    if (type.indexOf('image/') === 0) return '&#128247;';
    if (type === 'application/zip' || type === 'application/x-zip-compressed') return '&#128230;';
    return '&#128196;';
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
      scrollToBottom();
    }
  }

  function scrollToBottom() {
    var list = document.getElementById('chat-messages-list');
    if (list) list.scrollTop = list.scrollHeight;
  }

  async function handleSubmitComment() {
    var textarea = document.getElementById('chat-new-comment-input');
    if (!textarea) return;
    var content = textarea.value.trim();
    if (!content) return;

    if (selectedChannel !== 'in-app') {
      channelInfo = 'Message queued for ' + selectedChannel + ' channel. Configure your webhook in Settings to enable delivery.';
    }

    submitting = true;
    error = null;
    render();
    try {
      await API.addStepComment(activeStepId, content, attachmentFile, selectedChannel);
      textarea.value = '';
      attachmentFile = null;
      var fileInput = document.getElementById('chat-file-input');
      if (fileInput) fileInput.value = '';
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

  function handleFileSelect(e) {
    var file = e.target.files && e.target.files[0];
    if (!file) return;

    attachmentError = null;

    if (file.size > MAX_FILE_SIZE) {
      attachmentError = 'File size exceeds 5MB limit. Your file is ' + (file.size / 1048576).toFixed(2) + 'MB';
      render();
      return;
    }

    if (ALLOWED_FILE_TYPES.indexOf(file.type) === -1) {
      attachmentError = 'File type not supported. Allowed: PDF, Images, Word, Excel, ZIP';
      render();
      return;
    }

    attachmentFile = file;
    render();
  }

  function removeAttachment() {
    attachmentFile = null;
    attachmentError = null;
    var fileInput = document.getElementById('chat-file-input');
    if (fileInput) fileInput.value = '';
    render();
  }

  function handleKeyDown(e) {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      if (!submitting) handleSubmitComment();
    }
  }

  function selectChannel(chId) {
    selectedChannel = chId;
    channelInfo = null;
    render();
  }

  function render() {
    var container = document.getElementById('chat-panel-container');
    if (!container) return;

    var user = getCurrentUser();
    var html = '';

    // Channel Selector Bar
    html += '<div class="chat-channel-bar">';
    html += '  <span class="chat-channel-label">via</span>';
    CHANNELS.forEach(function (ch) {
      var isActive = selectedChannel === ch.id;
      html += '<button class="chat-channel-pill' + (isActive ? ' chat-channel-active' : '') + '" onclick="ChatPanel._selectChannel(\'' + ch.id + '\')" title="' + ch.label + '">';
      html += '<span>' + ch.label + '</span>';
      html += '</button>';
    });
    html += '</div>';

    // Channel Info Banner
    if (channelInfo) {
      html += '<div class="chat-channel-info">';
      html += '<span>' + escapeHtml(channelInfo) + '</span>';
      html += '<button onclick="ChatPanel._dismissChannelInfo()" class="chat-channel-info-close">&times;</button>';
      html += '</div>';
    }

    // Error Banner
    if (error) {
      html += '<div class="chat-error-banner">';
      html += '<span>' + escapeHtml(error) + '</span>';
      html += '<button onclick="ChatPanel._dismissError()" class="chat-error-dismiss">&times;</button>';
      html += '</div>';
    }

    // Messages Area
    html += '<div class="chat-messages" id="chat-messages-list">';
    if (loading && comments.length === 0) {
      html += '<div class="chat-loading"><div class="chat-spinner"></div></div>';
    } else if (comments.length === 0) {
      html += '<div class="chat-empty-state">';
      html += '  <div class="chat-empty-icon-circle">';
      html += '    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="chat-empty-plane"><line x1="22" y1="2" x2="11" y2="13"/><polygon points="22 2 15 22 11 13 2 9 22 2"/></svg>';
      html += '  </div>';
      html += '  <p class="chat-empty-title">No messages yet</p>';
      html += '  <p class="chat-empty-subtitle">Start the conversation below</p>';
      html += '</div>';
    } else {
      comments.forEach(function (comment) {
        var isOwn = user && user.id === comment.createdBy;
        var isEdited = comment.updatedAt && comment.updatedAt !== comment.createdAt;

        html += '<div class="chat-msg' + (isOwn ? ' chat-msg-own' : ' chat-msg-other') + '">';
        html += '  <div class="chat-msg-inner' + (isOwn ? ' chat-msg-inner-own' : '') + '">';
        // Avatar
        html += '  <div class="chat-avatar ' + getAvatarClass(comment.createdByRole) + '">' + getInitials(comment.createdByName) + '</div>';
        // Bubble
        html += '  <div class="chat-bubble-container">';
        // Meta
        html += '    <div class="chat-msg-meta">';
        html += '      <span class="chat-msg-author">' + escapeHtml(comment.createdByName || 'Unknown') + '</span>';
        if (comment.createdByRole) {
          html += '      <span class="chat-role-badge ' + getRoleBadgeClass(comment.createdByRole) + '">' + comment.createdByRole.toUpperCase() + '</span>';
        }
        html += '      <span class="chat-msg-time">' + formatTimestamp(comment.createdAt) + '</span>';
        if (isEdited) {
          html += '      <span class="chat-msg-edited">edited</span>';
        }
        html += '    </div>';

        if (editingCommentId === comment.id) {
          html += '    <div class="chat-edit-form">';
          html += '      <textarea id="chat-edit-input-' + comment.id + '" class="chat-edit-textarea" rows="3" placeholder="Edit your comment...">' + escapeHtml(comment.content || '') + '</textarea>';
          html += '      <div class="chat-edit-actions">';
          html += '        <button onclick="ChatPanel._saveEdit(\'' + comment.id + '\')" class="chat-btn chat-btn-primary">Save</button>';
          html += '        <button onclick="ChatPanel._cancelEdit()" class="chat-btn chat-btn-secondary">Cancel</button>';
          html += '      </div>';
          html += '    </div>';
        } else {
          html += '    <div class="chat-bubble' + (isOwn ? ' chat-bubble-own' : '') + '">';
          html += '      <p class="chat-bubble-text">' + escapeHtml(comment.content || '').replace(/\n/g, '<br>') + '</p>';
          // Attachment card
          if (comment.attachmentName) {
            html += '      <div class="chat-attachment-card' + (isOwn ? ' chat-attachment-own' : '') + '">';
            html += '        <span class="chat-attachment-icon">' + getFileIcon(comment.attachmentType) + '</span>';
            html += '        <span class="chat-attachment-name">' + escapeHtml(comment.attachmentName) + '</span>';
            html += '        <button onclick="ChatPanel._downloadAttachment(\'' + comment.id + '\', \'' + escapeHtml(comment.attachmentName) + '\')" class="chat-attachment-download" title="Download">';
            html += '          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>';
            html += '        </button>';
            html += '      </div>';
          }
          html += '    </div>';
        }

        // Edit/Delete actions
        if (isOwn && editingCommentId !== comment.id) {
          html += '    <div class="chat-msg-actions">';
          html += '      <button onclick="ChatPanel._startEdit(\'' + comment.id + '\', ' + JSON.stringify(comment.content || '') + ')" class="chat-action-btn" title="Edit">';
          html += '        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>';
          html += '      </button>';
          html += '      <button onclick="ChatPanel._deleteComment(\'' + comment.id + '\')" class="chat-action-btn chat-action-danger" title="Delete">';
          html += '        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>';
          html += '      </button>';
          html += '    </div>';
        }

        html += '  </div>'; // bubble-container
        html += '  </div>'; // msg-inner
        html += '</div>'; // msg
      });
    }
    html += '</div>'; // messages-list

    // Input Toolbar
    if (canParticipate()) {
      // Attachment preview
      if (attachmentFile) {
        html += '<div class="chat-attachment-preview">';
        html += '  <span class="chat-attachment-icon">' + getFileIcon(attachmentFile.type) + '</span>';
        html += '  <span class="chat-attachment-preview-name">' + escapeHtml(attachmentFile.name) + '</span>';
        html += '  <span class="chat-attachment-preview-size">' + formatFileSize(attachmentFile.size) + '</span>';
        html += '  <button onclick="ChatPanel._removeAttachment()" class="chat-attachment-remove">&times;</button>';
        html += '</div>';
      }

      // Attachment error
      if (attachmentError) {
        html += '<div class="chat-attachment-error">';
        html += '  <span>' + escapeHtml(attachmentError) + '</span>';
        html += '  <button onclick="ChatPanel._dismissAttachmentError()" class="chat-error-dismiss">&times;</button>';
        html += '</div>';
      }

      // Input row
      html += '<div class="chat-input-bar">';
      html += '  <button onclick="document.getElementById(\'chat-file-input\').click()" class="chat-paperclip-btn" title="Attach file"' + (submitting ? ' disabled' : '') + '>';
      html += '    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21.44 11.05l-9.19 9.19a6 6 0 0 1-8.49-8.49l9.19-9.19a4 4 0 0 1 5.66 5.66l-9.2 9.19a2 2 0 0 1-2.83-2.83l8.49-8.48"/></svg>';
      html += '  </button>';
      html += '  <input type="file" id="chat-file-input" class="chat-file-input" onchange="ChatPanel._handleFileSelect(event)" accept=".pdf,.jpg,.jpeg,.png,.gif,.doc,.docx,.xls,.xlsx,.zip" />';
      html += '  <textarea id="chat-new-comment-input" class="chat-input-textarea" rows="1" placeholder="Type a message... (Enter to send, Shift+Enter for new line)" onkeydown="ChatPanel._handleKeyDown(event)"' + (submitting ? ' disabled' : '') + '></textarea>';
      html += '  <button onclick="ChatPanel._submitComment()" class="chat-send-btn"' + (submitting ? ' disabled' : '') + ' title="Send">';
      if (submitting) {
        html += '    <div class="chat-send-spinner"></div>';
      } else {
        html += '    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="22" y1="2" x2="11" y2="13"/><polygon points="22 2 15 22 11 13 2 9 22 2"/></svg>';
      }
      html += '  </button>';
      html += '</div>';
    } else {
      html += '<div class="chat-no-access"><p>You are not part of this conversation.</p></div>';
    }

    container.innerHTML = html;
    if (!loading) scrollToBottom();
  }

  return {
    open: function (stepId, stepTitle, level, tAssignedTo, sAssignedTo) {
      activeStepId = stepId;
      activeStepTitle = stepTitle || '';
      stepLevel = level || 1;
      ticketAssignedTo = tAssignedTo || null;
      stepAssignedTo = sAssignedTo || null;
      editingCommentId = null;
      error = null;
      selectedChannel = 'in-app';
      channelInfo = null;
      attachmentFile = null;

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
        var header = document.createElement('div');
        header.className = 'chat-header';
        header.innerHTML = '<div class="chat-header-info">' +
          '<div class="chat-header-title"><svg class="chat-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg><span>Task Chat</span></div>' +
          '<p class="chat-header-subtitle" id="chat-header-subtitle"></p></div>' +
          '<button class="chat-close-btn" onclick="ChatPanel.close()" title="Close"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg></button>';
        panel.appendChild(header);

        var container = document.createElement('div');
        container.id = 'chat-panel-container';
        container.className = 'chat-panel-container';
        panel.appendChild(container);
        document.body.appendChild(panel);
      }

      var subtitle = document.getElementById('chat-header-subtitle');
      if (subtitle) subtitle.textContent = activeStepTitle;

      overlay.style.display = 'block';
      panel.style.display = 'flex';
      loadComments();
    },

    close: function () {
      activeStepId = null;
      comments = [];
      editingCommentId = null;
      error = null;
      attachmentFile = null;
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
    _selectChannel: selectChannel,
    _handleFileSelect: handleFileSelect,
    _removeAttachment: removeAttachment,
    _handleKeyDown: handleKeyDown,
    _dismissError: function () { error = null; render(); },
    _dismissChannelInfo: function () { channelInfo = null; render(); },
    _dismissAttachmentError: function () { attachmentError = null; render(); },
    _downloadAttachment: function (commentId, attachmentName) {
      window.open('/ticket-tracker/api/workflow-comments/' + commentId + '/attachment', '_blank');
    }
  };
})();
