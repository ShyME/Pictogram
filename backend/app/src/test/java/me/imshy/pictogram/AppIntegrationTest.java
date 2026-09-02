package me.imshy.pictogram;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import me.imshy.pictogram.testsupport.DatabaseTruncationExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Full boot of the whole app — all seven modules, real security, Flyway'd Postgres — driven through
 * {@code MockMvc}. Every class carrying this shares one {@code ApplicationContext} because the
 * property source is {@link SharedWebTestConfig}, not a per-class {@code @DynamicPropertySource}.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@SpringBootTest(classes = PictogramApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(SharedWebTestConfig.class)
@ExtendWith({DatabaseTruncationExtension.class, AppContextGuard.class})
public @interface AppIntegrationTest {}
