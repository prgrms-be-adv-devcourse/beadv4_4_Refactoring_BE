package com.thock.back.member.in.dto;

import com.thock.back.member.domain.command.SignUpCommand;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignUpRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 2, max = 50) String name,
        @NotBlank @Size(min = 8, max = 100) String password
) {
    public SignUpCommand toCommand() {
        return new SignUpCommand(email, name, password);
    }
}
