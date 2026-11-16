package com.taskflow.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Only presence and a sane upper bound are checked: format rules belong to registration, and
// a login attempt must not reveal which rule a stored credential satisfies.
public record LoginRequest(
        @NotBlank @Size(max = 254) String email,
        @NotBlank @Size(max = 72) String password) {
}
