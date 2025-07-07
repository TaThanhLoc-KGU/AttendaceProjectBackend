package com.tathanhloc.faceattendance.Service;

import com.tathanhloc.faceattendance.DTO.GiangVienDTO;
import com.tathanhloc.faceattendance.Model.GiangVien;
import com.tathanhloc.faceattendance.Model.Khoa;
import com.tathanhloc.faceattendance.Repository.GiangVienRepository;
import com.tathanhloc.faceattendance.Repository.KhoaRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GiangVienService {

    private final GiangVienRepository giangVienRepository;
    private final KhoaRepository khoaRepository;

    public List<GiangVienDTO> getAll() {
        return giangVienRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public GiangVienDTO getById(String maGv) {
        GiangVien gv = giangVienRepository.findById(maGv)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giảng viên"));
        return toDTO(gv);
    }

    @Transactional
    public GiangVienDTO create(GiangVienDTO dto) {
        // Validate mã giảng viên
        if (giangVienRepository.existsById(dto.getMaGv())) {
            throw new RuntimeException("Mã giảng viên đã tồn tại: " + dto.getMaGv());
        }

        // Validate email
        if (giangVienRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("Email đã được sử dụng: " + dto.getEmail());
        }

        GiangVien gv = toEntity(dto);
        return toDTO(giangVienRepository.save(gv));
    }

    @Transactional
    public GiangVienDTO update(String maGv, GiangVienDTO dto) {
        GiangVien existing = giangVienRepository.findById(maGv)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giảng viên"));

        // Validate email (loại trừ chính nó)
        if (!existing.getEmail().equals(dto.getEmail()) &&
                giangVienRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("Email đã được sử dụng: " + dto.getEmail());
        }

        existing.setHoTen(dto.getHoTen());
        existing.setEmail(dto.getEmail());
        existing.setIsActive(dto.getIsActive());

        if (!existing.getKhoa().getMaKhoa().equals(dto.getMaKhoa())) {
            Khoa khoa = khoaRepository.findById(dto.getMaKhoa())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy khoa"));
            existing.setKhoa(khoa);
        }

        return toDTO(giangVienRepository.save(existing));
    }

    // Mapping
    private GiangVienDTO toDTO(GiangVien gv) {
        return GiangVienDTO.builder()
                .maGv(gv.getMaGv())
                .hoTen(gv.getHoTen())
                .email(gv.getEmail())
                .isActive(gv.getIsActive())
                .maKhoa(gv.getKhoa().getMaKhoa())
                .build();
    }

    private GiangVien toEntity(GiangVienDTO dto) {
        Khoa khoa = khoaRepository.findById(dto.getMaKhoa())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khoa"));

        return GiangVien.builder()
                .maGv(dto.getMaGv())
                .hoTen(dto.getHoTen())
                .email(dto.getEmail())
                .isActive(dto.getIsActive())
                .khoa(khoa)
                .build();
    }

    public GiangVienDTO getByMaGv(String maGv) {
        GiangVien gv = giangVienRepository.findById(maGv)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giảng viên"));
        return toDTO(gv);
    }

    // Thêm các methods sau vào GiangVienService.java hiện tại:

    /**
     * Lấy danh sách giảng viên đang hoạt động
     */
    public List<GiangVienDTO> getAllActive() {
        return giangVienRepository.findByIsActiveTrue().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    /**
     * Lấy danh sách giảng viên đã nghỉ việc
     */
    public List<GiangVienDTO> getAllInactive() {
        return giangVienRepository.findByIsActiveFalse().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public boolean existsByMaGv(String maGv) {
        return giangVienRepository.existsById(maGv);
    }

    public boolean existsByEmail(String email, String excludeMaGv) {
        if (excludeMaGv != null && !excludeMaGv.trim().isEmpty()) {
            // Đang edit - loại trừ chính nó
            return giangVienRepository.existsByEmailAndMaGvNot(email, excludeMaGv);
        } else {
            // Đang tạo mới
            return giangVienRepository.existsByEmail(email);
        }
    }


    /**
     * Xóa mềm giảng viên (soft delete)
     */
    @Transactional
    public void softDelete(String maGv) {
        GiangVien gv = giangVienRepository.findById(maGv)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giảng viên"));
        gv.setIsActive(false);
        giangVienRepository.save(gv);
    }
    /**
     * Khôi phục giảng viên
     */
    @Transactional
    public void restore(String maGv) {
        GiangVien gv = giangVienRepository.findById(maGv)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giảng viên"));
        gv.setIsActive(true);
        giangVienRepository.save(gv);
    }

    /**
     * Đếm số lượng theo trạng thái
     */
    public long countByStatus(boolean isActive) {
        return isActive ?
                giangVienRepository.countByIsActiveTrue() :
                giangVienRepository.countByIsActiveFalse();
    }


    /**
     * Tìm kiếm giảng viên
     */
    public List<GiangVienDTO> search(String keyword) {
        return giangVienRepository.findByHoTenContainingIgnoreCaseOrEmailContainingIgnoreCase(keyword, keyword)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public void delete(String maGv) {
        softDelete(maGv); // Thay đổi từ hard delete sang soft delete
    }


    /**********************************/


}
