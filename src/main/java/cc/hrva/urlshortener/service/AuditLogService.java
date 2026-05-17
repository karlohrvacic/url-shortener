package cc.hrva.urlshortener.service;

import cc.hrva.urlshortener.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuditLogService {

    void log(String performedBy, String action, String targetType, String targetIdentifier, String details);
    Page<AuditLog> getAuditLogs(Pageable pageable);

}
