package com.tathanhloc.faceattendance.Controller;

import com.tathanhloc.faceattendance.DTO.*;
import com.tathanhloc.faceattendance.Service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/taikhoan")
@RequiredArgsConstructor
public class TaiKhoanController {

    private final TaiKhoanService taiKhoanService;

    @GetMapping
    public List<TaiKhoanDTO> getAll() {
        return taiKhoanService.getAll();
    }

    @GetMapping("/{id}")
    public TaiKhoanDTO getById(@PathVariable Long id) {
        return taiKhoanService.getById(id);
    }

// Thay thế method create() trong TaiKhoanController.java

    @PostMapping
    public ResponseEntity<?> create(@RequestBody TaiKhoanDTO dto) {
        try {
            // Validation cơ bản
            if (dto.getUsername() == null || dto.getUsername().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Username không được để trống");
            }

            if (dto.getPasswordHash() == null || dto.getPasswordHash().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Password không được để trống");
            }

            if (dto.getVaiTro() == null) {
                return ResponseEntity.badRequest().body("Vai trò không được để trống");
            }

            // Kiểm tra username đã tồn tại chưa
            if (taiKhoanService.existsByUsername(dto.getUsername())) {
                return ResponseEntity.badRequest().body("Username đã tồn tại: " + dto.getUsername());
            }

            // Set createdAt nếu null
            if (dto.getCreatedAt() == null) {
                dto.setCreatedAt(LocalDateTime.now());
            }

            // Set isActive mặc định nếu null
            if (dto.getIsActive() == null) {
                dto.setIsActive(true);
            }

            TaiKhoanDTO result = taiKhoanService.create(dto);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            System.err.println("❌ Error creating account: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi tạo tài khoản: " + e.getMessage());
        }
    }

    // Thêm method này vào TaiKhoanController
    public boolean existsByUsername(String username) {
        return taiKhoanService.existsByUsername(username);
    }

    @PutMapping("/{id}")
    public TaiKhoanDTO update(@PathVariable Long id, @RequestBody TaiKhoanDTO dto) {
        return taiKhoanService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        taiKhoanService.softDelete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/by-username/{username}")
    public ResponseEntity<TaiKhoanDTO> getByUsername(@PathVariable String username) {
        return ResponseEntity.ok(taiKhoanService.getByUsername(username));
    }

}
