package com.tathanhloc.faceattendance.Controller;

import com.tathanhloc.faceattendance.DTO.SinhVienDTO;
import com.tathanhloc.faceattendance.Service.SinhVienService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sinhvien")
@RequiredArgsConstructor
@Slf4j
public class SinhVienController {

    private final SinhVienService sinhVienService;

    @GetMapping
    public ResponseEntity<Page<SinhVienDTO>> getAll(@RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "10") int size,
                                                    @RequestParam(defaultValue = "maSv") String sortBy,
                                                    @RequestParam(defaultValue = "asc") String direction) {
        Pageable pageable = PageRequest.of(
                page,
                size,
                direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending()
        );
        return ResponseEntity.ok(sinhVienService.getAllWithPagination(pageable));
    }


    @GetMapping("/active")
    public ResponseEntity<List<SinhVienDTO>> getAllActive() {
        log.info("Lấy danh sách sinh viên đang hoạt động");
        return ResponseEntity.ok(sinhVienService.getAllActive());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SinhVienDTO> getById(@PathVariable String id) {
        log.info("Lấy thông tin sinh viên với ID: {}", id);
        return ResponseEntity.ok(sinhVienService.getById(id));
    }

    @PostMapping
    public ResponseEntity<SinhVienDTO> create(@Valid @RequestBody SinhVienDTO dto) {
        log.info("Tạo sinh viên mới: {}", dto);
        return ResponseEntity.ok(sinhVienService.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SinhVienDTO> update(@PathVariable String id, @Valid @RequestBody SinhVienDTO dto) {
        log.info("Cập nhật sinh viên với ID {}: {}", id, dto);
        return ResponseEntity.ok(sinhVienService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        log.info("Xóa sinh viên với ID: {}", id);
        sinhVienService.softDelete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/restore")
    public ResponseEntity<Void> restore(@PathVariable String id) {
        log.info("Khôi phục sinh viên với ID: {}", id);
        sinhVienService.restore(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/by-masv/{maSv}")
    public ResponseEntity<SinhVienDTO> getByMaSv(@PathVariable String maSv) {
        log.info("Tìm sinh viên theo mã: {}", maSv);
        return ResponseEntity.ok(sinhVienService.getByMaSv(maSv));
    }

    /**
     * API lấy tất cả embedding
     * @return Danh sách embedding
     */
    @GetMapping("/embeddings")
    public ResponseEntity<List<Map<String, Object>>> getAllEmbeddings() {
        return ResponseEntity.ok(sinhVienService.getAllEmbeddings());
    }

    /**
     * API lấy embedding của một sinh viên
     * @param maSv Mã sinh viên
     * @return Embedding của sinh viên
     */
    @GetMapping("/students/{maSv}/embedding")
    public ResponseEntity<Map<String, Object>> getEmbeddingByMaSv(@PathVariable String maSv) {
        return ResponseEntity.ok(sinhVienService.getEmbeddingByMaSv(maSv));
    }

    /**
     * API lưu embedding cho một sinh viên
     * @param maSv Mã sinh viên
     * @param requestBody Body chứa embedding
     * @return SinhVienDTO đã cập nhật
     */
    @PostMapping("/students/{maSv}/embedding")
    public ResponseEntity<SinhVienDTO> saveEmbedding(
            @PathVariable String maSv,
            @RequestBody Map<String, String> requestBody) {
        String embedding = requestBody.get("embedding");
        if (embedding == null || embedding.isEmpty()) {
            throw new RuntimeException("Embedding không được để trống");
        }
        return ResponseEntity.ok(sinhVienService.saveEmbedding(maSv, embedding));
    }
}
