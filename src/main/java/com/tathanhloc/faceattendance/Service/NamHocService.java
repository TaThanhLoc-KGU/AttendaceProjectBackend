package com.tathanhloc.faceattendance.Service;

import com.tathanhloc.faceattendance.DTO.NamHocDTO;
import com.tathanhloc.faceattendance.Model.NamHoc;
import com.tathanhloc.faceattendance.Repository.NamHocRepository;
import com.tathanhloc.faceattendance.Exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NamHocService {

    private final NamHocRepository namHocRepository;

    public List<NamHocDTO> getAll() {
        try {
            return namHocRepository.findAll().stream()
                    .filter(nh -> nh.getIsActive() != null && nh.getIsActive())
                    .map(this::toDTO)
                    .toList();
        } catch (Exception e) {
            log.error("Error in getAll(): ", e);
            throw new RuntimeException("Không thể lấy danh sách năm học: " + e.getMessage());
        }
    }

    public List<NamHocDTO> getAllIncludeInactive() {
        try {
            return namHocRepository.findAll().stream()
                    .map(this::toDTO)
                    .toList();
        } catch (Exception e) {
            log.error("Error in getAllIncludeInactive(): ", e);
            throw new RuntimeException("Không thể lấy danh sách năm học: " + e.getMessage());
        }
    }

    public NamHocDTO getById(String id) {
        return namHocRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException("NamHoc", "maNamHoc", id));
    }

    @Transactional
    public NamHocDTO create(NamHocDTO dto) {
        try {
            NamHoc namHoc = toEntity(dto);

            // If this is set as current, unset others
            if (Boolean.TRUE.equals(dto.getIsCurrent())) {
                unsetAllCurrent();
            }

            NamHoc saved = namHocRepository.save(namHoc);
            return toDTO(saved);
        } catch (Exception e) {
            log.error("Error creating NamHoc: ", e);
            throw new RuntimeException("Không thể tạo năm học: " + e.getMessage());
        }
    }

    public NamHocDTO createWithDefaultSemesters(NamHocDTO dto) {
        // For now, just create the academic year
        // TODO: Add logic to create default semesters
        return create(dto);
    }

    @Transactional
    public NamHocDTO update(String id, NamHocDTO dto) {
        try {
            NamHoc existing = namHocRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("NamHoc", "maNamHoc", id));

            updateEntity(existing, dto);

            NamHoc updated = namHocRepository.save(existing);
            return toDTO(updated);
        } catch (Exception e) {
            log.error("Error updating NamHoc: ", e);
            throw new RuntimeException("Không thể cập nhật năm học: " + e.getMessage());
        }
    }

    @Transactional
    public void softDelete(String id) {
        try {
            NamHoc namHoc = namHocRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("NamHoc", "maNamHoc", id));

            namHoc.setIsActive(false);
            namHoc.setIsCurrent(false);
            namHocRepository.save(namHoc);
        } catch (Exception e) {
            log.error("Error soft deleting NamHoc: ", e);
            throw new RuntimeException("Không thể xóa năm học: " + e.getMessage());
        }
    }

    @Transactional
    public NamHocDTO restore(String id) {
        try {
            NamHoc namHoc = namHocRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("NamHoc", "maNamHoc", id));

            namHoc.setIsActive(true);
            NamHoc restored = namHocRepository.save(namHoc);
            return toDTO(restored);
        } catch (Exception e) {
            log.error("Error restoring NamHoc: ", e);
            throw new RuntimeException("Không thể khôi phục năm học: " + e.getMessage());
        }
    }

    public void delete(String id) {
        softDelete(id);
    }

    // ============ SPECIAL QUERIES ============

    public Optional<NamHocDTO> getCurrentAcademicYear() {
        try {
            return namHocRepository.findAll().stream()
                    .filter(nh -> Boolean.TRUE.equals(nh.getIsCurrent()))
                    .map(this::toDTO)
                    .findFirst();
        } catch (Exception e) {
            log.error("Error getting current academic year: ", e);
            return Optional.empty();
        }
    }

    public List<NamHocDTO> getOngoingAcademicYears() {
        try {
            LocalDate now = LocalDate.now();
            return namHocRepository.findAll().stream()
                    .filter(nh -> nh.getIsActive() != null && nh.getIsActive())
                    .filter(nh -> isOngoing(nh, now))
                    .map(this::toDTO)
                    .toList();
        } catch (Exception e) {
            log.error("Error getting ongoing academic years: ", e);
            return List.of();
        }
    }

    public List<NamHocDTO> getUpcomingAcademicYears() {
        try {
            LocalDate now = LocalDate.now();
            return namHocRepository.findAll().stream()
                    .filter(nh -> nh.getIsActive() != null && nh.getIsActive())
                    .filter(nh -> isUpcoming(nh, now))
                    .map(this::toDTO)
                    .toList();
        } catch (Exception e) {
            log.error("Error getting upcoming academic years: ", e);
            return List.of();
        }
    }

    public List<NamHocDTO> getFinishedAcademicYears() {
        try {
            LocalDate now = LocalDate.now();
            return namHocRepository.findAll().stream()
                    .filter(nh -> nh.getIsActive() != null && nh.getIsActive())
                    .filter(nh -> isFinished(nh, now))
                    .map(this::toDTO)
                    .toList();
        } catch (Exception e) {
            log.error("Error getting finished academic years: ", e);
            return List.of();
        }
    }

    @Transactional
    public NamHocDTO setAsCurrent(String id) {
        try {
            // Unset all current flags
            unsetAllCurrent();

            // Set new current
            NamHoc namHoc = namHocRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("NamHoc", "maNamHoc", id));

            namHoc.setIsCurrent(true);
            NamHoc updated = namHocRepository.save(namHoc);

            return toDTO(updated);
        } catch (Exception e) {
            log.error("Error setting current academic year: ", e);
            throw new RuntimeException("Không thể đặt năm học hiện tại: " + e.getMessage());
        }
    }

    // ============ HELPER METHODS ============

    @Transactional
    private void unsetAllCurrent() {
        List<NamHoc> currentYears = namHocRepository.findAll().stream()
                .filter(nh -> Boolean.TRUE.equals(nh.getIsCurrent()))
                .toList();

        for (NamHoc nh : currentYears) {
            nh.setIsCurrent(false);
            namHocRepository.save(nh);
        }
    }

    private boolean isOngoing(NamHoc namHoc, LocalDate now) {
        if (namHoc.getNgayBatDau() == null || namHoc.getNgayKetThuc() == null) return false;
        return !now.isBefore(namHoc.getNgayBatDau()) && !now.isAfter(namHoc.getNgayKetThuc());
    }

    private boolean isUpcoming(NamHoc namHoc, LocalDate now) {
        if (namHoc.getNgayBatDau() == null) return false;
        return now.isBefore(namHoc.getNgayBatDau());
    }

    private boolean isFinished(NamHoc namHoc, LocalDate now) {
        if (namHoc.getNgayKetThuc() == null) return false;
        return now.isAfter(namHoc.getNgayKetThuc());
    }

    private void updateEntity(NamHoc existing, NamHocDTO dto) {
        if (dto.getTenNamHoc() != null) existing.setTenNamHoc(dto.getTenNamHoc());
        if (dto.getNgayBatDau() != null) existing.setNgayBatDau(dto.getNgayBatDau());
        if (dto.getNgayKetThuc() != null) existing.setNgayKetThuc(dto.getNgayKetThuc());
        if (dto.getMoTa() != null) existing.setMoTa(dto.getMoTa());
        if (dto.getIsActive() != null) existing.setIsActive(dto.getIsActive());

        // Handle current flag
        if (Boolean.TRUE.equals(dto.getIsCurrent()) && !Boolean.TRUE.equals(existing.getIsCurrent())) {
            unsetAllCurrent();
            existing.setIsCurrent(true);
        } else if (Boolean.FALSE.equals(dto.getIsCurrent())) {
            existing.setIsCurrent(false);
        }
    }

    // ============ DTO CONVERSION ============

    private NamHocDTO toDTO(NamHoc entity) {
        if (entity == null) return null;

        try {
            LocalDate now = LocalDate.now();

            // Calculate status
            String trangThai;
            if (isUpcoming(entity, now)) {
                trangThai = "Chưa bắt đầu";
            } else if (isOngoing(entity, now)) {
                trangThai = "Đang diễn ra";
            } else {
                trangThai = "Đã kết thúc";
            }

            // Calculate progress
            Integer soNgayConLai = null;
            Integer tongSoNgay = null;
            Double tiLePhanTram = null;

            if (entity.getNgayBatDau() != null && entity.getNgayKetThuc() != null) {
                tongSoNgay = (int) ChronoUnit.DAYS.between(entity.getNgayBatDau(), entity.getNgayKetThuc()) + 1;

                if (isOngoing(entity, now)) {
                    soNgayConLai = (int) ChronoUnit.DAYS.between(now, entity.getNgayKetThuc());
                    long ngayDaQua = ChronoUnit.DAYS.between(entity.getNgayBatDau(), now) + 1;
                    tiLePhanTram = (double) ngayDaQua / tongSoNgay * 100;
                } else if (isUpcoming(entity, now)) {
                    soNgayConLai = (int) ChronoUnit.DAYS.between(now, entity.getNgayBatDau());
                    tiLePhanTram = 0.0;
                } else {
                    soNgayConLai = 0;
                    tiLePhanTram = 100.0;
                }
            }

            return NamHocDTO.builder()
                    .maNamHoc(entity.getMaNamHoc())
                    .tenNamHoc(entity.getTenNamHoc())
                    .ngayBatDau(entity.getNgayBatDau())
                    .ngayKetThuc(entity.getNgayKetThuc())
                    .moTa(entity.getMoTa())
                    .isActive(entity.getIsActive())
                    .isCurrent(entity.getIsCurrent())
                    .trangThai(trangThai)
                    .soNgayConLai(soNgayConLai)
                    .tongSoNgay(tongSoNgay)
                    .tiLePhanTram(tiLePhanTram)
                    .startYear(entity.getNgayBatDau() != null ? entity.getNgayBatDau().getYear() : null)
                    .endYear(entity.getNgayKetThuc() != null ? entity.getNgayKetThuc().getYear() : null)
                    .soHocKy(0) // TODO: Calculate actual semester count
                    .build();
        } catch (Exception e) {
            log.error("Error converting to DTO: ", e);
            return NamHocDTO.builder()
                    .maNamHoc(entity.getMaNamHoc())
                    .tenNamHoc(entity.getTenNamHoc())
                    .isActive(entity.getIsActive())
                    .isCurrent(entity.getIsCurrent())
                    .build();
        }
    }

    private NamHoc toEntity(NamHocDTO dto) {
        return NamHoc.builder()
                .maNamHoc(dto.getMaNamHoc())
                .tenNamHoc(dto.getTenNamHoc())
                .ngayBatDau(dto.getNgayBatDau())
                .ngayKetThuc(dto.getNgayKetThuc())
                .moTa(dto.getMoTa())
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .isCurrent(dto.getIsCurrent() != null ? dto.getIsCurrent() : false)
                .build();
    }
}