package nl.leonw.competencymatrix;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class CompetencyMatrixApplicationTests {

    @Test
    void contextLoads() {
    }
}
