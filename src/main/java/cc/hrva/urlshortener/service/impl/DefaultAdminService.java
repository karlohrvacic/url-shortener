package cc.hrva.urlshortener.service.impl;

import cc.hrva.urlshortener.dto.AdminStatsResponse;
import cc.hrva.urlshortener.dto.AdminStatsResponse.RecentUrl;
import cc.hrva.urlshortener.repository.ApiKeyRepository;
import cc.hrva.urlshortener.repository.UrlRepository;
import cc.hrva.urlshortener.repository.UserRepository;
import cc.hrva.urlshortener.service.AdminService;
import io.micrometer.core.instrument.MeterRegistry;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class DefaultAdminService implements AdminService {

    private static final Logger log = LoggerFactory.getLogger(DefaultAdminService.class);

    private final UserRepository userRepository;
    private final UrlRepository urlRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final MeterRegistry meterRegistry;
    private final CacheManager cacheManager;
    private final Environment environment;

    @Value("${spring.application.name:url-shortener}")
    private String appName;

    public DefaultAdminService(
            final UserRepository userRepository,
            final UrlRepository urlRepository,
            final ApiKeyRepository apiKeyRepository,
            final MeterRegistry meterRegistry,
            final CacheManager cacheManager,
            final Environment environment) {
        this.userRepository = userRepository;
        this.urlRepository = urlRepository;
        this.apiKeyRepository = apiKeyRepository;
        this.meterRegistry = meterRegistry;
        this.cacheManager = cacheManager;
        this.environment = environment;
    }

    @Override
    public byte[] exportAllUrlsAsCsv() {
        final var urls = urlRepository.findAll(PageRequest.of(0, Integer.MAX_VALUE));
        final var sb = new StringBuilder();
        sb.append("ID,Short URL,Long URL,Owner,Visits,Visit Limit,Created,Expires,Active\n");
        for (final var url : urls) {
            sb.append(String.format("%d,%s,%s,%s,%d,%d,%s,%s,%b\n",
                    url.getId(),
                    url.getShortUrl(),
                    escapeCsv(url.getLongUrl()),
                    url.getOwner() != null ? url.getOwner().getEmail() : "",
                    url.getVisits(),
                    url.getVisitLimit() != null ? url.getVisitLimit() : 0,
                    url.getCreateDate() != null ? url.getCreateDate().toString() : "",
                    url.getExpirationDate() != null ? url.getExpirationDate().toString() : "",
                    url.isActive()));
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String escapeCsv(final String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    @Override
    public AdminStatsResponse getDashboardStats() {
        final var now = LocalDateTime.now();
        final var totalUsers = userRepository.count();
        final var newUsers7d = userRepository.countByCreateDateAfter(now.minusDays(7));
        final var newUsers30d = userRepository.countByCreateDateAfter(now.minusDays(30));
        final var totalUrls = urlRepository.count();
        final var activeUrls = urlRepository.countByActiveTrue();
        final var totalApiKeys = apiKeyRepository.count();
        final var uptimeFormatted = formatUptime(ManagementFactory.getRuntimeMXBean().getUptime());
        final var recentUrls = fetchRecentUrls();
        final var cacheHitRatio = getCacheHitRatio();
        final var activeProfiles = List.of(environment.getActiveProfiles());
        final var jvmMemory = getJvmMemory();
        final var requestsCount = getRequestCount();
        final var redirectTimer = getRedirectTimer();

        return buildAdminStatsResponse(
                totalUsers, newUsers7d, newUsers30d, totalUrls, activeUrls, totalApiKeys,
                uptimeFormatted, recentUrls, cacheHitRatio, activeProfiles,
                jvmMemory, requestsCount, redirectTimer);
    }

    private List<RecentUrl> fetchRecentUrls() {
        return urlRepository.findAll(PageRequest.of(0, 10)).stream()
                .map(url -> new RecentUrl(
                        url.getId(),
                        url.getShortUrl(),
                        url.getLongUrl(),
                        url.getCreateDate(),
                        url.getVisits(),
                        url.getOwner() != null ? url.getOwner().getEmail() : null))
                .toList();
    }

    private AdminStatsResponse buildAdminStatsResponse(
            final long totalUsers,
            final long newUsers7d,
            final long newUsers30d,
            final long totalUrls,
            final long activeUrls,
            final long totalApiKeys,
            final String uptimeFormatted,
            final List<RecentUrl> recentUrls,
            final String cacheHitRatio,
            final List<String> activeProfiles,
            final JvmMemory jvmMemory,
            final long requestsCount,
            final RedirectTiming redirectTimer) {
        return new AdminStatsResponse(
                totalUsers,
                newUsers7d,
                newUsers30d,
                totalUrls,
                activeUrls,
                totalApiKeys,
                "1.4.2",
                uptimeFormatted,
                true,
                true,
                System.getProperty("java.version"),
                LocalDateTime.now(),
                recentUrls,
                cacheHitRatio,
                activeProfiles,
                jvmMemory.used,
                jvmMemory.max,
                requestsCount,
                redirectTimer.avgMs,
                redirectTimer.maxMs,
                redirectTimer.count);
    }

    private record RedirectTiming(String avgMs, String maxMs, long count) {}

    private RedirectTiming getRedirectTimer() {
        try {
            return readRedirectTimer();
        } catch (final Exception e) {
            return new RedirectTiming("—", "—", 0);
        }
    }

    private RedirectTiming readRedirectTimer() {
        final var timer = meterRegistry.find("redirect.duration").timer();
        if (timer == null || timer.count() == 0) {
            return new RedirectTiming("—", "—", 0);
        }
        final var avgMs = String.format("%.0f", timer.mean(TimeUnit.MILLISECONDS));
        final var maxMs = String.format("%.0f", timer.max(TimeUnit.MILLISECONDS));
        return new RedirectTiming(avgMs, maxMs, timer.count());
    }

    private String getCacheHitRatio() {
        final var ratio = getCacheHitRatioFromMeterRegistry();
        if (ratio != null) {
            return ratio;
        }

        final var cache = cacheManager.getCache("urls");
        if (cache != null) {
            try {
                cache.get("__health__");
                return "Active";
            } catch (final Exception e) {
                return "Unreachable";
            }
        }

        return "—";
    }

    private String getCacheHitRatioFromMeterRegistry() {
        try {
            final var gets = meterRegistry.find("cache.gets").tag("result", "hit").counters();
            final var hits = gets.stream().mapToLong(counter -> (long) counter.count()).sum();
            final var misses = meterRegistry.find("cache.gets").tag("result", "miss").counters().stream()
                    .mapToLong(counter -> (long) counter.count()).sum();
            final var total = hits + misses;
            if (total > 0) {
                return String.format("%.1f%%", (double) hits / total * 100);
            }
        } catch (final Exception e) {
            log.debug("Failed to read Micrometer cache metrics", e);
        }

        return null;
    }

    private JvmMemory getJvmMemory() {
        try {
            final var memoryBean = ManagementFactory.getMemoryMXBean();
            final var heap = memoryBean.getHeapMemoryUsage();
            final var used = heap.getUsed() / (1024 * 1024);
            final var max = heap.getMax() / (1024 * 1024);
            return new JvmMemory(used + " MB", max + " MB");
        } catch (final Exception e) {
            return new JvmMemory("—", "—");
        }
    }

    private record JvmMemory(String used, String max) {}

    private long getRequestCount() {
        try {
            final var counter = meterRegistry.find("http.server.requests").counter();
            return counter != null ? (long) counter.count() : 0;
        } catch (final Exception e) {
            return 0;
        }
    }

    private String formatUptime(final long uptimeMillis) {
        final var duration = Duration.ofMillis(uptimeMillis);
        final var days = duration.toDays();
        final var hours = duration.toHours() % 24;
        final var minutes = duration.toMinutes() % 60;

        if (days > 0) {
            return String.format("%dd %dh %dm", days, hours, minutes);
        } else if (hours > 0) {
            return String.format("%dh %dm", hours, minutes);
        }
        return String.format("%dm", Math.max(1, minutes));
    }

}
