package com.shepherdmoney.interviewproject.repository;

import com.shepherdmoney.interviewproject.model.BalanceHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface BalanceHistoryRepository extends JpaRepository<BalanceHistory, LocalDate> {
}