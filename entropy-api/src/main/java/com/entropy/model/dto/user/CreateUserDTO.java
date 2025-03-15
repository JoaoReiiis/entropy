package com.entropy.model.dto.user;

import com.entropy.model.Role;
import jakarta.validation.constraints.NotBlank;

public record CreateUserDTO(@NotBlank(message = "Nome é obrigatório")
                            String username,
                            @NotBlank(message = "Login é obrigatório")
                            String login,
                            @NotBlank(message = "Role é obrigatório")
                            Role role,
                            @NotBlank(message = "Senha é obrigatório")
                            String password) {
}
