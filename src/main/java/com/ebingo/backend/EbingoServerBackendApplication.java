package com.ebingo.backend;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class EbingoServerBackendApplication {

    public static void main(String[] args) {
        // Load .env
        Dotenv dotenv = Dotenv.configure()
                .ignoreIfMissing() // optional: prevents crash if .env is missing
                .load();

        // Set each variable as a system property
        dotenv.entries().forEach(entry ->
                System.setProperty(entry.getKey(), entry.getValue())
        );
        SpringApplication.run(EbingoServerBackendApplication.class, args);
    }

}
