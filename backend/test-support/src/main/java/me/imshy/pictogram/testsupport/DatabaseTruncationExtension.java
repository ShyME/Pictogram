package me.imshy.pictogram.testsupport;

import javax.sql.DataSource;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Truncates every table (bar Flyway's history) after each test. One extension
 * referenced from the shared meta-annotations and base classes, replacing the
 * {@code @AfterEach} that was copied into {@code ModuleIntegrationTest},
 * {@code AppTest}, and every app API test.
 */
public final class DatabaseTruncationExtension implements AfterEachCallback {

    @Override
    public void afterEach(ExtensionContext context) {
        DataSource dataSource = SpringExtension.getApplicationContext(context).getBean(DataSource.class);
        new DatabaseCleaner(dataSource).truncateAll();
    }
}
