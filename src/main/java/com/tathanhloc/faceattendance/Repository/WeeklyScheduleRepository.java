package com.tathanhloc.faceattendance.Repository;

import com.tathanhloc.faceattendance.Model.WeeklySchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository cho WeeklySchedule (Templates)
 */
@Repository
public interface WeeklyScheduleRepository extends JpaRepository<WeeklySchedule, String> {

    // ===== BASIC QUERIES =====
    List<WeeklySchedule> findByIsActiveTrueOrderByCreatedAtDesc();

    List<WeeklySchedule> findByTrangThaiOrderByCreatedAtDesc(WeeklySchedule.TrangThaiTemplate trangThai);

    // ===== LỚP HỌC PHẦN QUERIES =====
    List<WeeklySchedule> findByLopHocPhanMaLhpAndIsActiveTrue(String maLhp);

    List<WeeklySchedule> findByLopHocPhanHocKyAndIsActiveTrue(String hocKy);

    List<WeeklySchedule> findByLopHocPhanNamHocAndIsActiveTrue(String namHoc);

    @Query("SELECT ws FROM WeeklySchedule ws WHERE ws.lopHocPhan.hocKy = :hocKy " +
            "AND ws.lopHocPhan.namHoc = :namHoc AND ws.isActive = true")
    List<WeeklySchedule> findByHocKyAndNamHoc(@Param("hocKy") String hocKy,
                                              @Param("namHoc") String namHoc);

    // ===== GIẢNG VIÊN QUERIES =====
    @Query("SELECT ws FROM WeeklySchedule ws WHERE ws.lopHocPhan.giangVien.maGv = :maGv " +
            "AND ws.isActive = true ORDER BY ws.thu, ws.tietBatDau")
    List<WeeklySchedule> findByGiangVien(@Param("maGv") String maGv);

    @Query("SELECT ws FROM WeeklySchedule ws WHERE ws.lopHocPhan.giangVien.maGv = :maGv " +
            "AND ws.thu = :thu AND ws.isActive = true")
    List<WeeklySchedule> findByGiangVienAndThu(@Param("maGv") String maGv, @Param("thu") Integer thu);

    // ===== PHÒNG HỌC QUERIES =====
    List<WeeklySchedule> findByPhongHocMacDinhMaPhongAndIsActiveTrue(String maPhong);

    @Query("SELECT ws FROM WeeklySchedule ws WHERE ws.phongHocMacDinh.maPhong = :maPhong " +
            "AND ws.thu = :thu AND ws.isActive = true")
    List<WeeklySchedule> findByPhongAndThu(@Param("maPhong") String maPhong, @Param("thu") Integer thu);

    // ===== THỜI GIAN QUERIES =====
    List<WeeklySchedule> findByThuAndIsActiveTrueOrderByTietBatDau(Integer thu);

    @Query("SELECT ws FROM WeeklySchedule ws WHERE ws.thu = :thu " +
            "AND ws.tietBatDau <= :tietKetThuc AND (ws.tietBatDau + ws.soTiet - 1) >= :tietBatDau " +
            "AND ws.isActive = true")
    List<WeeklySchedule> findConflictingTimeSlots(@Param("thu") Integer thu,
                                                  @Param("tietBatDau") Integer tietBatDau,
                                                  @Param("tietKetThuc") Integer tietKetThuc);

    // ===== TUẦN HỌC QUERIES =====
    @Query("SELECT ws FROM WeeklySchedule ws WHERE :tuanHoc >= ws.tuanBatDau " +
            "AND :tuanHoc <= ws.tuanKetThuc AND ws.isActive = true")
    List<WeeklySchedule> findByTuanHoc(@Param("tuanHoc") Integer tuanHoc);

    @Query("SELECT ws FROM WeeklySchedule ws WHERE ws.tuanBatDau <= :tuanKetThuc " +
            "AND ws.tuanKetThuc >= :tuanBatDau AND ws.isActive = true")
    List<WeeklySchedule> findByTuanRange(@Param("tuanBatDau") Integer tuanBatDau,
                                         @Param("tuanKetThuc") Integer tuanKetThuc);

    // ===== CONFLICT DETECTION =====
    @Query("SELECT ws FROM WeeklySchedule ws WHERE ws.lopHocPhan.giangVien.maGv = :maGv " +
            "AND ws.thu = :thu AND ws.tuanBatDau <= :tuanKetThuc AND ws.tuanKetThuc >= :tuanBatDau " +
            "AND ws.tietBatDau <= :tietKetThuc AND (ws.tietBatDau + ws.soTiet - 1) >= :tietBatDau " +
            "AND ws.isActive = true AND (:excludeTemplate IS NULL OR ws.maTemplate != :excludeTemplate)")
    List<WeeklySchedule> findTeacherConflicts(@Param("maGv") String maGv,
                                              @Param("thu") Integer thu,
                                              @Param("tuanBatDau") Integer tuanBatDau,
                                              @Param("tuanKetThuc") Integer tuanKetThuc,
                                              @Param("tietBatDau") Integer tietBatDau,
                                              @Param("tietKetThuc") Integer tietKetThuc,
                                              @Param("excludeTemplate") String excludeTemplate);

    @Query("SELECT ws FROM WeeklySchedule ws WHERE ws.phongHocMacDinh.maPhong = :maPhong " +
            "AND ws.thu = :thu AND ws.tuanBatDau <= :tuanKetThuc AND ws.tuanKetThuc >= :tuanBatDau " +
            "AND ws.tietBatDau <= :tietKetThuc AND (ws.tietBatDau + ws.soTiet - 1) >= :tietBatDau " +
            "AND ws.isActive = true AND (:excludeTemplate IS NULL OR ws.maTemplate != :excludeTemplate)")
    List<WeeklySchedule> findRoomConflicts(@Param("maPhong") String maPhong,
                                           @Param("thu") Integer thu,
                                           @Param("tuanBatDau") Integer tuanBatDau,
                                           @Param("tuanKetThuc") Integer tuanKetThuc,
                                           @Param("tietBatDau") Integer tietBatDau,
                                           @Param("tietKetThuc") Integer tietKetThuc,
                                           @Param("excludeTemplate") String excludeTemplate);

    // ===== STATISTICS =====
    @Query("SELECT COUNT(ws) FROM WeeklySchedule ws WHERE ws.isActive = true")
    long countActiveTemplates();

    @Query("SELECT ws.loaiLich, COUNT(ws) FROM WeeklySchedule ws WHERE ws.isActive = true GROUP BY ws.loaiLich")
    List<Object[]> countByLoaiLich();

    @Query("SELECT ws.trangThai, COUNT(ws) FROM WeeklySchedule ws GROUP BY ws.trangThai")
    List<Object[]> countByTrangThai();

    // ===== CUSTOM QUERIES =====
    @Query("SELECT DISTINCT ws.lopHocPhan.hocKy FROM WeeklySchedule ws WHERE ws.isActive = true ORDER BY ws.lopHocPhan.hocKy")
    List<String> findDistinctHocKy();

    @Query("SELECT ws FROM WeeklySchedule ws WHERE ws.createdBy = :userId AND ws.isActive = true")
    List<WeeklySchedule> findByCreatedBy(@Param("userId") String userId);
}