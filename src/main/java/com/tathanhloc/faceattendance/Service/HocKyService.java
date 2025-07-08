package com.tathanhloc.faceattendance.Service;

import com.tathanhloc.faceattendance.DTO.HocKyDTO;
import com.tathanhloc.faceattendance.Model.HocKy;
import com.tathanhloc.faceattendance.Repository.HocKyRepository;
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
public class HocKyService {

    private final HocKyRepository hocKyRepository;

    public List<HocKyDTO> getAll() {
        try {
            return hocKyRepository.findAll().stream()
                    .filter(hk -> hk.getIsActive() != null && hk.getIsActive())
                    .map(this::toDTO)
                    .toList();
        } catch (Exception e) {
            log.error("Error in getAll(): ", e);
            throw new RuntimeException("Không thể lấy danh sách học kỳ: " + e.getMessage());
        }
    }

    public List<HocKyDTO> getAllIncludeInactive() {
        try {
            return hocKyRepository.findAll().stream()
                    .map(this::toDTO)
                    .toList();
        } catch (Exception e) {
            log.error("Error in getAllIncludeInactive(): ", e);
            throw new RuntimeException("Không thể lấy danh sách học kỳ: " + e.getMessage());
        }
    }

    public HocKyDTO getById(String id) {
        return hocKyRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException("HocKy", "maHocKy", id));
    }

    @Transactional
    public HocKyDTO create(HocKyDTO dto) {
        try {
            HocKy hocKy = toEntity(dto);

            // If this is set as current, unset others
            if (Boolean.TRUE.equals(dto.getIsCurrent())) {
                unsetAllCurrent();
            }

            HocKy saved = hocKyRepository.save(hocKy);
            return toDTO(saved);
        } catch (Exception e) {
            log.error("Error creating HocKy: ", e);
            throw new RuntimeException("Không thể tạo học kỳ: " + e.getMessage());
        }
    }

    @Transactional
    public HocKyDTO update(String id, HocKyDTO dto) {
        try {
            HocKy existing = hocKyRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("HocKy", "maHocKy", id));

            updateEntity(existing, dto);

            HocKy updated = hocKyRepository.save(existing);
            return toDTO(updated);
        } catch (Exception e) {
            log.error("Error updating HocKy: ", e);
            throw new RuntimeException("Không thể cập nhật học kỳ: " + e.getMessage());
        }
    }

    @Transactional
    public void softDelete(String id) {
        try {
            HocKy hocKy = hocKyRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("HocKy", "maHocKy", id));

            hocKy.setIsActive(false);
            hocKy.setIsCurrent(false);
            hocKyRepository.save(hocKy);
        } catch (Exception e) {
            log.error("Error soft deleting HocKy: ", e);
            throw new RuntimeException("Không thể xóa học kỳ: " + e.getMessage());
        }
    }

    @Transactional
    public HocKyDTO restore(String id) {
        try {
            HocKy hocKy = hocKyRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("HocKy", "maHocKy", id));

            hocKy.setIsActive(true);
            HocKy restored = hocKyRepository.save(hocKy);
            return toDTO(restored);
        } catch (Exception e) {
            log.error("Error restoring HocKy: ", e);
            throw new RuntimeException("Không thể khôi phục học kỳ: " + e.getMessage());
        }
    }

    public void delete(String id) {
        softDelete(id);
    }

    // ============ SPECIAL QUERIES ============

    public Optional<HocKyDTO> getCurrentSemester() {
        try {
            return hocKyRepository.findAll().stream()
                    .filter(hk -> Boolean.TRUE.equals(hk.getIsCurrent()))
                    .map(this::toDTO)
                    .findFirst();
        } catch (Exception e) {
            log.error("Error getting current semester: ", e);
            return Optional.empty();
        }
    }

    public List<HocKyDTO> getOngoingSemesters() {
        try {
            LocalDate now = LocalDate.now();
            return hocKyRepository.findAll().stream()
                    .filter(hk -> hk.getIsActive() != null && hk.getIsActive())
                    .filter(hk -> isOngoing(hk, now))
                    .map(this::toDTO)
                    .toList();
        } catch (Exception e) {
            log.error("Error getting ongoing semesters: ", e);
            return List.of();
        }
    }

    public List<HocKyDTO> getUpcomingSemesters() {
        try {
            LocalDate now = LocalDate.now();
            return hocKyRepository.findAll().stream()
                    .filter(hk -> hk.getIsActive() != null && hk.getIsActive())
                    .filter(hk -> isUpcoming(hk, now))
                    .map(this::toDTO)
                    .toList();
        } catch (Exception e) {
            log.error("Error getting upcoming semesters: ", e);
            return List.of();
        }
    }

    public List<HocKyDTO> getFinishedSemesters() {
        try {
            LocalDate now = LocalDate.now();
            return hocKyRepository.findAll().stream()
                    .filter(hk -> hk.getIsActive() != null && hk.getIsActive())
                    .filter(hk -> isFinished(hk, now))
                    .map(this::toDTO)
                    .toList();
        } catch (Exception e) {
            log.error("Error getting finished semesters: ", e);
            return List.of();
        }
    }

    @Transactional
    public HocKyDTO setAsCurrent(String id) {
        try {
            // Unset all current flags
            unsetAllCurrent();

            // Set new current
            HocKy hocKy = hocKyRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("HocKy", "maHocKy", id));

            hocKy.setIsCurrent(true);
            HocKy updated = hocKyRepository.save(hocKy);

            return toDTO(updated);
        } catch (Exception e) {
            log.error("Error setting current semester: ", e);
            throw new RuntimeException("Không thể đặt học kỳ hiện tại: " + e.getMessage());
        }
    }

    // ============ HELPER METHODS ============


    @Transactional
    protected void unsetAllCurrent() {
        List<HocKy> currentSemesters = hocKyRepository.findAll().stream()
                .filter(hk -> Boolean.TRUE.equals(hk.getIsCurrent()))
                .toList();

        for (HocKy hk : currentSemesters) {
            hk.setIsCurrent(false);
            hocKyRepository.save(hk);
        }
    }

    private boolean isOngoing(HocKy hocKy, LocalDate now) {
        if (hocKy.getNgayBatDau() == null || hocKy.getNgayKetThuc() == null) return false;
        return !now.isBefore(hocKy.getNgayBatDau()) && !now.isAfter(hocKy.getNgayKetThuc());
    }

    private boolean isUpcoming(HocKy hocKy, LocalDate now) {
        if (hocKy.getNgayBatDau() == null) return false;
        return now.isBefore(hocKy.getNgayBatDau());
    }

    private boolean isFinished(HocKy hocKy, LocalDate now) {
        if (hocKy.getNgayKetThuc() == null) return false;
        return now.isAfter(hocKy.getNgayKetThuc());
    }

    private void updateEntity(HocKy existing, HocKyDTO dto) {
        if (dto.getTenHocKy() != null) existing.setTenHocKy(dto.getTenHocKy());
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

    private HocKyDTO toDTO(HocKy entity) {
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

            return HocKyDTO.builder()
                    .maHocKy(entity.getMaHocKy())
                    .tenHocKy(entity.getTenHocKy())
                    .ngayBatDau(entity.getNgayBatDau())
                    .ngayKetThuc(entity.getNgayKetThuc())
                    .moTa(entity.getMoTa())
                    .isActive(entity.getIsActive())
                    .isCurrent(entity.getIsCurrent())
                    .trangThai(trangThai)
                    .soNgayConLai(soNgayConLai)
                    .tongSoNgay(tongSoNgay)
                    .tiLePhanTram(tiLePhanTram)
                    .build();
        } catch (Exception e) {
            log.error("Error converting to DTO: ", e);
            return HocKyDTO.builder()
                    .maHocKy(entity.getMaHocKy())
                    .tenHocKy(entity.getTenHocKy())
                    .isActive(entity.getIsActive())
                    .isCurrent(entity.getIsCurrent())
                    .build();
        }
    }

    private HocKy toEntity(HocKyDTO dto) {
        return HocKy.builder()
                .maHocKy(dto.getMaHocKy())
                .tenHocKy(dto.getTenHocKy())
                .ngayBatDau(dto.getNgayBatDau())
                .ngayKetThuc(dto.getNgayKetThuc())
                .moTa(dto.getMoTa())
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .isCurrent(dto.getIsCurrent() != null ? dto.getIsCurrent() : false)
                .build();
    }
}