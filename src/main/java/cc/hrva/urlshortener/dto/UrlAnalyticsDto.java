package cc.hrva.urlshortener.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record UrlAnalyticsDto(
        Long id,
        String shortUrl,
        String longUrl,
        String status,
        long visits,
        long uniqueRecentVisitors,
        long totalRecentClicks,
        LocalDateTime lastAccessed,
        LocalDateTime createDate,
        List<DailyCount> dailyNewVisitors) {

    public record DailyCount(LocalDate date, long count) {}

}
