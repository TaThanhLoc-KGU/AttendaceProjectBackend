package com.tathanhloc.faceattendance.Controller;

import com.tathanhloc.faceattendance.DTO.GiangVienDTO;
import com.tathanhloc.faceattendance.Service.GiangVienService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/giangvien")
@RequiredArgsConstructor
public class GiangVienController {

    private final GiangVienService giangVienService;

    @GetMapping
    public List<GiangVienDTO> getAll() {
        return giangVienService.getAll();
    }

    @GetMapping("/{id}")
    public GiangVienDTO getById(@PathVariable String id) {
        return giangVienService.getById(id);
    }

    @PostMapping
    public GiangVienDTO create(@RequestBody GiangVienDTO dto) {
        return giangVienService.create(dto);
    }

    @PutMapping("/{id}")
    public GiangVienDTO update(@PathVariable String id, @RequestBody GiangVienDTO dto) {
        return giangVienService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        giangVienService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/by-magv/{maGv}")
    public ResponseEntity<GiangVienDTO> getByMaGv(@PathVariable String maGv) {
        return ResponseEntity.ok(giangVienService.getByMaGv(maGv));
    }

}
