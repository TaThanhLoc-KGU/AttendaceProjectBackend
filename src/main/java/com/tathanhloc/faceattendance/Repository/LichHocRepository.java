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
}
