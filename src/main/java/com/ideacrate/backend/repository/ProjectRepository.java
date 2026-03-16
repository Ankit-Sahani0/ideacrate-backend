package com.ideacrate.backend.repository;

import com.ideacrate.backend.entity.Project;
import com.ideacrate.backend.enums.ProjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    // -------------------------------------------------------------------------
    // JOIN FETCH queries — these replace the inherited findAll() / findById()
    // calls to eliminate the N+1 problem caused by LAZY author loading.
    // Without JOIN FETCH, every call to project.getAuthor() inside the service
    // mapper would fire a separate SELECT against the users table.
    // -------------------------------------------------------------------------

    /**
     * Fetches all projects together with their authors in a single SQL JOIN.
     * Replaces the bare projectRepository.findAll() call in getAllProjects().
     */
    @Query("SELECT p FROM Project p LEFT JOIN FETCH p.author")
    List<Project> findAllWithAuthor();

    /**
     * Fetches a single project together with its author in one query.
     * Replaces the bare projectRepository.findById() calls.
     */
    @Query("SELECT p FROM Project p LEFT JOIN FETCH p.author WHERE p.id = :id")
    Optional<Project> findByIdWithAuthor(@Param("id") Long id);

    @Query("SELECT p FROM Project p LEFT JOIN FETCH p.author WHERE p.status IN :statuses")
    List<Project> findAllWithAuthorByStatusIn(@Param("statuses") List<ProjectStatus> statuses);

    /**
     * Fetches all projects belonging to a given user with the author eagerly loaded.
     * Replaces the derived findByAuthorId() call in getProjectsByUser().
     */
    @Query("SELECT p FROM Project p LEFT JOIN FETCH p.author WHERE p.author.id = :authorId")
    List<Project> findByAuthorIdWithAuthor(@Param("authorId") Long authorId);

    // -------------------------------------------------------------------------
    // Search — native query kept as-is; author is accessed after the fact but
    // the result set is usually small and filtered, so the tradeoff is acceptable.
    // A JPQL version with JOIN FETCH is provided for completeness.
    // -------------------------------------------------------------------------

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
