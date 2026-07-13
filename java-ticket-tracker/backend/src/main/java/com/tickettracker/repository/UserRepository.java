package com.tickettracker.repository;

import com.tickettracker.model.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, String> {

    Optional<UserEntity> findByEmail(String email);

    List<UserEntity> findByRole(String role);

    List<UserEntity> findByDepartment(String department);

    List<UserEntity> findByActive(Boolean active);

    List<UserEntity> findByRoleAndDepartment(String role, String department);

    boolean existsByEmail(String email);
}
