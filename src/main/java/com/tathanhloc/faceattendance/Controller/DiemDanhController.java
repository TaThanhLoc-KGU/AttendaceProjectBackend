package com.tathanhloc.faceattendance.Controller;

import com.tathanhloc.faceattendance.DTO.*;
import com.tathanhloc.faceattendance.Service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/diemdanh")
@RequiredArgsConstructor
public class DiemDanhController {

    private final DiemDanhService diemDanhService;

    @GetMapping
    public List<DiemDanhDTO> getAll() {
        return diemDanhService.getAll();
    }

    @GetMapping("/{id}")
    public DiemDanhDTO getById(@PathVariable Long id) {
        return diemDanhService.getById(id);
    }

    @PostMapping
    public DiemDanhDTO create(@RequestBody DiemDanhDTO dto) {
        return diemDanhService.create(dto);
    }

    @PutMapping("/{id}")
    public DiemDanhDTO update(@PathVariable Long id, @RequestBody DiemDanhDTO dto) {
        return diemDanhService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        diemDanhService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/by-masv/{maSv}")
    public ResponseEntity<List<DiemDanhDTO>> getByMaSv(@PathVariable String maSv) {
        return ResponseEntity.ok(diemDanhService.getByMaSv(maSv));
    }

    @GetMapping("/by-malich/{maLich}")
    public ResponseEntity<List<DiemDanhDTO>> getByMaLich(@PathVariable String maLich) {
        return ResponseEntity.ok(diemDanhService.getByMaLich(maLich));
    }

    @GetMapping("/today/count")
    public ResponseEntity<Long> countTodayDiemDanh() {
        long count = diemDanhService.countTodayDiemDanh(); // Assuming this method exists in DiemDanhService
        return ResponseEntity.ok(count);
    }


    @PostMapping("/camera-attendance")
    public ResponseEntity<?> recordAttendanceFromCamera(@RequestBody Map<String, Object> attendanceData) {
        try {
            String studentId = (String) attendanceData.get("studentId");
            Long cameraId = Long.valueOf(attendanceData.get("cameraId").toString());

            DiemDanhDTO result = diemDanhService.recordAttendanceFromCamera(studentId, cameraId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Điểm danh thành công",
                    "data", result
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

}
