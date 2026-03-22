package com.medo.loans.mapper;

import com.medo.loans.dto.LoansDto;
import com.medo.loans.entity.Loans;

public class LoansMapper {
    public static LoansDto mapToLoansDto(Loans loan, LoansDto loansDto) {
        loansDto.setMobileNumber(loan.getMobileNumber());
        loansDto.setLoanNumber(loan.getLoanNumber());
        loansDto.setLoanType(loan.getLoanType());
        loansDto.setTotalLoan(loan.getTotalLoan());
        loansDto.setAmountPaid(loan.getAmountPaid());
        loansDto.setOutstandingAmount(loan.getOutstandingAmount());
        return  loansDto;
    }
    public static Loans mapToLoans(LoansDto loansDto, Loans loans) {
        loans.setMobileNumber(loansDto.getMobileNumber());
        loans.setLoanNumber(loansDto.getLoanNumber());
        loans.setLoanType(loansDto.getLoanType());
        loans.setTotalLoan(loansDto.getTotalLoan());
        loans.setAmountPaid(loansDto.getAmountPaid());
        loans.setOutstandingAmount(loansDto.getOutstandingAmount());
        return  loans;
    }
}
