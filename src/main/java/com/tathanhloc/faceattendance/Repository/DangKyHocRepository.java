package com.tathanhloc.faceattendance.Repository;

import com.tathanhloc.faceattendance.Model.DangKyHoc;
import com.tathanhloc.faceattendance.Model.DangKyHocId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DangKyHocRepository extends JpaRepository<DangKyHoc, DangKyHocId> {
    List<DangKyHoc> findBySinhVien_MaSv(String maSv);
    List<DangKyHoc> findByLopHocPhan_MaLhp(String maLhp);

    /**
     * Tìm đăng ký học theo lớp học phần
     */
    List<DangKyHoc> findByLopHocPhanMaLhp(String maLhp);

    /**
     * Đếm số sinh viên trong lớp học phần
     */
    long countByLopHocPhanMaLhp(String maLhp);

    /**
     * Kiểm tra sinh viên có đăng ký lớp không
     */
    boolean existsByLopHocPhanMaLhpAndSinhVienMaSv(String maLhp, String maSv);
    boolean existsByIdMaSvAndIdMaLhpAndIsActiveTrue(String maSv, String maLhp);

    // THÊM CÁC METHOD VALIDATION MỚI
    /**
     * Kiểm tra sinh viên đã đăng ký môn học này (bất kỳ nhóm nào) chưa
     */
    @Query("SELECT d FROM DangKyHoc d " +
            "JOIN d.lopHocPhan lhp " +
            "WHERE d.sinhVien.maSv = :maSv " +
            "AND lhp.monHoc.maMh = :maMh " +
            "AND d.isActive = true")
    List<DangKyHoc> findBySinhVienAndMonHoc(@Param("maSv") String maSv, @Param("maMh") String maMh);

    /**
     * Kiểm tra sinh viên có đăng ký môn học này chưa
     */
    @Query("SELECT COUNT(d) > 0 FROM DangKyHoc d " +
            "JOIN d.lopHocPhan lhp " +
            "WHERE d.sinhVien.maSv = :maSv " +
            "AND lhp.monHoc.maMh = :maMh " +
            "AND d.isActive = true")
    boolean existsBySinhVienAndMonHoc(@Param("maSv") String maSv, @Param("maMh") String maMh);

    /**
     * Tìm lớp học phần hiện tại của sinh viên trong môn học
     */
    @Query("SELECT d.lopHocPhan.maLhp FROM DangKyHoc d " +
            "JOIN d.lopHocPhan lhp " +
            "WHERE d.sinhVien.maSv = :maSv " +
            "AND lhp.monHoc.maMh = :maMh " +
            "AND d.isActive = true")
    Optional<String> findCurrentLhpByStudentAndSubject(@Param("maSv") String maSv, @Param("maMh") String maMh);


    /**
     * Đếm số đăng ký theo mã học kỳ
     */
    @Query("SELECT COUNT(dk) FROM DangKyHoc dk JOIN dk.lopHocPhan lhp WHERE lhp.hocKy = :hocKy AND dk.isActive = true")
    long countByHocKy(@Param("hocKy") String hocKy);

    /**
     * Đếm số sinh viên duy nhất đăng ký trong học kỳ
     */
    @Query("SELECT COUNT(DISTINCT dk.sinhVien.maSv) FROM DangKyHoc dk JOIN dk.lopHocPhan lhp WHERE lhp.hocKy = :hocKy AND dk.isActive = true")
    long countUniqueStudentsByHocKy(@Param("hocKy") String hocKy);

    /**
     * Tìm đăng ký theo học kỳ
     */
    @Query("SELECT dk FROM DangKyHoc dk JOIN dk.lopHocPhan lhp WHERE lhp.hocKy = :hocKy")
    List<DangKyHoc> findByHocKy(@Param("hocKy") String hocKy);

    // ← THÊM METHOD NÀY
    @Query("SELECT dk FROM DangKyHoc dk WHERE dk.lopHocPhan.maLhp = :maLhp AND dk.isActive = true")
    List<DangKyHoc> findByLopHocPhanMaLhpAndIsActiveTrue(@Param("maLhp") String maLhp);
    List<DangKyHoc> findBySinhVienMaSvAndIsActiveTrue(String maSv);


    /**
     * Tìm danh sách đăng ký theo mã lớp học phần và trạng thái
     */
    @Query("SELECT dk FROM DangKyHoc dk " +
            "WHERE dk.lopHocPhan.maLhp = :maLhp " +
            "AND dk.isActive = :isActive " +
            "ORDER BY dk.sinhVien.hoTen")
    List<DangKyHoc> findByMaLhpAndActive(@Param("maLhp") String maLhp,
                                         @Param("isActive") boolean isActive);

    /**
     * Đếm số sinh viên đăng ký theo lớp học phần
     */
    @Query("SELECT COUNT(dk) FROM DangKyHoc dk " +
            "WHERE dk.lopHocPhan.maLhp = :maLhp " +
            "AND dk.isActive = :isActive")
    int countByMaLhpAndActive(@Param("maLhp") String maLhp,
                              @Param("isActive") boolean isActive);

    /**
     * Tìm lớp học phần mà sinh viên đã đăng ký
     */
    @Query("SELECT dk FROM DangKyHoc dk " +
            "WHERE dk.sinhVien.maSv = :maSv " +
            "AND dk.isActive = true " +
            "ORDER BY dk.lopHocPhan.monHoc.tenMh")
    List<DangKyHoc> findActiveClassesByStudent(@Param("maSv") String maSv);

    /**
     * Kiểm tra sinh viên có đăng ký lớp học phần không
     */
    @Query("SELECT COUNT(dk) > 0 FROM DangKyHoc dk " +
            "WHERE dk.sinhVien.maSv = :maSv " +
            "AND dk.lopHocPhan.maLhp = :maLhp " +
            "AND dk.isActive = true")
    boolean existsByMaSvAndMaLhpAndActive(@Param("maSv") String maSv,
                                          @Param("maLhp") String maLhp);

    /**
     * Thống kê đăng ký theo lớp học phần
     */
    @Query("SELECT lhp.maLhp, lhp.monHoc.tenMh, COUNT(dk) as studentCount " +
            "FROM DangKyHoc dk " +
            "JOIN dk.lopHocPhan lhp " +
            "WHERE dk.isActive = true " +
            "GROUP BY lhp.maLhp, lhp.monHoc.tenMh " +
            "ORDER BY studentCount DESC")
    List<Object[]> getClassRegistrationStats();

    /**
     * Tìm sinh viên đăng ký nhiều lớp của cùng một giảng viên
     */
    @Query("SELECT dk.sinhVien.maSv, dk.sinhVien.hoTen, COUNT(dk) as classCount " +
            "FROM DangKyHoc dk " +
            "WHERE dk.lopHocPhan.giangVien.maGv = :maGv " +
            "AND dk.isActive = true " +
            "GROUP BY dk.sinhVien.maSv, dk.sinhVien.hoTen " +
            "HAVING COUNT(dk) > 1 " +
            "ORDER BY classCount DESC")
    List<Object[]> findStudentsWithMultipleClassesByLecturer(@Param("maGv") String maGv);
}