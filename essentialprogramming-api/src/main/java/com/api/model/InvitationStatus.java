package com.api.model;

public enum InvitationStatus {
    ACCEPTED ("ACCEPTED"),
    PENDING ("PENDING"),
    REJECTED("REJECTED");

    private final String invitationStatus;

    InvitationStatus(String value) {
        invitationStatus = value;
    }

    public String toString() {
        return this.invitationStatus;
    }

}
