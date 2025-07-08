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
    public List<PhongHocDTO> getAll() {
        return phongHocService.getAll();
    }

    @GetMapping("/{id}")
    public PhongHocDTO getById(@PathVariable String id) {
        return phongHocService.getById(id);
    }

    @PostMapping
    public PhongHocDTO create(@RequestBody PhongHocDTO dto) {
        return phongHocService.create(dto);
    }

    @PutMapping("/{id}")
    public PhongHocDTO update(@PathVariable String id, @RequestBody PhongHocDTO dto) {
        return phongHocService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        phongHocService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/by-maphong/{maPhong}")
    public ResponseEntity<PhongHocDTO> getByMaPhong(@PathVariable String maPhong) {
        return ResponseEntity.ok(phongHocService.getByMaPhong(maPhong));
    }
}