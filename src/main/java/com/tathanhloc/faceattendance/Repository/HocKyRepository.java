
package com.tathanhloc.faceattendance.Repository;

import com.tathanhloc.faceattendance.Model.HocKy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface HocKyRepository extends JpaRepository<HocKy, String> {

    // Tìm học kỳ hiện tại
    Optional<HocKy> findByIsCurrentTrue();

    // Tìm học kỳ theo trạng thái active
    List<HocKy> findByIsActiveTrue();

    // Tìm học kỳ theo trạng thái active và current
    List<HocKy> findByIsActiveTrueAndIsCurrentTrue();

    // Tìm học kỳ đang diễn ra (theo thời gian)
    @Query("SELECT h FROM HocKy h WHERE h.isActive = true AND :date BETWEEN h.ngayBatDau AND h.ngayKetThuc")
    List<HocKy> findOngoingAt(@Param("date") LocalDate date);

    // Tìm học kỳ sắp tới
    @Query("SELECT h FROM HocKy h WHERE h.isActive = true AND h.ngayBatDau > :date ORDER BY h.ngayBatDau ASC")
    List<HocKy> findUpcoming(@Param("date") LocalDate date);

    // Tìm học kỳ đã kết thúc
    @Query("SELECT h FROM HocKy h WHERE h.isActive = true AND h.ngayKetThuc < :date ORDER BY h.ngayKetThuc DESC")
    List<HocKy> findFinished(@Param("date") LocalDate date);

    // Tìm học kỳ theo khoảng thời gian (có overlap)
    @Query("SELECT h FROM HocKy h WHERE h.isActive = true AND " +
            "NOT (h.ngayKetThuc < :startDate OR h.ngayBatDau > :endDate)")
    List<HocKy> findByDateRangeOverlap(@Param("startDate") LocalDate startDate,
                                       @Param("endDate") LocalDate endDate);
}