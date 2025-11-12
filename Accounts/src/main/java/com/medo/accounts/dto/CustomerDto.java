package com.medo.accounts.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CustomerDto {
    @NotBlank(message = "V_NAME") @Size(min = 5, max = 30, message = "V_NAME")
    private String name;
    @NotBlank(message = "V_EMAIL") @Email(message = "V_EMAIL")
    private String email;
    @NotEmpty(message = "V_MOBILE") @Pattern(regexp = "(^$|[0-9]{10})", message = "V_MOBILE")
    private String mobileNumber;
    private AccountsDto accountsDto;
}
