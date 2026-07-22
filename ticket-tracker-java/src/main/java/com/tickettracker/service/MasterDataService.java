package com.tickettracker.service;

import com.tickettracker.dao.MasterDataDAO;
import com.tickettracker.exception.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;

public class MasterDataService {

    private static final Logger logger = LoggerFactory.getLogger(MasterDataService.class);
    private final MasterDataDAO masterDataDAO;

    public MasterDataService() {
        this.masterDataDAO = new MasterDataDAO();
    }

    public List<MasterDataDAO.MapItem> getAll(String type) throws Exception {
        String tableName = getTableName(type);
        return masterDataDAO.findAll(tableName);
    }

    public MasterDataDAO.MapItem add(String type, String name) throws Exception {
        if (name == null || name.trim().isEmpty()) {
            throw new ValidationException("Name is required");
        }
        String trimmed = name.trim();
        String tableName = getTableName(type);
        MasterDataDAO.MapItem existing = masterDataDAO.findByName(tableName, trimmed);
        if (existing != null) {
            throw new ValidationException("\"" + trimmed + "\" already exists");
        }
        logger.info("Adding master {} item: {}", type, trimmed);
        return masterDataDAO.add(tableName, trimmed);
    }

    public void remove(String type, byte[] id) throws Exception {
        String tableName = getTableName(type);
        masterDataDAO.delete(tableName, id);
        logger.info("Removed master {} item", type);
    }

    public void toggleActive(String type, byte[] id, boolean isActive) throws Exception {
        String tableName = getTableName(type);
        masterDataDAO.toggleActive(tableName, id, isActive);
    }

    public String getConfigValue(String key) throws Exception {
        return masterDataDAO.getConfigValue(key);
    }

    public void setConfigValue(String key, String value, String description) throws Exception {
        masterDataDAO.setConfigValue(key, value, description);
        logger.info("Updated config: {} = {}", key, value);
    }

    public String getModuleCode(byte[] moduleId) throws Exception {
        return masterDataDAO.getModuleCode(moduleId);
    }

    public void setModuleCode(byte[] moduleId, String moduleCode) throws Exception {
        masterDataDAO.setModuleCode(moduleId, moduleCode);
        logger.info("Updated module code for module");
    }

    public String generateTicketNumber(String locationPrefix, String moduleCode) throws Exception {
        String companyCode = masterDataDAO.getConfigValue("company_code");
        if (companyCode == null || companyCode.isEmpty()) {
            companyCode = "NMDC";
        }
        String loc3 = locationPrefix;
        if (loc3 == null || loc3.isEmpty()) {
            loc3 = "LOC";
        }
        if (loc3.length() > 3) {
            loc3 = loc3.substring(0, 3);
        }
        loc3 = loc3.toUpperCase();

        int nextCounter = masterDataDAO.getNextCounter(loc3, moduleCode);
        String paddedCounter = String.format("%06d", nextCounter);

        String ticketNumber = String.format("TKT-%s%s-%s-%s", companyCode, loc3, moduleCode, paddedCounter);
        logger.info("Generated ticket number: {}", ticketNumber);
        return ticketNumber;
    }

    private String getTableName(String type) throws ValidationException {
        switch (type) {
            case "categories": return "master_categories";
            case "departments": return "master_departments";
            case "locations": return "master_locations";
            default:
                throw new ValidationException("Invalid master data type: " + type);
        }
    }
}
