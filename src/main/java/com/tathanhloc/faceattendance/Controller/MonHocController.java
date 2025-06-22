package com.tathanhloc.faceattendance.Controller;

import com.tathanhloc.faceattendance.DTO.*;
import com.tathanhloc.faceattendance.Service.MonHocService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/monhoc")
@RequiredArgsConstructor
public class MonHocController {

    private final MonHocService monHocService;

    @GetMapping
    public List<MonHocDTO> getAll() {
        return monHocService.getAll();
    }

    @GetMapping("/{id}")
    public MonHocDTO getById(@PathVariable String id) {
        return monHocService.getById(id);
    }

    @PostMapping
    public MonHocDTO create(@RequestBody MonHocDTO dto) {
        return monHocService.create(dto);
    }

    @PutMapping("/{id}")
    public MonHocDTO update(@PathVariable String id, @RequestBody MonHocDTO dto) {
        return monHocService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        monHocService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/by-mamh/{maMh}")
    public ResponseEntity<MonHocDTO> getByMaMh(@PathVariable String maMh) {
        return ResponseEntity.ok(monHocService.getByMaMh(maMh));
    }

}
