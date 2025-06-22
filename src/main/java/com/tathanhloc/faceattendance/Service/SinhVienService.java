package com.tathanhloc.faceattendance.Service;

import com.tathanhloc.faceattendance.DTO.*;
import com.tathanhloc.faceattendance.Model.*;
import com.tathanhloc.faceattendance.Repository.*;
import org.springframework.data.jpa.repository.JpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SinhVienService extends BaseService<SinhVien, String, SinhVienDTO> {

    private final SinhVienRepository sinhVienRepository;
    private final LopRepository lopRepository;

    @Override
    protected JpaRepository<SinhVien, String> getRepository() {
        return sinhVienRepository;
    }

    @Override
    protected void setActive(SinhVien entity, boolean active) {
        entity.setIsActive(active);
    }

    @Override
    protected boolean isActive(SinhVien entity) {
        return entity.getIsActive() != null && entity.getIsActive();
    }

    public SinhVienDTO create(SinhVienDTO dto) {
        SinhVien entity = toEntity(dto);
        return toDTO(sinhVienRepository.save(entity));
    }

    public SinhVienDTO update(String id, SinhVienDTO dto) {
        SinhVien sv = sinhVienRepository.findById(id).orElseThrow(() ->
                new RuntimeException("Không tìm thấy sinh viên với mã: " + id));

        sv.setHoTen(dto.getHoTen());
        sv.setGioiTinh(dto.getGioiTinh());
        sv.setNgaySinh(dto.getNgaySinh());
        sv.setEmail(dto.getEmail());
        sv.setHinhAnh(dto.getHinhAnh());
        sv.setEmbedding(dto.getEmbedding());
        sv.setIsActive(dto.getIsActive());
        sv.setLop(lopRepository.findById(dto.getMaLop()).orElseThrow(() ->
                new RuntimeException("Không tìm thấy lớp với mã: " + dto.getMaLop())));

        return toDTO(sinhVienRepository.save(sv));
    }

    @Override
    protected SinhVienDTO toDTO(SinhVien sv) {
        return SinhVienDTO.builder()
                .maSv(sv.getMaSv())
                .hoTen(sv.getHoTen())
                .gioiTinh(sv.getGioiTinh())
                .ngaySinh(sv.getNgaySinh())
                .email(sv.getEmail())
                .hinhAnh(sv.getHinhAnh())
                .embedding(sv.getEmbedding())
                .isActive(sv.getIsActive())
                .maLop(sv.getLop().getMaLop())
                .build();
    }

    @Override
    protected SinhVien toEntity(SinhVienDTO dto) {
        return SinhVien.builder()
                .maSv(dto.getMaSv())
                .hoTen(dto.getHoTen())
                .gioiTinh(dto.getGioiTinh())
                .ngaySinh(dto.getNgaySinh())
                .email(dto.getEmail())
                .hinhAnh(dto.getHinhAnh())
                .embedding(dto.getEmbedding())
                .isActive(dto.getIsActive())
                .lop(lopRepository.findById(dto.getMaLop()).orElseThrow(() ->
                        new RuntimeException("Không tìm thấy lớp với mã: " + dto.getMaLop())))
                .build();
    }

    public SinhVienDTO getByMaSv(String maSv) {
        SinhVien sinhVien = sinhVienRepository.findById(maSv)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sinh viên với mã: " + maSv));
        return toDTO(sinhVien);
    }

    // Chỉ lấy sinh viên đang hoạt động
    public List<SinhVienDTO> getAllActive() {
        return sinhVienRepository.findAll().stream()
                .filter(sv -> sv.getIsActive() != null && sv.getIsActive())
                .map(this::toDTO)
                .toList();
    }

    /**
     * Lấy tất cả embedding của sinh viên đang hoạt động
     * @return Danh sách embedding của sinh viên
     */
    public List<Map<String, Object>> getAllEmbeddings() {
        return sinhVienRepository.findAll().stream()
                .filter(sv -> sv.getIsActive() != null && sv.getIsActive())
                .filter(sv -> sv.getEmbedding() != null && !sv.getEmbedding().isEmpty())
                .map(sv -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("studentId", sv.getMaSv());
                    result.put("name", sv.getHoTen());
                    result.put("embedding", sv.getEmbedding());
                    return result;
                })
                .collect(Collectors.toList());
    }

    /**
     * Lấy embedding của một sinh viên theo mã sinh viên
     * @param maSv Mã sinh viên
     * @return Embedding của sinh viên
     */
    public Map<String, Object> getEmbeddingByMaSv(String maSv) {
        SinhVien sinhVien = sinhVienRepository.findById(maSv)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sinh viên với mã: " + maSv));

        if (sinhVien.getEmbedding() == null || sinhVien.getEmbedding().isEmpty()) {
            throw new RuntimeException("Sinh viên chưa có dữ liệu embedding");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("studentId", sinhVien.getMaSv());
        result.put("name", sinhVien.getHoTen());
        result.put("embedding", sinhVien.getEmbedding());
        return result;
    }

    /**
     * Lưu embedding cho một sinh viên
     * @param maSv Mã sinh viên
     * @param embedding Dữ liệu embedding
     * @return SinhVienDTO đã cập nhật
     */
    public SinhVienDTO saveEmbedding(String maSv, String embedding) {
        SinhVien sinhVien = sinhVienRepository.findById(maSv)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sinh viên với mã: " + maSv));

        sinhVien.setEmbedding(embedding);
        sinhVienRepository.save(sinhVien);

        return toDTO(sinhVien);
    }



}
