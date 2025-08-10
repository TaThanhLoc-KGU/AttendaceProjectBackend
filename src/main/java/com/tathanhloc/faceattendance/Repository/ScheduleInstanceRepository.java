package com.tathanhloc.faceattendance.Repository;

import com.tathanhloc.faceattendance.Model.ScheduleInstance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository cho ScheduleInstance (Instances)
 */
@Repository
public interface ScheduleInstanceRepository extends JpaRepository<ScheduleInstance, String> {

    // ===== BASIC QUERIES =====
    List<ScheduleInstance> findByIsActiveTrueOrderByNgayCuTheAscTuanHocAsc();

    List<ScheduleInstance> findByTrangThaiOrderByNgayCuTheDesc(ScheduleInstance.TrangThaiInstance trangThai);

    // ===== TEMPLATE QUERIES =====
    List<ScheduleInstance> findByWeeklyScheduleMaTemplateAndIsActiveTrue(String maTemplate);

    @Query("SELECT COUNT(si) FROM ScheduleInstance si WHERE si.weeklySchedule.maTemplate = :maTemplate AND si.isActive = true")
    long countByWeeklyScheduleMaTemplateAndIsActiveTrue(@Param("maTemplate") String maTemplate);

    @Query("SELECT si FROM ScheduleInstance si WHERE si.weeklySchedule.maTemplate = :maTemplate " +
            "AND si.tuanHoc = :tuanHoc AND si.isActive = true")
    List<ScheduleInstance> findByTemplateAndTuan(@Param("maTemplate") String maTemplate,
                                                 @Param("tuanHoc") Integer tuanHoc);

    // ===== NGÀY QUERIES =====
    List<ScheduleInstance> findByNgayCuTheAndIsActiveTrueOrderByTuanHocAsc(LocalDate ngayCuThe);

    @Query("SELECT si FROM ScheduleInstance si WHERE si.ngayCuThe >= :startDate " +
            "AND si.ngayCuThe <= :endDate AND si.isActive = true ORDER BY si.ngayCuThe, si.tuanHoc")
    List<ScheduleInstance> findByDateRange(@Param("startDate") LocalDate startDate,
                                           @Param("endDate") LocalDate endDate);

    // ===== TUẦN QUERIES =====
    List<ScheduleInstance> findByTuanHocAndIsActiveTrueOrderByNgayCuTheAsc(Integer tuanHoc);

    @Query("SELECT si FROM ScheduleInstance si WHERE si.tuanHoc >= :tuanBatDau " +
            "AND si.tuanHoc <= :tuanKetThuc AND si.isActive = true")
    List<ScheduleInstance> findByTuanRange(@Param("tuanBatDau") Integer tuanBatDau,
                                           @Param("tuanKetThuc") Integer tuanKetThuc);

    // ===== LỚP HỌC PHẦN QUERIES =====
    @Query("SELECT si FROM ScheduleInstance si WHERE si.weeklySchedule.lopHocPhan.maLhp = :maLhp " +
            "AND si.isActive = true ORDER BY si.tuanHoc, si.ngayCuThe")
    List<ScheduleInstance> findByLopHocPhan(@Param("maLhp") String maLhp);

    @Query("SELECT si FROM ScheduleInstance si WHERE si.weeklySchedule.lopHocPhan.hocKy = :hocKy " +
            "AND si.isActive = true ORDER BY si.tuanHoc, si.ngayCuThe")
    List<ScheduleInstance> findByHocKy(@Param("hocKy") String hocKy);

    // ===== GIẢNG VIÊN QUERIES =====
    @Query("SELECT si FROM ScheduleInstance si WHERE " +
            "(si.giangVienOverride.maGv = :maGv OR " +
            "(si.giangVienOverride IS NULL AND si.weeklySchedule.lopHocPhan.giangVien.maGv = :maGv)) " +
            "AND si.isActive = true ORDER BY si.ngayCuThe")
    List<ScheduleInstance> findByGiangVien(@Param("maGv") String maGv);

    @Query("SELECT si FROM ScheduleInstance si WHERE " +
            "(si.giangVienOverride.maGv = :maGv OR " +
            "(si.giangVienOverride IS NULL AND si.weeklySchedule.lopHocPhan.giangVien.maGv = :maGv)) " +
            "AND si.ngayCuThe = :ngayCuThe AND si.isActive = true")
    List<ScheduleInstance> findByGiangVienAndDate(@Param("maGv") String maGv,
                                                  @Param("ngayCuThe") LocalDate ngayCuThe);

