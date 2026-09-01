package me.imshy.pictogram.testsupport;

import javax.sql.DataSource;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;

public final class DatabaseCleaner {

    private final JdbcTemplate jdbc;

    public DatabaseCleaner(DataSource dataSource) {
        this.jdbc = new JdbcTemplate(dataSource);
    }

    public void truncateAll() {
        List<String> tables = jdbc.queryForList("""
                select format('%I.%I', schemaname, tablename)
                from pg_tables
                where schemaname not in ('pg_catalog', 'information_schema')
                  and tablename <> 'flyway_schema_history'
                """, String.class);

        if (tables.isEmpty()) {
            return;
        }

        jdbc.execute("truncate table " + String.join(", ", tables) + " restart identity cascade");
    }
}
