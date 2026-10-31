package com.mobdata.pontocerto.repository;

import com.mobdata.pontocerto.model.FechamentoLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface FechamentoLogRepository extends JpaRepository<FechamentoLog, UUID>, JpaSpecificationExecutor<FechamentoLog> {
}
