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
import java.lang.management.MemoryMXBean;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
    private final Environment environment;

    @Value("${spring.application.name:url-shortener}")
    private String appName;

    public DefaultAdminService(
            final UserRepository userRepository,
            final UrlRepository urlRepository,
            final ApiKeyRepository apiKeyRepository,
            final MeterRegistry meterRegistry,
            final Environment environment) {
        this.userRepository = userRepository;
        this.urlRepository = urlRepository;
        this.apiKeyRepository = apiKeyRepository;
        this.meterRegistry = meterRegistry;
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
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    @Override
    public AdminStatsResponse getDashboardStats() {
        final var totalUsers = userRepository.count();
        final var totalUrls = urlRepository.count();
        final var activeUrls = urlRepository.countByActiveTrue();
        final var totalApiKeys = apiKeyRepository.count();

        final var uptime = ManagementFactory.getRuntimeMXBean().getUptime();
        final var uptimeFormatted = formatUptime(uptime);

        final var recentUrls = urlRepository.findAll(PageRequest.of(0, 10)).stream()
                .map(url -> new RecentUrl(
                        url.getId(),
                        url.getShortUrl(),
                        url.getLongUrl(),
                        url.getCreateDate(),
                        url.getVisits(),
                        url.getOwner() != null ? url.getOwner().getEmail() : null))
                .toList();

        final var cacheHitRatio = getCacheHitRatio();
        final var activeProfiles = List.of(environment.getActiveProfiles());
        final var jvmMemory = getJvmMemory();
        final var requestsCount = getRequestCount();

        return new AdminStatsResponse(
                totalUsers,
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
                requestsCount);
    }

    private String getCacheHitRatio() {
        try {
            final var gets = meterRegistry.find("cache.gets").tag("result", "hit").counters();
            final var hits = gets.stream().mapToLong(c -> (long) c.count()).sum();
            final var misses = meterRegistry.find("cache.gets").tag("result", "miss").counters().stream()
                    .mapToLong(c -> (long) c.count()).sum();
            final var total = hits + misses;
            if (total == 0) {
                return "—";
            }
            return String.format("%.1f%%", (double) hits / total * 100);
        } catch (final Exception e) {
            return "—";
        }
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
