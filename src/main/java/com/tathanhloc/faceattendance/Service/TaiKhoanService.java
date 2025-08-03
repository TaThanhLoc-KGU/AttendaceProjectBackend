package com.tathanhloc.faceattendance.Service;

import com.tathanhloc.faceattendance.DTO.TaiKhoanDTO;
import com.tathanhloc.faceattendance.Enum.VaiTroEnum;
import com.tathanhloc.faceattendance.Model.GiangVien;
import com.tathanhloc.faceattendance.Model.SinhVien;
import com.tathanhloc.faceattendance.Model.TaiKhoan;
import com.tathanhloc.faceattendance.Repository.GiangVienRepository;
import com.tathanhloc.faceattendance.Repository.SinhVienRepository;
import com.tathanhloc.faceattendance.Repository.TaiKhoanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaiKhoanService extends BaseService<TaiKhoan, Long, TaiKhoanDTO> {

    private final TaiKhoanRepository taiKhoanRepository;
    private final SinhVienRepository sinhVienRepository;
    private final GiangVienRepository giangVienRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;

    @Override
    protected JpaRepository<TaiKhoan, Long> getRepository() {
        return taiKhoanRepository;
    }

    @Override
    protected void setActive(TaiKhoan entity, boolean active) {
        entity.setIsActive(active);
    }

    @Override
    protected boolean isActive(TaiKhoan entity) {
        return entity.getIsActive() != null && entity.getIsActive();
    }

// Thêm vào TaiKhoanService.java

    public boolean existsByUsername(String username) {
        return taiKhoanRepository.existsByUsername(username);
    }

    // Cải thiện method create() trong TaiKhoanService.java
    public TaiKhoanDTO create(TaiKhoanDTO dto) {
        try {
            // Validation
            if (dto.getUsername() == null || dto.getUsername().trim().isEmpty()) {
                throw new RuntimeException("Username không được để trống");
            }

            if (existsByUsername(dto.getUsername())) {
                throw new RuntimeException("Username đã tồn tại: " + dto.getUsername());
            }

            // Set default values
            if (dto.getCreatedAt() == null) {
                dto.setCreatedAt(LocalDateTime.now());
            }

            if (dto.getIsActive() == null) {
                dto.setIsActive(true);
            }

            TaiKhoan entity = toEntity(dto);
            entity.setId(null); // Đảm bảo tạo mới

            TaiKhoan saved = taiKhoanRepository.save(entity);
            return toDTO(saved);

        } catch (Exception e) {
            System.err.println("❌ Error in TaiKhoanService.create(): " + e.getMessage());
            throw e;
        }
    }

    public TaiKhoanDTO update(Long id, TaiKhoanDTO dto) {
        TaiKhoan existing = taiKhoanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));

        existing.setUsername(dto.getUsername());
        if (dto.getPasswordHash() != null && !dto.getPasswordHash().equals(existing.getPasswordHash())) {
            existing.setPasswordHash(passwordEncoder.encode(dto.getPasswordHash()));
        }
        existing.setVaiTro(dto.getVaiTro());
        existing.setIsActive(dto.getIsActive());
        existing.setCreatedAt(dto.getCreatedAt());

        if (dto.getMaSv() != null) {
            existing.setSinhVien(sinhVienRepository.findById(dto.getMaSv())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy sinh viên")));
        } else {
            existing.setSinhVien(null);
        }

        if (dto.getMaGv() != null) {
            existing.setGiangVien(giangVienRepository.findById(dto.getMaGv())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy giảng viên")));
        } else {
            existing.setGiangVien(null);
        }

        return toDTO(taiKhoanRepository.save(existing));
    }

    @Override
    protected TaiKhoanDTO toDTO(TaiKhoan tk) {
        return TaiKhoanDTO.builder()
                .id(tk.getId())
                .username(tk.getUsername())
                .passwordHash(tk.getPasswordHash())
                .vaiTro(tk.getVaiTro())
                .isActive(tk.getIsActive())
                .createdAt(tk.getCreatedAt())
                .maSv(tk.getSinhVien() != null ? tk.getSinhVien().getMaSv() : null)
                .maGv(tk.getGiangVien() != null ? tk.getGiangVien().getMaGv() : null)
                .build();
    }

    @Override
    protected TaiKhoan toEntity(TaiKhoanDTO dto) {
        SinhVien sv = dto.getMaSv() != null ? sinhVienRepository.findById(dto.getMaSv()).orElse(null) : null;
        GiangVien gv = dto.getMaGv() != null ? giangVienRepository.findById(dto.getMaGv()).orElse(null) : null;

        return TaiKhoan.builder()
                .id(dto.getId())
                .username(dto.getUsername())
                .passwordHash(dto.getPasswordHash() != null && !dto.getPasswordHash().startsWith("$2a$") ?
                        passwordEncoder.encode(dto.getPasswordHash()) : dto.getPasswordHash())
                .vaiTro(dto.getVaiTro())
                .isActive(dto.getIsActive())
                .createdAt(dto.getCreatedAt())
                .sinhVien(sv)
                .giangVien(gv)
                .build();
    }

    public void resetPassword(String username) {
        TaiKhoan tk = taiKhoanRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));

        String email = null;

        if (tk.getGiangVien() != null) {
            email = tk.getGiangVien().getEmail();
        } else if (tk.getSinhVien() != null) {
            email = tk.getSinhVien().getEmail();
        }

        if (email == null || email.isEmpty()) {
            throw new RuntimeException("Tài khoản không có email để gửi mật khẩu mới");
        }

        String tempPassword = generateTempPassword();
        tk.setPasswordHash(passwordEncoder.encode(tempPassword));
        taiKhoanRepository.save(tk);

        mailService.sendResetPasswordEmail(email, tempPassword);
    }

    private String generateTempPassword() {
        return UUID.randomUUID().toString().substring(0, 8); // VD: "a9b3d7e2"
    }

    public TaiKhoan changePassword(String username, String newPassword) {
        TaiKhoan tk = taiKhoanRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));

        tk.setPasswordHash(passwordEncoder.encode(newPassword));
        return taiKhoanRepository.save(tk);
    }

    public TaiKhoanDTO getByUsername(String username) {
        TaiKhoan tk = taiKhoanRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));
        return toDTO(tk);
    }

    // Chỉ lấy tài khoản đang hoạt động
    @Override
    public List<TaiKhoanDTO> getAllActive() {
        return taiKhoanRepository.findAll().stream()
                .filter(tk -> tk.getIsActive() != null && tk.getIsActive())
                .map(this::toDTO)
                .toList();
    }

    // Phương thức public để AuthController có thể sử dụng
    public TaiKhoanDTO convertToDTO(TaiKhoan taiKhoan) {
        return toDTO(taiKhoan);
    }
}
