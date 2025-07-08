// 3. Cập nhật HocKyRepository.java - Thêm method tìm học kỳ hiện tại

package com.tathanhloc.faceattendance.Repository;

import com.tathanhloc.faceattendance.Model.HocKy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface HocKyRepository extends JpaRepository<HocKy, String> {

    /**
     * Tìm học kỳ hiện tại
     */
    Optional<HocKy> findByIsCurrentTrue();

    /**
     * Tìm các học kỳ đang hoạt động
     */
    List<HocKy> findByIsActiveTrue();

    /**
     * Tìm học kỳ theo trạng thái isActive
     */
    List<HocKy> findByIsActive(Boolean isActive);

    /**
     * Tìm các học kỳ đã bị xóa mềm
     */
    List<HocKy> findByIsActiveFalse();

    /**
     * Tìm học kỳ theo năm học (thông qua bảng trung gian HocKyNamHoc)
     */
    @Query("SELECT hk FROM HocKy hk " +
            "JOIN HocKyNamHoc hknh ON hk.maHocKy = hknh.hocKy.maHocKy " +
            "WHERE hknh.namHoc.maNamHoc = :maNamHoc AND hknh.isActive = true " +
            "ORDER BY hknh.thuTu")
    List<HocKy> findByNamHocMaNamHocAndIsActiveTrueOrderByThuTu(@Param("maNamHoc") String maNamHoc);

    /**
     * Tìm học kỳ đang diễn ra (ngày hiện tại nằm trong khoảng thời gian học kỳ)
     */
    @Query("SELECT h FROM HocKy h WHERE h.ngayBatDau <= :currentDate AND h.ngayKetThuc >= :currentDate AND h.isActive = true")
    List<HocKy> findOngoingSemesters(@Param("currentDate") LocalDate currentDate);

    /**
     * Tìm học kỳ sắp tới
     */
    @Query("SELECT h FROM HocKy h WHERE h.ngayBatDau > :currentDate AND h.isActive = true ORDER BY h.ngayBatDau")
    List<HocKy> findUpcomingSemesters(@Param("currentDate") LocalDate currentDate);

    /**
     * Tìm học kỳ đã kết thúc
     */
    @Query("SELECT h FROM HocKy h WHERE h.ngayKetThuc < :currentDate AND h.isActive = true ORDER BY h.ngayKetThuc DESC")
    List<HocKy> findFinishedSemesters(@Param("currentDate") LocalDate currentDate);

    /**
     * Kiểm tra trùng thời gian học kỳ trong cùng năm học (thông qua HocKyNamHoc)
     */
    @Query("SELECT hk FROM HocKy hk " +
            "JOIN HocKyNamHoc hknh ON hk.maHocKy = hknh.hocKy.maHocKy " +
            "WHERE hknh.namHoc.maNamHoc = :maNamHoc " +
            "AND hk.maHocKy != :excludeId " +
            "AND hk.isActive = true " +
            "AND hknh.isActive = true " +
            "AND ((hk.ngayBatDau <= :ngayBatDau AND hk.ngayKetThuc >= :ngayBatDau) " +
            "OR (hk.ngayBatDau <= :ngayKetThuc AND hk.ngayKetThuc >= :ngayKetThuc) " +
            "OR (hk.ngayBatDau >= :ngayBatDau AND hk.ngayKetThuc <= :ngayKetThuc))")
    List<HocKy> findOverlappingSemesters(@Param("maNamHoc") String maNamHoc,
                                         @Param("excludeId") String excludeId,
                                         @Param("ngayBatDau") LocalDate ngayBatDau,
                                         @Param("ngayKetThuc") LocalDate ngayKetThuc);

    /**
     * Tìm học kỳ theo thứ tự trong năm học
     */
    @Query("SELECT hk FROM HocKy hk " +
            "JOIN HocKyNamHoc hknh ON hk.maHocKy = hknh.hocKy.maHocKy " +
            "WHERE hknh.namHoc.maNamHoc = :maNamHoc AND hknh.thuTu = :thuTu " +
            "AND hknh.isActive = true AND hk.isActive = true")
    Optional<HocKy> findByNamHocAndThuTu(@Param("maNamHoc") String maNamHoc, @Param("thuTu") Integer thuTu);

    /**
     * Đếm số học kỳ trong năm học
     */
    @Query("SELECT COUNT(hk) FROM HocKy hk " +
            "JOIN HocKyNamHoc hknh ON hk.maHocKy = hknh.hocKy.maHocKy " +
            "WHERE hknh.namHoc.maNamHoc = :maNamHoc AND hknh.isActive = true AND hk.isActive = true")
    Long countByNamHoc(@Param("maNamHoc") String maNamHoc);
}