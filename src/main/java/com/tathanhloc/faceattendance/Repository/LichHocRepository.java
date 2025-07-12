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
    /**
     * Đếm số lượng điểm danh theo mã lịch
     */
    long countByLichHocMaLich(String maLich);
    long countByLopHocPhanMaLhp(String maLhp);

    /**
     * Tìm điểm danh theo mã lịch và mã sinh viên
     */
    List<DiemDanh> findByLichHocMaLichAndSinhVienMaSv(String maLich, String maSv);

    /**
     * Tìm điểm danh theo sinh viên và khoảng thời gian
     */
    List<DiemDanh> findBySinhVienMaSvAndNgayDiemDanhBetween(String maSv, LocalDate fromDate, LocalDate toDate);

    /**
     * Đếm điểm danh theo danh sách mã lịch
     */
    long countByLichHocMaLichIn(List<String> maLichList);

    /**
     * Đếm điểm danh theo danh sách mã lịch và trạng thái
     */
    long countByLichHocMaLichInAndTrangThai(List<String> maLichList, String trangThai);

    /**
     * Tìm điểm danh theo sinh viên
     */
    List<DiemDanh> findBySinhVienMaSv(String maSv);

    /**
     * Tìm điểm danh theo lớp học phần và khoảng thời gian
     */
    @Query("SELECT dd FROM DiemDanh dd " +
            "JOIN dd.lichHoc lh " +
            "JOIN lh.lopHocPhan lhp " +
            "WHERE lhp.maLhp = :maLhp " +
            "AND dd.ngayDiemDanh BETWEEN :fromDate AND :toDate " +
            "ORDER BY dd.ngayDiemDanh DESC")
    List<DiemDanh> findByLopHocPhanAndDateRange(@Param("maLhp") String maLhp,
                                                @Param("fromDate") LocalDate fromDate,
                                                @Param("toDate") LocalDate toDate);
}
