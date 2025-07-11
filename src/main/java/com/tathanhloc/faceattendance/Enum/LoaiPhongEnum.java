package com.tathanhloc.faceattendance.Enum;

public enum LoaiPhongEnum {
    LECTURE("Phòng giảng dạy"),
    LAB("Phòng thí nghiệm"),
    COMPUTER("Phòng máy tính"),
    CONFERENCE("Phòng hội thảo"),
    LIBRARY("Thư viện"),
    OTHER("Khác");

    private final String description;

    LoaiPhongEnum(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}