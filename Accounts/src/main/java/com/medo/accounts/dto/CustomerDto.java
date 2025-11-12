package com.medo.accounts.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
@Schema(name = "Customer",
        description = "Schema that holds customer and account data"
)
public class CustomerDto {
    @Schema(description = "customer's name", example = "John Doe")
    @NotBlank(message = "V_NAME") @Size(min = 5, max = 30, message = "V_NAME")
    private String name;
    @Schema(description = "customer's email", example = "medo@test.com")
    @NotBlank(message = "V_EMAIL") @Email(message = "V_EMAIL")
    private String email;
    @Schema(description = "customer's mobile", example = "123456789")
    @NotEmpty(message = "V_MOBILE") @Pattern(regexp = "(^$|[0-9]{10})", message = "V_MOBILE")
    private String mobileNumber;
    private AccountsDto accountsDto;
}
