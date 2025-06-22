package com.tathanhloc.faceattendance.Controller;

import com.tathanhloc.faceattendance.DTO.PhongHocDTO;
import com.tathanhloc.faceattendance.Service.PhongHocService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/phonghoc")
@RequiredArgsConstructor
public class PhongHocController {

    private final PhongHocService phongHocService;

    @GetMapping
    public ResponseEntity<List<PhongHocDTO>> getAll() {
        return ResponseEntity.ok(phongHocService.getAll());
    }

    @GetMapping("/by-maphong/{maPhong}")
    public ResponseEntity<PhongHocDTO> getByMaPhong(@PathVariable String maPhong) {
        return ResponseEntity.ok(phongHocService.getByMaPhong(maPhong));
    }

    @PostMapping
    public ResponseEntity<PhongHocDTO> create(@RequestBody PhongHocDTO dto) {
        return ResponseEntity.ok(phongHocService.create(dto));
    }

    @PutMapping("/{maPhong}")
    public ResponseEntity<PhongHocDTO> update(@PathVariable String maPhong, @RequestBody PhongHocDTO dto) {
        return ResponseEntity.ok(phongHocService.update(maPhong, dto));
    }

    @DeleteMapping("/{maPhong}")
    public ResponseEntity<Void> delete(@PathVariable String maPhong) {
        phongHocService.delete(maPhong);
        return ResponseEntity.noContent().build();
    }
}
