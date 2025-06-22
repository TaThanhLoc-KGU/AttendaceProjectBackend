package com.tathanhloc.faceattendance.Controller;

import com.tathanhloc.faceattendance.DTO.*;
import com.tathanhloc.faceattendance.Service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lichhoc")
@RequiredArgsConstructor
public class LichHocController {

    private final LichHocService lichHocService;

    @GetMapping
    public List<LichHocDTO> getAll() {
        return lichHocService.getAll();
    }

    @GetMapping("/{id}")
    public LichHocDTO getById(@PathVariable String id) {
        return lichHocService.getById(id);
    }
    @PostMapping
    public LichHocDTO create(@RequestBody LichHocDTO dto) {
        return lichHocService.create(dto);
    }

    @PutMapping("/{id}")
    public LichHocDTO update(@PathVariable String id, @RequestBody LichHocDTO dto) {
        return lichHocService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        lichHocService.delete(id);
        return ResponseEntity.noContent().build();
    }


}
