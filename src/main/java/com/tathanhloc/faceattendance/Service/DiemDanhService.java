package com.tathanhloc.faceattendance.Service;

import com.tathanhloc.faceattendance.DTO.DiemDanhDTO;
import com.tathanhloc.faceattendance.Enum.TrangThaiDiemDanhEnum;
import com.tathanhloc.faceattendance.Exception.ResourceNotFoundException;
import com.tathanhloc.faceattendance.Model.*;
import com.tathanhloc.faceattendance.Repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DiemDanhService extends BaseService<DiemDanh, Long, DiemDanhDTO> {

    private final DiemDanhRepository diemDanhRepository;
    private final LichHocRepository lichHocRepository;
    private final SinhVienRepository sinhVienRepository;
    private final DangKyHocRepository dangKyHocRepository;
    private final CameraRepository cameraRepository;

    @Override
    protected JpaRepository<DiemDanh, Long> getRepository() {
        return diemDanhRepository;
    }

    @Override
    protected void setActive(DiemDanh entity, boolean active) {
        // DiemDanh không có trường isActive
    }

    @Override
    protected boolean isActive(DiemDanh entity) {
        // DiemDanh không có trường isActive, luôn trả về true
        return true;
    }

    @Transactional
    public DiemDanhDTO create(DiemDanhDTO dto) {
        log.info("Tạo điểm danh mới: {}", dto);

        // Kiểm tra sinh viên có đăng ký lớp học phần không
        LichHoc lichHoc = lichHocRepository.findById(dto.getMaLich())
                .orElseThrow(() -> new ResourceNotFoundException("Lịch học", "mã lịch", dto.getMaLich()));

        String maLhp = lichHoc.getLopHocPhan().getMaLhp();
        String maSv = dto.getMaSv();

        DangKyHocId dangKyHocId = new DangKyHocId(maSv, maLhp);
        DangKyHoc dangKyHoc = dangKyHocRepository.findById(dangKyHocId)
                .orElseThrow(() -> new RuntimeException("Sinh viên chưa đăng ký lớp học phần này"));

        if (!dangKyHoc.isActive()) {
            throw new RuntimeException("Đăng ký học phần không còn hiệu lực");
        }

        DiemDanh entity = toEntity(dto);
        entity.setId(null); // auto-generated
        return toDTO(diemDanhRepository.save(entity));
    }

    @Transactional
    public DiemDanhDTO update(Long id, DiemDanhDTO dto) {
        log.info("Cập nhật điểm danh ID {}: {}", id, dto);

        DiemDanh existing = diemDanhRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Điểm danh", "ID", id));

        existing.setNgayDiemDanh(dto.getNgayDiemDanh());
        existing.setTrangThai(dto.getTrangThai());
        existing.setThoiGianVao(dto.getThoiGianVao());
        existing.setThoiGianRa(dto.getThoiGianRa());

        if (!existing.getLichHoc().getMaLich().equals(dto.getMaLich())) {
            existing.setLichHoc(lichHocRepository.findById(dto.getMaLich())
                    .orElseThrow(() -> new ResourceNotFoundException("Lịch học", "mã lịch", dto.getMaLich())));
        }

        if (!existing.getSinhVien().getMaSv().equals(dto.getMaSv())) {
            existing.setSinhVien(sinhVienRepository.findById(dto.getMaSv())
                    .orElseThrow(() -> new ResourceNotFoundException("Sinh viên", "mã sinh viên", dto.getMaSv())));

            // Kiểm tra sinh viên có đăng ký lớp học phần không
            String maLhp = existing.getLichHoc().getLopHocPhan().getMaLhp();
            String maSv = dto.getMaSv();

            DangKyHocId dangKyHocId = new DangKyHocId(maSv, maLhp);
            DangKyHoc dangKyHoc = dangKyHocRepository.findById(dangKyHocId)
                    .orElseThrow(() -> new RuntimeException("Sinh viên chưa đăng ký lớp học phần này"));

            if (!dangKyHoc.isActive()) {
                throw new RuntimeException("Đăng ký học phần không còn hiệu lực");
            }
        }

        return toDTO(diemDanhRepository.save(existing));
    }

    public void delete(Long id) {
        log.info("Xóa điểm danh ID: {}", id);

        if (!diemDanhRepository.existsById(id)) {
            throw new ResourceNotFoundException("Điểm danh", "ID", id);
        }
        diemDanhRepository.deleteById(id);
    }

    // Mapping
    @Override
    protected DiemDanhDTO toDTO(DiemDanh d) {
        return DiemDanhDTO.builder()
                .id(d.getId())
                .ngayDiemDanh(d.getNgayDiemDanh())
                .trangThai(d.getTrangThai())
                .thoiGianVao(d.getThoiGianVao())
                .thoiGianRa(d.getThoiGianRa())
                .maLich(d.getLichHoc().getMaLich())
                .maSv(d.getSinhVien().getMaSv())
                .build();
    }

    @Override
    protected DiemDanh toEntity(DiemDanhDTO dto) {
        LichHoc lichHoc = lichHocRepository.findById(dto.getMaLich())
                .orElseThrow(() -> new ResourceNotFoundException("Lịch học", "mã lịch", dto.getMaLich()));

        SinhVien sinhVien = sinhVienRepository.findById(dto.getMaSv())
                .orElseThrow(() -> new ResourceNotFoundException("Sinh viên", "mã sinh viên", dto.getMaSv()));

        return DiemDanh.builder()
                .id(dto.getId())
                .ngayDiemDanh(dto.getNgayDiemDanh() != null ? dto.getNgayDiemDanh() : LocalDate.now())
                .trangThai(dto.getTrangThai() != null ? dto.getTrangThai() : TrangThaiDiemDanhEnum.CO_MAT)
                .thoiGianVao(dto.getThoiGianVao())
                .thoiGianRa(dto.getThoiGianRa())
                .lichHoc(lichHoc)
                .sinhVien(sinhVien)
                .build();
    }

    public List<DiemDanhDTO> getByMaSv(String maSv) {
        log.info("Lấy danh sách điểm danh theo mã sinh viên: {}", maSv);

        if (!sinhVienRepository.existsById(maSv)) {
            throw new ResourceNotFoundException("Sinh viên", "mã sinh viên", maSv);
        }

        return diemDanhRepository.findBySinhVienMaSv(maSv).stream()
                .map(this::toDTO).toList();
    }

    public List<DiemDanhDTO> getByMaLich(String maLich) {
        log.info("Lấy danh sách điểm danh theo mã lịch: {}", maLich);

        if (!lichHocRepository.existsById(maLich)) {
            throw new ResourceNotFoundException("Lịch học", "mã lịch", maLich);
        }

        return diemDanhRepository.findByLichHocMaLich(maLich).stream()
                .map(this::toDTO).toList();
    }

    public long countTodayDiemDanh() {
            log.info("Đếm số lượng điểm danh trong ngày");
            return diemDanhRepository.countByNgayDiemDanh(LocalDate.now());
        }



    /**
     * API chính cho camera gọi - chỉ cần studentId và cameraId
     */
    @Transactional
    public DiemDanhDTO recordAttendanceFromCamera(String maSv, Long cameraId) {
        // 1. Lấy camera và phòng học
        Camera camera = cameraRepository.findById(cameraId)
                .orElseThrow(() -> new ResourceNotFoundException("Camera", "id", cameraId));

        if (camera.getMaPhong() == null) {
            throw new RuntimeException("Camera chưa được gán phòng học");
        }

        // 2. Tìm lịch học hiện tại ở phòng này
        String maLich = findCurrentScheduleAtRoom(camera.getMaPhong().getMaPhong());

        // 3. Tạo DTO và gọi method create() có sẵn
        DiemDanhDTO dto = DiemDanhDTO.builder()
                .maSv(maSv)
                .maLich(maLich)
                .ngayDiemDanh(LocalDate.now())
                .thoiGianVao(LocalTime.now())
                .trangThai(TrangThaiDiemDanhEnum.CO_MAT)
                .build();

        return create(dto); // Sử dụng logic create() đã có
    }

    /**
     * Tìm lịch học đang diễn ra tại phòng
     */
    private String findCurrentScheduleAtRoom(String maPhong) {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        int dayOfWeek = today.getDayOfWeek().getValue(); // 1=Mon, 7=Sun

        // Tìm tất cả lịch học ở phòng này hôm nay
        List<LichHoc> schedules = lichHocRepository
                .findByPhongHocMaPhongAndThuAndIsActiveTrue(maPhong, dayOfWeek);

        // Tìm lịch học đang diễn ra
        for (LichHoc lichHoc : schedules) {
            if (isTimeInSchedule(lichHoc, now)) {
                return lichHoc.getMaLich();
            }
        }

        throw new RuntimeException("Không có lịch học nào đang diễn ra tại phòng này");
    }

    /**
     * Kiểm tra thời gian hiện tại có trong khung giờ học không
     */
    private boolean isTimeInSchedule(LichHoc lichHoc, LocalTime currentTime) {
        // Giả sử: Tiết 1 = 7:00, mỗi tiết 45 phút + nghỉ 5 phút
        LocalTime startTime = LocalTime.of(7, 0).plusMinutes((lichHoc.getTietBatDau() - 1) * 50);
        LocalTime endTime = startTime.plusMinutes(lichHoc.getSoTiet() * 50);

        return !currentTime.isBefore(startTime) && !currentTime.isAfter(endTime);
    }

}
