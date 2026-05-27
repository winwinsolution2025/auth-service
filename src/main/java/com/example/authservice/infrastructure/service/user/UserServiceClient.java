package com.example.authservice.infrastructure.service.user;

import com.example.authservice.config.AppConfig;
import com.example.authservice.infrastructure.service.JwtService;
import com.example.authservice.infrastructure.service.user.user.CreateUserRequest;
import com.example.authservice.infrastructure.service.user.user.CreateUserResponse;
import com.example.authservice.infrastructure.service.user.user.UserResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.http.HttpResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;


public class UserServiceClient {
    private static final Logger appLog = LoggerFactory.getLogger(UserServiceClient.class);
    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public UserResponse addUser(CreateUserRequest user) throws Exception {
        AppConfig config = AppConfig.GetInstance();
        String url = config.getUserServiceOrigin() + "/api/v1/users";
        objectMapper.registerModule(new JavaTimeModule());
        String requestBody = objectMapper.writeValueAsString(user);

        String jwtToken = JwtService.generateTemporalToken();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + jwtToken)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                CreateUserResponse userResponse = objectMapper.readValue(response.body(), CreateUserResponse.class);
                return userResponse.data;
            } else {
                appLog.error("UserService call failed: {} {} => {} {}",
                        request.method(),
                        url,
                        response.statusCode(),
                        response.body()
                );
                throw new HttpResponseException(response.statusCode(), response.body());
            }
        } catch (HttpConnectTimeoutException e) {
            appLog.error("Timeout connecting to UserService: {} {}", request.method(), url, e);
            throw new RuntimeException("Timeout calling user service", e);
        } catch (IOException | InterruptedException e) {
            appLog.error("Error calling UserService: {} {}", request.method(), url, e);
            throw e;
        } catch (Exception e) {
            appLog.error("Unexpected exception calling UserService: {} {}", request.method(), url, e);
            throw e;
        }
    }
}
