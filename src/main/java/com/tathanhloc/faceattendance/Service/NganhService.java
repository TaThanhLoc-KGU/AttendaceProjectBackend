package com.tathanhloc.faceattendance.Service;

import com.tathanhloc.faceattendance.DTO.NganhDTO;
import com.tathanhloc.faceattendance.Exception.ResourceNotFoundException;
import com.tathanhloc.faceattendance.Model.Nganh;
import com.tathanhloc.faceattendance.Repository.KhoaRepository;
import com.tathanhloc.faceattendance.Repository.NganhRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NganhService extends BaseService<Nganh, String, NganhDTO> {

    private final NganhRepository nganhRepository;
    private final KhoaRepository khoaRepository;

    @Override
    protected JpaRepository<Nganh, String> getRepository() {
        return nganhRepository;
    }

    @Override
    protected void setActive(Nganh entity, boolean active) {
        entity.setActive(active);
    }

    @Override
    protected boolean isActive(Nganh entity) {
        return entity.isActive();
    }

    @Override
    @Cacheable(value = "nganh")
    public List<NganhDTO> getAllActive() {
        log.debug("Lấy danh sách ngành đang hoạt động từ database");
        return super.getAllActive();
    }

    @Override
    @Cacheable(value = "nganh", key = "#id")
    public NganhDTO getById(String id) {
        log.debug("Lấy thông tin ngành với ID {} từ database", id);
        return super.getById(id);
    }

    @Transactional
    @CacheEvict(value = "nganh", allEntries = true)
    public NganhDTO create(NganhDTO dto) {
        log.debug("Tạo ngành mới: {}", dto);
        if (nganhRepository.existsById(dto.getMaNganh())) {
            throw new RuntimeException("Mã ngành đã tồn tại");
        }
        Nganh nganh = toEntity(dto);
        return toDTO(nganhRepository.save(nganh));
    }

    @Transactional
    @CacheEvict(value = "nganh", allEntries = true)
    public NganhDTO update(String maNganh, NganhDTO dto) {
        log.debug("Cập nhật ngành với ID {}: {}", maNganh, dto);
        Nganh existing = nganhRepository.findById(maNganh)
                .orElseThrow(() -> new ResourceNotFoundException("Ngành", "mã ngành", maNganh));

        existing.setTenNganh(dto.getTenNganh());
        existing.setKhoa(khoaRepository.findById(dto.getMaKhoa()).orElseThrow());
        existing.setActive(dto.getIsActive() != null ? dto.getIsActive() : true);

        return toDTO(nganhRepository.save(existing));
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "nganh", key = "#maNganh"),
            @CacheEvict(value = "nganh", allEntries = true)
    })
    public void softDelete(String maNganh) {
        log.debug("Xóa mềm ngành với ID: {}", maNganh);
        super.softDelete(maNganh);
    }

    @Transactional
    @CacheEvict(value = "nganh", allEntries = true)
    public void restore(String maNganh) {
        log.debug("Khôi phục ngành với ID: {}", maNganh);
        super.restore(maNganh);
    }

    @Transactional
    @CacheEvict(value = "nganh", allEntries = true)
    public void hardDelete(String maNganh) {
        log.debug("Xóa vĩnh viễn ngành với ID: {}", maNganh);
        if (!nganhRepository.existsById(maNganh)) {
            throw new ResourceNotFoundException("Ngành", "mã ngành", maNganh);
        }
        nganhRepository.deleteById(maNganh);
    }

    // Business methods
    public NganhDTO getByMaNganh(String maNganh) {
        return toDTO(nganhRepository.findById(maNganh)
                .orElseThrow(() -> new ResourceNotFoundException("Ngành", "mã ngành", maNganh)));
    }

    public List<NganhDTO> getByKhoa(String maKhoa) {
        return nganhRepository.findByKhoaMaKhoa(maKhoa)
                .stream()
                .filter(this::isActive)
                .map(this::toDTO)
                .toList();
    }

    public List<NganhDTO> getAllWithDeleted() {
        return nganhRepository.findAll().stream()
                .map(this::toDTO)
                .toList();
    }

    public List<NganhDTO> getDeletedOnly() {
        return nganhRepository.findAll().stream()
                .filter(nganh -> !isActive(nganh))
                .map(this::toDTO)
                .toList();
    }

    /**
     * Lấy tất cả ngành với số lượng sinh viên (bao gồm cả sinh viên không hoạt động)
     */
    public List<NganhDTO> getAllWithAllStudents() {
        return nganhRepository.findAll().stream()
                .map(this::toDTOWithAllStudents)
                .toList();
    }

    /**
     * Lấy thống kê chi tiết về ngành
     */
    public Map<String, Object> getNganhStatistics(String maNganh) {
        Nganh nganh = nganhRepository.findById(maNganh)
                .orElseThrow(() -> new ResourceNotFoundException("Ngành", "mã ngành", maNganh));

        Map<String, Object> stats = new HashMap<>();
        stats.put("nganh", toDTO(nganh));
        stats.put("soSinhVienHoatDong", nganhRepository.countActiveSinhVienByNganh(maNganh));
        stats.put("tongSoSinhVien", nganhRepository.countAllSinhVienByNganh(maNganh));
        stats.put("soLopHoatDong", nganhRepository.countActiveByKhoaMaKhoa(maNganh));

        return stats;
    }

    // Mapping methods
    @Override
    protected NganhDTO toDTO(Nganh entity) {
        return NganhDTO.builder()
                .maNganh(entity.getMaNganh())
                .tenNganh(entity.getTenNganh())
                .maKhoa(entity.getKhoa().getMaKhoa())
                .tenKhoa(entity.getKhoa().getTenKhoa())
                .isActive(entity.isActive())
                .soSinhVien(nganhRepository.countActiveSinhVienByNganh(entity.getMaNganh()))
                .build();
    }

    /**
     * Chuyển đổi sang DTO với tùy chọn bao gồm sinh viên không hoạt động
     */
    public NganhDTO toDTOWithAllStudents(Nganh entity) {
        return NganhDTO.builder()
                .maNganh(entity.getMaNganh())
                .tenNganh(entity.getTenNganh())
                .maKhoa(entity.getKhoa().getMaKhoa())
                .tenKhoa(entity.getKhoa().getTenKhoa())
                .isActive(entity.isActive())
                .soSinhVien(nganhRepository.countAllSinhVienByNganh(entity.getMaNganh()))
                .build();
    }

    @Override
    protected Nganh toEntity(NganhDTO dto) {
        return Nganh.builder()
                .maNganh(dto.getMaNganh())
                .tenNganh(dto.getTenNganh())
                .khoa(khoaRepository.findById(dto.getMaKhoa())
                        .orElseThrow(() -> new ResourceNotFoundException("Khoa", "mã khoa", dto.getMaKhoa())))
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .build();
    }
}