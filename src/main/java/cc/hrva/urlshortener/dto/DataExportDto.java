package cc.hrva.urlshortener.dto;

import java.time.LocalDateTime;
import java.util.List;

public record DataExportDto(
        LocalDateTime exportedAt,
        UserDto profile,
        List<UrlResponse> urls,
        List<ApiKeyResponse> apiKeys,
        List<EmailExport> emails) {

    public record EmailExport(String subject, String status, LocalDateTime sentAt, LocalDateTime createdAt) {}

}
