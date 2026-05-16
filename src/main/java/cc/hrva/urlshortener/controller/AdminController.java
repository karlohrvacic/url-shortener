package cc.hrva.urlshortener.controller;

import cc.hrva.urlshortener.dto.AdminStatsResponse;
import cc.hrva.urlshortener.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/admin")
@PreAuthorize("hasRole('ROLE_ADMIN')")
@Tag(name = "Admin", description = "Admin dashboard operations")
public class AdminController {

    private final AdminService adminService;

    public AdminController(final AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/stats")
    @Operation(summary = "Get admin dashboard stats", description = "Returns system-wide statistics. Requires ROLE_ADMIN.")
    public ResponseEntity<AdminStatsResponse> getDashboardStats() {
        return ResponseEntity.ok(adminService.getDashboardStats());
    }

    @Operation(summary = "Export all URLs as CSV", description = "Download all URLs as a CSV file. Requires ROLE_ADMIN.")
    @GetMapping("/urls/export")
    public ResponseEntity<byte[]> exportAllUrls() {
        final var csv = adminService.exportAllUrlsAsCsv();
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=all-urls.csv")
                .contentType(MediaType.TEXT_PLAIN)
                .body(csv);
    }

}
