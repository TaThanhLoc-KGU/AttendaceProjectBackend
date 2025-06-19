package com.tathanhloc.faceattendance.Enum;

public enum TrangThaiDiemDanhEnum {
    CO_MAT("Có mặt"),
    VANG("Vắng"),
    TRE("Trễ");

    private final String value;

    TrangThaiDiemDanhEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static TrangThaiDiemDanhEnum fromValue(String value) {
        for (TrangThaiDiemDanhEnum trangThai : TrangThaiDiemDanhEnum.values()) {
            if (trangThai.getValue().equals(value)) {
                return trangThai;
            }
        }
        throw new IllegalArgumentException("Giá trị không hợp lệ: " + value);
    }
}
