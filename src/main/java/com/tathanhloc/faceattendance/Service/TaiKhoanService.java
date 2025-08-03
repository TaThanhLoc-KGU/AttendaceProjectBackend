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
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
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

    /**
     * Alternative: Reset password with token (more secure)
     */
    public String generatePasswordResetToken(String username) {
        TaiKhoan tk = taiKhoanRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));

        String email = getEmailFromAccount(tk);
        if (email == null || email.isEmpty()) {
            throw new RuntimeException("Tài khoản không có email để gửi link đặt lại mật khẩu");
        }

        // Generate reset token
        String resetToken = generateResetToken();

        // Save token with expiration (you may need to add these fields to TaiKhoan entity)
        // tk.setResetToken(resetToken);
        // tk.setResetTokenExpiry(LocalDateTime.now().plusHours(1));
        // taiKhoanRepository.save(tk);

        // Send email with reset link
        boolean emailSent = mailService.sendResetPasswordTokenEmail(email, resetToken);

        if (!emailSent) {
            throw new RuntimeException("Không thể gửi email đặt lại mật khẩu");
        }

        return resetToken;
    }



    /**
     * Generate reset token for password reset
     */
    private String generateResetToken() {
        return java.util.UUID.randomUUID().toString();
    }

    /**
     * Create new account with email notification
     */
    public TaiKhoan createAccountWithNotification(TaiKhoan taiKhoan, String email) {
        // Generate temporary password
        String tempPassword = generateTempPassword();
        taiKhoan.setPasswordHash(passwordEncoder.encode(tempPassword));

        // Save account
        TaiKhoan savedAccount = taiKhoanRepository.save(taiKhoan);

        // Send welcome email
        if (email != null && !email.isEmpty()) {
            boolean emailSent = mailService.sendWelcomeEmail(email, taiKhoan.getUsername(), tempPassword);
            if (emailSent) {
                log.info("Welcome email sent to: {}", email);
            } else {
                log.warn("Failed to send welcome email to: {}", email);
            }
        }

        return savedAccount;
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

    // Add these methods to your TaiKhoanService.java

    /**
     * Change password for existing user
     * @param username User's username
     * @param newPassword New password (plain text)
     * @return true if password changed successfully
     */
    public boolean changePassword(String username, String newPassword) {
        try {
            TaiKhoan taiKhoan = taiKhoanRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản với username: " + username));

            // Validate new password
            if (newPassword == null || newPassword.trim().isEmpty()) {
                throw new IllegalArgumentException("Mật khẩu mới không được để trống");
            }

            if (newPassword.length() < 6) {
                throw new IllegalArgumentException("Mật khẩu mới phải có ít nhất 6 ký tự");
            }

            // Encode and update password
            String encodedPassword = passwordEncoder.encode(newPassword);
            taiKhoan.setPasswordHash(encodedPassword);

            taiKhoanRepository.save(taiKhoan);

            log.info("Password changed successfully for user: {}", username);
            return true;

        } catch (Exception e) {
            log.error("Error changing password for user: {} - {}", username, e.getMessage());
            throw new RuntimeException("Không thể thay đổi mật khẩu: " + e.getMessage());
        }
    }

    /**
     * Change password with old password verification
     * @param username User's username
     * @param oldPassword Current password for verification
     * @param newPassword New password
     * @return true if password changed successfully
     */
    public boolean changePassword(String username, String oldPassword, String newPassword) {
        try {
            TaiKhoan taiKhoan = taiKhoanRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));

            // Verify old password
            if (!passwordEncoder.matches(oldPassword, taiKhoan.getPasswordHash())) {
                throw new IllegalArgumentException("Mật khẩu hiện tại không đúng");
            }

            // Validate new password
            if (newPassword == null || newPassword.trim().isEmpty()) {
                throw new IllegalArgumentException("Mật khẩu mới không được để trống");
            }

            if (newPassword.length() < 6) {
                throw new IllegalArgumentException("Mật khẩu mới phải có ít nhất 6 ký tự");
            }

            // Check if new password is different from old
            if (passwordEncoder.matches(newPassword, taiKhoan.getPasswordHash())) {
                throw new IllegalArgumentException("Mật khẩu mới phải khác mật khẩu hiện tại");
            }

            // Update password
            String encodedPassword = passwordEncoder.encode(newPassword);
            taiKhoan.setPasswordHash(encodedPassword);
            taiKhoanRepository.save(taiKhoan);

            log.info("Password changed successfully for user: {}", username);
            return true;

        } catch (Exception e) {
            log.error("Error changing password for user: {} - {}", username, e.getMessage());
            throw new RuntimeException("Không thể thay đổi mật khẩu: " + e.getMessage());
        }
    }


    /**
     * Reset password and send temporary password via email
     * @param username User's username
     */
    public void resetPassword(String username) {
        TaiKhoan tk = taiKhoanRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));

        String email = getEmailFromAccount(tk);
        if (email == null || email.isEmpty()) {
            throw new RuntimeException("Tài khoản không có email để gửi mật khẩu mới");
        }

        // Generate temporary password
        String tempPassword = generateTempPassword();

        // Update password in database
        tk.setPasswordHash(passwordEncoder.encode(tempPassword));
        taiKhoanRepository.save(tk);

        // Send email with temporary password
        boolean emailSent = mailService.sendResetPasswordEmail(email, tempPassword);

        if (!emailSent) {
            log.warn("Failed to send reset password email to: {}", email);
        } else {
            log.info("Password reset email sent successfully to: {}", email);
        }
    }

    /**
     * Check if password needs to be changed (first login with temp password)
     * @param username User's username
     * @return true if password should be changed
     */
    public boolean isPasswordChangeRequired(String username) {
        TaiKhoan taiKhoan = taiKhoanRepository.findByUsername(username).orElse(null);
        if (taiKhoan == null) {
            return false;
        }

        // You can implement logic here to check if password change is required
        // For example, if using temporary passwords or first login
        // return taiKhoan.isTemporaryPassword() || !taiKhoan.isPasswordChanged();

        return false; // Default implementation
    }

    /**
     * Validate password strength
     * @param password Password to validate
     * @return true if password is strong enough
     */
    public boolean validatePasswordStrength(String password) {
        if (password == null || password.length() < 6) {
            return false;
        }

        // Add more validation rules as needed
        // - Must contain uppercase letter
        // - Must contain lowercase letter
        // - Must contain number
        // - Must contain special character

        return true; // Basic validation
    }

    /**
     * Helper method to get email from account
     */
    private String getEmailFromAccount(TaiKhoan taiKhoan) {
        if (taiKhoan.getGiangVien() != null) {
            return taiKhoan.getGiangVien().getEmail();
        } else if (taiKhoan.getSinhVien() != null) {
            return taiKhoan.getSinhVien().getEmail();
        }
        return null;
    }

    /**
     * Generate temporary password
     */
    private String generateTempPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder tempPassword = new StringBuilder();
        java.util.Random random = new java.util.Random();

        for (int i = 0; i < 8; i++) {
            tempPassword.append(chars.charAt(random.nextInt(chars.length())));
        }

        return tempPassword.toString();
    }

    /**
     * Update last login time
     * @param username User's username
     */
    public void updateLastLogin(String username) {
        TaiKhoan taiKhoan = taiKhoanRepository.findByUsername(username).orElse(null);
        if (taiKhoan != null) {
            // taiKhoan.setLastLogin(LocalDateTime.now());
            taiKhoanRepository.save(taiKhoan);
            log.debug("Updated last login for user: {}", username);
        }
    }

    /**
     * Lock/unlock account
     * @param username User's username
     * @param locked true to lock, false to unlock
     */
    public void setAccountLocked(String username, boolean locked) {
        TaiKhoan taiKhoan = taiKhoanRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));

        // taiKhoan.setLocked(locked);
        taiKhoanRepository.save(taiKhoan);

        log.info("Account {} {} for user: {}",
                locked ? "locked" : "unlocked",
                locked ? "locked" : "unlocked",
                username);
    }

    // Phương thức public để AuthController có thể sử dụng
    public TaiKhoanDTO convertToDTO(TaiKhoan taiKhoan) {
        return toDTO(taiKhoan);
    }
}
