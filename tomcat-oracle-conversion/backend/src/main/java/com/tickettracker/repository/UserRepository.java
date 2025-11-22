package com.tickettracker.repository;

import com.tickettracker.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * User Repository
 *
 * Data access layer for User entities.
 * Provides CRUD operations and custom query methods.
 */
@Repository
public interface UserRepository extends JpaRepository<User, String> {

    /**
     * Find user by email address
     *
     * @param email User's email
     * @return Optional containing user if found
     */
    Optional<User> findByEmail(String email);

    /**
     * Find all users by role
     *
     * @param role User role
     * @return List of users with specified role
     */
    List<User> findByRole(User.UserRole role);

    /**
     * Find all users by department
     *
     * @param department Department name
     * @return List of users in specified department
     */
    List<User> findByDepartment(String department);

    /**
     * Find all active users
     *
     * @return List of active users
     */
    List<User> findByActiveTrue();

    /**
     * Find all active users in a department
     *
     * @param department Department name
     * @return List of active users in department
     */
    List<User> findByDepartmentAndActiveTrue(String department);

    /**
     * Check if user exists by email
     *
     * @param email Email address
     * @return true if user exists
     */
    boolean existsByEmail(String email);

    /**
     * Find all department officers in a specific department
     *
     * @param department Department name
     * @return List of department officers
     */
    @Query("SELECT u FROM User u WHERE u.role = 'DEPT_OFFICER' AND u.department = :department AND u.active = true")
    List<User> findDepartmentOfficersByDepartment(@Param("department") String department);
}
