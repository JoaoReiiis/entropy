package com.entropy.model.dto.submission;

import com.entropy.model.dto.testCase.TestCaseResultDTO;

import java.time.LocalDateTime;
import java.util.List;

public record CodeExecutionResult(
        List<TestCaseResultDTO> testResults,
        boolean problemSolved,
        int updatedUserScore,
        String errorMessage,
        LocalDateTime submissionTime
) {}