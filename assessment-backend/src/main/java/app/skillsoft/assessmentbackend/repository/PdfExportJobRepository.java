package app.skillsoft.assessmentbackend.repository;

import app.skillsoft.assessmentbackend.domain.entities.ExportStatus;
import app.skillsoft.assessmentbackend.domain.entities.PdfExportJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PdfExportJobRepository extends JpaRepository<PdfExportJob, UUID> {

    Optional<PdfExportJob> findByContentHashAndStatus(String contentHash, ExportStatus status);

    @Query("SELECT j FROM PdfExportJob j WHERE j.clerkUserId = :userId AND j.status IN :statuses")
    List<PdfExportJob> findByClerkUserIdAndStatusIn(
        @Param("userId") String userId,
        @Param("statuses") List<ExportStatus> statuses
    );

    List<PdfExportJob> findByStatusAndExpiresAtBefore(ExportStatus status, LocalDateTime before);

    @Query("SELECT j FROM PdfExportJob j WHERE j.status = 'GENERATING' AND j.startedAt < :cutoff")
    List<PdfExportJob> findStuckGeneratingJobs(@Param("cutoff") LocalDateTime cutoff);
}
