package com.mathmate.controller;

import com.mathmate.common.ApiResponse;
import com.mathmate.common.PageResult;
import com.mathmate.dto.wrongbook.MasteredRequest;
import com.mathmate.dto.wrongbook.WrongPracticeRequest;
import com.mathmate.dto.wrongbook.WrongQuestionView;
import com.mathmate.dto.practice.SessionDetail;
import com.mathmate.security.SecurityUtils;
import com.mathmate.service.WrongBookService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wrong-book")
public class WrongBookController {
    private final WrongBookService wrongBookService;

    public WrongBookController(WrongBookService wrongBookService) {
        this.wrongBookService = wrongBookService;
    }

    @GetMapping
    public ApiResponse<PageResult<WrongQuestionView>> list(
            @RequestParam(required = false) Boolean mastered,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        return ApiResponse.ok(
                wrongBookService.list(SecurityUtils.currentUserId(), mastered, page, size)
        );
    }

    @PostMapping("/practice")
    public ApiResponse<SessionDetail> createPractice(
            @Valid @RequestBody WrongPracticeRequest request
    ) {
        return ApiResponse.ok(
                wrongBookService.createPractice(SecurityUtils.currentUserId(), request)
        );
    }

    @PatchMapping("/{id}/mastered")
    public ApiResponse<WrongQuestionView> setMastered(
            @PathVariable Long id,
            @Valid @RequestBody MasteredRequest request
    ) {
        return ApiResponse.ok(
                wrongBookService.setMastered(SecurityUtils.currentUserId(), id, request.mastered())
        );
    }
}
