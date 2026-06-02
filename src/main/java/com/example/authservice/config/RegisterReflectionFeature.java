package com.example.authservice.config;

import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeReflection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

/**
 * Pure GraalVM Feature designed to safely register classes for reflection
 * without relying on external class-scanning libraries.
 * Integrated with SLF4J Logger for standard enterprise logging standards.
 */
public class RegisterReflectionFeature implements Feature {

    // Declare the logger instance according to your requirement
    private static final Logger logger = LoggerFactory.getLogger(RegisterReflectionFeature.class);

    @Override
    public void beforeAnalysis(BeforeAnalysisAccess access) {
        // Target packages that contain DTOs/Requests for Jackson serialization
        List<String> packagesToScan = List.of(
                "com.example.authservice.infrastructure.service.nats",
                "com.example.authservice.dto",
                "com.example.authservice.domain.entity",
                "com.example.authservice.infrastructure.service.user.user"
        );

        // Standard SLF4J Logging
        logger.info("GraalVM AOT Reflection Feature initialization started.");

        // System.out is strictly kept here to force output onto the Docker build terminal console
        System.out.println("====================================================");
        System.out.println(">>> [GraalVM] PURE REFLECTION FEATURE IS STARTING <<<");
        System.out.println("====================================================");

        // Register a callback handler that inspects every class entering the AOT compilation state
        access.registerReachabilityHandler(hint -> {
            Class<?> clazz = hint.getClass();
            String className = clazz.getName();

            // Check if the current processed class belongs to our target packages
            for (String pkg : packagesToScan) {
                if (className.startsWith(pkg)) {
                    // Log using both SLF4J and standard out for guaranteed visibility during Docker build
                    logger.debug("Intercepted reachability hint for class: {}", className);
                    System.out.println(">>> [GraalVM Auto-Register] Found: " + className);

                    try {
                        // Open up all metadata fields, methods, and constructors for Jackson Object Mapper
                        RuntimeReflection.register(clazz);
                        RuntimeReflection.register(clazz.getDeclaredConstructors());
                        RuntimeReflection.register(clazz.getDeclaredMethods());
                        RuntimeReflection.register(clazz.getDeclaredFields());
                    } catch (Exception e) {
                        logger.error("Failed to register reflection metadata for class: {}", className, e);
                        System.err.println(">>> [GraalVM Warning] Failed to register: " + className);
                    }
                    break;
                }
            }
        }, Object.class); // Intercept all instances extending Object

        logger.info("GraalVM AOT Reflection Feature event handler registered successfully.");
    }
}