package com.example.authservice.infrastructure.service.google;

import com.example.authservice.domain.gateway.oauth.OAuthVerifier;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

import java.util.Arrays;


public class GoogleTokenVerifier implements OAuthVerifier {
    private final GoogleIdTokenVerifier verifier;

    public GoogleTokenVerifier(String[] clientIds) throws Exception {
        this.verifier = new GoogleIdTokenVerifier.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance()
        )
                .setAudience(Arrays.asList(clientIds))
                .build();
    }

    @Override
    public GoogleUser verify(String oAuthToken) throws Exception {
        GoogleIdToken idToken = verifier.verify(oAuthToken);
        if (idToken == null) {
            throw new SecurityException("Invalid Google ID Token");
        }

        GoogleIdToken.Payload payload = idToken.getPayload();

        if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
            throw new SecurityException("Google email is not verified");
        }


        return new GoogleUser(
                payload.getSubject(),              // google_id
                payload.getEmail(),
                (String) payload.get("name"),
                (String) payload.get("picture")
        );
    }
}
