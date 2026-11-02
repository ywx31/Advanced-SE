package com.mathmate.controller;

import com.mathmate.common.ApiResponse;
import com.mathmate.common.PageResult;
import com.mathmate.dto.practice.PracticeConfigRequest;
import com.mathmate.dto.practice.SessionDetail;
import com.mathmate.dto.practice.SessionSummary;
import com.mathmate.dto.practice.SubmitPracticeRequest;
import com.mathmate.dto.practice.WorksheetView;
import com.mathmate.security.SecurityUtils;
import com.mathmate.service.PracticeService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/practice")
public class PracticeController {
    private final PracticeService practiceService;

    public PracticeController(PracticeService practiceService) {
        this.practiceService = practiceService;
    }

    @PostMapping("/sessions")
    public ApiResponse<SessionDetail> create(@Valid @RequestBody PracticeConfigRequest request) {
        return ApiResponse.ok(practiceService.createSession(SecurityUtils.currentUserId(), request));
    }

    @PostMapping("/worksheets")
    public ApiResponse<WorksheetView> createWorksheet(
            @Valid @RequestBody PracticeConfigRequest request
    ) {
        return ApiResponse.ok(practiceService.createWorksheet(request));
    }

    @GetMapping("/sessions")
    public ApiResponse<PageResult<SessionSummary>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.ok(practiceService.listSessions(SecurityUtils.currentUserId(), page, size));
    }

    @GetMapping("/sessions/{id}")
    public ApiResponse<SessionDetail> detail(@PathVariable Long id) {
        return ApiResponse.ok(practiceService.getSession(SecurityUtils.currentUserId(), id));
    }

    @PostMapping("/sessions/{id}/submit")
    public ApiResponse<SessionDetail> submit(
            @PathVariable Long id,
            @Valid @RequestBody SubmitPracticeRequest request
    ) {
        return ApiResponse.ok(practiceService.submit(SecurityUtils.currentUserId(), id, request));
    }
}
