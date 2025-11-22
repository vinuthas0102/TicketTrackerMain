# Backend Development Guide

## Overview

This guide covers backend development for the Ticket Tracker application using Spring Boot and Oracle Database.

## Architecture

The backend follows a layered architecture:

```
Controller Layer (REST API)
    ↓
Service Layer (Business Logic)
    ↓
Repository Layer (Data Access)
    ↓
Entity Layer (JPA Entities)
    ↓
Oracle Database
```

## Entities to Implement

Based on the Oracle schema, you need to create these entities:

### Core Entities

1. **User** ✅ (Already implemented as example)
2. **Module** (Workflow modules)
3. **Ticket** (Main tickets/work orders)
4. **WorkflowStep** (Steps within tickets)
5. **WorkflowStepDependency** (Step dependencies)
6. **WorkflowComment** (Comments on steps)
7. **Document** (File metadata)
8. **FileAttachment** (Legacy attachments)
9. **AuditLog** (Audit trail)

### Dynamic Field Entities

10. **FieldDefinition** (Field type definitions)
11. **ModuleFieldConfiguration** (Module-specific fields)
12. **FieldDropdownOption** (Dropdown options)
13. **TicketFieldValue** (Dynamic ticket fields)
14. **WorkflowStepFieldValue** (Dynamic step fields)

## Entity Implementation Template

Here's a template for creating entities:

```java
package com.tickettracker.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.ZonedDateTime;

@Entity
@Table(name = "TABLE_NAME")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EntityName {

    @Id
    @Column(name = "ID", length = 36)
    private String id;

    // Add other fields based on schema

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private ZonedDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (id == null || id.isEmpty()) {
            id = java.util.UUID.randomUUID().toString();
        }
        createdAt = ZonedDateTime.now();
        updatedAt = ZonedDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = ZonedDateTime.now();
    }
}
```

## Repository Implementation

Create repositories for each entity:

```java
package com.tickettracker.repository;

import com.tickettracker.entity.EntityName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EntityNameRepository extends JpaRepository<EntityName, String> {
    // Add custom query methods here
}
```

## Service Layer Implementation

Create service classes for business logic:

```java
package com.tickettracker.service;

import com.tickettracker.entity.EntityName;
import com.tickettracker.repository.EntityNameRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class EntityNameService {

    private final EntityNameRepository repository;

    public List<EntityName> findAll() {
        return repository.findAll();
    }

    public Optional<EntityName> findById(String id) {
        return repository.findById(id);
    }

    public EntityName save(EntityName entity) {
        return repository.save(entity);
    }

    public void deleteById(String id) {
        repository.deleteById(id);
    }

    // Add custom business logic methods
}
```

## Controller Implementation

Create REST controllers:

```java
package com.tickettracker.controller;

import com.tickettracker.entity.EntityName;
import com.tickettracker.service.EntityNameService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/entityname")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class EntityNameController {

    private the EntityNameService service;

    @GetMapping
    public ResponseEntity<List<EntityName>> getAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EntityName> getById(@PathVariable String id) {
        return service.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<EntityName> create(@RequestBody EntityName entity) {
        EntityName saved = service.save(entity);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EntityName> update(@PathVariable String id, @RequestBody EntityName entity) {
        return service.findById(id)
                .map(existing -> {
                    // Update fields
                    EntityName updated = service.save(existing);
                    return ResponseEntity.ok(updated);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
```

## Key Services to Implement

### 1. TicketService

Main ticket management:
- Create, read, update, delete tickets
- Status transitions with validation
- Bulk ticket operations
- Search and filter tickets

### 2. WorkflowStepService

Workflow step management:
- Create hierarchical steps (level_1, level_2, level_3)
- Manage dependencies
- Track progress
- Bulk step creation

### 3. FileService

File storage management:
- Upload files to filesystem
- Store metadata in database
- Generate unique filenames
- Download files
- Delete files

### 4. AuditService

Audit trail logging:
- Log all ticket actions
- Log workflow step changes
- Log status transitions
- Track user activities

### 5. AuthenticationService

User authentication:
- Validate credentials
- Create sessions
- Manage logout
- Role-based authorization

## Security Implementation

### Step 1: Create Security Configuration

```java
package com.tickettracker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll()
            )
            .formLogin().disable()
            .httpBasic().disable()
            .sessionManagement(session -> session
                .maximumSessions(1)
            );

        return http.build();
    }
}
```

### Step 2: Create Authentication Controller

Create an authentication endpoint for login/logout.

### Step 3: Implement Role-Based Access

Use Spring Security annotations:
- `@PreAuthorize("hasRole('EO')")`
- `@PreAuthorize("hasAnyRole('EO', 'DEPT_OFFICER')")`

## Testing

### Unit Tests

```java
package com.tickettracker.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class EntityNameServiceTest {

    @Autowired
    private EntityNameService service;

    @Test
    void testFindAll() {
        var results = service.findAll();
        assertNotNull(results);
    }

    // Add more tests
}
```

### Integration Tests

```java
package com.tickettracker.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class EntityNameControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testGetAll() throws Exception {
        mockMvc.perform(get("/api/entityname"))
                .andExpect(status().isOk());
    }
}
```

## Development Checklist

### Phase 1: Core Entities (Week 1)
- [ ] Module entity
- [ ] Ticket entity with relationships
- [ ] WorkflowStep entity with hierarchy
- [ ] Document entity
- [ ] AuditLog entity

### Phase 2: Repositories (Week 1)
- [ ] Create repositories for all entities
- [ ] Add custom query methods
- [ ] Test repository methods

### Phase 3: Services (Week 2)
- [ ] TicketService with CRUD + business logic
- [ ] WorkflowStepService with hierarchy management
- [ ] FileService with filesystem operations
- [ ] AuditService for logging
- [ ] AuthenticationService

### Phase 4: Controllers (Week 2)
- [ ] TicketController
- [ ] WorkflowStepController
- [ ] DocumentController
- [ ] UserController (enhance existing)
- [ ] ModuleController
- [ ] AuditController
- [ ] AuthenticationController

### Phase 5: Security (Week 3)
- [ ] Spring Security configuration
- [ ] Session management
- [ ] Password encoding
- [ ] Role-based access control
- [ ] CSRF protection

### Phase 6: Testing (Week 3)
- [ ] Unit tests for services
- [ ] Integration tests for controllers
- [ ] Security tests
- [ ] Test data setup

## Best Practices

1. **Use DTOs** for API requests/responses
2. **Validate input** using Bean Validation annotations
3. **Handle exceptions** with @ControllerAdvice
4. **Use transactions** (@Transactional) for data consistency
5. **Log important actions** using SLF4J
6. **Document APIs** with Swagger/OpenAPI
7. **Follow naming conventions**
8. **Write tests** for all business logic

## Reference Original Code

When implementing business logic, refer to the original React/TypeScript application:

- Service methods: `/src/services/ticketService.ts`
- Data models: `/src/types/index.ts`
- Business rules: Check React components for validation logic

## Next Steps

1. Implement remaining entities
2. Create repositories and services
3. Build REST controllers
4. Add security configuration
5. Write tests
6. Document APIs

See `API_SPECIFICATION.md` for detailed API endpoint documentation.
