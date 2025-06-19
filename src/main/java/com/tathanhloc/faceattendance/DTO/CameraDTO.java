package com.tathanhloc.faceattendance.DTO;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CameraDTO  {
    private Long id;

    private String tenCamera;

    private String ipAddress;

    private String maPhong;
    private String password;

    private String vungIn;

    private String vungOut;

    private Boolean active;

}
