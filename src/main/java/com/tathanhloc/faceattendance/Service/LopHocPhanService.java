package com.tathanhloc.faceattendance.Service;

import com.tathanhloc.faceattendance.DTO.LopHocPhanDTO;
import com.tathanhloc.faceattendance.Model.LopHocPhan;
import com.tathanhloc.faceattendance.Repository.GiangVienRepository;
import com.tathanhloc.faceattendance.Repository.LopHocPhanRepository;
import com.tathanhloc.faceattendance.Repository.MonHocRepository;
import com.tathanhloc.faceattendance.Repository.SinhVienRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LopHocPhanService {

    private final LopHocPhanRepository lopHocPhanRepository;
    private final MonHocRepository monHocRepository;
    private final GiangVienRepository giangVienRepository;
    private final SinhVienRepository sinhVienRepository;

    public List<LopHocPhanDTO> getAll() {
        return lopHocPhanRepository.findAll().stream().map(this::toDTO).toList();
    }

    public LopHocPhanDTO getById(String id) {
        return toDTO(lopHocPhanRepository.findById(id).orElseThrow());
    }

    public LopHocPhanDTO create(LopHocPhanDTO dto) {
        LopHocPhan entity = toEntity(dto);
        return toDTO(lopHocPhanRepository.save(entity));
    }

    public LopHocPhanDTO update(String id, LopHocPhanDTO dto) {
        LopHocPhan entity = lopHocPhanRepository.findById(id).orElseThrow();
        entity.setHocKy(dto.getHocKy());
        entity.setNamHoc(dto.getNamHoc());
        entity.setNhom(dto.getNhom());
        entity.setIsActive(dto.getIsActive());
        entity.setMonHoc(monHocRepository.findById(dto.getMaMh()).orElseThrow());
        entity.setGiangVien(giangVienRepository.findById(dto.getMaGv()).orElseThrow());
        entity.setSinhViens(dto.getMaSvs().stream().map(idSv -> sinhVienRepository.findById(idSv).orElseThrow()).collect(Collectors.toSet()));
        return toDTO(lopHocPhanRepository.save(entity));
    }

    public void delete(String id) {
        lopHocPhanRepository.deleteById(id);
    }

    private LopHocPhanDTO toDTO(LopHocPhan e) {
        return LopHocPhanDTO.builder()
                .maLhp(e.getMaLhp())
                .hocKy(e.getHocKy())
                .namHoc(e.getNamHoc())
                .nhom(e.getNhom())
                .isActive(e.getIsActive())
                .maMh(e.getMonHoc().getMaMh())
                .maGv(e.getGiangVien().getMaGv())
                .maSvs(e.getSinhViens().stream().map(sv -> sv.getMaSv()).collect(Collectors.toSet()))
                .build();
    }

    private LopHocPhan toEntity(LopHocPhanDTO dto) {
        return LopHocPhan.builder()
                .maLhp(dto.getMaLhp())
                .hocKy(dto.getHocKy())
                .namHoc(dto.getNamHoc())
                .nhom(dto.getNhom())
                .isActive(dto.getIsActive())
                .monHoc(monHocRepository.findById(dto.getMaMh()).orElseThrow())
                .giangVien(giangVienRepository.findById(dto.getMaGv()).orElseThrow())
                .sinhViens(dto.getMaSvs().stream()
                        .map(id -> sinhVienRepository.findById(id).orElseThrow())
                        .collect(Collectors.toSet()))
                .build();
    }

    public LopHocPhanDTO getByMaLhp(String maLhp) {
        LopHocPhan lhp = lopHocPhanRepository.findById(maLhp)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lớp học phần"));
        return toDTO(lhp);
    }

}
