package com.tathanhloc.faceattendance.Controller;

import com.tathanhloc.faceattendance.DTO.NamHocDTO;
import com.tathanhloc.faceattendance.Service.NamHocService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/namhoc")
@RequiredArgsConstructor
public class NamHocController {

    private final NamHocService namHocService;

    @GetMapping
    public List<NamHocDTO> getAll() {
        return namHocService.getAll();
    }

    @PostMapping
    public NamHocDTO create(@RequestBody NamHocDTO dto) {
        return namHocService.create(dto);
    }
}
