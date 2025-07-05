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
public class LopService {

    private final LopRepository lopRepository;
    private final NganhRepository nganhRepository;
    private final KhoaHocRepository khoaHocRepository;

    // Lấy tất cả lớp (bao gồm cả đã xóa)
    public List<LopDTO> getAll() {
        return lopRepository.findAll().stream().map(this::toDTO).toList();
    }

    // Chỉ lấy lớp đang hoạt động
    public List<LopDTO> getAllActive() {
        return lopRepository.findByIsActiveTrue().stream().map(this::toDTO).toList();
    }

    // Chỉ lấy lớp đã bị xóa mềm
    public List<LopDTO> getAllDeleted() {
        return lopRepository.findByIsActiveFalse().stream().map(this::toDTO).toList();
    }

    public LopDTO getById(String id) {
        return toDTO(lopRepository.findById(id).orElseThrow());
    }

    public LopDTO create(LopDTO dto) {
        Lop lop = toEntity(dto);
        return toDTO(lopRepository.save(lop));
    }

    public LopDTO update(String id, LopDTO dto) {
        Lop existing = lopRepository.findById(id).orElseThrow();
        existing.setTenLop(dto.getTenLop());
        existing.setNganh(nganhRepository.findById(dto.getMaNganh()).orElseThrow());
        existing.setKhoaHoc(khoaHocRepository.findById(dto.getMaKhoahoc()).orElseThrow());
        existing.setActive(dto.getIsActive());
        return toDTO(lopRepository.save(existing));
    }

    // Xóa mềm
    public void softDelete(String id) {
        Lop existing = lopRepository.findById(id).orElseThrow();
        existing.setActive(false);
        lopRepository.save(existing);
    }

    // Khôi phục lớp đã xóa mềm
    public LopDTO restore(String id) {
        Lop existing = lopRepository.findById(id).orElseThrow();
        existing.setActive(true);
        return toDTO(lopRepository.save(existing));
    }

    // Xóa vĩnh viễn
    public void hardDelete(String id) {
        lopRepository.deleteById(id);
    }

    // Xóa mềm (alias cho softDelete)
    public void delete(String id) {
        softDelete(id);
    }

    private LopDTO toDTO(Lop e) {
        return LopDTO.builder()
                .maLop(e.getMaLop())
                .tenLop(e.getTenLop())
                .maKhoahoc(e.getKhoaHoc().getMaKhoahoc())
                .maNganh(e.getNganh().getMaNganh())
                .isActive(e.isActive())
                .build();
    }

    private Lop toEntity(LopDTO dto) {
        return Lop.builder()
                .maLop(dto.getMaLop())
                .tenLop(dto.getTenLop())
                .nganh(nganhRepository.findById(dto.getMaNganh()).orElseThrow())
                .khoaHoc(khoaHocRepository.findById(dto.getMaKhoahoc()).orElseThrow())
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .build();
    }

    public LopDTO getByMaLop(String maLop) {
        Lop lop = lopRepository.findById(maLop)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lớp"));
        return toDTO(lop);
    }

    public long count() {
        return lopRepository.count();
    }

    public long countActive() {
        return lopRepository.countByIsActiveTrue();
    }

    public long countInactive() {
        return lopRepository.countByIsActiveFalse();
    }
}