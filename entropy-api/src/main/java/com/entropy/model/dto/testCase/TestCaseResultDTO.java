package com.entropy.model.dto.testCase;

import java.util.List;

public record TestCaseResultDTO (List<String> inputs,
                                String expectedOutput,
                                String actualOutput,
                                boolean passed){
}
