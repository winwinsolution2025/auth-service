package com.example.authservice.infrastructure.repository;

import com.example.authservice.domain.entity.Token;
import com.example.authservice.domain.repository.TokenRepository;
import com.example.jooq.authdb.tables.Tokens;
import org.jooq.DSLContext;

import java.time.Instant;
import java.util.Optional;

public class MySQLTokenRepository implements TokenRepository {
    private final DSLContext dbCtx;

    public MySQLTokenRepository(DSLContext dbCtx) {
        this.dbCtx = dbCtx;
    }


    @Override
    public Optional<Token> getTokenById(Integer id) {
        return dbCtx.selectFrom(Token.TABLE_NAME)
                .where("id = ?", id)
                .fetchOptionalInto(Token.class);
    }

    @Override
    public Optional<Token> getTokenByToken(String token) {
        return dbCtx.selectFrom(Token.TABLE_NAME)
                .where("token = ?", token)
                .fetchOptionalInto(Token.class);
    }

    @Override
    public Optional<Token> getTokenByEmail(String email) {
        return dbCtx.selectFrom(Token.TABLE_NAME)
                .where("email = ?", email)
                .fetchOptionalInto(Token.class);
    }

    @Override
    public void addToken(Token token) {
        dbCtx.insertInto(Tokens.TOKENS)
                .set(Tokens.TOKENS.TOKEN, token.getToken())
                .set(Tokens.TOKENS.EMAIL, token.getEmail())
                .set(Tokens.TOKENS.EXPIRED_AT, token.getExpired_at())
                .execute();
    }

    @Override
    public boolean deleteToken(Integer id) {
        var rows = dbCtx.deleteFrom(Tokens.TOKENS)
                .where("id = ?", id)
                .execute();

        return rows > 0;
    }

    @Override
    public int deleteExpiredTokens() {
        return dbCtx.deleteFrom(Tokens.TOKENS)
                .where("expired_at > ?", Instant.now())
                .execute();
    }
}
