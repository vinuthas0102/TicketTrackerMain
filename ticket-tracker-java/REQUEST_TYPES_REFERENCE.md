# Request Types Reference Guide

**Package:** ticket-tracker-java
**Date:** 2026-03-17

---

## Quick Answer

**Request Types are stored in the `modules` table, in the `config` column (JSON format).**

---

## Database Location

### Table Structure

```sql
-- modules table
CREATE TABLE modules (
    id           RAW(16) PRIMARY KEY,
    name         VARCHAR2(100),
    description  VARCHAR2(500),
    icon         VARCHAR2(50),
    color        VARCHAR2(50),
    schema_id    VARCHAR2(50),
    config       CLOB,              -- ← Request Types stored here (JSON)
    active       NUMBER(1) DEFAULT 1,
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### Config Column Structure

```json
{
  "categories": [
    "Electrical",
    "Plumbing",
    "HVAC",
    "General Maintenance",
    "Equipment Repair"
  ],
  "requestTypes": [
    {
      "label": "Pre-Occupation Maintenance",
      "value": "Pre-Occupation Maintenance",
      "requiresCEInspection": false
    },
    {
      "label": "Vacation Handover",
      "value": "Vacation Handover",
      "requiresCEInspection": true
    },
    {
      "label": "Annual Maintenance",
      "value": "Annual Maintenance",
      "requiresCEInspection": true
    },
    {
      "label": "Emergency Maintenance",
      "value": "Emergency Maintenance",
      "requiresCEInspection": false
    }
  ]
}
```

---

## Default Request Types (from seed data)

### All Modules Have These 4 Request Types:

| Label | Value | Requires C&E Inspection |
|-------|-------|-------------------------|
| Pre-Occupation Maintenance | `Pre-Occupation Maintenance` | ❌ No |
| Vacation Handover | `Vacation Handover` | ✅ Yes |
| Annual Maintenance | `Annual Maintenance` | ✅ Yes |
| Emergency Maintenance | `Emergency Maintenance` | ❌ No |

**File:** `database/07-oracle-seed-data.sql` (lines 29, 42, 55, 68, 81)

---

## C&E Inspection Logic

### When C&E Inspection Steps Are Created

The system automatically creates **Civil Inspection** and **Electrical Inspection** workflow steps when:

1. Request Type = `"Vacation Handover"` **OR**
2. Request Type = `"Annual Maintenance"`

**Implementation:**
- File: `WorkflowService.java`
- Method: `createCEInspectionStepsIfRequired()`
- Lines: 682-778

```java
boolean requiresCEInspection = "Vacation Handover".equals(requestType) ||
                                "Annual Maintenance".equals(requestType);

if (requiresCEInspection) {
    // Create Civil Inspection step → assigned to Civil Manager
    // Create Electrical Inspection step → assigned to Electrical Manager
}
```

### Assigned Users

- **Civil Inspection** → Assigned to user with `department = "Civil Manager"`
- **Electrical Inspection** → Assigned to user with `department = "Electrical Manager"`

---

## How to Query Request Types

### 1. Get Request Types from Module Config

```java
// Get module
ModuleDAO moduleDAO = new ModuleDAO();
Module module = moduleDAO.findBySchemaId("maintenance");

// Parse JSON config
String configJson = module.getConfig();
JSONObject config = new JSONObject(configJson);
JSONArray requestTypes = config.getJSONArray("requestTypes");

// Loop through request types
for (int i = 0; i < requestTypes.length(); i++) {
    JSONObject rt = requestTypes.getJSONObject(i);

    String label = rt.getString("label");
    String value = rt.getString("value");
    boolean requiresCE = rt.getBoolean("requiresCEInspection");

    System.out.println(label + " | " + value + " | C&E: " + requiresCE);
}
```

**Output:**
```
Pre-Occupation Maintenance | Pre-Occupation Maintenance | C&E: false
Vacation Handover | Vacation Handover | C&E: true
Annual Maintenance | Annual Maintenance | C&E: true
Emergency Maintenance | Emergency Maintenance | C&E: false
```

### 2. Get Request Type from Ticket

```java
TicketDAO ticketDAO = new TicketDAO();
Ticket ticket = ticketDAO.findById(ticketId);

String requestType = ticket.getRequestType();
// Example: "Vacation Handover"
```

### 3. Check if Ticket Requires C&E Inspection

```java
Ticket ticket = ticketDAO.findById(ticketId);
String requestType = ticket.getRequestType();

boolean requiresCE = "Vacation Handover".equals(requestType) ||
                     "Annual Maintenance".equals(requestType);

if (requiresCE) {
    System.out.println("This ticket requires C&E inspection");
}
```

---

## SQL Queries

### Get All Modules with Their Request Types

```sql
SELECT
    id,
    name,
    schema_id,
    config
FROM modules
WHERE active = 1
ORDER BY name;
```

### Get All Tickets with a Specific Request Type

```sql
SELECT
    t.id,
    t.ticket_number,
    t.title,
    t.request_type,
    t.status,
    t.created_at
