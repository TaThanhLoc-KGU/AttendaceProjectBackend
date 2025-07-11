package com.tathanhloc.faceattendance.Enum;

public enum ThietBiEnum {
    PROJECTOR("Máy chiếu"),
    COMPUTER("Máy tính"),
    AC("Điều hòa"),
    MICROPHONE("Micro"),
    SPEAKER("Loa"),
    WIFI("Wifi"),
    WHITEBOARD("Bảng trắng"),
    SMARTBOARD("Bảng thông minh");

    private final String description;

    ThietBiEnum(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}