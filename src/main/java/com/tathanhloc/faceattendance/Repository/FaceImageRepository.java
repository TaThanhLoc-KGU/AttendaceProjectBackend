package com.tathanhloc.faceattendance.Repository;

import com.tathanhloc.faceattendance.Model.FaceImage;
import com.tathanhloc.faceattendance.Model.SinhVien;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FaceImageRepository extends JpaRepository<FaceImage, Long> {

    /**
     * Tìm ảnh theo mã sinh viên và filename
     */
    @Query("SELECT f FROM FaceImage f WHERE f.sinhVien.maSv = :maSv AND f.filename = :filename AND f.isActive = true")
    Optional<FaceImage> findByMaSvAndFilename(@Param("maSv") String maSv, @Param("filename") String filename);

    /**
     * Tìm tất cả ảnh active của sinh viên
     */
    @Query("SELECT f FROM FaceImage f WHERE f.sinhVien.maSv = :maSv AND f.isActive = true ORDER BY f.slotIndex")
    List<FaceImage> findByMaSvAndActive(@Param("maSv") String maSv);

    /**
     * Tìm ảnh theo slot index của sinh viên
     */
    @Query("SELECT f FROM FaceImage f WHERE f.sinhVien.maSv = :maSv AND f.slotIndex = :slotIndex AND f.isActive = true")
    Optional<FaceImage> findByMaSvAndSlotIndex(@Param("maSv") String maSv, @Param("slotIndex") Integer slotIndex);

    /**
     * Đếm số lượng ảnh active của sinh viên
     */
    @Query("SELECT COUNT(f) FROM FaceImage f WHERE f.sinhVien.maSv = :maSv AND f.isActive = true")
    int countActiveByMaSv(@Param("maSv") String maSv);

    /**
     * Kiểm tra slot có trống không
     */
    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN false ELSE true END FROM FaceImage f WHERE f.sinhVien.maSv = :maSv AND f.slotIndex = :slotIndex AND f.isActive = true")
    boolean isSlotEmpty(@Param("maSv") String maSv, @Param("slotIndex") Integer slotIndex);

    /**
     * Tìm tất cả slot đã sử dụng của sinh viên
     */
    @Query("SELECT f.slotIndex FROM FaceImage f WHERE f.sinhVien.maSv = :maSv AND f.isActive = true")
    List<Integer> findUsedSlotsByMaSv(@Param("maSv") String maSv);

    /**
     * Tìm ảnh theo mã sinh viên - không cần active (để debug)
     */
    @Query("SELECT f FROM FaceImage f WHERE f.sinhVien.maSv = :maSv")
    List<FaceImage> findAllByMaSv(@Param("maSv") String maSv);
}