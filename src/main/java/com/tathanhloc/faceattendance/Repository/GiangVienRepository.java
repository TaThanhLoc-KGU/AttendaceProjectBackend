package com.tathanhloc.faceattendance.Repository;

import com.tathanhloc.faceattendance.Model.GiangVien;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GiangVienRepository extends JpaRepository<GiangVien, String> {
    Optional<GiangVien> findByEmail(String email);
    List<GiangVien> findByKhoaMaKhoa(String maKhoa);
    List<GiangVien> findByMaGv(String maGv);
    List<GiangVien> findByIsActiveTrue();

    /**
     * Tìm tất cả giảng viên đã nghỉ việc (soft deleted)
     */
    List<GiangVien> findByIsActiveFalse();

    /**
     * Đếm số giảng viên đang hoạt động
     */
    long countByIsActiveTrue();

    /**
     * Đếm số giảng viên đã nghỉ việc
     */
    long countByIsActiveFalse();

    /**
     * Kiểm tra email đã tồn tại
     */
    boolean existsByEmail(String email);

    /**
     * Kiểm tra email đã tồn tại (loại trừ một mã giảng viên cụ thể)
     */
    @Query("SELECT COUNT(gv) > 0 FROM GiangVien gv WHERE gv.email = :email AND gv.maGv != :maGv")
    boolean existsByEmailAndMaGvNot(@Param("email") String email, @Param("maGv") String maGv);

    /**
     * Tìm kiếm theo họ tên hoặc email
     */
    List<GiangVien> findByHoTenContainingIgnoreCaseOrEmailContainingIgnoreCase(String hoTen, String email);
}
