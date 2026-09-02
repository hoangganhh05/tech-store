package com.techstore.service.impl;

import com.techstore.dto.response.HealthResponse;
import com.techstore.exception.DatabaseUnavailableException;
import com.techstore.infrastructure.database.DatabaseProbe;
import com.techstore.mapper.HealthMapper;
import com.techstore.service.SystemHealthService;
import org.springframework.stereotype.Service;

@Service
public class SystemHealthServiceImpl implements SystemHealthService {

    private final DatabaseProbe databaseProbe;
    private final HealthMapper healthMapper;

    public SystemHealthServiceImpl(DatabaseProbe databaseProbe, HealthMapper healthMapper) {
        this.databaseProbe = databaseProbe;
        this.healthMapper = healthMapper;
    }

    @Override
    public HealthResponse checkHealth() {
        try {
            if (!databaseProbe.isAvailable()) {
                throw new DatabaseUnavailableException("Database health check returned an unexpected result");
            }
            return healthMapper.toResponse("UP");
        } catch (DatabaseUnavailableException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new DatabaseUnavailableException("Cannot connect to the database", exception);
        }
    }
}
