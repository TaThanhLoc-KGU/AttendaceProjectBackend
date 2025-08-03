package com.tathanhloc.faceattendance.Controller;

import com.tathanhloc.faceattendance.DTO.HocKyDTO;
import com.tathanhloc.faceattendance.DTO.HocKyNamHocDTO;
import com.tathanhloc.faceattendance.Model.HocKyNamHoc;
import com.tathanhloc.faceattendance.Repository.HocKyNamHocRepository;
import com.tathanhloc.faceattendance.Service.HocKyNamHocService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/hockynamhoc")
@RequiredArgsConstructor
public class HocKyNamHocController {

    private final HocKyNamHocService hocKyNamHocService;
    private final HocKyNamHocRepository hocKyNamHocRepository;

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
    /**
     * Xóa mềm học kỳ khỏi năm học
     */
    @DeleteMapping("/namhoc/{maNamHoc}/hocky/{maHocKy}")
    public ResponseEntity<Map<String, Object>> removeSemesterFromYear(
            @PathVariable String maNamHoc,
            @PathVariable String maHocKy) {

        try {
            hocKyNamHocService.removeSemesterFromYear(maNamHoc, maHocKy);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Đã xóa học kỳ khỏi năm học");

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Cập nhật lại thứ tự học kỳ trong năm học
     */
    @PutMapping("/namhoc/{maNamHoc}/reorder")
    public ResponseEntity<Map<String, Object>> reorderSemesters(@PathVariable String maNamHoc) {
        try {
            hocKyNamHocService.reorderSemestersInYear(maNamHoc);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Đã cập nhật lại thứ tự học kỳ");

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }


}
