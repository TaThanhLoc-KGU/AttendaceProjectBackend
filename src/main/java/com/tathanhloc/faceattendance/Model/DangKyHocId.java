package com.tathanhloc.faceattendance.Model;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DangKyHocId implements Serializable {
    private String maSv;
    private String maLhp;
}
