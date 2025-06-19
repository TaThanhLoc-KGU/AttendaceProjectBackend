package com.tathanhloc.faceattendance.Service;

import com.tathanhloc.faceattendance.DTO.MonHocDTO;
import com.tathanhloc.faceattendance.Exception.ResourceNotFoundException;
import com.tathanhloc.faceattendance.Model.MonHoc;
import com.tathanhloc.faceattendance.Model.Nganh;
import com.tathanhloc.faceattendance.Repository.MonHocRepository;
import com.tathanhloc.faceattendance.Repository.NganhRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;



@Service
@RequiredArgsConstructor
public class MonHocService {

    private final MonHocRepository monHocRepository;
    private final NganhRepository nganhRepository;

    public List<MonHocDTO> getAll() {
        return monHocRepository.findAll().stream().map(this::toDTO).toList();
    }

    public MonHocDTO getById(String id) {
        return toDTO(monHocRepository.findById(id).orElseThrow());
    }

    public MonHocDTO create(MonHocDTO dto) {
        MonHoc mh = toEntity(dto);
        return toDTO(monHocRepository.save(mh));
    }

    public MonHocDTO update(String id, MonHocDTO dto) {
        MonHoc mh = monHocRepository.findById(id).orElseThrow();

        // Batch query thay vì N+1
        Set<Nganh> nganhs = nganhRepository.findAllById(dto.getMaNganhs())
                .stream().collect(Collectors.toSet());

        mh.setNganhs(nganhs);
        return toDTO(monHocRepository.save(mh));
    }

    public void delete(String id) {
        monHocRepository.deleteById(id);
    }

    private MonHocDTO toDTO(MonHoc m) {
        return MonHocDTO.builder()
                .maMh(m.getMaMh())
                .tenMh(m.getTenMh())
                .soTinChi(m.getSoTinChi())
                .isActive(m.getIsActive())
                .maNganhs(m.getNganhs().stream().map(Nganh::getMaNganh).collect(Collectors.toSet()))
                .build();
    }

    private MonHoc toEntity(MonHocDTO dto) {
        return MonHoc.builder()
                .maMh(dto.getMaMh())
                .tenMh(dto.getTenMh())
                .soTinChi(dto.getSoTinChi())
                .isActive(dto.getIsActive())
                .nganhs(dto.getMaNganhs().stream()
                        .map(id -> nganhRepository.findById(id).orElseThrow())
                        .collect(Collectors.toSet()))
                .build();
    }

    public MonHocDTO getByMaMh(String maMh) {
        MonHoc mh = monHocRepository.findById(maMh)
                .orElseThrow(() -> new ResourceNotFoundException("MonHoc", "maMh", maMh));
        return toDTO(mh);
    }
}
