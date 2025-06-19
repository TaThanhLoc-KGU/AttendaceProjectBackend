package com.tathanhloc.faceattendance.Controller;

import com.tathanhloc.faceattendance.DTO.HocKyNamHocDTO;
import com.tathanhloc.faceattendance.Service.HocKyNamHocService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hockynamhoc")
@RequiredArgsConstructor
public class HocKyNamHocController {

    private final HocKyNamHocService hocKyNamHocService;

    @GetMapping
    public List<HocKyNamHocDTO> getAll() {
        return hocKyNamHocService.getAll();
    }

    @GetMapping("/{id}")
    public HocKyNamHocDTO getById(@PathVariable Integer id) {
        return hocKyNamHocService.getById(id);
    }

    @PostMapping
    public HocKyNamHocDTO create(@RequestBody HocKyNamHocDTO dto) {
        return hocKyNamHocService.create(dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        hocKyNamHocService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
