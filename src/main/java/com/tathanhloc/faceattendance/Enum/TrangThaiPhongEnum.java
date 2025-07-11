package com.tathanhloc.faceattendance.Enum;

public enum TrangThaiPhongEnum {
    AVAILABLE("Sẵn sàng"),
    OCCUPIED("Đang sử dụng"),
    MAINTENANCE("Bảo trì"),
    INACTIVE("Không hoạt động");

    private final String description;

    TrangThaiPhongEnum(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}