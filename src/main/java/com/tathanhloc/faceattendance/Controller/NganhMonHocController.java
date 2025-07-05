package com.tathanhloc.faceattendance.Controller;

import com.tathanhloc.faceattendance.DTO.NganhMonHocDTO;
import com.tathanhloc.faceattendance.Service.NganhMonHocService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/nganhmonhoc")
@RequiredArgsConstructor
@Slf4j
public class NganhMonHocController {

    private final NganhMonHocService nganhMonHocService;

    /**
     * Lấy tất cả mối quan hệ ngành-môn học
     */
    @GetMapping
    public ResponseEntity<List<NganhMonHocDTO>> getAll() {
        log.info("Lấy tất cả mối quan hệ ngành-môn học");
        return ResponseEntity.ok(nganhMonHocService.getAll());
    }

    /**
     * Lấy mối quan hệ theo mã ngành (chỉ active)
     */
    @GetMapping("/by-manganh/{maNganh}")
    public ResponseEntity<List<NganhMonHocDTO>> getByMaNganh(@PathVariable String maNganh) {
        log.info("Lấy mối quan hệ theo mã ngành: {}", maNganh);
        return ResponseEntity.ok(nganhMonHocService.findByMaNganh(maNganh));
    }

    /**
     * Lấy mối quan hệ theo mã môn học (chỉ active)
     */
    @GetMapping("/by-mamh/{maMh}")
    public ResponseEntity<List<NganhMonHocDTO>> getByMaMh(@PathVariable String maMh) {
        log.info("Lấy mối quan hệ theo mã môn học: {}", maMh);
        return ResponseEntity.ok(nganhMonHocService.findByMaMh(maMh));
    }

    /**
     * Lấy mối quan hệ theo mã ngành (bao gồm cả inactive)
     */
    @GetMapping("/by-manganh/{maNganh}/include-inactive")
    public ResponseEntity<List<NganhMonHocDTO>> getByMaNganhIncludeInactive(@PathVariable String maNganh) {
        log.info("Lấy mối quan hệ theo mã ngành (bao gồm inactive): {}", maNganh);
        return ResponseEntity.ok(nganhMonHocService.findByMaNganhIncludeInactive(maNganh));
    }

    /**
     * Lấy mối quan hệ theo mã môn học (bao gồm cả inactive)
     */
    @GetMapping("/by-mamh/{maMh}/include-inactive")
    public ResponseEntity<List<NganhMonHocDTO>> getByMaMhIncludeInactive(@PathVariable String maMh) {
        log.info("Lấy mối quan hệ theo mã môn học (bao gồm inactive): {}", maMh);
        return ResponseEntity.ok(nganhMonHocService.findByMaMhIncludeInactive(maMh));
    }

    /**
     * Tạo mối quan hệ ngành-môn học mới
     */
    @PostMapping
    public ResponseEntity<NganhMonHocDTO> create(@RequestBody NganhMonHocDTO dto) {
        log.info("Tạo mối quan hệ ngành-môn học: {} - {}", dto.getMaNganh(), dto.getMaMh());
        return ResponseEntity.ok(nganhMonHocService.create(dto));
    }

    /**
     * Xóa mềm mối quan hệ ngành-môn học (soft delete)
     * Set isActive = false thay vì xóa khỏi database
     */
    @DeleteMapping("/{maNganh}/{maMh}")
    public ResponseEntity<Void> softDelete(@PathVariable String maNganh, @PathVariable String maMh) {
        log.info("Xóa mềm mối quan hệ ngành-môn học: {} - {}", maNganh, maMh);
        nganhMonHocService.softDelete(maNganh, maMh);
        return ResponseEntity.noContent().build();
    }

    /**
     * Khôi phục mối quan hệ ngành-môn học đã xóa mềm
     */
    @PutMapping("/{maNganh}/{maMh}/restore")
    public ResponseEntity<Void> restore(@PathVariable String maNganh, @PathVariable String maMh) {
        log.info("Khôi phục mối quan hệ ngành-môn học: {} - {}", maNganh, maMh);
        nganhMonHocService.restore(maNganh, maMh);
        return ResponseEntity.noContent().build();
    }

    /**
     * Xóa vĩnh viễn mối quan hệ ngành-môn học (hard delete)
     */
    @DeleteMapping("/{maNganh}/{maMh}/permanent")
    public ResponseEntity<Void> hardDelete(@PathVariable String maNganh, @PathVariable String maMh) {
        log.info("Xóa vĩnh viễn mối quan hệ ngành-môn học: {} - {}", maNganh, maMh);
        nganhMonHocService.hardDelete(maNganh, maMh);
        return ResponseEntity.noContent().build();
    }
}