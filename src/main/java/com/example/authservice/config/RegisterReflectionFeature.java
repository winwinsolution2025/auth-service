package com.example.authservice.config;

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
        for (String packageToScan:  packagesToScan) {
            try {
                // scan these registered packages
                List<Class<?>> classes = getClasses(packageToScan);

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

    // scan class from package (Standard Java)
    private List<Class<?>> getClasses(String packageName) throws Exception {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        String path = packageName.replace('.', '/');
        URL resource = classLoader.getResource(path);
        if (resource == null) {
            throw new IllegalArgumentException("Package path not found: " + packageName);
        }

        File directory = new File(resource.getFile());
        List<Class<?>> classes = new ArrayList<>();
        if (directory.exists()) {
            File[] files = directory.listFiles();

            for (File file : Objects.requireNonNull(files, "Directory listing returned null for package: " + packageName)) {
                if (file.getName().endsWith(".class")) {
                    String className = packageName + '.' + file.getName().substring(0, file.getName().length() - 6);
                    classes.add(Class.forName(className));
                }
            }
        }
        return classes;
    }
}