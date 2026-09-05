package me.imshy.pictogram.testsupport;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@ActiveProfiles("test")
@ExtendWith({DatabaseTruncationExtension.class, ModuleSliceContextAmountGuard.class})
public abstract class ModuleIntegrationTest {

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        SharedPostgres.registerTo(registry);
    }
}
