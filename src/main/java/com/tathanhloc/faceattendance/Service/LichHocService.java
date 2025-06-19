package com.tathanhloc.faceattendance.Service;

import com.tathanhloc.faceattendance.DTO.LichHocDTO;
import com.tathanhloc.faceattendance.Exception.ResourceNotFoundException;
import com.tathanhloc.faceattendance.Model.LichHoc;
import com.tathanhloc.faceattendance.Repository.LichHocRepository;
import com.tathanhloc.faceattendance.Repository.LopHocPhanRepository;
import com.tathanhloc.faceattendance.Repository.PhongHocRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LichHocService {

    private final LichHocRepository lichHocRepository;
    private final LopHocPhanRepository lopHocPhanRepository;
    private final PhongHocRepository phongHocRepository;

    public List<LichHocDTO> getAll() {
        return lichHocRepository.findAll().stream().map(this::toDTO).toList();
    }
    public LichHocDTO getById(String id) {
        return lichHocRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException("LichHoc not found with id: " + id));
    }
    public LichHocDTO create(LichHocDTO dto) {
        LichHoc lh = toEntity(dto);
        return toDTO(lichHocRepository.save(lh));
    }

    public LichHocDTO update(String id, LichHocDTO dto) {
        LichHoc existing = lichHocRepository.findById(id).orElseThrow();
        existing.setThu(dto.getThu());
        existing.setTietBatDau(dto.getTietBatDau());
        existing.setSoTiet(dto.getSoTiet());
        existing.setPhongHoc(phongHocRepository.findById(dto.getMaPhong()).orElseThrow());
        existing.setLopHocPhan(lopHocPhanRepository.findById(dto.getMaLhp()).orElseThrow());
        return toDTO(lichHocRepository.save(existing));
    }

    public void delete(String id) {
        lichHocRepository.deleteById(id);
    }

    private LichHocDTO toDTO(LichHoc lh) {
        return LichHocDTO.builder()
                .maLich(lh.getMaLich())
                .thu(lh.getThu())
                .tietBatDau(lh.getTietBatDau())
                .soTiet(lh.getSoTiet())
                .maLhp(lh.getLopHocPhan().getMaLhp())
                .maPhong(lh.getPhongHoc().getMaPhong())
                .build();
    }

    private LichHoc toEntity(LichHocDTO dto) {
        return LichHoc.builder()
                .maLich(dto.getMaLich())
                .thu(dto.getThu())
                .tietBatDau(dto.getTietBatDau())
                .soTiet(dto.getSoTiet())
                .lopHocPhan(lopHocPhanRepository.findById(dto.getMaLhp()).orElseThrow())
                .phongHoc(phongHocRepository.findById(dto.getMaPhong()).orElseThrow())
                .isActive(true)
                .build();
    }

}
