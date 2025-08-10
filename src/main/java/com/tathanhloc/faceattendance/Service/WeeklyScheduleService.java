package com.tathanhloc.faceattendance.Service;

import com.tathanhloc.faceattendance.DTO.WeeklyScheduleDTO;
import com.tathanhloc.faceattendance.DTO.ScheduleInstanceDTO;
import com.tathanhloc.faceattendance.DTO.HocKyDTO;
import com.tathanhloc.faceattendance.Exception.ResourceNotFoundException;
import com.tathanhloc.faceattendance.Exception.BusinessException;
import com.tathanhloc.faceattendance.Model.*;
import com.tathanhloc.faceattendance.Repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.DayOfWeek;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WeeklyScheduleService {

    private final WeeklyScheduleRepository weeklyScheduleRepository;
    private final ScheduleInstanceRepository scheduleInstanceRepository;
    private final LopHocPhanRepository lopHocPhanRepository;
    private final PhongHocRepository phongHocRepository;
    private final GiangVienRepository giangVienRepository;
    private final HocKyService hocKyService;

    // ===== TEMPLATE MANAGEMENT =====

    /**
     * Tạo template lịch học mới
     */
    @Transactional
    public WeeklyScheduleDTO createTemplate(WeeklyScheduleDTO dto) {
        log.info("🆕 Creating weekly schedule template for LHP: {}", dto.getMaLhp());

        // Validation
        validateTemplateData(dto);
        checkTemplateConflicts(dto, null);

        // Convert DTO to Entity
        WeeklySchedule entity = toEntity(dto);
        entity.setMaTemplate(generateTemplateId(dto));
        entity.setTrangThai(WeeklySchedule.TrangThaiTemplate.DRAFT);
        entity.setIsActive(true);

        entity = weeklyScheduleRepository.save(entity);
        log.info("✅ Template created: {}", entity.getMaTemplate());

        return WeeklyScheduleDTO.fromEntity(entity);
    }

    /**
     * Cập nhật template
     */
    @Transactional
    public WeeklyScheduleDTO updateTemplate(String maTemplate, WeeklyScheduleDTO dto) {
        log.info("📝 Updating template: {}", maTemplate);

        WeeklySchedule existing = weeklyScheduleRepository.findById(maTemplate)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found: " + maTemplate));

        // Validation
        validateTemplateData(dto);
        checkTemplateConflicts(dto, maTemplate);

        // Update fields
        updateEntityFromDTO(existing, dto);
        existing = weeklyScheduleRepository.save(existing);

        // Regenerate instances if template is active
        if (existing.getTrangThai() == WeeklySchedule.TrangThaiTemplate.ACTIVE) {
            regenerateInstancesForTemplate(existing);
        }

        log.info("✅ Template updated: {}", maTemplate);
        return WeeklyScheduleDTO.fromEntity(existing);
    }

    /**
     * Kích hoạt template và tạo instances
     */
    @Transactional
    public WeeklyScheduleDTO activateTemplate(String maTemplate, String activatedBy) {
        log.info("🔥 Activating template: {}", maTemplate);

        WeeklySchedule template = weeklyScheduleRepository.findById(maTemplate)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found: " + maTemplate));

        // Validation
        if (template.getTrangThai() == WeeklySchedule.TrangThaiTemplate.ACTIVE) {
            throw new BusinessException("Template đã được kích hoạt");
        }

        // Check conflicts one more time
        checkTemplateConflicts(WeeklyScheduleDTO.fromEntity(template), maTemplate);

        // Activate template
        template.setTrangThai(WeeklySchedule.TrangThaiTemplate.ACTIVE);
        template.setUpdatedBy(activatedBy);
        template = weeklyScheduleRepository.save(template);

        // Generate instances
        generateInstancesForTemplate(template);

        log.info("✅ Template activated and instances generated: {}", maTemplate);
        return WeeklyScheduleDTO.fromEntity(template);
    }

    /**
     * Lấy template theo ID
     */
    public WeeklyScheduleDTO getTemplate(String maTemplate) {
        WeeklySchedule template = weeklyScheduleRepository.findById(maTemplate)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found: " + maTemplate));

        WeeklyScheduleDTO dto = WeeklyScheduleDTO.fromEntity(template);

        // Load instances count
        long instanceCount = scheduleInstanceRepository.countByWeeklyScheduleMaTemplateAndIsActiveTrue(maTemplate);
        dto.setSoInstanceDaTao((int) instanceCount);

        return dto;
    }

    /**
     * Lấy danh sách templates theo lớp học phần
     */
    public List<WeeklyScheduleDTO> getTemplatesByLopHocPhan(String maLhp) {
        return weeklyScheduleRepository.findByLopHocPhanMaLhpAndIsActiveTrue(maLhp)
                .stream()
                .map(WeeklyScheduleDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Lấy danh sách templates theo học kỳ
     */
    public List<WeeklyScheduleDTO> getTemplatesByHocKy(String hocKy) {
        return weeklyScheduleRepository.findByLopHocPhanHocKyAndIsActiveTrue(hocKy)
                .stream()
                .map(WeeklyScheduleDTO::fromEntity)
                .collect(Collectors.toList());
    }

    // ===== INSTANCE MANAGEMENT =====

    /**
     * Tự động tạo instances cho template
     */
    @Transactional
    public List<ScheduleInstanceDTO> generateInstancesForTemplate(WeeklySchedule template) {
        log.info("📅 Generating instances for template: {}", template.getMaTemplate());

        // Get semester info
        HocKy hocKy = getHocKyForTemplate(template);
        if (hocKy == null || !hocKy.isWeekBasedConfig()) {
            throw new BusinessException("Template phải thuộc học kỳ có cấu hình week-based");
        }

        List<ScheduleInstance> instances = new ArrayList<>();

        // Generate instances for each applicable week
        for (int tuanHoc = template.getTuanBatDau(); tuanHoc <= template.getTuanKetThuc(); tuanHoc++) {
            LocalDate ngayHoc = calculateDateForWeek(hocKy, tuanHoc, template.getThu());

            if (ngayHoc != null) {
                ScheduleInstance instance = ScheduleInstance.fromTemplate(template, tuanHoc, ngayHoc);
                instance.setCreatedBy(template.getUpdatedBy());
                instances.add(instance);
            }
        }

        // Save instances
        instances = scheduleInstanceRepository.saveAll(instances);
        log.info("✅ Generated {} instances for template: {}", instances.size(), template.getMaTemplate());

        return instances.stream()
                .map(ScheduleInstanceDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Tái tạo instances khi template thay đổi
     */
    @Transactional
    public void regenerateInstancesForTemplate(WeeklySchedule template) {
        log.info("🔄 Regenerating instances for template: {}", template.getMaTemplate());

        // Deactivate existing instances
        List<ScheduleInstance> existingInstances = scheduleInstanceRepository
                .findByWeeklyScheduleMaTemplateAndIsActiveTrue(template.getMaTemplate());

        existingInstances.forEach(instance -> {
            if (instance.getTrangThai() == ScheduleInstance.TrangThaiInstance.SCHEDULED) {
                instance.setIsActive(false); // Only deactivate if not started yet
            }
        });
        scheduleInstanceRepository.saveAll(existingInstances);

        // Generate new instances
        generateInstancesForTemplate(template);
    }

    /**
     * Lấy instances theo template
     */
    public List<ScheduleInstanceDTO> getInstancesByTemplate(String maTemplate) {
        return scheduleInstanceRepository.findByWeeklyScheduleMaTemplateAndIsActiveTrue(maTemplate)
                .stream()
                .map(ScheduleInstanceDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Cập nhật instance cụ thể
     */
    @Transactional
    public ScheduleInstanceDTO updateInstance(String maInstance, ScheduleInstanceDTO dto) {
        log.info("📝 Updating instance: {}", maInstance);

        ScheduleInstance instance = scheduleInstanceRepository.findById(maInstance)
                .orElseThrow(() -> new ResourceNotFoundException("Instance not found: " + maInstance));

        // Update overrides
        if (dto.getTietBatDauOverride() != null) {
            instance.setTietBatDauOverride(dto.getTietBatDauOverride());
        }
        if (dto.getSoTietOverride() != null) {
            instance.setSoTietOverride(dto.getSoTietOverride());
        }
        if (dto.getMaPhongOverride() != null) {
            PhongHoc phong = phongHocRepository.findById(dto.getMaPhongOverride())
                    .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + dto.getMaPhongOverride()));
            instance.setPhongHocOverride(phong);
        }
        if (dto.getMaGvOverride() != null) {
            GiangVien gv = giangVienRepository.findById(dto.getMaGvOverride())
                    .orElseThrow(() -> new ResourceNotFoundException("Teacher not found: " + dto.getMaGvOverride()));
            instance.setGiangVienOverride(gv);
        }

        if (dto.getTrangThai() != null) {
            instance.setTrangThai(dto.getTrangThai());
        }
        if (dto.getGhiChu() != null) {
            instance.setGhiChu(dto.getGhiChu());
        }

        instance = scheduleInstanceRepository.save(instance);
        return ScheduleInstanceDTO.fromEntity(instance);
    }

    // ===== QUERY METHODS =====

    /**
     * Lấy lịch học theo ngày
     */
    public List<ScheduleInstanceDTO> getScheduleByDate(LocalDate date) {
        return scheduleInstanceRepository.findByNgayCuTheAndIsActiveTrueOrderByTuanHocAsc(date)
                .stream()
                .map(ScheduleInstanceDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Lấy lịch học theo tuần
     */
    public List<ScheduleInstanceDTO> getScheduleByWeek(Integer tuanHoc) {
        return scheduleInstanceRepository.findByTuanHocAndIsActiveTrueOrderByNgayCuTheAsc(tuanHoc)
                .stream()
                .map(ScheduleInstanceDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Lấy lịch học theo giảng viên
     */
    public List<ScheduleInstanceDTO> getScheduleByGiangVien(String maGv) {
        return scheduleInstanceRepository.findByGiangVien(maGv)
                .stream()
                .map(ScheduleInstanceDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Lấy lịch học hôm nay
     */
    public List<ScheduleInstanceDTO> getTodaySchedule() {
        LocalDate today = LocalDate.now();
        List<ScheduleInstance.TrangThaiInstance> validStates = Arrays.asList(
                ScheduleInstance.TrangThaiInstance.SCHEDULED,
                ScheduleInstance.TrangThaiInstance.CONFIRMED,
                ScheduleInstance.TrangThaiInstance.IN_PROGRESS
        );

        return scheduleInstanceRepository.findTodaySchedule(today, validStates)
                .stream()
                .map(ScheduleInstanceDTO::fromEntity)
                .collect(Collectors.toList());
    }

    // ===== VALIDATION METHODS =====

    private void validateTemplateData(WeeklyScheduleDTO dto) {
        if (!dto.isValidTemplate()) {
            throw new BusinessException("Dữ liệu template không hợp lệ");
        }

        // Check if LopHocPhan exists
        if (!lopHocPhanRepository.existsById(dto.getMaLhp())) {
            throw new ResourceNotFoundException("Lớp học phần không tồn tại: " + dto.getMaLhp());
        }

        // Check if room exists (if specified)
        if (dto.getMaPhongMacDinh() != null && !phongHocRepository.existsById(dto.getMaPhongMacDinh())) {
            throw new ResourceNotFoundException("Phòng học không tồn tại: " + dto.getMaPhongMacDinh());
        }
    }

    private void checkTemplateConflicts(WeeklyScheduleDTO dto, String excludeTemplate) {
        // Get LHP info for conflict checking
        LopHocPhan lhp = lopHocPhanRepository.findById(dto.getMaLhp()).orElse(null);
        if (lhp == null) return;

        String maGv = lhp.getGiangVien().getMaGv();
        Integer tietKetThuc = dto.getTietBatDau() + dto.getSoTiet() - 1;

        // Check teacher conflicts
        List<WeeklySchedule> teacherConflicts = weeklyScheduleRepository.findTeacherConflicts(
                maGv, dto.getThu(), dto.getTuanBatDau(), dto.getTuanKetThuc(),
                dto.getTietBatDau(), tietKetThuc, excludeTemplate);

        if (!teacherConflicts.isEmpty()) {
            throw new BusinessException("Giảng viên đã có lịch dạy trùng giờ trong khoảng thời gian này");
        }

        // Check room conflicts (if room is specified)
        if (dto.getMaPhongMacDinh() != null) {
            List<WeeklySchedule> roomConflicts = weeklyScheduleRepository.findRoomConflicts(
                    dto.getMaPhongMacDinh(), dto.getThu(), dto.getTuanBatDau(), dto.getTuanKetThuc(),
                    dto.getTietBatDau(), tietKetThuc, excludeTemplate);

            if (!roomConflicts.isEmpty()) {
                throw new BusinessException("Phòng học đã được sử dụng trong khoảng thời gian này");
            }
        }
    }

    // ===== HELPER METHODS =====

    private WeeklySchedule toEntity(WeeklyScheduleDTO dto) {
        WeeklySchedule entity = WeeklySchedule.builder()
                .thu(dto.getThu())
                .tietBatDau(dto.getTietBatDau())
                .soTiet(dto.getSoTiet())
                .tuanBatDau(dto.getTuanBatDau())
                .tuanKetThuc(dto.getTuanKetThuc())
                .moTa(dto.getMoTa())
                .loaiLich(dto.getLoaiLich())
                .createdBy(dto.getCreatedBy())
                .build();

        // Set relationships
        entity.setLopHocPhan(lopHocPhanRepository.findById(dto.getMaLhp()).orElse(null));

        if (dto.getMaPhongMacDinh() != null) {
            entity.setPhongHocMacDinh(phongHocRepository.findById(dto.getMaPhongMacDinh()).orElse(null));
        }

        return entity;
    }

    private void updateEntityFromDTO(WeeklySchedule entity, WeeklyScheduleDTO dto) {
        entity.setThu(dto.getThu());
        entity.setTietBatDau(dto.getTietBatDau());
        entity.setSoTiet(dto.getSoTiet());
        entity.setTuanBatDau(dto.getTuanBatDau());
        entity.setTuanKetThuc(dto.getTuanKetThuc());
        entity.setMoTa(dto.getMoTa());
        entity.setLoaiLich(dto.getLoaiLich());
        entity.setUpdatedBy(dto.getUpdatedBy());

        if (dto.getMaPhongMacDinh() != null) {
            entity.setPhongHocMacDinh(phongHocRepository.findById(dto.getMaPhongMacDinh()).orElse(null));
        }
    }

    private String generateTemplateId(WeeklyScheduleDTO dto) {
        return WeeklySchedule.generateTemplateId(dto.getMaLhp(), dto.getThu(), dto.getTietBatDau());
    }

    private HocKy getHocKyForTemplate(WeeklySchedule template) {
        if (template.getLopHocPhan() == null) return null;

        String hocKyCode = template.getLopHocPhan().getHocKy();
        HocKyDTO hocKyDTO = hocKyService.getById(hocKyCode);
        return hocKyDTO != null ? convertToHocKyEntity(hocKyDTO) : null;
    }

    private HocKy convertToHocKyEntity(HocKyDTO dto) {
        return HocKy.builder()
                .maHocKy(dto.getMaHocKy())
                .tuanBatDau(dto.getTuanBatDau())
                .soTuanHoc(dto.getSoTuanHoc())
                .ngayBatDauTuan1(dto.getNgayBatDauTuan1())
                .ngayBatDau(dto.getNgayBatDau())
                .ngayKetThuc(dto.getNgayKetThuc())
                .build();
    }

    private LocalDate calculateDateForWeek(HocKy hocKy, Integer tuanHoc, Integer thu) {
        if (!hocKy.isWeekBasedConfig()) return null;

        // Calculate the start date of the specified week
        LocalDate weekStart = hocKy.getNgayBatDauThucTe().plusWeeks(tuanHoc - 1);

        // Convert thu (2=Monday, 3=Tuesday, ..., 8=Sunday) to DayOfWeek
        DayOfWeek targetDay = DayOfWeek.of(thu == 8 ? 7 : thu - 1);

        // Find the target day in that week
        return weekStart.with(targetDay);
    }
}