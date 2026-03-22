package com.medo.loans.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoansDto {
    @NotEmpty(message = "mobile number can not be null ore empty")
    @Pattern(regexp = "^$|[0-9]{10}", message = "Mobile number must be 10 digits")
    private String mobileNumber;
    @NotEmpty(message = "Loan Number can not be a null or empty")
    @Pattern(regexp="(^$|[0-9]{12})",message = "LoanNumber must be 12 digits")
    private String loanNumber;
    @NotEmpty(message = "LoanType can not be a null or empty")
    private String loanType;
    @Positive(message = "Total loan amount should be greater than zero")
    private int totalLoan;
    @PositiveOrZero(message = "Total loan amount paid should be equal or greater than zero")
    private int amountPaid;
    @PositiveOrZero(message = "Total outstanding amount should be equal or greater than zero")
    private int outstandingAmount;
}
