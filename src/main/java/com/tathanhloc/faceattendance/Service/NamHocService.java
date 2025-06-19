package com.tathanhloc.faceattendance.Service;

import com.tathanhloc.faceattendance.DTO.NamHocDTO;
import com.tathanhloc.faceattendance.Model.NamHoc;
import com.tathanhloc.faceattendance.Repository.NamHocRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NamHocService {

    private final NamHocRepository namHocRepository;

    public List<NamHocDTO> getAll() {
        return namHocRepository.findAll().stream().map(this::toDTO).toList();
    }

    public NamHocDTO create(NamHocDTO dto) {
        NamHoc entity = NamHoc.builder()
                .maNamHoc(dto.getMaNamHoc())
                .isActive(dto.getIsActive())
                .build();
        return toDTO(namHocRepository.save(entity));
    }

    private NamHocDTO toDTO(NamHoc entity) {
        return NamHocDTO.builder()
                .maNamHoc(entity.getMaNamHoc())
                .isActive(entity.getIsActive())
                .build();
    }
}
