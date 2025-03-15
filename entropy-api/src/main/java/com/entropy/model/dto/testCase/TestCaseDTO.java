package com.entropy.model.dto.testCase;

import java.util.List;

public record TestCaseDTO (
        List<String> inputs,
        String expectedOutput
){
}
