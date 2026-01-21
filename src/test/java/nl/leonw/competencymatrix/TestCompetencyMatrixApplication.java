package nl.leonw.competencymatrix;

import org.springframework.boot.SpringApplication;

public class TestCompetencyMatrixApplication {

    public static void main(String[] args) {
        SpringApplication.from(CompetencyMatrixApplication::main)
                .with(TestcontainersConfiguration.class)
                .run(args);
    }
}
