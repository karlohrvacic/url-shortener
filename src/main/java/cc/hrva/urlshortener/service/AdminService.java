package cc.hrva.urlshortener.service;

import cc.hrva.urlshortener.dto.AdminStatsResponse;

public interface AdminService {

    AdminStatsResponse getDashboardStats();
    byte[] exportAllUrlsAsCsv();

}