FROM tickets t
WHERE t.request_type = 'Vacation Handover'
ORDER BY t.created_at DESC;
```

### Count Tickets by Request Type

```sql
SELECT
    request_type,
    COUNT(*) as ticket_count
FROM tickets
WHERE request_type IS NOT NULL
GROUP BY request_type
ORDER BY ticket_count DESC;
```

### Get Tickets Requiring C&E Inspection

```sql
SELECT
    t.id,
    t.ticket_number,
    t.title,
    t.request_type,
    t.status
FROM tickets t
WHERE t.request_type IN ('Vacation Handover', 'Annual Maintenance')
AND t.status NOT IN ('completed', 'cancelled')
ORDER BY t.created_at DESC;
```

---

## How to Add/Modify Request Types

### Option 1: Update Seed Data (Fresh Install)

**File:** `database/07-oracle-seed-data.sql`

```sql
-- Modify the JSON in the config column
INSERT INTO modules (id, name, description, icon, color, schema_id, config, active)
VALUES (
  ...,
  '{"categories": [...], "requestTypes": [
    {"label": "New Request Type", "value": "New Request Type", "requiresCEInspection": false}
  ]}',
  1
);
```

### Option 2: Update Existing Module (Running System)

```sql
UPDATE modules
SET config = '{"categories": ["Electrical", "Plumbing", "HVAC"], "requestTypes": [
  {"label": "Pre-Occupation Maintenance", "value": "Pre-Occupation Maintenance", "requiresCEInspection": false},
  {"label": "Vacation Handover", "value": "Vacation Handover", "requiresCEInspection": true},
  {"label": "Annual Maintenance", "value": "Annual Maintenance", "requiresCEInspection": true},
  {"label": "Emergency Maintenance", "value": "Emergency Maintenance", "requiresCEInspection": false},
  {"label": "Routine Inspection", "value": "Routine Inspection", "requiresCEInspection": false}
]}'
WHERE schema_id = 'maintenance';

COMMIT;
```

### Option 3: Java Code

```java
ModuleDAO moduleDAO = new ModuleDAO();
Module module = moduleDAO.findBySchemaId("maintenance");

// Parse existing config
JSONObject config = new JSONObject(module.getConfig());
JSONArray requestTypes = config.getJSONArray("requestTypes");

// Add new request type
JSONObject newRequestType = new JSONObject();
newRequestType.put("label", "Routine Inspection");
newRequestType.put("value", "Routine Inspection");
newRequestType.put("requiresCEInspection", false);
requestTypes.put(newRequestType);

// Update config
config.put("requestTypes", requestTypes);
module.setConfig(config.toString());

// Save to database
moduleDAO.update(module);
```

---

## Important Notes

### ⚠️ Data Consistency

1. **No Foreign Key** - The `tickets.request_type` column is free-text (VARCHAR2)
2. **No Validation** - Database doesn't enforce that `request_type` exists in module config
3. **Frontend Validation** - The UI should validate against available request types

### ✅ Best Practices

1. **Always use exact values** - Match the `value` field exactly (case-sensitive)
2. **Update C&E logic** - If you add new request types requiring C&E inspection, update `WorkflowService.java`
3. **Test after changes** - Verify dropdown shows new options in frontend
4. **Backup before changes** - Always backup the `modules` table before modifying config

### 🔧 C&E Inspection Requirements

If you add a new request type that requires C&E inspection:

**Step 1:** Add to module config
```json
{
  "label": "Major Renovation",
  "value": "Major Renovation",
  "requiresCEInspection": true
}
```

**Step 2:** Update `WorkflowService.java` line 690-691
```java
boolean requiresCEInspection = "Vacation Handover".equals(requestType) ||
                                "Annual Maintenance".equals(requestType) ||
                                "Major Renovation".equals(requestType);  // Add this
```

---

## Related Files

| File | Purpose |
|------|---------|
| `database/07-oracle-seed-data.sql` | Initial request types configuration |
| `database/13-oracle-add-step-type-and-request-type.sql` | Migration that adds `request_type` column |
| `service/WorkflowService.java` | C&E inspection logic implementation |
| `service/TicketService.java` | Alternative C&E inspection implementation |
| `dao/ModuleDAO.java` | Module data access |
| `model/Module.java` | Module entity |
| `model/Ticket.java` | Ticket entity (has `request_type` field) |

---

## Summary

✅ **Request Types Location:** `modules.config` (JSON)
✅ **Ticket's Selected Type:** `tickets.request_type` (VARCHAR2)
✅ **C&E Inspection Trigger:** "Vacation Handover" OR "Annual Maintenance"
✅ **Default Count:** 4 request types per module
✅ **Modification:** Update JSON in `modules.config` column

---

## Need Help?

- Adding a new request type? → Update `modules.config` JSON
- Changing C&E requirements? → Update `WorkflowService.java` line 690
- Querying tickets by request type? → Use SQL: `WHERE request_type = '...'`
- Seeing list in frontend? → Request types come from `/api/modules` endpoint
