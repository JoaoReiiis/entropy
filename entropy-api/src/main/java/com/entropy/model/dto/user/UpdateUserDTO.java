package com.entropy.model.dto.user;

import jakarta.validation.constraints.NotBlank;

public record UpdateUserDTO(@NotBlank(message = "Nome é obrigatório")
                            String username,
                            @NotBlank(message = "Senha é obrigatório")
                            String password) {
}
