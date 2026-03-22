package com.medo.loans.service;

import com.medo.loans.dto.LoansDto;


public interface ILoansService {
    void createLoan(String mobileNumber);
    LoansDto getLoan(String mobileNumber);
    boolean updateLoan(LoansDto loansDto);
    boolean deleteLoan(String mobileNumber);
}
