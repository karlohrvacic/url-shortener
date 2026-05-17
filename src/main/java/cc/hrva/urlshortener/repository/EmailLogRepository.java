package cc.hrva.urlshortener.repository;

import cc.hrva.urlshortener.model.EmailLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailLogRepository extends JpaRepository<EmailLog, Long> {

    Page<EmailLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

}
