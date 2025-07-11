package com.tathanhloc.faceattendance.Repository;

import com.tathanhloc.faceattendance.Model.LopHocPhan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LopHocPhanRepository extends JpaRepository<LopHocPhan, String> {
    List<LopHocPhan> findByMonHocMaMh(String maMh);
    List<LopHocPhan> findByGiangVienMaGv(String maGv);


    // Bổ sung methods mới cho giảng viên

    /**
     * Tìm lớp học phần theo giảng viên và trạng thái hoạt động
     */
    List<LopHocPhan> findByGiangVienMaGvAndIsActive(String maGv, Boolean isActive);

    /**
     * Tìm lớp học phần theo giảng viên và học kỳ
     */
    List<LopHocPhan> findByGiangVienMaGvAndHocKy(String maGv, String hocKy);

    /**
     * Tìm lớp học phần theo giảng viên và năm học
     */
    List<LopHocPhan> findByGiangVienMaGvAndNamHoc(String maGv, String namHoc);

    /**
     * Tìm lớp học phần theo giảng viên, học kỳ và năm học
     */
    List<LopHocPhan> findByGiangVienMaGvAndHocKyAndNamHoc(String maGv, String hocKy, String namHoc);

    /**
     * Tìm lớp học phần đang hoạt động theo giảng viên, học kỳ và năm học
     */
    List<LopHocPhan> findByGiangVienMaGvAndHocKyAndNamHocAndIsActive(String maGv, String hocKy, String namHoc, Boolean isActive);

    /**
     * Đếm số lượng lớp học phần theo giảng viên
     */
    long countByGiangVienMaGv(String maGv);

    /**
     * Đếm số lượng lớp học phần đang hoạt động theo giảng viên
     */
    long countByGiangVienMaGvAndIsActive(String maGv, Boolean isActive);

    /**
     * Lấy danh sách môn học (không trùng lặp) mà giảng viên đã/đang dạy
     */
    @Query("SELECT DISTINCT lhp.monHoc.maMh FROM LopHocPhan lhp WHERE lhp.giangVien.maGv = :maGv")
    List<String> findDistinctMonHocByGiangVien(@Param("maGv") String maGv);

    /**
     * Lấy tổng số sinh viên của tất cả lớp mà giảng viên dạy
     */
    @Query("SELECT COALESCE(SUM(SIZE(lhp.sinhViens)), 0) FROM LopHocPhan lhp WHERE lhp.giangVien.maGv = :maGv")
    Long countTotalStudentsByGiangVien(@Param("maGv") String maGv);

    /**
     * Lấy tổng số sinh viên của các lớp đang hoạt động mà giảng viên dạy
     */
    @Query("SELECT COALESCE(SUM(SIZE(lhp.sinhViens)), 0) FROM LopHocPhan lhp WHERE lhp.giangVien.maGv = :maGv AND lhp.isActive = true")
    Long countActiveStudentsByGiangVien(@Param("maGv") String maGv);

    /**
     * Lấy danh sách lớp học phần với thông tin đầy đủ của giảng viên
     */
    @Query("SELECT lhp FROM LopHocPhan lhp " +
            "JOIN FETCH lhp.monHoc " +
            "JOIN FETCH lhp.giangVien " +
            "WHERE lhp.giangVien.maGv = :maGv " +
            "ORDER BY lhp.namHoc DESC, lhp.hocKy DESC, lhp.monHoc.tenMh ASC")
    List<LopHocPhan> findByGiangVienWithFullInfo(@Param("maGv") String maGv);

    /**
     * Tìm kiếm lớp học phần theo giảng viên và từ khóa (tên môn học, mã môn, mã lớp)
     */
    @Query("SELECT lhp FROM LopHocPhan lhp " +
            "JOIN lhp.monHoc mh " +
            "WHERE lhp.giangVien.maGv = :maGv " +
            "AND (LOWER(mh.tenMh) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(mh.maMh) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(lhp.maLhp) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<LopHocPhan> searchByGiangVienAndKeyword(@Param("maGv") String maGv, @Param("keyword") String keyword);
}
