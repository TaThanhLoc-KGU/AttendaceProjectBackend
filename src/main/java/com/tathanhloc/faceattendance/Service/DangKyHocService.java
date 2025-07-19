package com.tathanhloc.faceattendance.Service;

import com.tathanhloc.faceattendance.DTO.DangKyHocDTO;
import com.tathanhloc.faceattendance.DTO.SinhVienDTO;
import com.tathanhloc.faceattendance.Model.DangKyHoc;
import com.tathanhloc.faceattendance.Model.DangKyHocId;
import com.tathanhloc.faceattendance.Model.LopHocPhan;
import com.tathanhloc.faceattendance.Model.SinhVien;
import com.tathanhloc.faceattendance.Repository.DangKyHocRepository;
import com.tathanhloc.faceattendance.Repository.LopHocPhanRepository;
import com.tathanhloc.faceattendance.Repository.SinhVienRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DangKyHocService {

    private final DangKyHocRepository dangKyHocRepository;
    private final SinhVienRepository sinhVienRepository;
    private final LopHocPhanRepository lopHocPhanRepository;

    public List<DangKyHocDTO> getAll() {
        return dangKyHocRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public DangKyHocDTO getById(String maSv, String maLhp) {
        DangKyHocId id = new DangKyHocId(maSv, maLhp);
        return toDTO(dangKyHocRepository.findById(id).orElseThrow());
    }

    public DangKyHocDTO create(DangKyHocDTO dto) {
        SinhVien sv = sinhVienRepository.findById(dto.getMaSv()).orElseThrow();
        LopHocPhan lhp = lopHocPhanRepository.findById(dto.getMaLhp()).orElseThrow();

        DangKyHoc dk = DangKyHoc.builder()
                .id(new DangKyHocId(dto.getMaSv(), dto.getMaLhp()))
                .sinhVien(sv)
                .lopHocPhan(lhp)
                .isActive(true)
                .build();

        return toDTO(dangKyHocRepository.save(dk));
    }

    /**
     * Thêm sinh viên với validation (không cho phép trùng môn học)
     */
    public DangKyHocDTO createWithValidation(DangKyHocDTO dto) {
        // Lấy thông tin lớp học phần
        LopHocPhan lhp = lopHocPhanRepository.findById(dto.getMaLhp())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lớp học phần"));

        String maMh = lhp.getMonHoc().getMaMh();

        // Kiểm tra sinh viên đã đăng ký môn học này chưa
        List<DangKyHoc> existingRegistrations = dangKyHocRepository.findBySinhVienAndMonHoc(dto.getMaSv(), maMh);

        if (!existingRegistrations.isEmpty()) {
            DangKyHoc existing = existingRegistrations.get(0);
            String currentLhp = existing.getLopHocPhan().getMaLhp();
            throw new RuntimeException(
                    String.format("Sinh viên đã đăng ký môn học '%s' ở nhóm khác (Lớp: %s). " +
                                    "Vui lòng chuyển nhóm thay vì thêm mới.",
                            lhp.getMonHoc().getTenMh(), currentLhp)
            );
        }

        // Nếu chưa có trong môn học, tiến hành thêm bình thường
        return create(dto);
    }

    /**
     * Chuyển nhóm trong cùng môn học
     */
    @Transactional
    public void transferStudent(String maSv, String fromLhp, String toLhp) {
        // Kiểm tra 2 lớp có cùng môn học không
        LopHocPhan fromClass = lopHocPhanRepository.findById(fromLhp)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lớp học phần nguồn"));
        LopHocPhan toClass = lopHocPhanRepository.findById(toLhp)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lớp học phần đích"));

        if (!fromClass.getMonHoc().getMaMh().equals(toClass.getMonHoc().getMaMh())) {
            throw new RuntimeException("Chỉ có thể chuyển giữa các nhóm trong cùng môn học!");
        }

        // Kiểm tra sinh viên có trong lớp nguồn không
        if (!dangKyHocRepository.existsByIdMaSvAndIdMaLhpAndIsActiveTrue(maSv, fromLhp)) {
            throw new RuntimeException("Sinh viên không có trong lớp học phần nguồn");
        }

        // Kiểm tra sinh viên đã có trong lớp đích chưa
        if (dangKyHocRepository.existsByIdMaSvAndIdMaLhpAndIsActiveTrue(maSv, toLhp)) {
            throw new RuntimeException("Sinh viên đã có trong lớp học phần đích");
        }

        // Thực hiện chuyển (xóa khỏi lớp cũ, thêm vào lớp mới)
        delete(maSv, fromLhp);

        DangKyHocDTO newRegistration = DangKyHocDTO.builder()
                .maSv(maSv)
                .maLhp(toLhp)
                .isActive(true)
                .build();
        create(newRegistration);

        log.info("✅ Transferred student {} from {} to {}", maSv, fromLhp, toLhp);
    }

    /**
     * Lấy danh sách sinh viên có thể thêm vào lớp (chưa học môn này)
     */
    public List<SinhVienDTO> getAvailableStudentsForSubject(String maLhp) {
        LopHocPhan lhp = lopHocPhanRepository.findById(maLhp)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lớp học phần"));

        String maMh = lhp.getMonHoc().getMaMh();

        // Lấy tất cả sinh viên hoạt động
        List<SinhVien> allStudents = sinhVienRepository.findByIsActive(true);

        // Filter những sinh viên chưa đăng ký môn học này
        return allStudents.stream()
                .filter(sv -> !dangKyHocRepository.existsBySinhVienAndMonHoc(sv.getMaSv(), maMh))
                .map(this::convertToSinhVienDTO)
                .collect(Collectors.toList());
    }

    public void delete(String maSv, String maLhp) {
        dangKyHocRepository.deleteById(new DangKyHocId(maSv, maLhp));
    }

    private DangKyHocDTO toDTO(DangKyHoc e) {
        return DangKyHocDTO.builder()
                .maSv(e.getSinhVien().getMaSv())
                .maLhp(e.getLopHocPhan().getMaLhp())
                .isActive(e.getSinhVien().getIsActive())
                .build();
    }

    private SinhVienDTO convertToSinhVienDTO(SinhVien sv) {
        return SinhVienDTO.builder()
                .maSv(sv.getMaSv())
                .hoTen(sv.getHoTen())
                .gioiTinh(sv.getGioiTinh())
                .ngaySinh(sv.getNgaySinh())
                .email(sv.getEmail())
                .hinhAnh(sv.getHinhAnh())
                .isActive(sv.getIsActive())
                .maLop(sv.getLop().getMaLop())
                .build();
    }

    public List<DangKyHocDTO> getByMaSv(String maSv) {
        return dangKyHocRepository.findBySinhVien_MaSv(maSv).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<DangKyHocDTO> getByMaLhp(String maLhp) {
        return dangKyHocRepository.findByLopHocPhan_MaLhp(maLhp).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
}