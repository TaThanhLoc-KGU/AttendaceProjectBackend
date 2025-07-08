package com.tathanhloc.faceattendance.Service;

import com.tathanhloc.faceattendance.DTO.HocKyDTO;
import com.tathanhloc.faceattendance.DTO.NamHocDTO;
import com.tathanhloc.faceattendance.Model.HocKy;
import com.tathanhloc.faceattendance.Model.HocKyNamHoc;
import com.tathanhloc.faceattendance.Model.NamHoc;
import com.tathanhloc.faceattendance.Repository.HocKyNamHocRepository;
import com.tathanhloc.faceattendance.Repository.HocKyRepository;
import com.tathanhloc.faceattendance.Repository.NamHocRepository;
import com.tathanhloc.faceattendance.Exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NamHocService {

    private final NamHocRepository namHocRepository;
    private final HocKyNamHocRepository hocKyNamHocRepository;
    private final HocKyRepository hocKyRepository;

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
    protected void unsetAllCurrent() {
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

    @Transactional
    public Map<String, Object> createSemestersForYear(String maNamHoc) {
        log.info("Creating default semesters for academic year: {}", maNamHoc);

        try {
            // Kiểm tra năm học tồn tại
            NamHoc namHoc = namHocRepository.findById(maNamHoc)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy năm học: " + maNamHoc));

            // Kiểm tra đã có học kỳ chưa
            List<HocKyNamHoc> existingRelations = hocKyNamHocRepository.findByNamHoc_MaNamHoc(maNamHoc);
            if (!existingRelations.isEmpty()) {
                throw new RuntimeException("Năm học này đã có học kỳ được tạo");
            }

            // Parse năm học để tạo học kỳ
            String[] years = maNamHoc.split("-");
            if (years.length != 2) {
                throw new RuntimeException("Định dạng mã năm học không hợp lệ: " + maNamHoc);
            }

            int startYear = Integer.parseInt(years[0].trim());
            int endYear = Integer.parseInt(years[1].trim());

            List<HocKy> createdSemesters = new ArrayList<>();
            List<HocKyNamHoc> createdRelations = new ArrayList<>();

            // Tạo Học kỳ 1 (Tháng 9 - Tháng 12)
            HocKy semester1 = createSemester(
                    "HK1_" + maNamHoc,
                    "Học kỳ 1 - " + maNamHoc,
                    LocalDate.of(startYear, 9, 1),
                    LocalDate.of(startYear, 12, 31),
                    "Học kỳ 1 của năm học " + maNamHoc
            );
            createdSemesters.add(semester1);

            // Tạo Học kỳ 2 (Tháng 1 - Tháng 5)
            HocKy semester2 = createSemester(
                    "HK2_" + maNamHoc,
                    "Học kỳ 2 - " + maNamHoc,
                    LocalDate.of(endYear, 1, 15),
                    LocalDate.of(endYear, 5, 15),
                    "Học kỳ 2 của năm học " + maNamHoc
            );
            createdSemesters.add(semester2);

            // Tạo Học kỳ hè (Tháng 6 - Tháng 8)
            HocKy semester3 = createSemester(
                    "HKH_" + maNamHoc,
                    "Học kỳ hè - " + maNamHoc,
                    LocalDate.of(endYear, 6, 1),
                    LocalDate.of(endYear, 8, 31),
                    "Học kỳ hè của năm học " + maNamHoc
            );
            createdSemesters.add(semester3);

            // Tạo relationships trong bảng hoc_ky_nam_hoc với thứ tự
            int thuTu = 1;
            for (HocKy semester : createdSemesters) {
                HocKyNamHoc relation = HocKyNamHoc.builder()
                        .hocKy(semester)
                        .namHoc(namHoc)
                        .thuTu(thuTu++)
                        .isActive(true)
                        .build();

                HocKyNamHoc savedRelation = hocKyNamHocRepository.save(relation);
                createdRelations.add(savedRelation);

                log.debug("✅ Created relationship: {} - {} (thứ tự: {})",
                        semester.getMaHocKy(), maNamHoc, relation.getThuTu());
            }

            // Chuẩn bị response
            Map<String, Object> result = new HashMap<>();
            result.put("maNamHoc", maNamHoc);
            result.put("createdSemesters", createdSemesters.size());
            result.put("semesters", createdSemesters.stream()
                    .map(this::convertToHocKyDTO)
                    .collect(Collectors.toList()));
            result.put("message", "Đã tạo thành công " + createdSemesters.size() + " học kỳ cho năm học " + maNamHoc);

            log.info("✅ Successfully created {} semesters for academic year: {}",
                    createdSemesters.size(), maNamHoc);

            return result;

        } catch (Exception e) {
            log.error("❌ Error creating semesters for academic year: {}", maNamHoc, e);
            throw new RuntimeException("Lỗi khi tạo học kỳ: " + e.getMessage(), e);
        }
    }

    /**
     * Lấy danh sách học kỳ theo năm học
     */
    public List<HocKyDTO> getSemestersByYear(String maNamHoc) {
        log.debug("Getting semesters for academic year: {}", maNamHoc);

        try {
            List<HocKyNamHoc> relations = hocKyNamHocRepository.findByNamHoc_MaNamHocAndIsActive(maNamHoc, true);

            return relations.stream()
                    .map(relation -> convertToHocKyDTO(relation.getHocKy()))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("❌ Error getting semesters for academic year: {}", maNamHoc, e);
            return Collections.emptyList();
        }
    }

    /**
     * Xóa tất cả học kỳ của năm học
     */
    @Transactional
    public void deleteSemestersOfYear(String maNamHoc) {
        log.info("Deleting all semesters of academic year: {}", maNamHoc);

        try {
            List<HocKyNamHoc> relations = hocKyNamHocRepository.findByNamHoc_MaNamHoc(maNamHoc);

            for (HocKyNamHoc relation : relations) {
                // Soft delete relation
                relation.setIsActive(false);
                hocKyNamHocRepository.save(relation);

                // Soft delete học kỳ
                HocKy hocKy = relation.getHocKy();
                hocKy.setIsActive(false);
                hocKyRepository.save(hocKy);

                log.debug("✅ Deleted semester: {}", hocKy.getMaHocKy());
            }

            log.info("✅ Successfully deleted {} semesters for academic year: {}",
                    relations.size(), maNamHoc);

        } catch (Exception e) {
            log.error("❌ Error deleting semesters for academic year: {}", maNamHoc, e);
            throw new RuntimeException("Lỗi khi xóa học kỳ: " + e.getMessage(), e);
        }
    }

    /**
     * Helper method để tạo học kỳ
     */
    private HocKy createSemester(String maHocKy, String tenHocKy, LocalDate ngayBatDau,
                                 LocalDate ngayKetThuc, String moTa) {

        // Kiểm tra học kỳ đã tồn tại chưa
        if (hocKyRepository.existsById(maHocKy)) {
            throw new RuntimeException("Học kỳ đã tồn tại: " + maHocKy);
        }

        HocKy hocKy = HocKy.builder()
                .maHocKy(maHocKy)
                .tenHocKy(tenHocKy)
                .ngayBatDau(ngayBatDau)
                .ngayKetThuc(ngayKetThuc)
                .moTa(moTa)
                .isActive(true)
                .isCurrent(false)
                .build();

        HocKy saved = hocKyRepository.save(hocKy);
        log.debug("✅ Created semester: {}", saved.getMaHocKy());

        return saved;
    }

    /**
     * Convert HocKy to DTO
     */
    private HocKyDTO convertToHocKyDTO(HocKy hocKy) {
        return HocKyDTO.builder()
                .maHocKy(hocKy.getMaHocKy())
                .tenHocKy(hocKy.getTenHocKy())
                .ngayBatDau(hocKy.getNgayBatDau())
                .ngayKetThuc(hocKy.getNgayKetThuc())
                .moTa(hocKy.getMoTa())
                .isActive(hocKy.getIsActive())
                .isCurrent(hocKy.getIsCurrent())
                .build();
    }
    /**
     * Xóa mềm năm học
     */
    @Transactional
    public void softDelete(String maNamHoc) {
        log.info("Soft deleting academic year: {}", maNamHoc);

        NamHoc namHoc = namHocRepository.findById(maNamHoc)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy năm học: " + maNamHoc));

        // Xóa mềm năm học
        namHoc.setIsActive(false);
        namHoc.setIsCurrent(false); // Bỏ current nếu đang là current
        namHocRepository.save(namHoc);

        // Xóa mềm tất cả học kỳ của năm học này
        deleteSemestersOfYear(maNamHoc);

        log.info("✅ Đã xóa mềm năm học và tất cả học kỳ: {}", maNamHoc);
    }

    /**
     * Khôi phục năm học đã xóa mềm
     */
    @Transactional
    public NamHocDTO restore(String maNamHoc) {
        log.info("Restoring academic year: {}", maNamHoc);

        NamHoc namHoc = namHocRepository.findById(maNamHoc)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy năm học: " + maNamHoc));

        // Khôi phục năm học
        namHoc.setIsActive(true);
        NamHoc restored = namHocRepository.save(namHoc);

        // Khôi phục tất cả relationships của năm học
        hocKyNamHocRepository.restoreByYear(maNamHoc);

        // Khôi phục tất cả học kỳ của năm học
        List<HocKyNamHoc> relations = hocKyNamHocRepository.findByNamHoc_MaNamHoc(maNamHoc);
        for (HocKyNamHoc relation : relations) {
            HocKy hocKy = relation.getHocKy();
            if (!hocKy.getIsActive()) {
                hocKy.setIsActive(true);
                hocKyRepository.save(hocKy);
            }
        }

        log.info("✅ Đã khôi phục năm học và tất cả học kỳ: {}", maNamHoc);
        return toDTO(restored);
    }

    /**
     * Xóa vĩnh viễn năm học (hard delete)
     */
    @Transactional
    public void hardDelete(String maNamHoc) {
        log.info("Hard deleting academic year: {}", maNamHoc);

        // Xóa tất cả relationships trước
        List<HocKyNamHoc> relations = hocKyNamHocRepository.findByNamHoc_MaNamHoc(maNamHoc);

        // Xóa tất cả học kỳ của năm học
        for (HocKyNamHoc relation : relations) {
            hocKyRepository.deleteById(relation.getHocKy().getMaHocKy());
        }

        // Xóa tất cả relationships
        hocKyNamHocRepository.deleteAll(relations);

        // Xóa năm học
        namHocRepository.deleteById(maNamHoc);

        log.info("✅ Đã xóa vĩnh viễn năm học và tất cả học kỳ: {}", maNamHoc);
    }

    /**
     * Lấy danh sách năm học đã xóa mềm
     */
    public List<NamHocDTO> getDeletedAcademicYears() {
        log.debug("Getting deleted academic years");
        return namHocRepository.findByIsActive(false).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Kiểm tra năm học có đang được sử dụng không
     */
    public boolean isAcademicYearInUse(String maNamHoc) {
        // Kiểm tra có học kỳ đang hoạt động không
        Long semesterCount = hocKyNamHocRepository.countByNamHoc(maNamHoc);
        return semesterCount > 0;
    }

    /**
     * Lấy thống kê năm học
     */
    public Map<String, Object> getAcademicYearStats(String maNamHoc) {
        Map<String, Object> stats = new HashMap<>();

        // Đếm số học kỳ
        Long totalSemesters = hocKyNamHocRepository.countByNamHoc(maNamHoc);
        Long activeSemesters = hocKyNamHocRepository.countActiveSemestersByYear(maNamHoc);

        stats.put("totalSemesters", totalSemesters);
        stats.put("activeSemesters", activeSemesters);
        stats.put("deletedSemesters", totalSemesters - activeSemesters);

        // Lấy danh sách học kỳ
        List<HocKyDTO> semesters = getSemestersByYear(maNamHoc);
        stats.put("semesters", semesters);

        return stats;
    }


}