package com.medo.accounts.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class AccountsDto {
    @NotBlank(message = "V_ACCOUNT_NUM") @Pattern(regexp = "(^$|[0-9]{10})", message = "V_ACCOUNT_NUM")
    private Long accountNumber;
    @NotBlank(message = "V_ACCOUNT_TYPE")
    private String accountType;
    @NotBlank(message = "V_BRANCH_ADDRESS")
    private String branchAddress;
}
