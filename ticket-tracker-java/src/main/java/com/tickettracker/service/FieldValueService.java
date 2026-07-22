package com.tickettracker.service;

import com.tickettracker.dao.TicketFieldValueDAO;
import com.tickettracker.dao.WorkflowStepFieldValueDAO;
import com.tickettracker.exception.DatabaseException;
import com.tickettracker.exception.TicketTrackerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FieldValueService {

    private static final Logger logger = LoggerFactory.getLogger(FieldValueService.class);

    private final TicketFieldValueDAO ticketFieldValueDAO;
    private final WorkflowStepFieldValueDAO workflowStepFieldValueDAO;

    public FieldValueService() {
        this.ticketFieldValueDAO = new TicketFieldValueDAO();
        this.workflowStepFieldValueDAO = new WorkflowStepFieldValueDAO();
    }

    public Map<String, String> getTicketFieldValues(byte[] ticketId) throws TicketTrackerException {
        try {
            return ticketFieldValueDAO.findByTicketId(ticketId);
        } catch (SQLException e) {
            logger.error("Error fetching ticket field values", e);
            throw new DatabaseException("Failed to fetch ticket field values", e);
        }
    }

    public void updateTicketFieldValue(byte[] ticketId, String fieldKey, String fieldValue)
            throws TicketTrackerException {
        try {
            ticketFieldValueDAO.upsert(ticketId, fieldKey, fieldValue);
        } catch (SQLException e) {
            logger.error("Error updating ticket field value", e);
            throw new DatabaseException("Failed to update ticket field value", e);
        }
    }

    public void deleteTicketFieldValue(byte[] ticketId, String fieldKey)
            throws TicketTrackerException {
        try {
            ticketFieldValueDAO.delete(ticketId, fieldKey);
        } catch (SQLException e) {
            logger.error("Error deleting ticket field value", e);
            throw new DatabaseException("Failed to delete ticket field value", e);
        }
    }

    public Map<byte[], Map<String, String>> getBatchTicketFieldValues(List<byte[]> ticketIds)
            throws TicketTrackerException {
        try {
            return ticketFieldValueDAO.findByTicketIds(ticketIds);
        } catch (SQLException e) {
            logger.error("Error fetching batch ticket field values", e);
            throw new DatabaseException("Failed to fetch batch ticket field values", e);
        }
    }

    public Map<String, String> getWorkflowStepFieldValues(byte[] stepId)
            throws TicketTrackerException {
        try {
            return workflowStepFieldValueDAO.findByStepId(stepId);
        } catch (SQLException e) {
            logger.error("Error fetching workflow step field values", e);
            throw new DatabaseException("Failed to fetch workflow step field values", e);
        }
    }

    public void saveWorkflowStepFieldValues(byte[] stepId, Map<String, String> values)
            throws TicketTrackerException {
        try {
            for (Map.Entry<String, String> entry : values.entrySet()) {
                workflowStepFieldValueDAO.upsert(stepId, entry.getKey(), entry.getValue());
            }
        } catch (SQLException e) {
            logger.error("Error saving workflow step field values", e);
            throw new DatabaseException("Failed to save workflow step field values", e);
        }
    }

    public void updateWorkflowStepFieldValue(byte[] stepId, String fieldKey, String fieldValue)
            throws TicketTrackerException {
        try {
            workflowStepFieldValueDAO.upsert(stepId, fieldKey, fieldValue);
        } catch (SQLException e) {
            logger.error("Error updating workflow step field value", e);
            throw new DatabaseException("Failed to update workflow step field value", e);
        }
    }

    public void deleteWorkflowStepFieldValue(byte[] stepId, String fieldKey)
            throws TicketTrackerException {
        try {
            workflowStepFieldValueDAO.delete(stepId, fieldKey);
        } catch (SQLException e) {
            logger.error("Error deleting workflow step field value", e);
            throw new DatabaseException("Failed to delete workflow step field value", e);
        }
    }

    public Map<byte[], Map<String, String>> getBatchWorkflowStepFieldValues(List<byte[]> stepIds)
            throws TicketTrackerException {
        try {
            return workflowStepFieldValueDAO.findByStepIds(stepIds);
        } catch (SQLException e) {
            logger.error("Error fetching batch workflow step field values", e);
            throw new DatabaseException("Failed to fetch batch workflow step field values", e);
        }
    }

    public Map<String, Object> getFieldValues(byte[] entityId, byte[] currentUserId)
            throws TicketTrackerException {
        Map<String, Object> result = new HashMap<>();
        result.put("entityId", bytesToHex(entityId));
        result.put("values", getTicketFieldValues(entityId));
        return result;
    }

    public void saveFieldValues(byte[] entityId, Map<String, Object> values, byte[] currentUserId)
            throws TicketTrackerException {
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            updateTicketFieldValue(entityId, entry.getKey(),
                    entry.getValue() != null ? entry.getValue().toString() : null);
        }
    }

    private String bytesToHex(byte[] bytes) {
        if (bytes == null) return null;
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
}
