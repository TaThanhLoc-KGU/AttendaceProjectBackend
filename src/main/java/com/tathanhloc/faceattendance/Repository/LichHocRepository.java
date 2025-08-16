package com.tathanhloc.faceattendance.Repository;

import com.tathanhloc.faceattendance.Model.DiemDanh;
import com.tathanhloc.faceattendance.Model.LichHoc;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface LichHocRepository extends JpaRepository<LichHoc, String> {
    List<LichHoc> findByLopHocPhanMaLhp(String maLhp);
    List<LichHoc> findByPhongHocMaPhong(String maPhong);
    List<LichHoc> findByThu(Integer thu);
    List<LichHoc> findByPhongHocMaPhongAndThuAndIsActiveTrue(String maPhong, Integer thu);

    // Đếm số lịch học theo lớp học phần
    long countByLopHocPhanMaLhp(String maLhp);

    /**
     * Đếm số buổi học trong tuần theo lớp học phần
     */
    @Query("SELECT COUNT(DISTINCT lh.thu) FROM LichHoc lh WHERE lh.lopHocPhan.maLhp = :maLhp")
    long countDistinctThuByLopHocPhanMaLhp(@Param("maLhp") String maLhp);

    /**
     * Lấy thông tin học kỳ của lớp học phần
     */
    @Query("SELECT DISTINCT new map(lhp.hocKy as hocKy, lhp.namHoc as namHoc) " +
            "FROM LichHoc lh JOIN lh.lopHocPhan lhp " +
            "WHERE lhp.maLhp = :maLhp")
    List<Map<String, Object>> findSemesterInfoByMaLhp(@Param("maLhp") String maLhp);

    /**
     * Tìm lịch học theo lớp học phần, học kỳ và năm học
     */
    @Query("SELECT lh FROM LichHoc lh JOIN lh.lopHocPhan lhp " +
            "WHERE lhp.maLhp = :maLhp AND lhp.hocKy = :hocKy AND lhp.namHoc = :namHoc")
    List<LichHoc> findByLopHocPhanMaLhpAndHocKyAndNamHoc(@Param("maLhp") String maLhp,
                                                         @Param("hocKy") String hocKy,
                                                         @Param("namHoc") String namHoc);

    /**
     * Tìm lịch học của giảng viên theo thứ trong tuần
     */
    @Query("SELECT lh FROM LichHoc lh " +
            "WHERE lh.lopHocPhan.giangVien.maGv = :maGv " +
            "AND lh.thu = :thu " +
            "AND lh.isActive = true")
    List<LichHoc> findByMaGvAndThu(@Param("maGv") String maGv, @Param("thu") Integer thu);

    /**
     * Tìm lịch học của giảng viên trong khoảng thời gian
     */
    @Query("SELECT lh FROM LichHoc lh " +
            "WHERE lh.lopHocPhan.giangVien.maGv = :maGv " +
            "AND lh.isActive = true " +
            "ORDER BY lh.thu, lh.tietBatDau")
    List<LichHoc> findByMaGvOrderByThuAndTiet(@Param("maGv") String maGv);

    /**
     * Tìm lịch học theo giảng viên và lớp học phần
     */
    @Query("SELECT lh FROM LichHoc lh " +
            "WHERE lh.lopHocPhan.giangVien.maGv = :maGv " +
            "AND lh.lopHocPhan.maLhp = :maLhp " +
            "AND lh.isActive = true")
    List<LichHoc> findByMaGvAndMaLhp(@Param("maGv") String maGv, @Param("maLhp") String maLhp);

    /**
     * Đếm số tiết dạy của giảng viên theo thứ
     */
    @Query("SELECT lh.thu, COUNT(lh) FROM LichHoc lh " +
            "WHERE lh.lopHocPhan.giangVien.maGv = :maGv " +
            "AND lh.isActive = true " +
            "GROUP BY lh.thu " +
            "ORDER BY lh.thu")
    List<Object[]> countByMaGvGroupByThu(@Param("maGv") String maGv);

    /**
     * Tìm lịch học có xung đột thời gian cho giảng viên
     */
    @Query("SELECT lh FROM LichHoc lh " +
            "WHERE lh.lopHocPhan.giangVien.maGv = :maGv " +
            "AND lh.thu = :thu " +
            "AND lh.isActive = true " +
            "AND ((lh.tietBatDau <= :tietBatDau AND (lh.tietBatDau + lh.soTiet - 1) >= :tietBatDau) " +
            "     OR (lh.tietBatDau <= :tietKetThuc AND (lh.tietBatDau + lh.soTiet - 1) >= :tietKetThuc) " +
            "     OR (lh.tietBatDau >= :tietBatDau AND (lh.tietBatDau + lh.soTiet - 1) <= :tietKetThuc))")
    List<LichHoc> findConflictingSchedules(@Param("maGv") String maGv,
                                           @Param("thu") Integer thu,
                                           @Param("tietBatDau") Integer tietBatDau,
                                           @Param("tietKetThuc") Integer tietKetThuc);

    /**
     * Thống kê giảng viên theo số tiết dạy
     */
    @Query("SELECT lh.lopHocPhan.giangVien.maGv, lh.lopHocPhan.giangVien.hoTen, " +
            "SUM(lh.soTiet) as totalPeriods, COUNT(DISTINCT lh.lopHocPhan.maLhp) as totalClasses " +
            "FROM LichHoc lh " +
            "WHERE lh.isActive = true " +
            "GROUP BY lh.lopHocPhan.giangVien.maGv, lh.lopHocPhan.giangVien.hoTen " +
            "ORDER BY totalPeriods DESC")
    List<Object[]> getTeacherWorkloadStats();
}
