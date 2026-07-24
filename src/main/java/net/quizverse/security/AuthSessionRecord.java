package net.quizverse.security;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthSessionRecord {

    private String token;
    private String ownerId;
    private String kind;
    private long createdAtEpochMs;

    public AuthSessionRecord() {
    }

    public AuthSessionRecord(String token, String ownerId, String kind, long createdAtEpochMs) {
        this.token = token;
        this.ownerId = ownerId;
        this.kind = kind;
        this.createdAtEpochMs = createdAtEpochMs;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public long getCreatedAtEpochMs() {
        return createdAtEpochMs;
    }

    public void setCreatedAtEpochMs(long createdAtEpochMs) {
        this.createdAtEpochMs = createdAtEpochMs;
    }
}
