package com.tathanhloc.faceattendance.Repository;

import com.tathanhloc.faceattendance.Model.MonHoc;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MonHocRepository extends JpaRepository<MonHoc, String> {

    /**
     * Lấy quan hệ MonHoc-Nganh bằng native query
     */
    @Query(value = """
        SELECT nm.ma_mh, nm.ma_nganh 
        FROM nganh_monhoc nm 
        WHERE nm.ma_mh IN :monHocIds AND nm.is_active = true
        """, nativeQuery = true)
    List<Object[]> findMonHocNganhRelations(@Param("monHocIds") List<String> monHocIds);

    /**
     * Thêm quan hệ MonHoc-Nganh
     */
    @Modifying
    @Query(value = """
        INSERT INTO nganh_monhoc (ma_mh, ma_nganh, is_active) 
        VALUES (:maMh, :maNganh, true)
        """, nativeQuery = true)
    void insertMonHocNganhRelation(@Param("maMh") String maMh, @Param("maNganh") String maNganh);

    /**
     * Xóa tất cả quan hệ của một MonHoc
     */
    @Modifying
    @Query(value = "DELETE FROM nganh_monhoc WHERE ma_mh = :maMh", nativeQuery = true)
    void deleteMonHocNganhRelations(@Param("maMh") String maMh);
}