package com.tathanhloc.faceattendance.Repository;

import com.tathanhloc.faceattendance.Model.PhongHoc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface PhongHocRepository extends JpaRepository<PhongHoc, String> {

    // Soft delete support
    List<PhongHoc> findByIsActiveTrue();

    Page<PhongHoc> findByIsActiveTrue(Pageable pageable);

    Optional<PhongHoc> findByMaPhongAndIsActiveTrue(String maPhong);

    // Search functionality
    @Query("SELECT p FROM PhongHoc p WHERE p.isActive = true AND " +
            "(LOWER(p.maPhong) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(p.tenPhong) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(p.toaNha) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<PhongHoc> searchPhongHoc(@Param("keyword") String keyword, Pageable pageable);

    // Filter by type
    Page<PhongHoc> findByIsActiveTrueAndLoaiPhong(String loaiPhong, Pageable pageable);

    // Filter by status
    Page<PhongHoc> findByIsActiveTrueAndTrangThai(String trangThai, Pageable pageable);

    // Statistics
    @Query("SELECT COUNT(p) FROM PhongHoc p WHERE p.isActive = true")
    long countActiveRooms();

    @Query("SELECT COUNT(p) FROM PhongHoc p WHERE p.isActive = true AND p.trangThai = :trangThai")
    long countByTrangThai(@Param("trangThai") String trangThai);

    // Soft delete
    @Modifying
    @Transactional
    @Query("UPDATE PhongHoc p SET p.isActive = false WHERE p.maPhong = :maPhong")
    void softDelete(@Param("maPhong") String maPhong);

    // Check if room exists
    boolean existsByMaPhongAndIsActiveTrue(String maPhong);
}