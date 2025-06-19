package com.tathanhloc.faceattendance.Controller;

import com.tathanhloc.faceattendance.DTO.HocKyDTO;
import com.tathanhloc.faceattendance.Service.HocKyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hocky")
@RequiredArgsConstructor
public class HocKyController {

    private final HocKyService hocKyService;

    @GetMapping
    public List<HocKyDTO> getAll() {
        return hocKyService.getAll();
    }

    @PostMapping
    public HocKyDTO create(@RequestBody HocKyDTO dto) {
        return hocKyService.create(dto);
    }
}
