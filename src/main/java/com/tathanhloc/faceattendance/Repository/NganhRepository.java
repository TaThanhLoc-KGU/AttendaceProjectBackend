package com.tathanhloc.faceattendance.Repository;

import com.tathanhloc.faceattendance.Model.Nganh;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NganhRepository extends JpaRepository<Nganh, String> {

    /**
     * Tìm ngành theo khoa (chỉ các ngành đang hoạt động)
     */
    @Query("SELECT n FROM Nganh n WHERE n.khoa.maKhoa = :maKhoa AND n.isActive = true")
    List<Nganh> findActiveByKhoaMaKhoa(@Param("maKhoa") String maKhoa);

    /**
     * Tìm tất cả ngành theo khoa (bao gồm đã xóa)
     */
    List<Nganh> findByKhoaMaKhoa(String maKhoa);

    /**
     * Tìm tất cả ngành đang hoạt động
     */
    @Query("SELECT n FROM Nganh n WHERE n.isActive = true")
    List<Nganh> findAllActive();

    /**
     * Tìm tất cả ngành đã bị xóa
     */
    @Query("SELECT n FROM Nganh n WHERE n.isActive = false")
    List<Nganh> findAllDeleted();

    /**
     * Tìm ngành theo ID chỉ khi đang hoạt động
     */
    @Query("SELECT n FROM Nganh n WHERE n.maNganh = :maNganh AND n.isActive = true")
    Optional<Nganh> findActiveById(@Param("maNganh") String maNganh);

    /**
     * Kiểm tra xem ngành có tồn tại và đang hoạt động không
     */
    @Query("SELECT COUNT(n) > 0 FROM Nganh n WHERE n.maNganh = :maNganh AND n.isActive = true")
    boolean existsActiveById(@Param("maNganh") String maNganh);

    /**
     * Đếm số ngành đang hoạt động theo khoa
     */
    @Query("SELECT COUNT(n) FROM Nganh n WHERE n.khoa.maKhoa = :maKhoa AND n.isActive = true")
    long countActiveByKhoaMaKhoa(@Param("maKhoa") String maKhoa);

    /**
     * Tìm ngành theo tên (không phân biệt chữ hoa thường) - chỉ ngành đang hoạt động
     */
    @Query("SELECT n FROM Nganh n WHERE LOWER(n.tenNganh) LIKE LOWER(CONCAT('%', :tenNganh, '%')) AND n.isActive = true")
    List<Nganh> findActiveByTenNganhContainingIgnoreCase(@Param("tenNganh") String tenNganh);

    /**
     * Soft delete ngành (cập nhật isActive = false)
     */
    @Query("UPDATE Nganh n SET n.isActive = false WHERE n.maNganh = :maNganh")
    int softDeleteById(@Param("maNganh") String maNganh);

    /**
     * Restore ngành (cập nhật isActive = true)
     */
    @Query("UPDATE Nganh n SET n.isActive = true WHERE n.maNganh = :maNganh")
    int restoreById(@Param("maNganh") String maNganh);

    /**
     * Đếm số sinh viên trong ngành (chỉ sinh viên đang hoạt động)
     */
    @Query("SELECT COUNT(sv) FROM SinhVien sv " +
            "JOIN sv.lop l " +
            "WHERE l.nganh.maNganh = :maNganh " +
            "AND sv.isActive = true " +
            "AND l.isActive = true")
    long countActiveSinhVienByNganh(@Param("maNganh") String maNganh);

    /**
     * Đếm tất cả sinh viên trong ngành (bao gồm không hoạt động)
     */
    @Query("SELECT COUNT(sv) FROM SinhVien sv " +
            "JOIN sv.lop l " +
            "WHERE l.nganh.maNganh = :maNganh")
    long countAllSinhVienByNganh(@Param("maNganh") String maNganh);
}