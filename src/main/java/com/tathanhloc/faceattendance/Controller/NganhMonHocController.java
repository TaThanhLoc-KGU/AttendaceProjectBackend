package com.tathanhloc.faceattendance.Controller;

import com.tathanhloc.faceattendance.DTO.NganhMonHocDTO;
import com.tathanhloc.faceattendance.Service.NganhMonHocService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/nganhmonhoc")
@RequiredArgsConstructor
public class NganhMonHocController {

    private final NganhMonHocService nganhMonHocService;

    @GetMapping("/by-manganh/{maNganh}")
    public ResponseEntity<List<NganhMonHocDTO>> getByMaNganh(@PathVariable String maNganh) {
        return ResponseEntity.ok(nganhMonHocService.findByMaNganh(maNganh));
    }

    @GetMapping("/by-mamh/{maMh}")
    public ResponseEntity<List<NganhMonHocDTO>> getByMaMh(@PathVariable String maMh) {
        return ResponseEntity.ok(nganhMonHocService.findByMaMh(maMh));
    }

    @PostMapping
    public ResponseEntity<NganhMonHocDTO> create(@RequestBody NganhMonHocDTO dto) {
        return ResponseEntity.ok(nganhMonHocService.create(dto));
    }

    @DeleteMapping("/{maNganh}/{maMh}")
    public ResponseEntity<Void> delete(@PathVariable String maNganh, @PathVariable String maMh) {
        nganhMonHocService.delete(maNganh, maMh);
        return ResponseEntity.noContent().build();
    }
}
