package me.imshy.pictogram;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import me.imshy.pictogram.testsupport.DatabaseTruncationExtension;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

/**
 * Full boot on a real port with the shared {@link SharedGoogle} client wired — the Google sign-in
 * journeys and every {@code scenario/*} test. Shares one context across all such classes.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@SpringBootTest(classes = PictogramApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Tag("fast")
@Import(SharedWebTestConfig.class)
@ContextConfiguration(initializers = SharedGoogleInitializer.class)
@ExtendWith({DatabaseTruncationExtension.class, AppContextGuard.class})
public @interface AppOAuthWebIntegrationTest {}
