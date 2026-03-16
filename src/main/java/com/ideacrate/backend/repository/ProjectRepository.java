package com.ideacrate.backend.project;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    // Combined search with sorting
    @Query(value = """
        SELECT * FROM projects p
        WHERE
          ( :searchTerm IS NULL OR (
              LOWER(p.title) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
              OR LOWER(p.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
              OR LOWER(p.full_description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
              OR LOWER(p.tech_stack) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
          ))
          AND ( :category IS NULL OR LOWER(p.category) = LOWER(:category) )
        """, nativeQuery = true)
    List<Project> searchProjects(
            @Param("searchTerm") String searchTerm,
            @Param("category") String category
    );
}
