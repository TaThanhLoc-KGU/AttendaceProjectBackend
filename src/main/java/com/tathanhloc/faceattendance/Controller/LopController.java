package com.tathanhloc.faceattendance.Controller;
import com.tathanhloc.faceattendance.DTO.LopDTO;
import com.tathanhloc.faceattendance.Service.LopService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lop")
@RequiredArgsConstructor
public class LopController {

    private final LopService lopService;

    @GetMapping
    public List<LopDTO> getAll() {
        return lopService.getAll();
    }

    @GetMapping("/{id}")
    public LopDTO getById(@PathVariable String id) {
        return lopService.getById(id);
    }

    @PostMapping
    public LopDTO create(@RequestBody LopDTO dto) {
        return lopService.create(dto);
    }

    @PutMapping("/{id}")
    public LopDTO update(@PathVariable String id, @RequestBody LopDTO dto) {
        return lopService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        lopService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/by-malop/{maLop}")
    public ResponseEntity<LopDTO> getByMaLop(@PathVariable String maLop) {
        return ResponseEntity.ok(lopService.getByMaLop(maLop));
    }

    @GetMapping("/count")
    public ResponseEntity<Long> count() {
        long count = lopService.count();
        return ResponseEntity.ok(count);
    }

}
