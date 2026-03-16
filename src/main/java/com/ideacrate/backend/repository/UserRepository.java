package com.ideacrate.backend.repository;

import com.ideacrate.backend.entity.User;
import com.ideacrate.backend.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

        @Query("""
                SELECT u FROM User u
                WHERE u.role = :role
                    AND (
                        LOWER(u.firstName) LIKE LOWER(CONCAT('%', :query, '%'))
                        OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :query, '%'))
                        OR LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%'))
                    )
                """)
        List<User> searchByQueryAndRole(@Param("query") String query, @Param("role") Role role);
}