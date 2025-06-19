package com.tathanhloc.faceattendance.Service;

import com.tathanhloc.faceattendance.DTO.HocKyNamHocDTO;
import com.tathanhloc.faceattendance.Model.HocKy;
import com.tathanhloc.faceattendance.Model.HocKyNamHoc;
import com.tathanhloc.faceattendance.Model.NamHoc;
import com.tathanhloc.faceattendance.Repository.HocKyNamHocRepository;
import com.tathanhloc.faceattendance.Repository.HocKyRepository;
import com.tathanhloc.faceattendance.Repository.NamHocRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HocKyNamHocService {

    private final HocKyRepository hocKyRepository;
    private final NamHocRepository namHocRepository;
    private final HocKyNamHocRepository hocKyNamHocRepository;

    public List<HocKyNamHocDTO> getAll() {
        return hocKyNamHocRepository.findAll().stream().map(this::toDTO).toList();
    }

    public HocKyNamHocDTO getById(Integer id) {
        return toDTO(hocKyNamHocRepository.findById(id).orElseThrow());
    }

    public HocKyNamHocDTO create(HocKyNamHocDTO dto) {
        HocKy hocKy = hocKyRepository.findById(dto.getMaHocKy()).orElseThrow();
        NamHoc namHoc = namHocRepository.findById(dto.getMaNamHoc()).orElseThrow();
        HocKyNamHoc entity = HocKyNamHoc.builder()
                .hocKy(hocKy)
                .namHoc(namHoc)
                .isActive(dto.getIsActive())
                .build();
        return toDTO(hocKyNamHocRepository.save(entity));
    }

    public void delete(Integer id) {
        hocKyNamHocRepository.deleteById(id);
    }

    private HocKyNamHocDTO toDTO(HocKyNamHoc entity) {
        return HocKyNamHocDTO.builder()
                .id(entity.getId())
                .maHocKy(entity.getHocKy().getMaHocKy())
                .maNamHoc(entity.getNamHoc().getMaNamHoc())
                .isActive(entity.getIsActive())
                .build();
    }
}
