package com.example.authservice.config;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeReflection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class RegisterReflectionFeature implements Feature {
    private static final Logger logger = LoggerFactory.getLogger(RegisterReflectionFeature.class);
    @Override
    public void beforeAnalysis(BeforeAnalysisAccess access) {
        String[] packagesToScan = new String[]{
                "com.example.authservice.infrastructure.service.nats",
                "com.example.authservice.dto",
                "com.example.authservice.domain.entity",
                "com.example.authservice.infrastructure.service.user.user"
        };

        String currentClasspath = System.getProperty("java.class.path");
        try (ScanResult scanResult = new ClassGraph()
                .acceptPackages(packagesToScan)
                .enableClassInfo()
                .ignoreClassVisibility()
                .overrideClasspath(currentClasspath)
                .scan()) {

            // scan these registered packages
            List<Class<?>> classes = scanResult.getAllClasses().loadClasses();


            for (Class<?> clazz : classes) {
                    // Register each class
                    RuntimeReflection.register(clazz);
                    RuntimeReflection.register(clazz.getDeclaredConstructors());
                    RuntimeReflection.register(clazz.getDeclaredMethods());
                    RuntimeReflection.register(clazz.getDeclaredFields());
                }
            } catch (Exception e) {
                logger.error(e.getMessage());
                throw new RuntimeException("[GraalVM CRITICAL BUILD FAILURE] Reflection scan aborted: " + e.getMessage(), e);
            }
    }
}