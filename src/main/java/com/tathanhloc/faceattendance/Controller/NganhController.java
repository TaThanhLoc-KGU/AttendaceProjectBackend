package com.tathanhloc.faceattendance.Controller;

import com.tathanhloc.faceattendance.DTO.NganhDTO;
import com.tathanhloc.faceattendance.Service.NganhService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.List;

@RestController
@RequestMapping("/api/nganh")
@RequiredArgsConstructor
public class NganhController {

    private final NganhService nganhService;

    @GetMapping
    public List<NganhDTO> getAll() {
        return nganhService.getAll();
    }

    @GetMapping("/{id}")
    public NganhDTO getById(@PathVariable String id) {
        return nganhService.getById(id);
    }

    @PostMapping
    public NganhDTO create(@RequestBody NganhDTO dto) {
        return nganhService.create(dto);
    }

    @PutMapping("/{id}")
    public NganhDTO update(@PathVariable String id, @RequestBody NganhDTO dto) {
        return nganhService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        nganhService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/by-manganh/{maNganh}")
    public ResponseEntity<NganhDTO> getByMaNganh(@PathVariable String maNganh) {
        return ResponseEntity.ok(nganhService.getByMaNganh(maNganh));
    }

}
