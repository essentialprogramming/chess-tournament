package com.util.enums;

public enum PlatformType {

    PLAYER(1),
    SUPER_ADMIN(2),
    ADMIN(3),
    REFEREE(4),
    VIEWER(5);

    private final Integer value;

    PlatformType(Integer value) {
        this.value = value;
    }

    public int value(){
        return value;
    }

    public static PlatformType fromId(int id) {
        switch (id) {
            case 1:
                return PLAYER;
            case 2:
                return SUPER_ADMIN;
            case 3:
                return ADMIN;
            case 4:
                return REFEREE;
            case 5:
                return VIEWER;
            default:
                throw new IllegalArgumentException("No Platform type with id : " + id + " found.");
        }
    }
}
