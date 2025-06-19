package com.tathanhloc.faceattendance.Controller;

import com.tathanhloc.faceattendance.DTO.DangKyHocDTO;
import com.tathanhloc.faceattendance.Service.DangKyHocService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dangkyhoc")
@RequiredArgsConstructor
public class DangKyHocController {

    private final DangKyHocService dangKyHocService;

    @GetMapping
    public ResponseEntity<List<DangKyHocDTO>> getAll() {
        return ResponseEntity.ok(dangKyHocService.getAll());
    }

    @GetMapping("/{maSv}/{maLhp}")
    public ResponseEntity<DangKyHocDTO> getById(@PathVariable String maSv, @PathVariable String maLhp) {
        return ResponseEntity.ok(dangKyHocService.getById(maSv, maLhp));
    }

    @PostMapping
    public ResponseEntity<DangKyHocDTO> create(@RequestBody DangKyHocDTO dto) {
        return ResponseEntity.ok(dangKyHocService.create(dto));
    }

    @DeleteMapping("/{maSv}/{maLhp}")
    public ResponseEntity<Void> delete(@PathVariable String maSv, @PathVariable String maLhp) {
        dangKyHocService.delete(maSv, maLhp);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/by-masv/{maSv}")
    public ResponseEntity<List<DangKyHocDTO>> getByMaSv(@PathVariable String maSv) {
        return ResponseEntity.ok(dangKyHocService.getByMaSv(maSv));
    }

    @GetMapping("/by-malhp/{maLhp}")
    public ResponseEntity<List<DangKyHocDTO>> getByMaLhp(@PathVariable String maLhp) {
        return ResponseEntity.ok(dangKyHocService.getByMaLhp(maLhp));
    }

}
