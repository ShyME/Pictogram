package me.imshy.pictogram.identity;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;
import me.imshy.pictogram.testsupport.ModuleIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Proves the harness wires up. There is no domain behaviour yet, so that is all it asserts. */
class IdentityModuleTest extends ModuleIntegrationTest {

    @Autowired
    DataSource dataSource;

    @Test
    void bootstrapsAgainstPostgres() throws Exception {
        try (var connection = dataSource.getConnection()) {
            assertThat(connection.getMetaData().getDatabaseProductName()).isEqualTo("PostgreSQL");
        }
    }
}
