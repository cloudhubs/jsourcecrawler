package org.baylor.ecs.cloudhubs.sourcecrawler;

import org.baylor.ecs.cloudhubs.sourcecrawler.request.RequestLogger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;

@SpringBootApplication
public class Application {
    private static final Logger log = LoggerFactory.getLogger(Application.class);
    public static void main(String[] args) throws IOException {
        RequestLogger logger = new RequestLogger(); // Test logger for requests
        SpringApplication.run(Application.class, args);
    }
}