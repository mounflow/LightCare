package com.lightcare.server.report;

import com.lightcare.server.common.ApiResponse;
import com.lightcare.server.common.CurrentUserAnnotation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final WeeklyReportService service;

    @GetMapping("/weekly")
    public ApiResponse<WeeklyReportService.WeeklyReport> weekly(
            @CurrentUserAnnotation long userId,
            @RequestParam long profileId) {
        return ApiResponse.ok(service.build(profileId));
    }
}
