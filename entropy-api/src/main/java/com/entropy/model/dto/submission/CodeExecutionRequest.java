package com.entropy.model.dto.submission;

import com.entropy.model.Language;
import com.entropy.model.Problem;
import com.entropy.model.Users;

public record CodeExecutionRequest(
        Users user,
        Problem problem,
        String code,
        Language language,
        String filename
) {}
