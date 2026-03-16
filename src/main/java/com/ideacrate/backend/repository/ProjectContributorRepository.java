package com.ideacrate.backend.project;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectContributorRepository extends JpaRepository<ProjectContributor, Long> {

    List<ProjectContributor> findByProjectId(Long projectId);

    List<ProjectContributor> findByUserId(Long userId);

    Optional<ProjectContributor> findByProjectIdAndUserId(Long projectId, Long userId);

    boolean existsByProjectIdAndUserId(Long projectId, Long userId);

    @Query("SELECT COUNT(pc) FROM ProjectContributor pc WHERE pc.project.id = :projectId")
    long countByProjectId(@Param("projectId") Long projectId);

    void deleteByProjectIdAndUserId(Long projectId, Long userId);
}
