# =========================================================================
# STAGE 1: Build the Native Binary using GraalVM JDK 21+
# =========================================================================
FROM ghcr.io/graalvm/native-image-community:25 AS builder
WORKDIR /build

# Copy only the files needed for dependency resolution first for better caching
# copy maven wrapper
COPY .mvn/ .mvn
COPY mvnw pom.xml ./

RUN ./mvnw dependency:go-offline -B

# Copy source and build
COPY src ./src
# Build the Native Image
RUN ./mvnw clean package -Pnative -DskipTests

# =========================================================================
# STAGE 2: extract zlib from bookworm-slim
# =========================================================================
FROM debian:bookworm-slim AS zlib-extractor
RUN apt-get update && apt-get install -y --no-install-recommends zlib1g
RUN mkdir /toptop && cp /lib/*/libz.so.1 /toptop/
# =========================================================================
# STAGE 3: Run the Native Binary in a lightweight environment
# =========================================================================
# Using Google's Distroless static image for maximum security and minimal size (~30MB)
# FROM gcr.io/distroless/static-debian12:latest
FROM gcr.io/distroless/base-debian12:latest
# FROM gcr.io/distroless/java21-debian12:latest
# FROM debian:bookworm-slim
WORKDIR /app

# Copy the compiled native binary from the builder stage
# (Adjust "app-java25-native" to match the <imageName> in your pom.xml)
COPY --from=builder /build/target/app-java25-native /app/server
COPY --from=zlib-extractor /toptop/libz.so.1 /lib/

# Expose a common web port (adjust if your app listens on a different port)
EXPOSE 8080

# Run the native binary directly
ENTRYPOINT ["/app/server"]