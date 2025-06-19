package com.tathanhloc.faceattendance.Enum;

public enum VaiTroEnum {
    ADMIN("admin"),
    GIANGVIEN("giangvien"),
    SINHVIEN("sinhvien");

    private final String value;

    VaiTroEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static VaiTroEnum fromValue(String value) {
        for (VaiTroEnum vaiTro : VaiTroEnum.values()) {
            if (vaiTro.getValue().equals(value)) {
                return vaiTro;
            }
        }
        throw new IllegalArgumentException("Giá trị không hợp lệ: " + value);
    }
}
