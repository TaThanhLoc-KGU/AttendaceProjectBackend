package com.tathanhloc.faceattendance.Repository;

import com.tathanhloc.faceattendance.Model.NamHoc;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface NamHocRepository extends JpaRepository<NamHoc, String> {

    // Tìm năm học hiện tại
    Optional<NamHoc> findByIsCurrentTrue();

    // Tìm năm học theo trạng thái active
    List<NamHoc> findByIsActiveTrue();

    // Tìm năm học theo trạng thái active và current
    List<NamHoc> findByIsActiveTrueAndIsCurrentTrue();

    // Tìm năm học đang diễn ra (theo thời gian)
    @Query("SELECT n FROM NamHoc n WHERE n.isActive = true AND :date BETWEEN n.ngayBatDau AND n.ngayKetThuc")
    List<NamHoc> findOngoingAt(@Param("date") LocalDate date);

    // Tìm năm học sắp tới
    @Query("SELECT n FROM NamHoc n WHERE n.isActive = true AND n.ngayBatDau > :date ORDER BY n.ngayBatDau ASC")
    List<NamHoc> findUpcoming(@Param("date") LocalDate date);

    // Tìm năm học đã kết thúc
    @Query("SELECT n FROM NamHoc n WHERE n.isActive = true AND n.ngayKetThuc < :date ORDER BY n.ngayKetThuc DESC")
    List<NamHoc> findFinished(@Param("date") LocalDate date);

    // Tìm năm học theo khoảng thời gian (có overlap)
    @Query("SELECT n FROM NamHoc n WHERE n.isActive = true AND " +
            "NOT (n.ngayKetThuc < :startDate OR n.ngayBatDau > :endDate)")
    List<NamHoc> findByDateRangeOverlap(@Param("startDate") LocalDate startDate,
                                        @Param("endDate") LocalDate endDate);

    // Tìm năm học theo năm bắt đầu
    @Query("SELECT n FROM NamHoc n WHERE n.isActive = true AND YEAR(n.ngayBatDau) = :year")
    List<NamHoc> findByStartYear(@Param("year") int year);

    // Tìm năm học theo năm kết thúc
    @Query("SELECT n FROM NamHoc n WHERE n.isActive = true AND YEAR(n.ngayKetThuc) = :year")
    List<NamHoc> findByEndYear(@Param("year") int year);

    /**
     * Tìm năm học theo trạng thái hoạt động
     */
    List<NamHoc> findByIsActive(Boolean isActive);

    /**
     * Tìm năm học theo trạng thái current
     */
    List<NamHoc> findByIsCurrent(Boolean isCurrent);

    /**
     * Đếm năm học theo trạng thái
     */
    long countByIsActive(Boolean isActive);
}