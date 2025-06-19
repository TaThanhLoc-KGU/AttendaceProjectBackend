package com.tathanhloc.faceattendance.Controller;

import com.tathanhloc.faceattendance.DTO.CameraDTO;
import com.tathanhloc.faceattendance.Service.CameraService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
}