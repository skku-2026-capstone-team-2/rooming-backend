package com.skku.zip.domain.user.entity;

public interface User {
    Long getId();

    String getEmail();

    String getName();

    String getProvider();

    String getLoginId();

    AccountType getAccountType();
}
