package com.lexicon.backend;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BackendApplication {

    private static final Logger logger = LoggerFactory.getLogger(BackendApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);

        long maxMemoryMb = Runtime.getRuntime().maxMemory() / (1024 * 1024);
        logger.info("JVM max heap: {} MB", maxMemoryMb);
    }

}
