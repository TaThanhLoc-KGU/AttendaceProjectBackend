package com.tathanhloc.faceattendance.Controller;

import com.tathanhloc.faceattendance.DTO.CameraDTO;
import com.tathanhloc.faceattendance.Service.CameraService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cameras")
@CrossOrigin(origins = "*")
public class CameraController {

    @Autowired
    private CameraService cameraService;

    @GetMapping
    public List<CameraDTO> getAll() {
        return cameraService.getAll();
    }

    @GetMapping("/{id}")
    public CameraDTO getById(@PathVariable Long id) {
        return cameraService.getById(id);
    }

    @PostMapping
    public CameraDTO create(@RequestBody CameraDTO dto) {
        return cameraService.create(dto);
    }

    @PutMapping("/{id}")
    public CameraDTO update(@PathVariable Long id, @RequestBody CameraDTO dto) {
        return cameraService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id) {
        boolean deleted = cameraService.delete(id);
        return deleted ? "Deleted successfully." : "Camera not found.";
    }
    @GetMapping("/count")
    public ResponseEntity<Map<String, Object>> getCameraCount() {
        try {
            List<CameraDTO> cameras = cameraService.getAll();
            long totalCount = cameras.size();
            long activeCount = cameras.stream().filter(c -> c.getActive()).count();

            Map<String, Object> response = new HashMap<>();
            response.put("total", totalCount);
            response.put("active", activeCount);
            response.put("inactive", totalCount - activeCount);
            response.put("status", "success");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("total", 0);
            response.put("active", 0);
            response.put("inactive", 0);
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.ok(response);
        }
    }
}