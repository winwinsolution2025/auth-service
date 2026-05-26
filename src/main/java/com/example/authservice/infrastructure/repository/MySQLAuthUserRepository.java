package com.example.authservice.infrastructure.repository;

import com.example.authservice.domain.entity.AuthUser;
import com.example.authservice.domain.repository.AuthUserRepository;
import org.jooq.DSLContext;

import java.util.Optional;

import static com.example.jooq.authdb.tables.AuthUsers.AUTH_USERS;


public class MySQLAuthUserRepository implements AuthUserRepository {
    private final DSLContext dbCtx;

    public MySQLAuthUserRepository(DSLContext dbCtx) {
        this.dbCtx = dbCtx;
    }


    @Override
    public Optional<AuthUser> getAuthUserById(Integer id) {
        return dbCtx.selectFrom(AuthUser.TABLE_NAME)
                .where("id = ?", id)
                .fetchOptionalInto(AuthUser.class);

    }

    @Override
    public Optional<AuthUser> getAuthUserByEmail(String email) {
        return dbCtx.selectFrom(AuthUser.TABLE_NAME)
                .where("email = ?", email)
                .fetchOptionalInto(AuthUser.class);
    }

    @Override
    public void addAuthUser(AuthUser authUser) {
        var record = dbCtx.insertInto(AUTH_USERS)
                .set(AUTH_USERS.EMAIL, authUser.getEmail())
                .set(AUTH_USERS.ROLE, authUser.getRole().toString())
                .set(AUTH_USERS.USER_ID, authUser.getUserId())
                .set(AUTH_USERS.PASSWORD, authUser.getPassword())
                .set(AUTH_USERS.UUID, authUser.getUUID())
                .returning(AUTH_USERS.ID)
                .fetchOne();
        if (record == null) {
            throw new RuntimeException("Failed to insert record");
        }
        authUser.setId(record.get(AUTH_USERS.ID));
    }

    @Override
    public boolean deleteAuthUser(Integer id) {
        var rows = dbCtx.deleteFrom(AUTH_USERS)
                .where("id = ?", id)
                .execute();
        return rows > 0;
    }

    @Override
    public boolean updateAuthUser(AuthUser updatedAuthUser) {
        var rows = dbCtx.update(AUTH_USERS)
                .set(AUTH_USERS.ROLE, updatedAuthUser.getRole().toString())
                .set(AUTH_USERS.PASSWORD, updatedAuthUser.getPassword())
                .set(AUTH_USERS.UUID, updatedAuthUser.getUUID())
                .set(AUTH_USERS.USER_ID, updatedAuthUser.getId())
                .where("id = ?", updatedAuthUser.getId())
                .execute();
        return rows > 0;
    }
}
