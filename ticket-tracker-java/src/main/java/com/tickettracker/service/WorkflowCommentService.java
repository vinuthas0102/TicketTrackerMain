package com.tickettracker.service;

import com.tickettracker.dao.UserDAO;
import com.tickettracker.dao.WorkflowCommentDAO;
import com.tickettracker.dao.WorkflowStepDAO;
import com.tickettracker.exception.*;
import com.tickettracker.model.User;
import com.tickettracker.model.WorkflowComment;
import com.tickettracker.model.WorkflowStep;
import com.tickettracker.util.UuidUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

public class WorkflowCommentService {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowCommentService.class);

    private final WorkflowCommentDAO commentDAO;
    private final WorkflowStepDAO workflowStepDAO;
    private final UserDAO userDAO;

    public WorkflowCommentService() {
        this.commentDAO = new WorkflowCommentDAO();
        this.workflowStepDAO = new WorkflowStepDAO();
        this.userDAO = new UserDAO();
    }

    public WorkflowComment createComment(byte[] stepId, String content, byte[] userId)
            throws TicketTrackerException {
        try {
            if (content == null || content.trim().isEmpty()) {
                throw new ValidationException("content", "Comment content cannot be empty");
            }

            WorkflowStep step = workflowStepDAO.findById(stepId);
            if (step == null) {
                throw new ResourceNotFoundException("Workflow step", UuidUtil.bytesToUuidString(stepId));
            }

            WorkflowComment comment = new WorkflowComment();
            comment.setStepId(stepId);
            comment.setContent(content.trim());
            comment.setCreatedBy(userId);

            WorkflowComment createdComment = commentDAO.create(comment);

            logger.info("Created workflow comment for step: {} by user: {}",
                    UuidUtil.bytesToUuidString(stepId), UuidUtil.bytesToUuidString(userId));

            return createdComment;
        } catch (SQLException e) {
            logger.error("Error creating workflow comment", e);
            throw new DatabaseException("Failed to create workflow comment", e);
        }
    }

    public WorkflowComment updateComment(byte[] commentId, String content, byte[] userId)
            throws TicketTrackerException {
        try {
            if (content == null || content.trim().isEmpty()) {
                throw new ValidationException("content", "Comment content cannot be empty");
            }

            WorkflowComment existingComment = commentDAO.findById(commentId);
            if (existingComment == null) {
                throw new ResourceNotFoundException("Workflow comment", UuidUtil.bytesToUuidString(commentId));
            }

            if (!Arrays.equals(existingComment.getCreatedBy(), userId)) {
                throw new ForbiddenException("You can only edit your own comments");
            }

            existingComment.setContent(content.trim());
            WorkflowComment updatedComment = commentDAO.update(existingComment);

            logger.info("Updated workflow comment: {} by user: {}",
                    UuidUtil.bytesToUuidString(commentId), UuidUtil.bytesToUuidString(userId));

            return updatedComment;
        } catch (SQLException e) {
            logger.error("Error updating workflow comment", e);
            throw new DatabaseException("Failed to update workflow comment", e);
        }
    }

    public List<WorkflowComment> getStepComments(byte[] stepId, byte[] userId)
            throws TicketTrackerException {
        try {
            WorkflowStep step = workflowStepDAO.findById(stepId);
            if (step == null) {
                throw new ResourceNotFoundException("Workflow step", UuidUtil.bytesToUuidString(stepId));
            }

            User user = userDAO.findById(userId);
            if (user == null) {
                throw new ResourceNotFoundException("User", UuidUtil.bytesToUuidString(userId));
            }

            if ("eo".equalsIgnoreCase(user.getRole())) {
                return commentDAO.findByStepId(stepId);
            } else {
                return commentDAO.findByStepIdAndUser(stepId, userId);
            }
        } catch (SQLException e) {
            logger.error("Error fetching workflow comments", e);
            throw new DatabaseException("Failed to fetch workflow comments", e);
        }
    }

    public WorkflowComment getCommentById(byte[] commentId) throws TicketTrackerException {
        try {
            WorkflowComment comment = commentDAO.findById(commentId);
            if (comment == null) {
                throw new ResourceNotFoundException("Workflow comment", UuidUtil.bytesToUuidString(commentId));
            }
            return comment;
        } catch (SQLException e) {
            logger.error("Error fetching workflow comment", e);
            throw new DatabaseException("Failed to fetch workflow comment", e);
        }
    }

    public boolean deleteComment(byte[] commentId, byte[] userId) throws TicketTrackerException {
        try {
            WorkflowComment existingComment = commentDAO.findById(commentId);
            if (existingComment == null) {
                throw new ResourceNotFoundException("Workflow comment", UuidUtil.bytesToUuidString(commentId));
            }

            if (!Arrays.equals(existingComment.getCreatedBy(), userId)) {
                throw new ForbiddenException("You can only delete your own comments");
            }

            boolean deleted = commentDAO.delete(commentId);

            if (deleted) {
                logger.info("Deleted workflow comment: {} by user: {}",
                        UuidUtil.bytesToUuidString(commentId), UuidUtil.bytesToUuidString(userId));
            }

            return deleted;
        } catch (SQLException e) {
            logger.error("Error deleting workflow comment", e);
            throw new DatabaseException("Failed to delete workflow comment", e);
        }
    }

    public void validatePermissions(byte[] commentId, byte[] userId) throws TicketTrackerException {
        try {
            WorkflowComment comment = commentDAO.findById(commentId);
            if (comment == null) {
                throw new ResourceNotFoundException("Workflow comment", UuidUtil.bytesToUuidString(commentId));
            }

            if (!Arrays.equals(comment.getCreatedBy(), userId)) {
                throw new ForbiddenException("You can only edit your own comments");
            }
        } catch (SQLException e) {
            logger.error("Error validating comment permissions", e);
            throw new DatabaseException("Failed to validate comment permissions", e);
        }
    }
}
