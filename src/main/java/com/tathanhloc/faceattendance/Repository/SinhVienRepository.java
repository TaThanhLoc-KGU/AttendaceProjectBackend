package com.tathanhloc.faceattendance.Repository;

import com.tathanhloc.faceattendance.Model.SinhVien;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface SinhVienRepository extends JpaRepository<SinhVien, String> {
    List<SinhVien> findByLopMaLop(String maLop);
    Optional<SinhVien> findByEmail(String email);
    Optional<SinhVien> findByMaSv(String maSv);


    @Query("SELECT COUNT(sv) FROM SinhVien sv WHERE sv.lop.maLop = :maLop AND sv.isActive = true")
    long countByLopMaLopAndIsActiveTrue(@Param("maLop") String maLop);
    // THÊM CÁC METHOD KHÁC
    List<SinhVien> findByIsActiveFalse();
    List<SinhVien> findByIsActive(Boolean isActive);
    long countByIsActiveTrue();
    boolean existsByEmail(String email);

}
