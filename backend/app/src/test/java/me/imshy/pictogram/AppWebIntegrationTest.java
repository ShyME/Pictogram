package me.imshy.pictogram;

import java.lang.annotation.*;
import me.imshy.pictogram.testsupport.DatabaseTruncationExtension;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Full boot on a real port, no Google client configured — the probes and
 * deep-link journeys that only need HTTP. Shares one context with every other
 * class carrying this annotation.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@SpringBootTest(classes = PictogramApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Tag("fast")
@Import(SharedWebTestConfig.class)
@ExtendWith({DatabaseTruncationExtension.class, AppContextAmountGuard.class})
public @interface AppWebIntegrationTest {
}
