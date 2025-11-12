package com.medo.accounts.constants;

import java.util.HashMap;
import java.util.Map;

public class AccountsConstants {
    private AccountsConstants() {
        // restrict instantiation
    }

    public static Map<String, String> CONST = new HashMap<>();

    static {
        CONST.put("SAVINGS", "Savings");
        CONST.put("ADDRESS", "123 Main Street, New York");
        CONST.put("STATUS_201", "201");
        CONST.put("MESSAGE_201", "Account created successfully");
        CONST.put("STATUS_200", "200");
        CONST.put("MESSAGE_200", "Request processed successfully");
        CONST.put("STATUS_417", "417");
        CONST.put("MESSAGE_417_UPDAT", "Update operation failed. Please try again or contact Dev team");
        CONST.put("MESSAGE_417_DELET", "Delete operation failed. Please try again or contact Dev team");
        CONST.put("V_NAME", "Name length must be 5 to 30");
        CONST.put("V_EMAIL", "Email must be valid");
        CONST.put("V_MOBILE", "MobileNumber must be 10 digits");
        CONST.put("V_ACCOUNT_NUM", "AccountNumber must be 10 digits");
        CONST.put("V_ACCOUNT_TYPE", "AccountType is mandatory");
        CONST.put("V_BRANCH_ADDRESS", "BranchAddress is mandatory");
    }
}
