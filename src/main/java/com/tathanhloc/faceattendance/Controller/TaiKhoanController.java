package com.tathanhloc.faceattendance.Controller;

import com.tathanhloc.faceattendance.DTO.*;
import com.tathanhloc.faceattendance.Service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/taikhoan")
@RequiredArgsConstructor
public class TaiKhoanController {

    private final TaiKhoanService taiKhoanService;

    @GetMapping
    public List<TaiKhoanDTO> getAll() {
        return taiKhoanService.getAll();
    }

    @GetMapping("/{id}")
    public TaiKhoanDTO getById(@PathVariable Long id) {
        return taiKhoanService.getById(id);
    }

    @PostMapping
    public TaiKhoanDTO create(@RequestBody TaiKhoanDTO dto) {
        return taiKhoanService.create(dto);
    }

    @PutMapping("/{id}")
    public TaiKhoanDTO update(@PathVariable Long id, @RequestBody TaiKhoanDTO dto) {
        return taiKhoanService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        taiKhoanService.softDelete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/by-username/{username}")
    public ResponseEntity<TaiKhoanDTO> getByUsername(@PathVariable String username) {
        return ResponseEntity.ok(taiKhoanService.getByUsername(username));
    }

}
