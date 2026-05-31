package cc.hrva.urlshortener.dto;

import java.util.List;

public record AnalyticsOverviewDto(
        long totalUrls,
        long activeUrls,
        long expiredUrls,
        long totalVisits,
        List<TopUrl> topUrls) {

    public record TopUrl(Long id, String shortUrl, String longUrl, long visits) {}

}
