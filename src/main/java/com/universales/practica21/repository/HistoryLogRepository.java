package com.universales.practica21.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.universales.practica21.entity.HistoryLog;

@Repository("historyLogRepository")
public interface HistoryLogRepository extends JpaRepository<HistoryLog, Long>{

}
