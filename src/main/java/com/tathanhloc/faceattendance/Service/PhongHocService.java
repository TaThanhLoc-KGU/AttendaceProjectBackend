package com.tathanhloc.faceattendance.Service;

import com.tathanhloc.faceattendance.DTO.PhongHocDTO;
import com.tathanhloc.faceattendance.Model.PhongHoc;
import com.tathanhloc.faceattendance.Repository.PhongHocRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PhongHocService {

    private final PhongHocRepository phongHocRepository;

    // Get all with pagination
    public Page<PhongHocDTO> getAllWithPagination(int page, int size, String sortBy, String sortDir) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        return phongHocRepository.findByIsActiveTrue(pageable).map(this::toDTO);
    }

    // Get all active rooms
    public List<PhongHocDTO> getAll() {
        return phongHocRepository.findByIsActiveTrue().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // Get by ID
    public PhongHocDTO getById(String id) {
        PhongHoc phongHoc = phongHocRepository.findByMaPhongAndIsActiveTrue(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng học: " + id));
        return toDTO(phongHoc);
    }

    // Search rooms
    public Page<PhongHocDTO> searchRooms(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("maPhong"));
        return phongHocRepository.searchPhongHoc(keyword, pageable).map(this::toDTO);
    }

    // Filter by type
    public Page<PhongHocDTO> filterByType(String loaiPhong, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("maPhong"));
        return phongHocRepository.findByIsActiveTrueAndLoaiPhong(loaiPhong, pageable).map(this::toDTO);
    }

    // Filter by status
    public Page<PhongHocDTO> filterByStatus(String trangThai, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("maPhong"));
        return phongHocRepository.findByIsActiveTrueAndTrangThai(trangThai, pageable).map(this::toDTO);
    }

    // Get statistics
    public Map<String, Long> getRoomStatistics() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("total", phongHocRepository.countActiveRooms());
        stats.put("available", phongHocRepository.countByTrangThai("AVAILABLE"));
        stats.put("occupied", phongHocRepository.countByTrangThai("OCCUPIED"));
        stats.put("maintenance", phongHocRepository.countByTrangThai("MAINTENANCE"));
        return stats;
    }

    // Create new room
    @Transactional
    public PhongHocDTO create(PhongHocDTO dto) {
        // Validate unique room code
        if (phongHocRepository.existsByMaPhongAndIsActiveTrue(dto.getMaPhong())) {
            throw new IllegalArgumentException("Mã phòng đã tồn tại: " + dto.getMaPhong());
        }

        PhongHoc entity = toEntity(dto);
        PhongHoc saved = phongHocRepository.save(entity);
        log.info("Created new room: {}", saved.getMaPhong());
        return toDTO(saved);
    }

    // Update room
    @Transactional
    public PhongHocDTO update(String id, PhongHocDTO dto) {
        PhongHoc existing = phongHocRepository.findByMaPhongAndIsActiveTrue(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng học: " + id));

        // Update fields
        existing.setTenPhong(dto.getTenPhong());
        existing.setLoaiPhong(dto.getLoaiPhong());
        existing.setSucChua(dto.getSucChua());
        existing.setToaNha(dto.getToaNha());
        existing.setTang(dto.getTang());
        existing.setTrangThai(dto.getTrangThai());
        existing.setViTri(dto.getViTri());
        existing.setThietBi(dto.getThietBi());
        existing.setMoTa(dto.getMoTa());

        PhongHoc updated = phongHocRepository.save(existing);
        log.info("Updated room: {}", updated.getMaPhong());
        return toDTO(updated);
    }

    // Soft delete
    @Transactional
    public void delete(String id) {
        if (!phongHocRepository.existsByMaPhongAndIsActiveTrue(id)) {
            throw new RuntimeException("Không tìm thấy phòng học: " + id);
        }

        phongHocRepository.softDelete(id);
        log.info("Soft deleted room: {}", id);
    }

    // DTO conversion methods
    private PhongHocDTO toDTO(PhongHoc entity) {
        PhongHocDTO dto = PhongHocDTO.builder()
                .maPhong(entity.getMaPhong())
                .tenPhong(entity.getTenPhong())
                .loaiPhong(entity.getLoaiPhong())
                .sucChua(entity.getSucChua())
                .toaNha(entity.getToaNha())
                .tang(entity.getTang())
                .trangThai(entity.getTrangThai())
                .viTri(entity.getViTri())
                .thietBi(entity.getThietBi())
                .moTa(entity.getMoTa())
                .isActive(entity.getIsActive())
                .build();

        // Add display fields
        dto.setLoaiPhongDisplay(getLoaiPhongDisplay(entity.getLoaiPhong()));
        dto.setTrangThaiDisplay(getTrangThaiDisplay(entity.getTrangThai()));
        dto.setThietBiDisplay(getThietBiDisplay(entity.getThietBi()));

        return dto;
    }

    private PhongHoc toEntity(PhongHocDTO dto) {
        return PhongHoc.builder()
                .maPhong(dto.getMaPhong())
                .tenPhong(dto.getTenPhong())
                .loaiPhong(dto.getLoaiPhong())
                .sucChua(dto.getSucChua())
                .toaNha(dto.getToaNha())
                .tang(dto.getTang())
                .trangThai(dto.getTrangThai() != null ? dto.getTrangThai() : "AVAILABLE")
                .viTri(dto.getViTri())
                .thietBi(dto.getThietBi())
                .moTa(dto.getMoTa())
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .build();
    }

    // Helper methods for display
    private String getLoaiPhongDisplay(String loaiPhong) {
        if (loaiPhong == null) return "";
        switch (loaiPhong) {
            case "LECTURE": return "Phòng giảng dạy";
            case "LAB": return "Phòng thí nghiệm";
            case "COMPUTER": return "Phòng máy tính";
            case "CONFERENCE": return "Phòng hội thảo";
            case "LIBRARY": return "Thư viện";
            case "OTHER": return "Khác";
            default: return loaiPhong;
        }
    }

    private String getTrangThaiDisplay(String trangThai) {
        if (trangThai == null) return "";
        switch (trangThai) {
            case "AVAILABLE": return "Sẵn sàng";
            case "OCCUPIED": return "Đang sử dụng";
            case "MAINTENANCE": return "Bảo trì";
            case "INACTIVE": return "Không hoạt động";
            default: return trangThai;
        }
    }

    private String getThietBiDisplay(String thietBi) {
        if (thietBi == null || thietBi.trim().isEmpty()) return "";

        // Convert comma-separated to display names
        String[] items = thietBi.split(",");
        StringBuilder display = new StringBuilder();

        for (String item : items) {
            if (display.length() > 0) display.append(", ");

            switch (item.trim()) {
                case "PROJECTOR": display.append("Máy chiếu"); break;
                case "COMPUTER": display.append("Máy tính"); break;
                case "AC": display.append("Điều hòa"); break;
                case "MICROPHONE": display.append("Micro"); break;
                case "SPEAKER": display.append("Loa"); break;
                case "WIFI": display.append("Wifi"); break;
                case "WHITEBOARD": display.append("Bảng trắng"); break;
                case "SMARTBOARD": display.append("Bảng thông minh"); break;
                default: display.append(item.trim());
            }
        }

        return display.toString();
    }
}