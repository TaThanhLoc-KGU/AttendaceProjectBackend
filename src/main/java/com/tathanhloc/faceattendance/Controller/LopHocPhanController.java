package com.tathanhloc.faceattendance.Controller;

import com.tathanhloc.faceattendance.DTO.*;
import com.tathanhloc.faceattendance.Service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/lophocphan")
@RequiredArgsConstructor
public class LopHocPhanController {

    private final LopHocPhanService lopHocPhanService;
    private final DangKyHocService dangKyHocService;
    private final SinhVienService sinhVienService;

    @GetMapping
    public List<LopHocPhanDTO> getAll() {
        return lopHocPhanService.getAllWithNames(); // SỬ DỤNG METHOD MỚI
    }

    @GetMapping("/{id}")
    public LopHocPhanDTO getById(@PathVariable String id) {
        return lopHocPhanService.getById(id);
    }

    @PostMapping
    public LopHocPhanDTO create(@RequestBody LopHocPhanDTO dto) {
        return lopHocPhanService.create(dto);
    }

    @PutMapping("/{id}")
    public LopHocPhanDTO update(@PathVariable String id, @RequestBody LopHocPhanDTO dto) {
        return lopHocPhanService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        lopHocPhanService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/by-malhp/{maLhp}")
    public ResponseEntity<LopHocPhanDTO> getByMaLhp(@PathVariable String maLhp) {
        return ResponseEntity.ok(lopHocPhanService.getByMaLhp(maLhp));
    }

    // ============ STUDENT MANAGEMENT APIs ============

    /**
     * Lấy danh sách sinh viên trong lớp học phần
     */
    @GetMapping("/{maLhp}/sinhvien")
    public ResponseEntity<List<SinhVienDTO>> getStudentsByLhp(@PathVariable String maLhp) {
        try {
            List<DangKyHocDTO> dangKyList = dangKyHocService.getByMaLhp(maLhp);
            List<SinhVienDTO> students = dangKyList.stream()
                    .map(dk -> sinhVienService.getByMaSv(dk.getMaSv()))
                    .toList();
            return ResponseEntity.ok(students);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi lấy danh sách sinh viên: " + e.getMessage());
        }
    }

    /**
     * Thêm sinh viên vào lớp học phần
     */
    @PostMapping("/{maLhp}/sinhvien/{maSv}")
    public ResponseEntity<String> addStudentToLhp(@PathVariable String maLhp, @PathVariable String maSv) {
        try {
            // Kiểm tra lớp học phần tồn tại
            lopHocPhanService.getByMaLhp(maLhp);

            // Kiểm tra sinh viên tồn tại
            sinhVienService.getByMaSv(maSv);

            // Tạo đăng ký học
            DangKyHocDTO dangKyDTO = DangKyHocDTO.builder()
                    .maSv(maSv)
                    .maLhp(maLhp)
                    .build();

            dangKyHocService.create(dangKyDTO);
            return ResponseEntity.ok("Thêm sinh viên thành công");
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi thêm sinh viên: " + e.getMessage());
        }
    }

    /**
     * Xóa sinh viên khỏi lớp học phần
     */
    @DeleteMapping("/{maLhp}/sinhvien/{maSv}")
    public ResponseEntity<String> removeStudentFromLhp(@PathVariable String maLhp, @PathVariable String maSv) {
        try {
            dangKyHocService.delete(maSv, maLhp);
            return ResponseEntity.ok("Xóa sinh viên thành công");
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi xóa sinh viên: " + e.getMessage());
        }
    }

    /**
     * Chuyển sinh viên sang nhóm khác
     */
    @PostMapping("/transfer")
    public ResponseEntity<String> transferStudent(@RequestBody Map<String, String> transferData) {
        try {
            String maSv = transferData.get("maSv");
            String fromLhp = transferData.get("fromLhp");
            String toLhp = transferData.get("toLhp");

            // Xóa khỏi lớp cũ
            dangKyHocService.delete(maSv, fromLhp);

            // Thêm vào lớp mới
            DangKyHocDTO dangKyDTO = DangKyHocDTO.builder()
                    .maSv(maSv)
                    .maLhp(toLhp)
                    .build();
            dangKyHocService.create(dangKyDTO);

            return ResponseEntity.ok("Chuyển nhóm thành công");
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi chuyển nhóm: " + e.getMessage());
        }
    }
    // THÊM ENDPOINT MỚI CHỈ LẤY LỚP ACTIVE
    @GetMapping("/active")
    public List<LopHocPhanDTO> getAllActive() {
        return lopHocPhanService.getAllActiveWithNames();
    }

}