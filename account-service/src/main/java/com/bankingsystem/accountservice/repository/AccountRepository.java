package com.bankingsystem.accountservice.repository;

import com.bankingsystem.accountservice.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account,String> {
}
