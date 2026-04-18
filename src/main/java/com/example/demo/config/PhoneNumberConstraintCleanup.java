package com.example.demo.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PhoneNumberConstraintCleanup {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void dropPhoneNumberUniqueConstraint() {
        try {
            List<String> constraintNames = jdbcTemplate.queryForList(
                    """
                    select c.conname
                    from pg_constraint c
                    join pg_class t on c.conrelid = t.oid
                    join pg_attribute a on a.attrelid = t.oid and a.attnum = any (c.conkey)
                    where t.relname = 'users'
                      and a.attname = 'phone_number'
                      and c.contype = 'u'
                    """,
                    String.class
            );

            for (String constraintName : constraintNames) {
                jdbcTemplate.execute("alter table users drop constraint if exists " + constraintName);
            }
        } catch (Exception ignored) {
            // Ignore cleanup failures so the app can still start even if the DB is not PostgreSQL-ready yet.
        }
    }
}
