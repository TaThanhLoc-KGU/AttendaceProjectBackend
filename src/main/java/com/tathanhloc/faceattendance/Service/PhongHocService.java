package com.tathanhloc.faceattendance.Service;


import com.tathanhloc.faceattendance.DTO.*;
import com.tathanhloc.faceattendance.Model.*;
import com.tathanhloc.faceattendance.Repository.*;
import com.tathanhloc.faceattendance.Util.AutoLogUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PhongHocService {

    private final PhongHocRepository phongHocRepository;

    public List<PhongHocDTO> getAll() {
        return phongHocRepository.findAll().stream().map(this::toDTO).toList();
    }

    public PhongHocDTO getById(String id) {
        return toDTO(phongHocRepository.findById(id).orElseThrow());
    }

    public PhongHocDTO create(PhongHocDTO dto) {
        PhongHoc entity = toEntity(dto);
        return toDTO(phongHocRepository.save(entity));
    }

    public PhongHocDTO update(String id, PhongHocDTO dto) {
        PhongHoc existing = phongHocRepository.findById(id).orElseThrow();
        existing.setTenPhong(dto.getTenPhong());
        existing.setMoTa(dto.getMoTa());
        return toDTO(phongHocRepository.save(existing));
    }

    public void delete(String id) {
        phongHocRepository.deleteById(id);
    }

    private PhongHocDTO toDTO(PhongHoc p) {
        return PhongHocDTO.builder()
                .maPhong(p.getMaPhong())
                .tenPhong(p.getTenPhong())
                .moTa(p.getMoTa())
                .build();
    }

    private PhongHoc toEntity(PhongHocDTO dto) {
        return PhongHoc.builder()
                .maPhong(dto.getMaPhong())
                .tenPhong(dto.getTenPhong())
                .moTa(dto.getMoTa())
                .build();
    }

    public PhongHocDTO getByMaPhong(String maPhong) {
        return toDTO(phongHocRepository.findById(maPhong)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng học")));
    }
}
