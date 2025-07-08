package com.tathanhloc.faceattendance.Controller;

import com.tathanhloc.faceattendance.DTO.HocKyDTO;
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
    // Thêm vào HocKyNamHocController.java
    @PostMapping("/namhoc/{maNamHoc}/hocky")
    public ResponseEntity<HocKyNamHocDTO> createHocKyInNamHoc(
            @PathVariable String maNamHoc,
            @RequestBody HocKyDTO hocKyDTO) {
        HocKyNamHocDTO result = hocKyNamHocService.createHocKyInNamHoc(maNamHoc, hocKyDTO);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/namhoc/{maNamHoc}/hocky")
    public ResponseEntity<List<HocKyDTO>> getHocKyByNamHoc(@PathVariable String maNamHoc) {
        return ResponseEntity.ok(hocKyNamHocService.getHocKyByNamHoc(maNamHoc));
    }

    @PostMapping("/namhoc/{maNamHoc}/create-default-semesters")
    public ResponseEntity<List<HocKyNamHocDTO>> createDefaultSemesters(@PathVariable String maNamHoc) {
        List<HocKyNamHocDTO> result = hocKyNamHocService.createDefaultSemestersForNamHoc(maNamHoc);
        return ResponseEntity.ok(result);
    }
}
