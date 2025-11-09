package com.medo.accounts.controller;

import com.medo.accounts.constants.AccountsConstants;
import com.medo.accounts.dto.AccountsDto;
import com.medo.accounts.dto.CustomerDto;
import com.medo.accounts.dto.ResponseDto;
import com.medo.accounts.service.IAccountsService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/v1/accounts", produces = MediaType.APPLICATION_JSON_VALUE)
@AllArgsConstructor
public class AccountsController {
    private IAccountsService accountsService;

    @PostMapping("/create")
    public ResponseEntity<ResponseDto> createAccount(@RequestBody CustomerDto customerDto) {
        accountsService.createAccount(customerDto);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ResponseDto.builder()
                        .statusCode(AccountsConstants.STATUS_201)
                        .statusMsg(AccountsConstants.MESSAGE_201)
                        .build());
    }

}
