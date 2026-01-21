package com.example.demo.student;

import org.springframework.context.annotation.Configuration;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;

@Configuration
public class StudentConfig {

    @Bean
    CommandLineRunner commandLineRunner(StudentRepository repository) {
        return args -> {
            Student mariam = new Student(
                    "mariam",
                    "mariam.jamal@gmail.com",
                    LocalDate.of(2003, Month.JANUARY, 5),
                    21
            );

            Student alex = new Student(
                    "koloooo",
                    "alex.jamal@gmail.com",
                    LocalDate.of(2004, Month.JUNE, 10),
                    20
            );

            repository.saveAll(List.of(mariam, alex));
        };
    }
}
