package com.tathanhloc.faceattendance.Service;

import com.tathanhloc.faceattendance.DTO.HocKyDTO;
import com.tathanhloc.faceattendance.Model.HocKy;
import com.tathanhloc.faceattendance.Repository.HocKyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HocKyService {

    private final HocKyRepository hocKyRepository;

    public List<HocKyDTO> getAll() {
        return hocKyRepository.findAll().stream().map(this::toDTO).toList();
    }

    public HocKyDTO create(HocKyDTO dto) {
        HocKy hocKy = HocKy.builder()
                .maHocKy(dto.getMaHocKy())
                .tenHocKy(dto.getTenHocKy())
                .isActive(dto.getIsActive())
                .build();
        return toDTO(hocKyRepository.save(hocKy));
    }

    private HocKyDTO toDTO(HocKy entity) {
        return HocKyDTO.builder()
                .maHocKy(entity.getMaHocKy())
                .tenHocKy(entity.getTenHocKy())
                .isActive(entity.getIsActive())
                .build();
    }
}
