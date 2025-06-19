package com.tathanhloc.faceattendance.Service;

import com.tathanhloc.faceattendance.DTO.NganhDTO;
import com.tathanhloc.faceattendance.Model.Nganh;
import com.tathanhloc.faceattendance.Repository.KhoaRepository;
import com.tathanhloc.faceattendance.Repository.NganhRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class NganhService {

    private final NganhRepository nganhRepository;
    private final KhoaRepository khoaRepository;

    public List<NganhDTO> getAll() {
        return nganhRepository.findAll().stream().map(this::toDTO).toList();
    }

    public NganhDTO getById(String id) {
        return toDTO(nganhRepository.findById(id).orElseThrow());
    }

    public NganhDTO create(NganhDTO dto) {
        Nganh entity = toEntity(dto);
        return toDTO(nganhRepository.save(entity));
    }

    public NganhDTO update(String id, NganhDTO dto) {
        Nganh existing = nganhRepository.findById(id).orElseThrow();
        existing.setTenNganh(dto.getTenNganh());
        existing.setKhoa(khoaRepository.findById(dto.getMaKhoa()).orElseThrow());
        return toDTO(nganhRepository.save(existing));
    }

    public void delete(String id) {
        nganhRepository.deleteById(id);
    }

    private NganhDTO toDTO(Nganh e) {
        return NganhDTO.builder()
                .maNganh(e.getMaNganh())
                .tenNganh(e.getTenNganh())
                .maKhoa(e.getKhoa().getMaKhoa())
                .build();
    }

    private Nganh toEntity(NganhDTO dto) {
        return Nganh.builder()
                .maNganh(dto.getMaNganh())
                .tenNganh(dto.getTenNganh())
                .khoa(khoaRepository.findById(dto.getMaKhoa()).orElseThrow())
                .isActive(true)
                .build();
    }

    public NganhDTO getByMaNganh(String maNganh) {
        return toDTO(nganhRepository.findById(maNganh)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ngành")));
    }
}
