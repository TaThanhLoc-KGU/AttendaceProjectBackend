package com.tathanhloc.faceattendance.Service;

import com.tathanhloc.faceattendance.DTO.LopHocPhanDTO;
import com.tathanhloc.faceattendance.Model.LopHocPhan;
import com.tathanhloc.faceattendance.Repository.GiangVienRepository;
import com.tathanhloc.faceattendance.Repository.LopHocPhanRepository;
import com.tathanhloc.faceattendance.Repository.MonHocRepository;
import com.tathanhloc.faceattendance.Repository.SinhVienRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
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

        // FIX: Check null trước khi xử lý maSvs
        if (dto.getMaSvs() != null && !dto.getMaSvs().isEmpty()) {
            entity.setSinhViens(dto.getMaSvs().stream()
                    .map(idSv -> sinhVienRepository.findById(idSv).orElseThrow())
                    .collect(Collectors.toSet()));
        } else {
            entity.setSinhViens(new HashSet<>());
        }

        return toDTO(lopHocPhanRepository.save(entity));
    }

    public void delete(String id) {
        lopHocPhanRepository.deleteById(id);
    }

    private LopHocPhanDTO toDTO(LopHocPhan e) {
        try {
            LopHocPhanDTO.LopHocPhanDTOBuilder builder = LopHocPhanDTO.builder()
                    .maLhp(e.getMaLhp())
                    .hocKy(e.getHocKy())
                    .namHoc(e.getNamHoc())
                    .nhom(e.getNhom())
                    .isActive(e.getIsActive())
                    .maMh(e.getMonHoc().getMaMh())
                    .maGv(e.getGiangVien().getMaGv())
                    // FIX: Check null trước khi xử lý sinhViens
                    .maSvs(e.getSinhViens() != null ?
                            e.getSinhViens().stream().map(sv -> sv.getMaSv()).collect(Collectors.toSet()) :
                            new HashSet<>())
                    // Thêm số lượng sinh viên để hiển thị
                    .soLuongSinhVien(e.getSinhViens() != null ? e.getSinhViens().size() : 0);

            // THÊM TÊN MÔN HỌC
            if (e.getMonHoc() != null) {
                builder.tenMonHoc(e.getMonHoc().getTenMh())
                        .soTinChi(e.getMonHoc().getSoTinChi());
            } else {
                builder.tenMonHoc("N/A")
                        .soTinChi(0);
            }

            // THÊM TÊN GIẢNG VIÊN
            if (e.getGiangVien() != null) {
                builder.tenGiangVien(e.getGiangVien().getHoTen());
            } else {
                builder.tenGiangVien("N/A");
            }

            return builder.build();

        } catch (Exception ex) {
            log.error("Error converting LopHocPhan to DTO: {}", e.getMaLhp(), ex);

            // Return basic DTO on error
            return LopHocPhanDTO.builder()
                    .maLhp(e.getMaLhp())
                    .hocKy(e.getHocKy())
                    .namHoc(e.getNamHoc())
                    .nhom(e.getNhom())
                    .isActive(e.getIsActive())
                    .maMh(e.getMonHoc() != null ? e.getMonHoc().getMaMh() : "N/A")
                    .maGv(e.getGiangVien() != null ? e.getGiangVien().getMaGv() : "N/A")
                    .tenMonHoc(e.getMonHoc() != null ? e.getMonHoc().getTenMh() : "N/A")
                    .tenGiangVien(e.getGiangVien() != null ? e.getGiangVien().getHoTen() : "N/A")
                    .soTinChi(e.getMonHoc() != null ? e.getMonHoc().getSoTinChi() : 0)
                    .maSvs(new HashSet<>())
                    .soLuongSinhVien(0)
                    .build();
        }
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
                // FIX: Khởi tạo HashSet rỗng nếu maSvs null
                .sinhViens(dto.getMaSvs() != null && !dto.getMaSvs().isEmpty() ?
                        dto.getMaSvs().stream()
                                .map(id -> sinhVienRepository.findById(id).orElseThrow())
                                .collect(Collectors.toSet()) :
                        new HashSet<>())
                .build();
    }

    public LopHocPhanDTO getByMaLhp(String maLhp) {
        LopHocPhan lhp = lopHocPhanRepository.findById(maLhp)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lớp học phần"));
        return toDTO(lhp);
    }

    // THÊM METHOD ĐỂ LẤY DANH SÁCH VỚI TÊN ĐẦY ĐỦ
    public List<LopHocPhanDTO> getAllWithNames() {
        log.info("Getting all LopHocPhan with full names");
        return lopHocPhanRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // THÊM METHOD LẤY CHỈ NHỮNG LỚP ĐANG ACTIVE
    public List<LopHocPhanDTO> getAllActiveWithNames() {
        log.info("Getting active LopHocPhan with full names");
        return lopHocPhanRepository.findAll().stream()
                .filter(lhp -> Boolean.TRUE.equals(lhp.getIsActive()))
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
}