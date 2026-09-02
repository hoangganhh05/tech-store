package com.techstore.infrastructure.database;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class JdbcDatabaseProbe implements DatabaseProbe {

    private final JdbcTemplate jdbcTemplate;

    public JdbcDatabaseProbe(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean isAvailable() {
        Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        return result != null && result == 1;
    }
}
