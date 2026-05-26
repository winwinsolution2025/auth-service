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
RUN ./mvnw clean package -DskipTests

# =========================================================================
# STAGE 2: Run the Native Binary in a lightweight environment
# =========================================================================
# Using Google's Distroless static image for maximum security and minimal size (~30MB)
FROM gcr.io/distroless/static-debian12:latest
WORKDIR /app
# Copy the compiled native binary from the builder stage
# (Adjust "app-java25-native" to match the <imageName> in your pom.xml)
COPY --from=builder /build/target/app-java25-native /app/server

# Expose a common web port (adjust if your app listens on a different port)
EXPOSE 8080

# Run the native binary directly
ENTRYPOINT ["/app/server"]