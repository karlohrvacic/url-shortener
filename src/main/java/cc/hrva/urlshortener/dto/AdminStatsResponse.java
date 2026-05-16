package cc.hrva.urlshortener.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AdminStatsResponse(
        long totalUsers,
        long totalUrls,
        long activeUrls,
        long totalApiKeys,
        String appVersion,
        String uptime,
        boolean cacheActive,
        boolean databaseActive,
        String javaVersion,
        LocalDateTime serverTime,
        List<RecentUrl> recentUrls,
        String cacheHitRatio,
        List<String> activeProfiles,
        String jvmMemoryUsed,
        String jvmMemoryMax,
        long requestsCount) {

    public record RecentUrl(Long id, String shortUrl, String longUrl, LocalDateTime createDate, long visits, String ownerEmail) {}

}
