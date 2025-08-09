package com.tathanhloc.faceattendance.Repository;

import com.tathanhloc.faceattendance.Model.HocKy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface HocKyRepository extends JpaRepository<HocKy, String> {

    // ===== EXISTING METHODS (Keep for backward compatibility) =====

    List<HocKy> findByIsActiveTrueOrderByNgayBatDauDesc();

    Optional<HocKy> findByIsCurrentTrue();

    List<HocKy> findByLoaiHocKy(HocKy.LoaiHocKy loaiHocKy);

    // ===== STATUS MANAGEMENT =====

    /**
     * Xóa flag current từ tất cả học kỳ
     */
    @Modifying
    @Query("UPDATE HocKy h SET h.isCurrent = false WHERE h.isCurrent = true")
    void clearAllCurrentFlags();

    /**
     * Đặt học kỳ làm current
     */
    @Modifying
    @Query("UPDATE HocKy h SET h.isCurrent = :isCurrent WHERE h.maHocKy = :maHocKy")
    void setCurrentFlag(@Param("maHocKy") String maHocKy, @Param("isCurrent") Boolean isCurrent);

    /**
     * Cập nhật trạng thái active
     */
    @Modifying
    @Query("UPDATE HocKy h SET h.isActive = :isActive WHERE h.maHocKy = :maHocKy")
    void updateActiveStatus(@Param("maHocKy") String maHocKy, @Param("isActive") Boolean isActive);

    // ===== DATE-BASED QUERIES (for backward compatibility) =====

    /**
     * Tìm học kỳ theo năm (dựa trên ngayBatDau)
     */
    @Query("SELECT h FROM HocKy h WHERE YEAR(h.ngayBatDau) = :year ORDER BY h.ngayBatDau")
    List<HocKy> findByYear(@Param("year") int year);

    /**
     * Tìm học kỳ trong khoảng thời gian
     */
    @Query("SELECT h FROM HocKy h WHERE h.isActive = true " +
            "AND NOT (h.ngayKetThuc < :startDate OR h.ngayBatDau > :endDate)")
    List<HocKy> findByDateRange(@Param("startDate") LocalDate startDate,
                                @Param("endDate") LocalDate endDate);

    /**
     * Tìm học kỳ xung đột thời gian (dựa trên ngày)
     */
    @Query("SELECT h FROM HocKy h WHERE h.isActive = true " +
            "AND (:excludeMaHocKy IS NULL OR h.maHocKy != :excludeMaHocKy) " +
            "AND NOT (h.ngayKetThuc < :startDate OR h.ngayBatDau > :endDate)")
    List<HocKy> findConflictingSemesters(@Param("startDate") LocalDate startDate,
                                         @Param("endDate") LocalDate endDate,
                                         @Param("excludeMaHocKy") String excludeMaHocKy);

    /**
     * Tìm học kỳ đang diễn ra tại thời điểm hiện tại
     */
    @Query("SELECT h FROM HocKy h WHERE h.isActive = true " +
            "AND :currentDate >= h.ngayBatDau " +
            "AND :currentDate <= h.ngayKetThuc")
    List<HocKy> findOngoingSemesters(@Param("currentDate") LocalDate currentDate);

    /**
     * Tìm học kỳ sắp tới
     */
    @Query("SELECT h FROM HocKy h WHERE h.isActive = true " +
            "AND :currentDate < h.ngayBatDau " +
            "ORDER BY h.ngayBatDau")
    List<HocKy> findUpcomingSemesters(@Param("currentDate") LocalDate currentDate);

    /**
     * Tìm học kỳ đã kết thúc
     */
    @Query("SELECT h FROM HocKy h WHERE h.isActive = true " +
            "AND :currentDate > h.ngayKetThuc " +
            "ORDER BY h.ngayBatDau DESC")
    List<HocKy> findFinishedSemesters(@Param("currentDate") LocalDate currentDate);

    // ===== WEEK-BASED QUERIES (for new functionality) =====

    /**
     * Tìm học kỳ có cấu hình week-based
     */
    @Query("SELECT h FROM HocKy h WHERE h.tuanBatDau IS NOT NULL " +
            "AND h.soTuanHoc IS NOT NULL AND h.ngayBatDauTuan1 IS NOT NULL " +
            "AND h.isActive = true ORDER BY h.ngayBatDauTuan1, h.tuanBatDau")
    List<HocKy> findWeekBasedSemesters();

    /**
     * Tìm học kỳ trong khoảng tuần (chỉ cho week-based)
     */
    @Query("SELECT h FROM HocKy h WHERE h.ngayBatDauTuan1 = :ngayBatDauTuan1 " +
            "AND h.tuanBatDau IS NOT NULL AND h.soTuanHoc IS NOT NULL " +
            "AND h.isActive = true " +
            "AND h.tuanBatDau <= :tuanKetThuc " +
            "AND (h.tuanBatDau + h.soTuanHoc - 1) >= :tuanBatDau")
    List<HocKy> findByWeekRange(@Param("ngayBatDauTuan1") LocalDate ngayBatDauTuan1,
                                @Param("tuanBatDau") Integer tuanBatDau,
                                @Param("tuanKetThuc") Integer tuanKetThuc);

    /**
     * Tìm học kỳ chứa tuần cụ thể (chỉ cho week-based)
     */
    @Query("SELECT h FROM HocKy h WHERE h.ngayBatDauTuan1 = :ngayBatDauTuan1 " +
            "AND h.tuanBatDau IS NOT NULL AND h.soTuanHoc IS NOT NULL " +
            "AND h.isActive = true " +
            "AND h.tuanBatDau <= :tuanTrongNam " +
            "AND (h.tuanBatDau + h.soTuanHoc - 1) >= :tuanTrongNam")
    List<HocKy> findContainingWeek(@Param("ngayBatDauTuan1") LocalDate ngayBatDauTuan1,
                                   @Param("tuanTrongNam") Integer tuanTrongNam);

    // ===== STATISTICS QUERIES =====

    /**
     * Đếm số học kỳ theo trạng thái (dựa trên ngày hiện tại)
     */
    @Query("SELECT " +
            "SUM(CASE WHEN :currentDate < h.ngayBatDau THEN 1 ELSE 0 END) as upcoming, " +
            "SUM(CASE WHEN :currentDate >= h.ngayBatDau AND :currentDate <= h.ngayKetThuc THEN 1 ELSE 0 END) as ongoing, " +
            "SUM(CASE WHEN :currentDate > h.ngayKetThuc THEN 1 ELSE 0 END) as finished " +
            "FROM HocKy h WHERE h.isActive = true")
    Object[] countByStatus(@Param("currentDate") LocalDate currentDate);

    /**
     * Đếm số học kỳ theo loại
     */
    @Query("SELECT h.loaiHocKy, COUNT(h) FROM HocKy h WHERE h.isActive = true GROUP BY h.loaiHocKy")
    List<Object[]> countByLoaiHocKy();

    /**
     * Đếm số học kỳ sử dụng week-based config
     */
    @Query("SELECT " +
            "SUM(CASE WHEN h.tuanBatDau IS NOT NULL AND h.soTuanHoc IS NOT NULL AND h.ngayBatDauTuan1 IS NOT NULL THEN 1 ELSE 0 END) as weekBased, " +
            "SUM(CASE WHEN h.tuanBatDau IS NULL OR h.soTuanHoc IS NULL OR h.ngayBatDauTuan1 IS NULL THEN 1 ELSE 0 END) as dateBased " +
            "FROM HocKy h WHERE h.isActive = true")
    Object[] countByConfigType();

    // ===== YEAR-BASED QUERIES =====

    /**
     * Tìm tất cả năm học khác nhau (dựa trên ngayBatDau)
     */
    @Query("SELECT DISTINCT YEAR(h.ngayBatDau) FROM HocKy h ORDER BY YEAR(h.ngayBatDau) DESC")
    List<Integer> findDistinctYears();

    /**
     * Tìm học kỳ theo khoảng năm học
     */
    @Query("SELECT h FROM HocKy h WHERE YEAR(h.ngayBatDau) BETWEEN :startYear AND :endYear " +
            "ORDER BY h.ngayBatDau")
    List<HocKy> findByYearRange(@Param("startYear") int startYear, @Param("endYear") int endYear);

    // ===== SEARCH QUERIES =====

    /**
     * Tìm kiếm học kỳ theo tên hoặc mã
     */
    @Query("SELECT h FROM HocKy h WHERE h.isActive = true " +
            "AND (LOWER(h.tenHocKy) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "     OR LOWER(h.maHocKy) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "ORDER BY h.ngayBatDau DESC")
    List<HocKy> searchByKeyword(@Param("keyword") String keyword);

    /**
     * Tìm học kỳ theo multiple criteria
     */
    @Query("SELECT h FROM HocKy h WHERE " +
            "(:maHocKy IS NULL OR h.maHocKy = :maHocKy) " +
            "AND (:loaiHocKy IS NULL OR h.loaiHocKy = :loaiHocKy) " +
            "AND (:year IS NULL OR YEAR(h.ngayBatDau) = :year) " +
            "AND (:isActive IS NULL OR h.isActive = :isActive) " +
            "ORDER BY h.ngayBatDau DESC")
    List<HocKy> findByCriteria(@Param("maHocKy") String maHocKy,
                               @Param("loaiHocKy") HocKy.LoaiHocKy loaiHocKy,
                               @Param("year") Integer year,
                               @Param("isActive") Boolean isActive);

    // ===== VALIDATION QUERIES =====

    /**
     * Kiểm tra tên học kỳ đã tồn tại chưa
     */
    @Query("SELECT COUNT(h) > 0 FROM HocKy h WHERE h.tenHocKy = :tenHocKy " +
            "AND (:excludeMaHocKy IS NULL OR h.maHocKy != :excludeMaHocKy)")
    boolean existsByTenHocKy(@Param("tenHocKy") String tenHocKy,
                             @Param("excludeMaHocKy") String excludeMaHocKy);

    /**
     * Kiểm tra có học kỳ nào trong khoảng thời gian không (date-based)
     */
    @Query("SELECT COUNT(h) FROM HocKy h WHERE h.isActive = true " +
            "AND NOT (h.ngayKetThuc < :startDate OR h.ngayBatDau > :endDate)")
    long countInDateRange(@Param("startDate") LocalDate startDate,
                          @Param("endDate") LocalDate endDate);

    /**
     * Kiểm tra có học kỳ nào trong khoảng tuần không (week-based)
     */
    @Query("SELECT COUNT(h) FROM HocKy h WHERE h.ngayBatDauTuan1 = :ngayBatDauTuan1 " +
            "AND h.tuanBatDau IS NOT NULL AND h.soTuanHoc IS NOT NULL " +
            "AND h.isActive = true " +
            "AND h.tuanBatDau <= :tuanKetThuc " +
            "AND (h.tuanBatDau + h.soTuanHoc - 1) >= :tuanBatDau")
    long countInWeekRange(@Param("ngayBatDauTuan1") LocalDate ngayBatDauTuan1,
                          @Param("tuanBatDau") Integer tuanBatDau,
                          @Param("tuanKetThuc") Integer tuanKetThuc);

    // ===== ORDERING VARIANTS =====

    /**
     * Tìm học kỳ active sắp xếp theo tuần bắt đầu (week-based trước)
     */
    @Query("SELECT h FROM HocKy h WHERE h.isActive = true " +
            "ORDER BY " +
            "CASE WHEN h.tuanBatDau IS NOT NULL THEN 0 ELSE 1 END, " +
            "h.ngayBatDauTuan1, h.tuanBatDau, h.ngayBatDau")
    List<HocKy> findActiveOrderByWeekFirst();

    /**
     * Tìm học kỳ theo học kỳ và năm học (để tương thích với LopHocPhan)
     */
    @Query("SELECT h FROM HocKy h WHERE h.maHocKy = :hocKy")
    Optional<HocKy> findByMaHocKy(@Param("hocKy") String hocKy);
}