package com.tathanhloc.faceattendance.Repository;

import com.tathanhloc.faceattendance.DTO.AttendanceHistoryDTO;
import com.tathanhloc.faceattendance.DTO.SubjectAttendanceDTO;
import com.tathanhloc.faceattendance.Enum.TrangThaiDiemDanhEnum;
import com.tathanhloc.faceattendance.Model.DiemDanh;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Pageable;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface DiemDanhRepository extends JpaRepository<DiemDanh, Long> {

    // Lịch sử điểm danh gần nhất - FIXED
    @Query(value = """
        SELECT 
            lh.ngay_hoc as date,
            mh.ten_mon_hoc as subjectName,
            mh.ma_mon_hoc as subjectCode,
            lhp.ma_lhp as className,
            gv.ho_ten as lecturerName,
            ph.ten_phong as roomName,
            CONCAT('Ca ', lh.ca_hoc, ' (', lh.gio_bat_dau, '-', lh.gio_ket_thuc, ')') as session,
            COUNT(CASE WHEN dd.trang_thai = 'CO_MAT' THEN 1 END) as present,
            COUNT(CASE WHEN dd.trang_thai = 'VANG_MAT' THEN 1 END) as absent,
            COUNT(CASE WHEN dd.trang_thai = 'DI_TRE' THEN 1 END) as late,
            COUNT(CASE WHEN dd.trang_thai = 'VANG_CO_PHEP' THEN 1 END) as excused,
            COUNT(dd.id) as totalStudents
        FROM lich_hoc lh
        JOIN lop_hoc_phan lhp ON lh.ma_lhp = lhp.ma_lhp
        JOIN mon_hoc mh ON lhp.ma_mon_hoc = mh.ma_mon_hoc
        JOIN giang_vien gv ON lhp.ma_gv = gv.ma_gv
        JOIN phong_hoc ph ON lh.ma_phong = ph.ma_phong
        LEFT JOIN diem_danh dd ON lh.ma_lich = dd.ma_lich
        WHERE lh.ngay_hoc <= CURRENT_DATE
        GROUP BY lh.ma_lich, lh.ngay_hoc, mh.ten_mon_hoc, mh.ma_mon_hoc, 
                 lhp.ma_lhp, gv.ho_ten, ph.ten_phong, lh.ca_hoc, lh.gio_bat_dau, lh.gio_ket_thuc
        ORDER BY lh.ngay_hoc DESC, lh.gio_bat_dau DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> findRecentAttendanceHistory(@Param("limit") int limit);

    // Báo cáo điểm danh theo bộ lọc - FIXED
    @Query(value = """
        SELECT 
            lh.ngay_hoc as date,
            mh.ten_mon_hoc as subjectName,
            mh.ma_mon_hoc as subjectCode,
            lhp.ma_lhp as className,
            gv.ho_ten as lecturerName,
            ph.ten_phong as roomName,
            CONCAT('Ca ', lh.ca_hoc) as session,
            lh.gio_bat_dau as timeStart,
            lh.gio_ket_thuc as timeEnd,
            COUNT(CASE WHEN dd.trang_thai = 'CO_MAT' THEN 1 END) as present,
            COUNT(CASE WHEN dd.trang_thai = 'VANG_MAT' THEN 1 END) as absent,
            COUNT(CASE WHEN dd.trang_thai = 'DI_TRE' THEN 1 END) as late,
            COUNT(CASE WHEN dd.trang_thai = 'VANG_CO_PHEP' THEN 1 END) as excused,
            COUNT(dd.id) as totalStudents
        FROM lich_hoc lh
        JOIN lop_hoc_phan lhp ON lh.ma_lhp = lhp.ma_lhp
        JOIN mon_hoc mh ON lhp.ma_mon_hoc = mh.ma_mon_hoc
        JOIN giang_vien gv ON lhp.ma_gv = gv.ma_gv
        JOIN phong_hoc ph ON lh.ma_phong = ph.ma_phong
        LEFT JOIN diem_danh dd ON lh.ma_lich = dd.ma_lich
        WHERE lh.ngay_hoc BETWEEN :fromDate AND :toDate
        AND (:subjectCode IS NULL OR mh.ma_mon_hoc = :subjectCode)
        AND (:lecturerCode IS NULL OR gv.ma_gv = :lecturerCode)
        AND (:classCode IS NULL OR lhp.ma_lhp = :classCode)
        GROUP BY lh.ma_lich, lh.ngay_hoc, mh.ten_mon_hoc, mh.ma_mon_hoc, 
                 lhp.ma_lhp, gv.ho_ten, ph.ten_phong, lh.ca_hoc, lh.gio_bat_dau, lh.gio_ket_thuc
        ORDER BY lh.ngay_hoc DESC, lh.gio_bat_dau DESC
        """, nativeQuery = true)
    List<Object[]> findFilteredAttendanceReport(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("subjectCode") String subjectCode,
            @Param("lecturerCode") String lecturerCode,
            @Param("classCode") String classCode);

    // Thống kê theo học kỳ - môn học - FIXED
    @Query(value = """
        SELECT 
            mh.ma_mon_hoc as subjectCode,
            mh.ten_mon_hoc as subjectName,
            COUNT(CASE WHEN dd.trang_thai = 'CO_MAT' THEN 1 END) as present,
            COUNT(CASE WHEN dd.trang_thai = 'VANG_MAT' THEN 1 END) as absent,
            COUNT(CASE WHEN dd.trang_thai = 'DI_TRE' THEN 1 END) as late,
            COUNT(CASE WHEN dd.trang_thai = 'VANG_CO_PHEP' THEN 1 END) as excused,
            COUNT(dd.id) as total
        FROM lich_hoc lh
        JOIN lop_hoc_phan lhp ON lh.ma_lhp = lhp.ma_lhp
        JOIN mon_hoc mh ON lhp.ma_mon_hoc = mh.ma_mon_hoc
        JOIN diem_danh dd ON lh.ma_lich = dd.ma_lich
        WHERE lhp.ma_hoc_ky = :semesterCode AND lhp.ma_nam_hoc = :yearCode
        GROUP BY mh.ma_mon_hoc, mh.ten_mon_hoc
        ORDER BY mh.ten_mon_hoc
        """, nativeQuery = true)
    List<Object[]> findAttendanceStatsBySubject(@Param("semesterCode") String semesterCode, @Param("yearCode") String yearCode);

    // Thống kê theo học kỳ - giảng viên - FIXED
    @Query(value = """
        SELECT 
            gv.ma_gv as lecturerCode,
            gv.ho_ten as lecturerName,
            COUNT(CASE WHEN dd.trang_thai = 'CO_MAT' THEN 1 END) as present,
            COUNT(CASE WHEN dd.trang_thai = 'VANG_MAT' THEN 1 END) as absent,
            COUNT(CASE WHEN dd.trang_thai = 'DI_TRE' THEN 1 END) as late,
            COUNT(CASE WHEN dd.trang_thai = 'VANG_CO_PHEP' THEN 1 END) as excused,
            COUNT(dd.id) as total
        FROM lich_hoc lh
        JOIN lop_hoc_phan lhp ON lh.ma_lhp = lhp.ma_lhp
        JOIN giang_vien gv ON lhp.ma_gv = gv.ma_gv
        JOIN diem_danh dd ON lh.ma_lich = dd.ma_lich
        WHERE lhp.ma_hoc_ky = :semesterCode AND lhp.ma_nam_hoc = :yearCode
        GROUP BY gv.ma_gv, gv.ho_ten
        ORDER BY gv.ho_ten
        """, nativeQuery = true)
    List<Object[]> findAttendanceStatsByLecturer(@Param("semesterCode") String semesterCode, @Param("yearCode") String yearCode);

    // Thống kê theo học kỳ - lớp - FIXED
    @Query(value = """
        SELECT 
            lhp.ma_lhp as classCode,
            mh.ten_mon_hoc as subjectName,
            gv.ho_ten as lecturerName,
            COUNT(CASE WHEN dd.trang_thai = 'CO_MAT' THEN 1 END) as present,
            COUNT(CASE WHEN dd.trang_thai = 'VANG_MAT' THEN 1 END) as absent,
            COUNT(CASE WHEN dd.trang_thai = 'DI_TRE' THEN 1 END) as late,
            COUNT(CASE WHEN dd.trang_thai = 'VANG_CO_PHEP' THEN 1 END) as excused,
            COUNT(dd.id) as total
        FROM lich_hoc lh
        JOIN lop_hoc_phan lhp ON lh.ma_lhp = lhp.ma_lhp
        JOIN mon_hoc mh ON lhp.ma_mon_hoc = mh.ma_mon_hoc
        JOIN giang_vien gv ON lhp.ma_gv = gv.ma_gv
        JOIN diem_danh dd ON lh.ma_lich = dd.ma_lich
        WHERE lhp.ma_hoc_ky = :semesterCode AND lhp.ma_nam_hoc = :yearCode
        GROUP BY lhp.ma_lhp, mh.ten_mon_hoc, gv.ho_ten
        ORDER BY lhp.ma_lhp
        """, nativeQuery = true)
    List<Object[]> findAttendanceStatsByClass(@Param("semesterCode") String semesterCode, @Param("yearCode") String yearCode);

    /**
     * Tìm điểm danh theo danh sách mã lịch và khoảng thời gian
     */
    @Query("SELECT dd FROM DiemDanh dd WHERE dd.lichHoc.maLich IN :maLichList " +
            "AND (:fromDate IS NULL OR dd.ngayDiemDanh >= :fromDate) " +
            "AND (:toDate IS NULL OR dd.ngayDiemDanh <= :toDate) " +
            "ORDER BY dd.ngayDiemDanh DESC, dd.thoiGianVao DESC")
    List<DiemDanh> findByLichHocMaLichInAndDateRange(@Param("maLichList") List<String> maLichList,
                                                     @Param("fromDate") LocalDate fromDate,
                                                     @Param("toDate") LocalDate toDate);

    /**
     * Tìm điểm danh hôm nay theo mã lịch
     */
    @Query("SELECT dd FROM DiemDanh dd WHERE dd.lichHoc.maLich IN :maLichList " +
            "AND dd.ngayDiemDanh = :today")
    List<DiemDanh> findTodayAttendanceBySchedules(@Param("maLichList") List<String> maLichList,
                                                  @Param("today") LocalDate today);

    /**
     * Đếm điểm danh theo trạng thái trong khoảng thời gian
     */
    @Query("SELECT dd.trangThai, COUNT(dd) FROM DiemDanh dd " +
            "WHERE dd.lichHoc.maLich IN :maLichList " +
            "AND (:fromDate IS NULL OR dd.ngayDiemDanh >= :fromDate) " +
            "AND (:toDate IS NULL OR dd.ngayDiemDanh <= :toDate) " +
            "GROUP BY dd.trangThai")
    List<Object[]> countAttendanceByStatusAndDateRange(@Param("maLichList") List<String> maLichList,
                                                       @Param("fromDate") LocalDate fromDate,
                                                       @Param("toDate") LocalDate toDate);

// Thêm vào DiemDanhRepository.java

    List<DiemDanh> findByLichHoc_LopHocPhan_MaLhpAndNgayDiemDanh(String maLhp, LocalDate ngayDiemDanh);

    List<DiemDanh> findByLichHocMaLichAndSinhVienMaSvAndNgayDiemDanh(String maLich, String maSv, LocalDate ngayDiemDanh);

    // Đếm theo lớp học phần và trạng thái
    long countByLichHoc_LopHocPhan_MaLhpAndTrangThai(String maLhp, TrangThaiDiemDanhEnum trangThai);

    // Đếm theo sinh viên, lớp và trạng thái
    long countBySinhVienMaSvAndLichHoc_LopHocPhan_MaLhpAndTrangThai(String maSv, String maLhp, TrangThaiDiemDanhEnum trangThai);

    // Lấy điểm danh theo lớp học phần
    List<DiemDanh> findByLichHoc_LopHocPhan_MaLhp(String maLhp);

    // Kiểm tra điểm danh đã tồn tại
    boolean existsByLichHocMaLichAndSinhVienMaSvAndNgayDiemDanh(String maLich, String maSv, LocalDate ngayDiemDanh);
    List<DiemDanh> findByLichHocMaLichAndNgayDiemDanh(String maLich, LocalDate ngayDiemDanh);

    /**
     * Đếm điểm danh theo danh sách mã lịch và trạng thái
     */
    long countByLichHocMaLichInAndTrangThai(List<String> maLichList, String trangThai);


    List<DiemDanh> findBySinhVienMaSv(String maSv);
    List<DiemDanh> findByLichHocMaLich(String maLich);
    long countByNgayDiemDanh(LocalDate ngayDiemDanh);

    // Thống kê theo trạng thái
    long countByTrangThai(TrangThaiDiemDanhEnum trangThai);
    long countByNgayDiemDanhAndTrangThai(LocalDate ngayDiemDanh, TrangThaiDiemDanhEnum trangThai);
    long countByNgayDiemDanhBetweenAndTrangThai(LocalDate fromDate, LocalDate toDate, TrangThaiDiemDanhEnum trangThai);

    @Query("SELECT COUNT(DISTINCT dd.lichHoc) FROM DiemDanh dd WHERE dd.ngayDiemDanh = :ngayDiemDanh")
    long countDistinctLichHocByNgayDiemDanh(@Param("ngayDiemDanh") LocalDate ngayDiemDanh);

    // ===== CÁC METHODS CHUYỂN TỪ LichHocRepository =====

    /**
     * Đếm số lượng điểm danh theo mã lịch
     */
    long countByLichHocMaLich(String maLich);

    /**
     * Tìm điểm danh theo mã lịch và mã sinh viên
     */
    List<DiemDanh> findByLichHocMaLichAndSinhVienMaSv(String maLich, String maSv);

    /**
     * Tìm điểm danh theo sinh viên và khoảng thời gian
     */
    List<DiemDanh> findBySinhVienMaSvAndNgayDiemDanhBetween(String maSv, LocalDate fromDate, LocalDate toDate);

    /**
     * Đếm điểm danh theo danh sách mã lịch
     */
    long countByLichHocMaLichIn(List<String> maLichList);

    /**
     * Đếm điểm danh theo danh sách mã lịch và trạng thái
     */
    long countByLichHocMaLichInAndTrangThai(List<String> maLichList, TrangThaiDiemDanhEnum trangThai);

    /**
     * Tìm điểm danh theo lớp học phần và khoảng thời gian
     */
    @Query("SELECT dd FROM DiemDanh dd " +
            "JOIN dd.lichHoc lh " +
            "JOIN lh.lopHocPhan lhp " +
            "WHERE lhp.maLhp = :maLhp " +
            "AND dd.ngayDiemDanh BETWEEN :fromDate AND :toDate " +
            "ORDER BY dd.ngayDiemDanh DESC")
    List<DiemDanh> findByLopHocPhanAndDateRange(@Param("maLhp") String maLhp,
                                                @Param("fromDate") LocalDate fromDate,
                                                @Param("toDate") LocalDate toDate);

    // Thống kê theo ngày - FIXED
    @Query(value = """
        SELECT 
            dd.ngay_diem_danh as date,
            COUNT(CASE WHEN dd.trang_thai = 'CO_MAT' THEN 1 END) as present,
            COUNT(CASE WHEN dd.trang_thai = 'VANG_MAT' THEN 1 END) as absent,
            COUNT(CASE WHEN dd.trang_thai = 'DI_TRE' THEN 1 END) as late,
            COUNT(CASE WHEN dd.trang_thai = 'VANG_CO_PHEP' THEN 1 END) as excused
        FROM diem_danh dd
        WHERE dd.ngay_diem_danh BETWEEN :fromDate AND :toDate
        GROUP BY dd.ngay_diem_danh
        ORDER BY dd.ngay_diem_danh DESC
        """, nativeQuery = true)
    List<Object[]> findDailyAttendanceStats(@Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);


// XÓA CÁC QUERIES LỖI VÀ THAY BẰNG CÁC QUERIES SAU:

    /**
     * Lấy thống kê điểm danh theo sinh viên - SỬA LẠI
     */
    @Query("SELECT COUNT(d) FROM DiemDanh d " +
            "WHERE d.sinhVien.maSv = :maSv " +
            "AND d.trangThai = 'CO_MAT'")
    Long countPresentByStudent(@Param("maSv") String maSv);

    @Query("SELECT COUNT(d) FROM DiemDanh d " +
            "WHERE d.sinhVien.maSv = :maSv " +
            "AND d.trangThai = 'VANG_MAT'")
    Long countAbsentByStudent(@Param("maSv") String maSv);

    /**
     * Lấy thống kê theo môn học - RAW DATA (FIXED)
     */
    @Query("SELECT lhp.monHoc.maMh, lhp.monHoc.tenMh, lhp.maLhp, lhp.nhom, " +
            "COUNT(DISTINCT lh.maLich), " +
            "SUM(CASE WHEN d.trangThai = 'CO_MAT' THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN d.trangThai = 'VANG_MAT' THEN 1 ELSE 0 END), " +
            "(CASE WHEN COUNT(DISTINCT lh.maLich) > 0 THEN " +
            "SUM(CASE WHEN d.trangThai = 'CO_MAT' THEN 1 ELSE 0 END) * 100.0 / COUNT(DISTINCT lh.maLich) " +
            "ELSE 0.0 END) " +
            "FROM DiemDanh d " +
            "RIGHT JOIN d.lichHoc lh " +
            "JOIN lh.lopHocPhan lhp " +
            "WHERE d.sinhVien.maSv = :maSv " +
            "AND lhp.isActive = true " +
            "GROUP BY lhp.monHoc.maMh, lhp.monHoc.tenMh, lhp.maLhp, lhp.nhom")
    List<Object[]> getAttendanceBySubjectAndStudentRaw(@Param("maSv") String maSv);
    /**
     * Lấy lịch sử điểm danh - RAW DATA
     */
    @Query("SELECT d.id, d.ngayDiemDanh, lhp.monHoc.maMh, lhp.monHoc.tenMh, " +
            "lhp.maLhp, lhp.nhom, lh.tietBatDau, lh.soTiet, " +
            "d.trangThai, d.thoiGianVao " +
            "FROM DiemDanh d " +
            "JOIN d.lichHoc lh " +
            "JOIN lh.lopHocPhan lhp " +
            "WHERE d.sinhVien.maSv = :maSv " +
            "ORDER BY d.ngayDiemDanh DESC")
    List<Object[]> getAttendanceHistoryByStudentRaw(@Param("maSv") String maSv, Pageable pageable);

    @Query("SELECT d.id, d.ngayDiemDanh, lhp.monHoc.maMh, lhp.monHoc.tenMh, " +
            "lhp.maLhp, lhp.nhom, lh.tietBatDau, lh.soTiet, " +
            "d.trangThai, d.thoiGianVao " +
            "FROM DiemDanh d " +
            "JOIN d.lichHoc lh " +
            "JOIN lh.lopHocPhan lhp " +
            "WHERE d.sinhVien.maSv = :maSv " +
            "AND lhp.monHoc.maMh = :maMh " +
            "ORDER BY d.ngayDiemDanh DESC")
    List<Object[]> getAttendanceHistoryByStudentAndSubjectRaw(@Param("maSv") String maSv,
                                                              @Param("maMh") String maMh,
                                                              Pageable pageable);


    List<DiemDanh> findByScheduleInstanceMaInstanceAndNgayDiemDanh(String maInstance, LocalDate ngayDiemDanh);

    List<DiemDanh> findByScheduleInstanceMaInstance(String maInstance);



    /**
     * Find attendance by either legacy or week-based schedule ID
     */
    @Query("SELECT dd FROM DiemDanh dd WHERE " +
            "(dd.scheduleInstance.maInstance = :scheduleId OR dd.lichHoc.maLich = :scheduleId) " +
            "AND dd.ngayDiemDanh = :ngayDiemDanh")
    List<DiemDanh> findByScheduleIdAndDate(@Param("scheduleId") String scheduleId,
                                           @Param("ngayDiemDanh") LocalDate ngayDiemDanh);

    /**
     * Find all attendance for a LopHocPhan (both schedule types)
     */
    @Query("SELECT dd FROM DiemDanh dd WHERE " +
            "(dd.scheduleInstance.weeklySchedule.lopHocPhan.maLhp = :maLhp OR " +
            "dd.lichHoc.lopHocPhan.maLhp = :maLhp)")
    List<DiemDanh> findByLopHocPhanAllTypes(@Param("maLhp") String maLhp);

    /**
     * Find attendance by student and LopHocPhan (both types)
     */
    @Query("SELECT dd FROM DiemDanh dd WHERE dd.sinhVien.maSv = :maSv AND " +
            "(dd.scheduleInstance.weeklySchedule.lopHocPhan.maLhp = :maLhp OR " +
            "dd.lichHoc.lopHocPhan.maLhp = :maLhp)")
    List<DiemDanh> findByStudentAndLopHocPhan(@Param("maSv") String maSv, @Param("maLhp") String maLhp);

    /**
     * Count attendance by schedule type
     */
    @Query("SELECT COUNT(dd) FROM DiemDanh dd WHERE dd.scheduleInstance IS NOT NULL")
    long countWeekBasedAttendance();

    @Query("SELECT COUNT(dd) FROM DiemDanh dd WHERE dd.lichHoc IS NOT NULL")
    long countLegacyAttendance();

    /**
     * Find today's attendance (both types)
     */
    @Query("SELECT dd FROM DiemDanh dd WHERE dd.ngayDiemDanh = :today " +
            "ORDER BY dd.sinhVien.maSv")
    List<DiemDanh> findTodayAttendance(@Param("today") LocalDate today);

    /**
     * Find attendance by date range (both types)
     */
    @Query("SELECT dd FROM DiemDanh dd WHERE dd.ngayDiemDanh >= :startDate " +
            "AND dd.ngayDiemDanh <= :endDate " +
            "ORDER BY dd.ngayDiemDanh DESC")
    List<DiemDanh> findByDateRange(@Param("startDate") LocalDate startDate,
                                   @Param("endDate") LocalDate endDate);

    /**
     * Find week-based attendance by week number
     */
    @Query("SELECT dd FROM DiemDanh dd WHERE dd.scheduleInstance.tuanHoc = :tuanHoc " +
            "ORDER BY dd.ngayDiemDanh")
    List<DiemDanh> findByWeekNumber(@Param("tuanHoc") Integer tuanHoc);

    /**
     * Get attendance statistics for a student in a class
     */
    @Query("SELECT COUNT(dd) as total, " +
            "SUM(CASE WHEN dd.trangThai = 'CO_MAT' THEN 1 ELSE 0 END) as present, " +
            "SUM(CASE WHEN dd.trangThai = 'VANG_MAT' THEN 1 ELSE 0 END) as absent, " +
            "SUM(CASE WHEN dd.trangThai = 'MUON' THEN 1 ELSE 0 END) as late " +
            "FROM DiemDanh dd WHERE dd.sinhVien.maSv = :maSv AND " +
            "(dd.scheduleInstance.weeklySchedule.lopHocPhan.maLhp = :maLhp OR " +
            "dd.lichHoc.lopHocPhan.maLhp = :maLhp)")
    Object getAttendanceStatsByStudentAndClass(@Param("maSv") String maSv, @Param("maLhp") String maLhp);

    /**
     * Find attendance without time records (need manual entry)
     */
    @Query("SELECT dd FROM DiemDanh dd WHERE dd.thoiGianVao IS NULL " +
            "AND dd.ngayDiemDanh = :date " +
            "ORDER BY dd.sinhVien.maSv")
    List<DiemDanh> findPendingTimeEntry(@Param("date") LocalDate date);

    /**
     * Find attendance by teacher (both schedule types)
     */
    @Query("SELECT dd FROM DiemDanh dd WHERE " +
            "(dd.scheduleInstance.weeklySchedule.lopHocPhan.giangVien.maGv = :maGv OR " +
            "dd.lichHoc.lopHocPhan.giangVien.maGv = :maGv) " +
            "AND dd.ngayDiemDanh = :date")
    List<DiemDanh> findByTeacherAndDate(@Param("maGv") String maGv, @Param("date") LocalDate date);
}