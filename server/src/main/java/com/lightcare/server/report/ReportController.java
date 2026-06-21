package com.lightcare.server.report;

import com.lightcare.server.common.ApiError;
import com.lightcare.server.common.ApiException;
import com.lightcare.server.common.ApiResponse;
import com.lightcare.server.common.CurrentUserAnnotation;
import com.lightcare.server.profile.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final WeeklyReportService service;
    private final ProfileRepository profileRepo;   // 鉴权薄弱点补

    @GetMapping("/weekly")
    public ApiResponse<WeeklyReportService.WeeklyReport> weekly(
            @CurrentUserAnnotation long userId,
            @RequestParam long profileId) {
        // 鉴权薄弱点补：必须 userId 拥有该 profile
        var p = profileRepo.findById(profileId)
            .orElseThrow(() -> new ApiException(ApiError.NOT_FOUND));
        if (p.getOwnerUserId() != userId && !java.util.Objects.equals(p.getManagedByUserId(), userId)) {
            throw new ApiException(ApiError.FORBIDDEN);
        }
        return ApiResponse.ok(service.build(profileId));
    }
}