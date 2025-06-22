package com.tathanhloc.faceattendance.Service;

import com.tathanhloc.faceattendance.DTO.DangKyHocDTO;
import com.tathanhloc.faceattendance.Model.DangKyHoc;
import com.tathanhloc.faceattendance.Model.DangKyHocId;
import com.tathanhloc.faceattendance.Model.LopHocPhan;
import com.tathanhloc.faceattendance.Model.SinhVien;
import com.tathanhloc.faceattendance.Repository.DangKyHocRepository;
import com.tathanhloc.faceattendance.Repository.LopHocPhanRepository;
import com.tathanhloc.faceattendance.Repository.SinhVienRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DangKyHocService {

    private final DangKyHocRepository dangKyHocRepository;
    private final SinhVienRepository sinhVienRepository;
    private final LopHocPhanRepository lopHocPhanRepository;

    public List<DangKyHocDTO> getAll() {
        return dangKyHocRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public DangKyHocDTO getById(String maSv, String maLhp) {
        DangKyHocId id = new DangKyHocId(maSv, maLhp);
        return toDTO(dangKyHocRepository.findById(id).orElseThrow());
    }

    public DangKyHocDTO create(DangKyHocDTO dto) {
        SinhVien sv = sinhVienRepository.findById(dto.getMaSv()).orElseThrow();
        LopHocPhan lhp = lopHocPhanRepository.findById(dto.getMaLhp()).orElseThrow();

        DangKyHoc dk = DangKyHoc.builder()
                .id(new DangKyHocId(dto.getMaSv(), dto.getMaLhp()))
                .sinhVien(sv)
                .lopHocPhan(lhp)
                .isActive(true)
                .build();

        return toDTO(dangKyHocRepository.save(dk));
    }

    public void delete(String maSv, String maLhp) {
        dangKyHocRepository.deleteById(new DangKyHocId(maSv, maLhp));
    }

    private DangKyHocDTO toDTO(DangKyHoc e) {
        return DangKyHocDTO.builder()
                .maSv(e.getSinhVien().getMaSv())
                .maLhp(e.getLopHocPhan().getMaLhp())
                .build();
    }

    public List<DangKyHocDTO> getByMaSv(String maSv) {
        return dangKyHocRepository.findBySinhVien_MaSv(maSv).stream()
                .map(this::toDTO).toList();
    }

    public List<DangKyHocDTO> getByMaLhp(String maLhp) {
        return dangKyHocRepository.findByLopHocPhan_MaLhp(maLhp).stream()
                .map(this::toDTO).toList();
    }

}
