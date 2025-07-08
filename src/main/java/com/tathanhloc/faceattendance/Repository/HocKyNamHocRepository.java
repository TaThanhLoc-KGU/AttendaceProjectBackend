package com.tathanhloc.faceattendance.Repository;

import com.tathanhloc.faceattendance.Model.HocKyNamHoc;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HocKyNamHocRepository extends JpaRepository<HocKyNamHoc, Integer> {

    /**
     * Tìm tất cả relationship theo mã năm học
     */
    List<HocKyNamHoc> findByNamHoc_MaNamHoc(String maNamHoc);

    /**
     * Tìm tất cả relationship đang hoạt động theo mã năm học
     */
    List<HocKyNamHoc> findByNamHoc_MaNamHocAndIsActive(String maNamHoc, Boolean isActive);

    /**
     * Tìm tất cả relationship theo mã học kỳ
     */
    List<HocKyNamHoc> findByHocKy_MaHocKy(String maHocKy);

    /**
     * Tìm tất cả relationship đang hoạt động theo mã học kỳ
     */
    List<HocKyNamHoc> findByHocKy_MaHocKyAndIsActive(String maHocKy, Boolean isActive);

    /**
     * Tìm relationship cụ thể giữa học kỳ và năm học
     */
    Optional<HocKyNamHoc> findByHocKy_MaHocKyAndNamHoc_MaNamHoc(String maHocKy, String maNamHoc);

    /**
     * Kiểm tra tồn tại relationship
     */
    boolean existsByHocKy_MaHocKyAndNamHoc_MaNamHoc(String maHocKy, String maNamHoc);

    /**
     * Đếm số lượng học kỳ của một năm học
     */
    @Query("SELECT COUNT(h) FROM HocKyNamHoc h WHERE h.namHoc.maNamHoc = :maNamHoc AND h.isActive = true")
    long countActiveSemestersByYear(@Param("maNamHoc") String maNamHoc);

    /**
     * Lấy danh sách học kỳ theo năm học với thông tin chi tiết
     */
    @Query("SELECT h FROM HocKyNamHoc h " +
            "JOIN FETCH h.hocKy hk " +
            "JOIN FETCH h.namHoc nh " +
            "WHERE h.namHoc.maNamHoc = :maNamHoc AND h.isActive = true " +
            "ORDER BY hk.ngayBatDau ASC")
    List<HocKyNamHoc> findSemestersWithDetailsByYear(@Param("maNamHoc") String maNamHoc);

    /**
     * Lấy danh sách năm học theo học kỳ với thông tin chi tiết
     */
    @Query("SELECT h FROM HocKyNamHoc h " +
            "JOIN FETCH h.hocKy hk " +
            "JOIN FETCH h.namHoc nh " +
            "WHERE h.hocKy.maHocKy = :maHocKy AND h.isActive = true")
    List<HocKyNamHoc> findYearsWithDetailsBySemester(@Param("maHocKy") String maHocKy);

    /**
     * Xóa mềm tất cả relationship của một năm học
     */
    @Query("UPDATE HocKyNamHoc h SET h.isActive = false WHERE h.namHoc.maNamHoc = :maNamHoc")
    void softDeleteByYear(@Param("maNamHoc") String maNamHoc);

    /**
     * Xóa mềm tất cả relationship của một học kỳ
     */
    @Query("UPDATE HocKyNamHoc h SET h.isActive = false WHERE h.hocKy.maHocKy = :maHocKy")
    void softDeleteBySemester(@Param("maHocKy") String maHocKy);

    /**
     * Khôi phục tất cả relationship của một năm học
     */
    @Query("UPDATE HocKyNamHoc h SET h.isActive = true WHERE h.namHoc.maNamHoc = :maNamHoc")
    void restoreByYear(@Param("maNamHoc") String maNamHoc);

    /**
     * Tìm tất cả relationship đang hoạt động
     */
    List<HocKyNamHoc> findByIsActive(Boolean isActive);
}