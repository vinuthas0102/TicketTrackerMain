package com.tickettracker.repository;

import com.tickettracker.model.TicketEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<TicketEntity, String> {

    Optional<TicketEntity> findByTicketNumber(String ticketNumber);

    List<TicketEntity> findByModuleId(String moduleId);

    List<TicketEntity> findByStatus(String status);

    List<TicketEntity> findByCreatedById(String createdById);

    List<TicketEntity> findByAssignedToId(String assignedToId);

    List<TicketEntity> findByPriority(String priority);

    @Query("SELECT t FROM TicketEntity t WHERE t.module.id = :moduleId AND t.status = :status")
    List<TicketEntity> findByModuleAndStatus(@Param("moduleId") String moduleId, @Param("status") String status);

    @Query("SELECT t FROM TicketEntity t WHERE t.dueDate BETWEEN :startDate AND :endDate")
    List<TicketEntity> findByDueDateBetween(@Param("startDate") Timestamp startDate, @Param("endDate") Timestamp endDate);

    @Query("SELECT t FROM TicketEntity t WHERE " +
           "(LOWER(t.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(t.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(t.ticketNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    List<TicketEntity> searchTickets(@Param("searchTerm") String searchTerm);

    boolean existsByTicketNumber(String ticketNumber);
}