    // ===== PHÒNG HỌC QUERIES =====
    @Query("SELECT si FROM ScheduleInstance si WHERE " +
            "(si.phongHocOverride.maPhong = :maPhong OR " +
            "(si.phongHocOverride IS NULL AND si.weeklySchedule.phongHocMacDinh.maPhong = :maPhong)) " +
            "AND si.isActive = true ORDER BY si.ngayCuThe")
    List<ScheduleInstance> findByPhongHoc(@Param("maPhong") String maPhong);

    @Query("SELECT si FROM ScheduleInstance si WHERE " +
            "(si.phongHocOverride.maPhong = :maPhong OR " +
            "(si.phongHocOverride IS NULL AND si.weeklySchedule.phongHocMacDinh.maPhong = :maPhong)) " +
            "AND si.ngayCuThe = :ngayCuThe AND si.isActive = true")
    List<ScheduleInstance> findByPhongHocAndDate(@Param("maPhong") String maPhong,
                                                 @Param("ngayCuThe") LocalDate ngayCuThe);

    // ===== INSTANCE CONFLICT DETECTION =====
    @Query("SELECT si FROM ScheduleInstance si WHERE si.ngayCuThe = :ngayCuThe " +
            "AND ((si.tietBatDauOverride IS NOT NULL AND si.soTietOverride IS NOT NULL " +
            "AND si.tietBatDauOverride <= :tietKetThuc AND (si.tietBatDauOverride + si.soTietOverride - 1) >= :tietBatDau) " +
            "OR (si.tietBatDauOverride IS NULL AND si.soTietOverride IS NULL " +
            "AND si.weeklySchedule.tietBatDau <= :tietKetThuc AND (si.weeklySchedule.tietBatDau + si.weeklySchedule.soTiet - 1) >= :tietBatDau)) " +
            "AND si.isActive = true AND (:excludeInstance IS NULL OR si.maInstance != :excludeInstance)")
    List<ScheduleInstance> findTimeConflicts(@Param("ngayCuThe") LocalDate ngayCuThe,
                                             @Param("tietBatDau") Integer tietBatDau,
                                             @Param("tietKetThuc") Integer tietKetThuc,
                                             @Param("excludeInstance") String excludeInstance);

    // ===== STATISTICS =====
    @Query("SELECT COUNT(si) FROM ScheduleInstance si WHERE si.isActive = true")
    long countActiveInstances();

    @Query("SELECT si.trangThai, COUNT(si) FROM ScheduleInstance si WHERE si.isActive = true GROUP BY si.trangThai")
    List<Object[]> countByTrangThai();

    @Query("SELECT si.tuanHoc, COUNT(si) FROM ScheduleInstance si WHERE si.isActive = true GROUP BY si.tuanHoc ORDER BY si.tuanHoc")
    List<Object[]> countByTuanHoc();

    // ===== TODAY'S SCHEDULE =====
    @Query("SELECT si FROM ScheduleInstance si WHERE si.ngayCuThe = :today " +
            "AND si.trangThai IN :validStates AND si.isActive = true " +
            "ORDER BY COALESCE(si.tietBatDauOverride, si.weeklySchedule.tietBatDau)")
    List<ScheduleInstance> findTodaySchedule(@Param("today") LocalDate today,
                                             @Param("validStates") List<ScheduleInstance.TrangThaiInstance> validStates);

    // ===== ATTENDANCE READY =====
    @Query("SELECT si FROM ScheduleInstance si WHERE si.ngayCuThe = :ngayCuThe " +
            "AND si.trangThai IN ('SCHEDULED', 'CONFIRMED', 'IN_PROGRESS') " +
            "AND si.isActive = true")
    List<ScheduleInstance> findAttendanceReadyInstances(@Param("ngayCuThe") LocalDate ngayCuThe);

    // ===== CLEANUP QUERIES =====
    @Query("SELECT si FROM ScheduleInstance si WHERE si.ngayCuThe < :cutoffDate " +
            "AND si.trangThai = 'SCHEDULED' AND si.isActive = true")
    List<ScheduleInstance> findExpiredScheduledInstances(@Param("cutoffDate") LocalDate cutoffDate);
}