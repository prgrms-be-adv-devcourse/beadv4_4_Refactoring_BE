package com.thock.back.member.in.dto;

import com.thock.back.member.domain.command.LoginCommand;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 100) String password
) {
    public LoginCommand toCommand() {
        return new LoginCommand(email, password);
    }
}
