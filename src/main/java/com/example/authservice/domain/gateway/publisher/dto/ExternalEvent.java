package com.example.authservice.domain.gateway.publisher.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ExternalEvent {
    private String type; // ping, message, broadcast
    private String source; // auth-service
    @JsonProperty("sender_id")
    private int senderId;
    @JsonProperty("target_room_id")
    private int targetRoomId;
    @JsonProperty("target_ids")
    private int[] targetIds;
    private Object data;

    public ExternalEvent(String type, String source, int senderId, int targetRoomId, int[] targetIds, Object data) {
        this.type = type;
        this.source = source;
        this.senderId = senderId;
        this.targetRoomId = targetRoomId;
        this.targetIds = targetIds;
        this.data = data;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public int getSenderId() {
        return senderId;
    }

    public void setSenderId(int senderId) {
        this.senderId = senderId;
    }

    public int getTargetRoomId() {
        return targetRoomId;
    }

    public void setTargetRoomId(int targetRoomId) {
        this.targetRoomId = targetRoomId;
    }

    public int[] getTargetIds() {
        return targetIds;
    }

    public void setTargetIds(int[] targetIds) {
        this.targetIds = targetIds;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}
