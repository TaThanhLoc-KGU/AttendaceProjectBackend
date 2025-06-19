package com.tathanhloc.faceattendance.Service;

import com.tathanhloc.faceattendance.DTO.KhoaHocDTO;
import com.tathanhloc.faceattendance.Model.KhoaHoc;
import com.tathanhloc.faceattendance.Repository.KhoaHocRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KhoaHocService {

    private final KhoaHocRepository khoaHocRepository;

    public List<KhoaHocDTO> getAll() {
        return khoaHocRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public KhoaHocDTO getById(String maKhoahoc) {
        KhoaHoc kh = khoaHocRepository.findById(maKhoahoc)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khóa học"));
        return toDTO(kh);
    }

    @Transactional
    public KhoaHocDTO create(KhoaHocDTO dto) {
        if (khoaHocRepository.existsById(dto.getMaKhoahoc())) {
            throw new RuntimeException("Mã khóa học đã tồn tại");
        }
        KhoaHoc kh = toEntity(dto);
        return toDTO(khoaHocRepository.save(kh));
    }

    @Transactional
    public KhoaHocDTO update(String maKhoahoc, KhoaHocDTO dto) {
        KhoaHoc existing = khoaHocRepository.findById(maKhoahoc)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khóa học"));

        existing.setTenKhoahoc(dto.getTenKhoahoc());
        existing.setNamBatDau(dto.getNamBatDau());
        existing.setNamKetThuc(dto.getNamKetThuc());

        return toDTO(khoaHocRepository.save(existing));
    }

    public void delete(String maKhoahoc) {
        if (!khoaHocRepository.existsById(maKhoahoc)) {
            throw new RuntimeException("Không tìm thấy khóa học để xóa");
        }
        khoaHocRepository.deleteById(maKhoahoc);
    }

    // Mapping methods
    private KhoaHocDTO toDTO(KhoaHoc kh) {
        return KhoaHocDTO.builder()
                .maKhoahoc(kh.getMaKhoahoc())
                .tenKhoahoc(kh.getTenKhoahoc())
                .namBatDau(kh.getNamBatDau())
                .namKetThuc(kh.getNamKetThuc())
                .build();
    }

    private KhoaHoc toEntity(KhoaHocDTO dto) {
        return KhoaHoc.builder()
                .maKhoahoc(dto.getMaKhoahoc())
                .tenKhoahoc(dto.getTenKhoahoc())
                .namBatDau(dto.getNamBatDau())
                .namKetThuc(dto.getNamKetThuc())
                .build();
    }

    public KhoaHocDTO getByMaKhoahoc(String maKhoahoc) {
        return toDTO(khoaHocRepository.findById(maKhoahoc)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khóa học")));
    }
}
