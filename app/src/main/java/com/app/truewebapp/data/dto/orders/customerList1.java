package com.app.truewebapp.data.dto.orders;

public class customerList1 {
    String AccountId;
    String Account;

    public String getAccountId() {
        return AccountId;
    }

    public void setAccountId(String accountId) {
        AccountId = accountId;
    }

    public String getAccount() {
        return Account;
    }

    public void setAccount(String account) {
        Account = account;
    }

    @Override
    public String toString() {
        return "customerList{" +
                "AccountId='" + AccountId + '\'' +
                ", Account='" + Account + '\'' +
                '}';
    }
}
