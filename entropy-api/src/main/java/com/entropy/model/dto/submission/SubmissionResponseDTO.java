package com.entropy.model.dto.submission;

import com.entropy.model.dto.testCase.TestCaseResultDTO;

import java.time.LocalDateTime;
import java.util.List;

public record SubmissionResponseDTO(
        Long problemId,
        String problemTitle,
        List<TestCaseResultDTO> testResults,
        String username,
        int userScore,
        boolean problemSolved,
        LocalDateTime submissionTime,
        String errorMessage
) {}
