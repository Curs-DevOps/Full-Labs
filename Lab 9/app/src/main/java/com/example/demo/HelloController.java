package com.example.demo;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Random;

@RestController
public class HelloController {

    private static final Logger logger = LoggerFactory.getLogger(HelloController.class);
    private final Random random = new Random();
    private final Tracer tracer = GlobalOpenTelemetry.getTracer("com.example.demo.HelloController");

    @GetMapping("/hello")
    public String hello() {
        logger.info("Hello request received");

        // Simulate some work
        try {
            Thread.sleep(random.nextInt(200));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        if (random.nextInt(10) > 8) {
            logger.error("Something went wrong!");
            throw new RuntimeException("Chaos monkey struck!");
        }

        return "Hello, World!";
    }

    @GetMapping("/ping")
    public String ping() {
        return "pong";
    }

    @GetMapping("/custom")
    public String custom() {
        Span span = tracer.spanBuilder("custom-operation").startSpan();
        try (Scope scope = span.makeCurrent()) {
            span.setAttribute("custom.param", "example-value");
            span.addEvent("start_processing");

            logger.info("Starting custom processing");
            // Simulate work
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Create a child span
            Span childSpan = tracer.spanBuilder("calculate-something").startSpan();
            try (Scope childScope = childSpan.makeCurrent()) {
                childSpan.setAttribute("calculation.result", 42);
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                childSpan.end();
            }

            span.addEvent("end_processing");
            return "Custom trace with child span generated!";
        } finally {
            span.end();
        }
    }
}
