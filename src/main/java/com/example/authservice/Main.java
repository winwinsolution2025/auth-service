package com.example.authservice;

import com.example.authservice.config.AppConfig;
import com.example.authservice.domain.exception.HttpException;
import com.example.authservice.domain.exception.InvalidParameterException;
import com.example.authservice.domain.gateway.publisher.MessagePublisher;
import com.example.authservice.domain.repository.TokenRepository;
import com.example.authservice.domain.usecase.impl.*;
import com.example.authservice.dto.ErrorResponse;
import com.example.authservice.infrastructure.controller.AuthController;
import com.example.authservice.infrastructure.service.google.GoogleTokenVerifier;
import com.example.authservice.infrastructure.service.nats.NatsJetStreamClient;
import com.example.authservice.infrastructure.repository.MySQLAuthUserRepository;
import com.example.authservice.infrastructure.repository.MySQLTokenRepository;
import com.example.authservice.infrastructure.service.redis.RedisPublisher;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.javalin.Javalin;
import io.javalin.http.HttpResponseException;
import io.javalin.http.HttpStatus;
import io.javalin.json.JavalinJackson;
import io.javalin.plugin.bundled.CorsPluginConfig;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;


import static io.javalin.apibuilder.ApiBuilder.*;

public class Main {
    private static final Logger appLog = LoggerFactory.getLogger(Main.class);
    private static final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();

    public static void main(String[] args) throws Exception {
        AppConfig config = AppConfig.GetInstance();
//        migrate(config);

        NatsJetStreamClient natBroker = new NatsJetStreamClient(config.getNatsOrigin(), config.getNatsUsername(), config.getNatsPassword());

        RedisPublisher redisPublisher = new RedisPublisher(config.getRedisPubSubHost(), config.getRedisPubSubPort(), config.getRedisPubSubPass());

        DataSource ds = createHikariDataSource(config);
        DSLContext dbCtx = DSL.using(ds, SQLDialect.MYSQL);

        var clientIds = config.getGoogleOAuthClientIds().split(",");
        var googleTokenVerifier = new GoogleTokenVerifier(clientIds);

        Validator validator = factory.getValidator();

        String redisInChannel = config.getRedisPubSubInChannel();


        var authController = getAuthController(dbCtx, natBroker, redisPublisher, redisInChannel, googleTokenVerifier, config, validator);

        Javalin app = Javalin.create(conf -> {
            //enable Virtual Threads (Loom Project)
            conf.jetty.threadPool = new org.eclipse.jetty.util.thread.QueuedThreadPool();
            //enable cors
            conf.bundledPlugins.enableCors(cors -> {
                cors.addRule(CorsPluginConfig.CorsRule::anyHost);
            });



            conf.jsonMapper(new JavalinJackson(JsonMapper.builder()
                    .addModule(new JavaTimeModule())
                    .build(), true));

            conf.router.apiBuilder(() -> {
                // Auth route with api
                path("/api/v1", () -> {
                    post("/auth/login", authController.login);
                    post("/auth/logout", authController.logout);
                    post("/auth/register", authController.register);
                    put("/auth/password", authController.updatePassword);
                    get("/auth/verify", authController.verify);
                });

            });
        });

        app.exception(UnrecognizedPropertyException.class, (e, ctx) -> {
            throw new InvalidParameterException(e.getMessage());
        });

        app.exception(HttpException.class, (e, ctx) -> {
            ctx.status(e.getStatus());
            ctx.json(new ErrorResponse(getStatusMessage(e.getStatus()), e.getMessage()));
        });

        // Add custom error handler for HttpResponseException
        app.exception(HttpResponseException.class, (e, ctx) -> {
            appLog.error("call service exception!", e);
            ctx.status(e.getStatus());
            ctx.json(new ErrorResponse(getStatusMessage(e.getStatus()), e.getMessage()));
        });

        app.exception(RuntimeException.class, (e, ctx) -> {
            appLog.error("run time exception!", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            ctx.json(new ErrorResponse(getStatusMessage(500), e.getMessage()));
        });


        // Start the server
        app.start("0.0.0.0", config.getPort());
        System.out.println("Server started on port " + config.getPort());

        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            app.stop();
        }));
    }

    @NotNull
    private static AuthController getAuthController(DSLContext dbCtx, NatsJetStreamClient natBroker, MessagePublisher publisher, String redisInChannel, GoogleTokenVerifier googleTokenVerifier, AppConfig config, Validator validator) {
        var authUserRepository = new MySQLAuthUserRepository(dbCtx);
        var tokenRepository = new MySQLTokenRepository(dbCtx);
        removeExpireSession(tokenRepository);

        LogoutUseCaseImpl logoutUseCase = new LogoutUseCaseImpl(tokenRepository);
        LoginUseCaseImpl loginUseCase = new LoginUseCaseImpl(authUserRepository, tokenRepository, natBroker, config.getNatsAuthUserLoginSubject());
        RegisterUseCaseImpl registerUseCase = new RegisterUseCaseImpl(authUserRepository, natBroker, config.getNatsAuthUserRegisterSubject());
        LoginByGoogleUseCaseImpl loginByGoogleUseCase = new LoginByGoogleUseCaseImpl(authUserRepository, tokenRepository, natBroker, config.getNatsAuthUserLoginSubject(), googleTokenVerifier, registerUseCase, publisher, redisInChannel);
        VerifyTokenUseCaseImpl verifyTokenUseCase = new VerifyTokenUseCaseImpl(tokenRepository);
        UpdatePasswordUseCaseImpl updatePasswordUseCase = new UpdatePasswordUseCaseImpl(authUserRepository);

        return new AuthController(
                validator,
                loginUseCase,
                logoutUseCase,
                loginByGoogleUseCase,
                registerUseCase,
                updatePasswordUseCase,
                verifyTokenUseCase
        );
    }

    static void removeExpireSession(TokenRepository tokenRepository) {
        int deletedRows = tokenRepository.deleteExpiredTokens();
        appLog.info("Deleted " + deletedRows + " expired tokens");
    }

    public static String getStatusMessage(int statusCode) {
        return switch (statusCode) {
            case 400 -> "BAD_REQUEST";
            case 401 -> "UNAUTHORIZED";
            case 403 -> "FORBIDDEN";
            case 404 -> "NOT_FOUND";
            case 409 -> "CONFLICT_RESOURCE";
            case 500 -> "INTERNAL_SERVER_ERROR";
            default -> "UNKNOWN";
        };
    }

//    static void migrate(AppConfig config) {
//        try (Connection connection = DriverManager.getConnection(config.getDbUrl(), config.getDbUser(), config.getDbPass())) {
//            Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
//            try (Liquibase liquibase = new Liquibase("db/changelog/db.changelog-master.xml", new ClassLoaderResourceAccessor(), database)) {
//                liquibase.update(new Contexts(), new LabelExpression());
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//            throw new RuntimeException("Migration failed", e);
//        }
//    }

    private static DataSource createHikariDataSource(AppConfig cfg) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(cfg.getDbUrl()); // replace with your DB
        config.setUsername(cfg.getDbUser());
        config.setPassword(cfg.getDbPass());

        // Optional tuning
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000); // 30s
        config.setIdleTimeout(600000);      // 10 min
        config.setMaxLifetime(1800000);     // 30 min

        // Create the HikariCP DataSource
        HikariDataSource ds = new HikariDataSource(config);
        return ds;
    }

}
