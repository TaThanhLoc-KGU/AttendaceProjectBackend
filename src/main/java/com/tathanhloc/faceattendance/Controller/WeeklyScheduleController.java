package com.tathanhloc.faceattendance.Controller;

import com.tathanhloc.faceattendance.DTO.ScheduleInstanceDTO;
import com.tathanhloc.faceattendance.DTO.TimetableSlotDTO;
import com.tathanhloc.faceattendance.DTO.WeeklyScheduleBulkDTO;
import com.tathanhloc.faceattendance.DTO.WeeklyScheduleDTO;
import com.tathanhloc.faceattendance.Service.WeeklyScheduleService;
import com.tathanhloc.faceattendance.Exception.BusinessException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/weekly-schedule")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class WeeklyScheduleController {

    private final WeeklyScheduleService weeklyScheduleService;

    // ===== TEMPLATE MANAGEMENT =====

    /**
     * Tạo template lịch học mới
     */
    @PostMapping("/templates")
    public ResponseEntity<?> createTemplate(@Valid @RequestBody WeeklyScheduleDTO dto) {
        log.info("API: Creating weekly schedule template for LHP: {}", dto.getMaLhp());

        try {
            WeeklyScheduleDTO created = weeklyScheduleService.createTemplate(dto);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Tạo template lịch học thành công!",
                    "data", created
            ));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error creating template: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi tạo template: " + e.getMessage()
            ));
        }
    }

    /**
     * Cập nhật template
     */
    @PutMapping("/templates/{maTemplate}")
    public ResponseEntity<?> updateTemplate(@PathVariable String maTemplate,
                                            @Valid @RequestBody WeeklyScheduleDTO dto) {
        log.info("API: Updating template: {}", maTemplate);

        try {
            WeeklyScheduleDTO updated = weeklyScheduleService.updateTemplate(maTemplate, dto);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Cập nhật template thành công!",
                    "data", updated
            ));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error updating template: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi cập nhật template: " + e.getMessage()
            ));
        }
    }

    /**
     * Kích hoạt template và tạo instances
     */
    @PostMapping("/templates/{maTemplate}/activate")
    public ResponseEntity<?> activateTemplate(@PathVariable String maTemplate,
                                              @RequestParam(required = false) String activatedBy) {
        log.info("API: Activating template: {}", maTemplate);

        try {
            WeeklyScheduleDTO activated = weeklyScheduleService.activateTemplate(maTemplate, activatedBy);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Kích hoạt template và tạo lịch học thành công!",
                    "data", activated
            ));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error activating template: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi kích hoạt template: " + e.getMessage()
            ));
        }
    }

    /**
     * Lấy thông tin template
     */
    @GetMapping("/templates/{maTemplate}")
    public ResponseEntity<WeeklyScheduleDTO> getTemplate(@PathVariable String maTemplate) {
        log.info("API: Getting template: {}", maTemplate);
        return ResponseEntity.ok(weeklyScheduleService.getTemplate(maTemplate));
    }

    /**
     * Lấy templates theo lớp học phần
     */
    @GetMapping("/templates/by-lhp/{maLhp}")
    public ResponseEntity<List<WeeklyScheduleDTO>> getTemplatesByLopHocPhan(@PathVariable String maLhp) {
        log.info("API: Getting templates for LHP: {}", maLhp);
        return ResponseEntity.ok(weeklyScheduleService.getTemplatesByLopHocPhan(maLhp));
    }

    /**
     * Lấy templates theo học kỳ
     */
    @GetMapping("/templates/by-semester/{hocKy}")
    public ResponseEntity<List<WeeklyScheduleDTO>> getTemplatesByHocKy(@PathVariable String hocKy) {
        log.info("API: Getting templates for semester: {}", hocKy);
        return ResponseEntity.ok(weeklyScheduleService.getTemplatesByHocKy(hocKy));
    }

    // ===== INSTANCE MANAGEMENT =====

    /**
     * Lấy instances của một template
     */
    @GetMapping("/templates/{maTemplate}/instances")
    public ResponseEntity<List<ScheduleInstanceDTO>> getInstancesByTemplate(@PathVariable String maTemplate) {
        log.info("API: Getting instances for template: {}", maTemplate);
        return ResponseEntity.ok(weeklyScheduleService.getInstancesByTemplate(maTemplate));
    }

    /**
     * Cập nhật instance cụ thể
     */
    @PutMapping("/instances/{maInstance}")
    public ResponseEntity<?> updateInstance(@PathVariable String maInstance,
                                            @Valid @RequestBody ScheduleInstanceDTO dto) {
        log.info("API: Updating instance: {}", maInstance);

        try {
            ScheduleInstanceDTO updated = weeklyScheduleService.updateInstance(maInstance, dto);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Cập nhật lịch học thành công!",
                    "data", updated
            ));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error updating instance: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi cập nhật lịch học: " + e.getMessage()
            ));
        }
    }

    // ===== SCHEDULE QUERIES =====

    /**
     * Lấy lịch học theo ngày
     */
    @GetMapping("/by-date/{date}")
    public ResponseEntity<List<ScheduleInstanceDTO>> getScheduleByDate(
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        log.info("API: Getting schedule for date: {}", date);
        return ResponseEntity.ok(weeklyScheduleService.getScheduleByDate(date));
    }

    /**
     * Lấy lịch học theo tuần
     */
    @GetMapping("/by-week/{tuanHoc}")
    public ResponseEntity<List<ScheduleInstanceDTO>> getScheduleByWeek(@PathVariable Integer tuanHoc) {
        log.info("API: Getting schedule for week: {}", tuanHoc);
        return ResponseEntity.ok(weeklyScheduleService.getScheduleByWeek(tuanHoc));
    }

    /**
     * Lấy lịch học theo giảng viên
     */
    @GetMapping("/by-teacher/{maGv}")
    public ResponseEntity<List<ScheduleInstanceDTO>> getScheduleByTeacher(@PathVariable String maGv) {
        log.info("API: Getting schedule for teacher: {}", maGv);
        return ResponseEntity.ok(weeklyScheduleService.getScheduleByGiangVien(maGv));
    }

    /**
     * Lấy lịch học hôm nay
     */
    @GetMapping("/today")
    public ResponseEntity<List<ScheduleInstanceDTO>> getTodaySchedule() {
        log.info("API: Getting today's schedule");
        return ResponseEntity.ok(weeklyScheduleService.getTodaySchedule());
    }

    // ===== BULK OPERATIONS =====

    /**
     * Tạo lịch học hàng loạt cho nhiều lớp
     */
    @PostMapping("/bulk-create")
    public ResponseEntity<?> bulkCreateSchedules(@Valid @RequestBody WeeklyScheduleBulkDTO bulkDto) {
        log.info("API: Bulk creating schedules for {} classes", bulkDto.getMaLhpList().size());

        try {
            Map<String, Object> result = new HashMap<>();
            List<WeeklyScheduleDTO> createdTemplates = new ArrayList<>();
            List<String> errors = new ArrayList<>();

            // Process each class
            for (String maLhp : bulkDto.getMaLhpList()) {
                for (TimetableSlotDTO slot : bulkDto.getTimeSlots()) {
                    try {
                        WeeklyScheduleDTO template = WeeklyScheduleDTO.builder()
                                .maLhp(maLhp)
                                .thu(slot.getThu())
                                .tietBatDau(slot.getTietBatDau())
                                .soTiet(slot.getSoTiet())
                                .tuanBatDau(bulkDto.getTuanBatDau())
                                .tuanKetThuc(bulkDto.getTuanKetThuc())
                                .maPhongMacDinh(slot.getMaPhong())
                                .loaiLich(slot.getLoaiLich())
                                .createdBy(bulkDto.getCreatedBy())
                                .build();

                        WeeklyScheduleDTO created = weeklyScheduleService.createTemplate(template);

                        // Auto-activate if specified
                        if (Boolean.TRUE.equals(bulkDto.getAutoActivate())) {
                            weeklyScheduleService.activateTemplate(created.getMaTemplate(), bulkDto.getCreatedBy());
                        }

                        createdTemplates.add(created);

                    } catch (Exception e) {
                        errors.add(String.format("Lỗi tạo lịch cho lớp %s, slot %s: %s",
                                maLhp, slot.getThu() + "-" + slot.getTietBatDau(), e.getMessage()));
                    }
                }
            }

            result.put("success", true);
            result.put("message", String.format("Đã tạo %d template thành công", createdTemplates.size()));
            result.put("createdTemplates", createdTemplates);

            if (!errors.isEmpty()) {
                result.put("errors", errors);
                result.put("message", result.get("message") + String.format(", %d lỗi", errors.size()));
            }

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Error in bulk create: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi tạo lịch hàng loạt: " + e.getMessage()
            ));
        }
    }

    /**
     * Copy template từ học kỳ khác
     */
    @PostMapping("/copy-from-semester")
    public ResponseEntity<?> copyFromSemester(@RequestParam String fromHocKy,
                                              @RequestParam String toHocKy,
                                              @RequestParam(required = false) String copiedBy) {
        log.info("API: Copying templates from {} to {}", fromHocKy, toHocKy);

        try {
            List<WeeklyScheduleDTO> sourceTemplates = weeklyScheduleService.getTemplatesByHocKy(fromHocKy);
            List<WeeklyScheduleDTO> copiedTemplates = new ArrayList<>();

            for (WeeklyScheduleDTO source : sourceTemplates) {
                try {
                    // Update semester info for target
                    WeeklyScheduleDTO target = WeeklyScheduleDTO.builder()
                            .maLhp(source.getMaLhp().replace(fromHocKy, toHocKy)) // Assume LHP code contains semester
                            .thu(source.getThu())
                            .tietBatDau(source.getTietBatDau())
                            .soTiet(source.getSoTiet())
                            .tuanBatDau(source.getTuanBatDau())
                            .tuanKetThuc(source.getTuanKetThuc())
                            .maPhongMacDinh(source.getMaPhongMacDinh())
                            .loaiLich(source.getLoaiLich())
                            .moTa("Copy từ " + fromHocKy)
                            .createdBy(copiedBy)
                            .build();

                    WeeklyScheduleDTO copied = weeklyScheduleService.createTemplate(target);
                    copiedTemplates.add(copied);

                } catch (Exception e) {
                    log.warn("Failed to copy template {} from {} to {}: {}",
                            source.getMaTemplate(), fromHocKy, toHocKy, e.getMessage());
                }
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", String.format("Đã copy %d/%d template từ %s sang %s",
                            copiedTemplates.size(), sourceTemplates.size(), fromHocKy, toHocKy),
                    "data", copiedTemplates
            ));

        } catch (Exception e) {
            log.error("Error copying templates: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi copy template: " + e.getMessage()
            ));
        }
    }

    // ===== STATISTICS & REPORTS =====

    /**
     * Lấy thống kê lịch học
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        log.info("API: Getting schedule statistics");

        try {
            Map<String, Object> stats = new HashMap<>();

            // Basic counts (these would need to be implemented in service)
            stats.put("totalTemplates", 0);
            stats.put("activeTemplates", 0);
            stats.put("totalInstances", 0);
            stats.put("todaySchedules", weeklyScheduleService.getTodaySchedule().size());

            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            log.error("Error getting statistics: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi lấy thống kê: " + e.getMessage()
            ));
        }
    }

    /**
     * Validate template trước khi tạo
     */
    @PostMapping("/validate-template")
    public ResponseEntity<?> validateTemplate(@Valid @RequestBody WeeklyScheduleDTO dto) {
        log.info("API: Validating template for LHP: {}", dto.getMaLhp());

        try {
            Map<String, Object> validation = new HashMap<>();
            validation.put("isValid", dto.isValidTemplate());
            validation.put("conflicts", new ArrayList<>()); // This would check for conflicts
            validation.put("warnings", new ArrayList<>());
            validation.put("suggestions", new ArrayList<>());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", validation
            ));

        } catch (Exception e) {
            log.error("Error validating template: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi validate template: " + e.getMessage()
            ));
        }
    }
}