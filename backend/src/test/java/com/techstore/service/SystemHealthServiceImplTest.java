package com.techstore.service;

import com.techstore.dto.response.HealthResponse;
import com.techstore.exception.DatabaseUnavailableException;
import com.techstore.infrastructure.database.DatabaseProbe;
import com.techstore.mapper.HealthMapper;
import com.techstore.service.impl.SystemHealthServiceImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SystemHealthServiceImplTest {

    private final DatabaseProbe databaseProbe = mock(DatabaseProbe.class);
    private final SystemHealthService service = new SystemHealthServiceImpl(databaseProbe, new HealthMapper());

    @Test
    void checkHealthReturnsUpWhenDatabaseResponds() {
        when(databaseProbe.isAvailable()).thenReturn(true);

        HealthResponse response = service.checkHealth();

        assertEquals("UP", response.status());
        assertEquals("UP", response.database());
    }

    @Test
    void checkHealthThrowsDomainExceptionWhenDatabaseFails() {
        when(databaseProbe.isAvailable())
                .thenThrow(new IllegalStateException("connection refused"));

        assertThrows(DatabaseUnavailableException.class, service::checkHealth);
    }
}
