package com.tathanhloc.faceattendance.Service;

import com.tathanhloc.faceattendance.DTO.NganhMonHocDTO;
import com.tathanhloc.faceattendance.Model.NganhMonHoc;
import com.tathanhloc.faceattendance.Model.NganhMonHocId;
import com.tathanhloc.faceattendance.Repository.MonHocRepository;
import com.tathanhloc.faceattendance.Repository.NganhMonHocRepository;
import com.tathanhloc.faceattendance.Repository.NganhRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NganhMonHocService {

    private final NganhMonHocRepository nganhMonHocRepository;
    private final NganhRepository nganhRepository;
    private final MonHocRepository monHocRepository;

    public List<NganhMonHocDTO> getAll() {
        return nganhMonHocRepository.findAll().stream()
                .map(e -> new NganhMonHocDTO(e.getId().getMaNganh(), e.getId().getMaMh(),e.isActive()))
                .toList();
    }

    public NganhMonHocDTO create(NganhMonHocDTO dto) {
        var id = new NganhMonHocId(dto.getMaNganh(), dto.getMaMh());

        if (nganhMonHocRepository.existsById(id)) {
            throw new RuntimeException("Mối quan hệ đã tồn tại");
        }

        NganhMonHoc entity = NganhMonHoc.builder()
                .id(id)
                .nganh(nganhRepository.findById(dto.getMaNganh()).orElseThrow())
                .monHoc(monHocRepository.findById(dto.getMaMh()).orElseThrow())
                .isActive(true)
                .build();

        nganhMonHocRepository.save(entity);
        return dto;
    }

    public void delete(String maNganh, String maMh) {
        NganhMonHocId id = new NganhMonHocId(maNganh, maMh);
        nganhMonHocRepository.deleteById(id);
    }

    public List<NganhMonHocDTO> findByMaNganh(String maNganh) {
        return nganhMonHocRepository.findByNganhMaNganh(maNganh)
                .stream()
                .map(e -> new NganhMonHocDTO(maNganh, e.getMonHoc().getMaMh(),e.isActive()))
                .toList();
    }


    public List<NganhMonHocDTO> findByMaMh(String maMh) {
        return nganhMonHocRepository.findByMonHocMaMh(maMh)
                .stream()
                .map(e -> new NganhMonHocDTO(e.getNganh().getMaNganh(), maMh,e.isActive()))
                .toList();
    }



}
