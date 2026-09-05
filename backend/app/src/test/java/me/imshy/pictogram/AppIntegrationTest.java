package me.imshy.pictogram;

import java.lang.annotation.*;
import me.imshy.pictogram.testsupport.DatabaseTruncationExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@SpringBootTest(classes = PictogramApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(SharedWebTestConfig.class)
@ExtendWith({DatabaseTruncationExtension.class, AppContextAmountGuard.class})
public @interface AppIntegrationTest {
}
