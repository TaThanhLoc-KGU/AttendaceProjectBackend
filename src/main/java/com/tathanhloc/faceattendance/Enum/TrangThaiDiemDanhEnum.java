package com.tathanhloc.faceattendance.Enum;

import lombok.Getter;

@Getter
public enum TrangThaiDiemDanhEnum {
    CO_MAT("Có mặt"),
    VANG_MAT("Vắng"),
    DI_TRE("Trễ"),
    VANG_CO_PHEP("Vắng có phép");

    private final String value;

    TrangThaiDiemDanhEnum(String value) {
        this.value = value;
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
