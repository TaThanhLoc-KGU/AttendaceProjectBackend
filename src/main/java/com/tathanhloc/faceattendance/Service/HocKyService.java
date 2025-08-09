package com.tathanhloc.faceattendance.Service;

import com.tathanhloc.faceattendance.DTO.HocKyDTO;
import com.tathanhloc.faceattendance.Model.HocKy;
import com.tathanhloc.faceattendance.Repository.HocKyRepository;
import com.tathanhloc.faceattendance.Repository.LopHocPhanRepository;
import com.tathanhloc.faceattendance.Repository.DangKyHocRepository;
import com.tathanhloc.faceattendance.Exception.ResourceNotFoundException;
import com.tathanhloc.faceattendance.Exception.BusinessException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class HocKyService {

    private final HocKyRepository hocKyRepository;
    private final LopHocPhanRepository lopHocPhanRepository;
    private final DangKyHocRepository dangKyHocRepository;

    // ===== EXISTING METHODS (Keep for backward compatibility) =====

    public List<HocKyDTO> getAll() {
        log.info("📚 Lấy danh sách tất cả học kỳ");
        return hocKyRepository.findAll().stream()
                .map(this::toDetailedDTO)
                .collect(Collectors.toList());
    }

    public List<HocKyDTO> getAllActive() {
        log.info("📚 Lấy danh sách học kỳ đang hoạt động");
        return hocKyRepository.findByIsActiveTrueOrderByNgayBatDauDesc().stream()
                .map(this::toDetailedDTO)
                .collect(Collectors.toList());
    }

    public HocKyDTO getById(String maHocKy) {
        log.info("🔍 Lấy thông tin học kỳ: {}", maHocKy);
        HocKy hocKy = hocKyRepository.findById(maHocKy)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy học kỳ: " + maHocKy));
        return toDetailedDTO(hocKy);
    }

    public Optional<HocKyDTO> getCurrentSemester() {
        log.info("🎯 Lấy học kỳ hiện tại");
        return hocKyRepository.findByIsCurrentTrue()
                .map(this::toDetailedDTO);
    }

    @Transactional
    public HocKyDTO create(HocKyDTO dto) {
        log.info("➕ Tạo học kỳ mới: {}", dto.getMaHocKy());

        // Validation
        validateHocKyData(dto);
        validateNoConflicts(dto, null);

        HocKy entity = toEntity(dto);

        // Auto-sync dates if week-based config
        if (entity.isWeekBasedConfig()) {
            entity.syncDatesFromWeekConfig();
            log.info("📅 Auto-synced dates from week config for: {}", entity.getMaHocKy());
        }

        entity = hocKyRepository.save(entity);

        log.info("✅ Đã tạo học kỳ: {}", entity.getMaHocKy());
        return toDetailedDTO(entity);
    }

    @Transactional
    public HocKyDTO update(String maHocKy, HocKyDTO dto) {
        log.info("📝 Cập nhật học kỳ: {}", maHocKy);

        HocKy existing = hocKyRepository.findById(maHocKy)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy học kỳ: " + maHocKy));

        // Validation
        validateHocKyData(dto);
        validateNoConflicts(dto, maHocKy);
        validateCanUpdate(existing);

        // Update fields
        existing.setTenHocKy(dto.getTenHocKy());
        existing.setMoTa(dto.getMoTa());
        existing.setLoaiHocKy(dto.getLoaiHocKy());
        existing.setIsActive(dto.getIsActive());

        // Update date fields
        if (dto.getNgayBatDau() != null) existing.setNgayBatDau(dto.getNgayBatDau());
        if (dto.getNgayKetThuc() != null) existing.setNgayKetThuc(dto.getNgayKetThuc());

        // Update week-based fields (if provided)
        if (dto.getTuanBatDau() != null) existing.setTuanBatDau(dto.getTuanBatDau());
        if (dto.getSoTuanHoc() != null) existing.setSoTuanHoc(dto.getSoTuanHoc());
        if (dto.getNgayBatDauTuan1() != null) existing.setNgayBatDauTuan1(dto.getNgayBatDauTuan1());

        // Auto-sync if week-based config is complete
        if (existing.isWeekBasedConfig()) {
            existing.syncDatesFromWeekConfig();
            log.info("📅 Auto-synced dates from week config for: {}", existing.getMaHocKy());
        }

        existing = hocKyRepository.save(existing);

        log.info("✅ Đã cập nhật học kỳ: {}", existing.getMaHocKy());
        return toDetailedDTO(existing);
    }

    @Transactional
    public void delete(String maHocKy) {
        log.info("🗑️ Xóa học kỳ: {}", maHocKy);

        HocKy hocKy = hocKyRepository.findById(maHocKy)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy học kỳ: " + maHocKy));

        validateCanDelete(hocKy);

        hocKyRepository.delete(hocKy);
        log.info("✅ Đã xóa học kỳ: {}", maHocKy);
    }

    @Transactional
    public HocKyDTO setAsCurrent(String maHocKy) {
        log.info("🎯 Đặt học kỳ hiện tại: {}", maHocKy);

        // Clear current flag from all semesters
        hocKyRepository.clearAllCurrentFlags();

        // Set new current semester
        HocKy hocKy = hocKyRepository.findById(maHocKy)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy học kỳ: " + maHocKy));

        if (!Boolean.TRUE.equals(hocKy.getIsActive())) {
            throw new BusinessException("Không thể đặt học kỳ không hoạt động làm học kỳ hiện tại");
        }

        hocKy.setIsCurrent(true);
        hocKy = hocKyRepository.save(hocKy);

        log.info("✅ Đã đặt học kỳ hiện tại: {}", maHocKy);
        return toDetailedDTO(hocKy);
    }

    // ===== NEW WEEK-BASED METHODS =====

    public List<HocKyDTO.TuanHocDTO> getTuanHocList(String maHocKy) {
        log.info("📅 Lấy danh sách tuần học: {}", maHocKy);

        HocKy hocKy = hocKyRepository.findById(maHocKy)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy học kỳ: " + maHocKy));

        if (!hocKy.isWeekBasedConfig()) {
            log.warn("Học kỳ {} không có cấu hình week-based", maHocKy);
            return new ArrayList<>();
        }

        return generateTuanHocList(hocKy);
    }

    @Transactional
    public HocKyDTO convertToWeekBased(String maHocKy, Integer tuanBatDau, Integer soTuanHoc, LocalDate ngayBatDauTuan1) {
        log.info("🔄 Chuyển đổi học kỳ {} sang week-based", maHocKy);

        HocKy hocKy = hocKyRepository.findById(maHocKy)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy học kỳ: " + maHocKy));

        if (hocKy.isWeekBasedConfig()) {
            throw new BusinessException("Học kỳ đã sử dụng cấu hình week-based");
        }

        // Validate conversion
        if (!hocKy.isUpcoming()) {
            throw new BusinessException("Chỉ có thể chuyển đổi học kỳ chưa bắt đầu");
        }

        // Set week-based config
        hocKy.setTuanBatDau(tuanBatDau);
        hocKy.setSoTuanHoc(soTuanHoc);
        hocKy.setNgayBatDauTuan1(ngayBatDauTuan1);

        // Sync dates
        hocKy.syncDatesFromWeekConfig();

        hocKy = hocKyRepository.save(hocKy);

        log.info("✅ Đã chuyển đổi học kỳ {} sang week-based", maHocKy);
        return toDetailedDTO(hocKy);
    }

    // ===== NEW METHODS FOR COMPATIBILITY =====

    public List<HocKyDTO> search(String keyword) {
        log.info("🔍 Tìm kiếm học kỳ với keyword: {}", keyword);
        return hocKyRepository.searchByKeyword(keyword).stream()
                .map(this::toDetailedDTO)
                .collect(Collectors.toList());
    }

    public List<HocKyDTO> filter(HocKy.LoaiHocKy loaiHocKy, Integer year, String trangThai, Boolean isActive) {
        log.info("🔍 Lọc học kỳ - loai: {}, year: {}, trangThai: {}, active: {}",
                loaiHocKy, year, trangThai, isActive);

        return hocKyRepository.findByCriteria(null, loaiHocKy, year, isActive).stream()
                .map(this::toDetailedDTO)
                .filter(dto -> trangThai == null || trangThai.equals(dto.getTrangThai()))
                .collect(Collectors.toList());
    }

    public List<Integer> getAvailableYears() {
        log.info("📅 Lấy danh sách năm học khả dụng");
        return hocKyRepository.findDistinctYears();
    }

    public Map<String, Object> getStatistics() {
        log.info("📊 Lấy thống kê học kỳ");

        LocalDate today = LocalDate.now();
        Object[] statusCounts = hocKyRepository.countByStatus(today);
        List<Object[]> typeCounts = hocKyRepository.countByLoaiHocKy();
        Object[] configCounts = hocKyRepository.countByConfigType();

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", hocKyRepository.count());
        stats.put("upcoming", statusCounts[0]);
        stats.put("ongoing", statusCounts[1]);
        stats.put("finished", statusCounts[2]);

        // Type statistics
        Map<String, Long> typeStats = new HashMap<>();
        for (Object[] count : typeCounts) {
            typeStats.put(((HocKy.LoaiHocKy) count[0]).name(), (Long) count[1]);
        }
        stats.put("byType", typeStats);

        // Config type statistics
        stats.put("weekBasedCount", configCounts[0]);
        stats.put("dateBasedCount", configCounts[1]);

        return stats;
    }

    public List<HocKyDTO> checkConflicts(LocalDate ngayBatDauTuan1, Integer tuanBatDau, Integer soTuanHoc, String excludeMaHocKy) {
        log.info("⚠️ Kiểm tra xung đột thời gian");

        if (tuanBatDau != null && soTuanHoc != null && ngayBatDauTuan1 != null) {
            // Week-based conflict check
            int tuanKetThuc = tuanBatDau + soTuanHoc - 1;
            return hocKyRepository.findByWeekRange(ngayBatDauTuan1, tuanBatDau, tuanKetThuc).stream()
                    .filter(hk -> excludeMaHocKy == null || !excludeMaHocKy.equals(hk.getMaHocKy()))
                    .map(this::toBasicDTO)
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }

    public List<String> validateSemesterData(HocKyDTO dto) {
        List<String> errors = new ArrayList<>();

        try {
            validateHocKyData(dto);
        } catch (BusinessException e) {
            errors.add(e.getMessage());
        }

        return errors;
    }

    private List<HocKyDTO.TuanHocDTO> generateTuanHocList(HocKy hocKy) {
        List<HocKyDTO.TuanHocDTO> danhSach = new ArrayList<>();

        if (!hocKy.isWeekBasedConfig()) return danhSach;

        LocalDate ngayBatDauHocKy = hocKy.getNgayBatDauThucTe();
        LocalDate today = LocalDate.now();

        for (int i = 0; i < hocKy.getSoTuanHoc(); i++) {
            LocalDate ngayBatDauTuan = ngayBatDauHocKy.plusWeeks(i);
            LocalDate ngayKetThucTuan = ngayBatDauTuan.plusDays(6);

            boolean isHienTai = !today.isBefore(ngayBatDauTuan) && !today.isAfter(ngayKetThucTuan);

            HocKyDTO.TuanHocDTO tuan = HocKyDTO.TuanHocDTO.builder()
                    .soTuan(i + 1)
                    .tuanTrongNam(hocKy.getTuanBatDau() + i)
                    .ngayBatDau(ngayBatDauTuan)
                    .ngayKetThuc(ngayKetThucTuan)
                    .isHienTai(isHienTai)
                    .isNghiLe(checkNghiLe(ngayBatDauTuan, ngayKetThucTuan))
                    .build();

            danhSach.add(tuan);
        }

        return danhSach;
    }

    private Boolean checkNghiLe(LocalDate ngayBatDau, LocalDate ngayKetThuc) {
        // TODO: Implement holiday checking logic
        return false;
    }

    // ===== VALIDATION METHODS =====

    private void validateHocKyData(HocKyDTO dto) {
        List<String> errors = new ArrayList<>();

        // Basic validation
        if (dto.getNgayBatDau() != null && dto.getNgayKetThuc() != null) {
            if (!dto.getNgayKetThuc().isAfter(dto.getNgayBatDau())) {
                errors.add("Ngày kết thúc phải sau ngày bắt đầu");
            }
        }

        // Week-based validation (if provided)
        if (dto.getTuanBatDau() != null && dto.getSoTuanHoc() != null) {
            int tuanKetThuc = dto.getTuanBatDau() + dto.getSoTuanHoc() - 1;
            if (tuanKetThuc > 52) {
                errors.add("Tuần kết thúc (" + tuanKetThuc + ") không được vượt quá tuần 52");
            }
        }

        // Start date validation (if week-based)
        if (dto.getNgayBatDauTuan1() != null &&
                dto.getNgayBatDauTuan1().getDayOfWeek().getValue() != 1) {
            errors.add("Ngày bắt đầu tuần 1 phải là thứ Hai");
        }

        if (!errors.isEmpty()) {
            throw new BusinessException("Dữ liệu không hợp lệ: " + String.join(", ", errors));
        }
    }

    private void validateNoConflicts(HocKyDTO dto, String excludeMaHocKy) {
        // Use date-based conflict checking (compatible with existing data)
        LocalDate ngayBatDau = dto.getNgayBatDau();
        LocalDate ngayKetThuc = dto.getNgayKetThuc();

        // If week-based, calculate dates
        if (dto.getTuanBatDau() != null && dto.getSoTuanHoc() != null && dto.getNgayBatDauTuan1() != null) {
            ngayBatDau = dto.getNgayBatDauTuan1().plusWeeks(dto.getTuanBatDau() - 1);
            ngayKetThuc = ngayBatDau.plusWeeks(dto.getSoTuanHoc() - 1).plusDays(6);
        }

        if (ngayBatDau == null || ngayKetThuc == null) return;

        List<HocKy> conflicts = hocKyRepository.findConflictingSemesters(
                ngayBatDau, ngayKetThuc, excludeMaHocKy);

        if (!conflicts.isEmpty()) {
            String conflictNames = conflicts.stream()
                    .map(HocKy::getTenHocKy)
                    .collect(Collectors.joining(", "));
            throw new BusinessException("Xung đột thời gian với các học kỳ: " + conflictNames);
        }
    }

    private void validateCanUpdate(HocKy hocKy) {
        if (hocKy.isFinished()) {
            throw new BusinessException("Không thể sửa học kỳ đã kết thúc");
        }
    }

    private void validateCanDelete(HocKy hocKy) {
        if (Boolean.TRUE.equals(hocKy.getIsCurrent())) {
            throw new BusinessException("Không thể xóa học kỳ hiện tại");
        }

        long soLopHocPhan = lopHocPhanRepository.countByHocKy(hocKy.getMaHocKy());
        if (soLopHocPhan > 0) {
            throw new BusinessException("Không thể xóa học kỳ đã có lớp học phần");
        }
    }

    // ===== CONVERSION METHODS =====

    private HocKyDTO toDetailedDTO(HocKy entity) {
        HocKyDTO dto = toBasicDTO(entity);

        // Add computed fields
        dto.setTuanKetThuc(entity.getTuanKetThuc());
        dto.setNgayBatDauThucTe(entity.getNgayBatDauThucTe());
        dto.setNgayKetThucThucTe(entity.getNgayKetThucThucTe());
        dto.setTrangThai(getTrangThai(entity));
        dto.setTuanHienTai(entity.getTuanHienTai());
        dto.setTiLePhanTram(entity.getTienDoPercent());
        dto.setIsWeekBasedConfig(entity.isWeekBasedConfig());

        // Calculate remaining days
        LocalDate endDate = entity.getNgayKetThucThucTe();
        if (endDate != null) {
            LocalDate today = LocalDate.now();
            if (today.isBefore(endDate)) {
                dto.setSoNgayConLai((int) ChronoUnit.DAYS.between(today, endDate));
            } else {
                dto.setSoNgayConLai(0);
            }
        }

        // Calculate total days
        LocalDate startDate = entity.getNgayBatDauThucTe();
        if (startDate != null && endDate != null) {
            dto.setTongSoNgay((int) ChronoUnit.DAYS.between(startDate, endDate) + 1);
        }

        // Add academic metrics
        if (entity.isWeekBasedConfig() && entity.getSoTuanHoc() != null) {
            dto.setSoBuoiHocDuKien(entity.getSoTuanHoc() * 3); // Giả sử 3 buổi/tuần
            dto.setDanhSachTuanHoc(generateTuanHocList(entity));
        }

        // Add statistics
        // TODO: Thêm repository methods để tối ưu performance
        // Hiện tại dùng stream filter tạm thời
        long soLopHocPhan = lopHocPhanRepository.findAll().stream()
                .filter(lhp -> entity.getMaHocKy().equals(lhp.getHocKy()))
                .count();
        dto.setSoLopHocPhan((int) soLopHocPhan);

        long soSinhVienDangKy = dangKyHocRepository.findAll().stream()
                .filter(dk -> dk.getLopHocPhan().getHocKy().equals(entity.getMaHocKy()))
                .count();
        dto.setSoSinhVienDangKy((int) soSinhVienDangKy);

        // AFTER adding repository methods, use:
        // dto.setSoLopHocPhan((int) lopHocPhanRepository.countByHocKy(entity.getMaHocKy()));
        // dto.setSoSinhVienDangKy((int) dangKyHocRepository.countUniqueStudentsByHocKy(entity.getMaHocKy()));

        return dto;
    }

    private HocKyDTO toBasicDTO(HocKy entity) {
        return HocKyDTO.builder()
                .maHocKy(entity.getMaHocKy())
                .tenHocKy(entity.getTenHocKy())
                .ngayBatDau(entity.getNgayBatDau())
                .ngayKetThuc(entity.getNgayKetThuc())
                .tuanBatDau(entity.getTuanBatDau())
                .soTuanHoc(entity.getSoTuanHoc())
                .ngayBatDauTuan1(entity.getNgayBatDauTuan1())
                .moTa(entity.getMoTa())
                .isActive(entity.getIsActive())
                .isCurrent(entity.getIsCurrent())
                .loaiHocKy(entity.getLoaiHocKy())
                .build();
    }

    private HocKy toEntity(HocKyDTO dto) {
        return HocKy.builder()
                .maHocKy(dto.getMaHocKy())
                .tenHocKy(dto.getTenHocKy())
                .ngayBatDau(dto.getNgayBatDau())
                .ngayKetThuc(dto.getNgayKetThuc())
                .tuanBatDau(dto.getTuanBatDau())
                .soTuanHoc(dto.getSoTuanHoc())
                .ngayBatDauTuan1(dto.getNgayBatDauTuan1())
                .moTa(dto.getMoTa())
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .isCurrent(dto.getIsCurrent() != null ? dto.getIsCurrent() : false)
                .loaiHocKy(dto.getLoaiHocKy() != null ? dto.getLoaiHocKy() : HocKy.LoaiHocKy.CHINH_QUY)
                .build();
    }

    private String getTrangThai(HocKy entity) {
        if (entity.isUpcoming()) return "Chưa bắt đầu";
        if (entity.isOngoing()) return "Đang diễn ra";
        if (entity.isFinished()) return "Đã kết thúc";
        return "Không xác định";
    }
}