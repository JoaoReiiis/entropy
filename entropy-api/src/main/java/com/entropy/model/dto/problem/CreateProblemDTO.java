package com.entropy.model.dto.problem;

public record CreateProblemDTO (String title,
                                String description,
                                Integer maxSubmissions,
                                Integer points){
}
