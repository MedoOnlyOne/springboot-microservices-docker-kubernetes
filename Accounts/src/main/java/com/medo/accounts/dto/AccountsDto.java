package com.medo.accounts.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Schema(
        name = "Accounts",
        description = "Schema that holds account data"
)
public class AccountsDto {
    @Schema(description = "customer's account", example = "1234567890")
    @NotBlank(message = "V_ACCOUNT_NUM") @Pattern(regexp = "(^$|[0-9]{10})", message = "V_ACCOUNT_NUM")
    private Long accountNumber;
    @Schema(description = "customer's account type", example = "Saving")
    @NotBlank(message = "V_ACCOUNT_TYPE")
    private String accountType;
    @Schema(description = "bank's branch address", example = "123 Cairo")
    @NotBlank(message = "V_BRANCH_ADDRESS")
    private String branchAddress;
}
