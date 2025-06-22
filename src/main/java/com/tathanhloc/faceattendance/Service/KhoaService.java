package com.tathanhloc.faceattendance.Service;

import com.tathanhloc.faceattendance.DTO.KhoaDTO;
import com.tathanhloc.faceattendance.Exception.ResourceNotFoundException;
import com.tathanhloc.faceattendance.Model.Khoa;
import com.tathanhloc.faceattendance.Repository.KhoaRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class KhoaService extends BaseService<Khoa, String, KhoaDTO> {

    private final KhoaRepository khoaRepository;

    @Override
    protected JpaRepository<Khoa, String> getRepository() {
        return khoaRepository;
    }

    @Override
    protected void setActive(Khoa entity, boolean active) {
        entity.setIsActive(active);
    }

    @Override
    protected boolean isActive(Khoa entity) {
        return entity.getIsActive() != null && entity.getIsActive();
    }

    @Override
    @Cacheable(value = "khoa")
    public List<KhoaDTO> getAllActive() {
        log.debug("Lấy danh sách khoa đang hoạt động từ database");
        return super.getAllActive();
    }

    @Override
    @Cacheable(value = "khoa", key = "#id")
    public KhoaDTO getById(String id) {
        log.debug("Lấy thông tin khoa với ID {} từ database", id);
        return super.getById(id);
    }

    @Transactional
    @CacheEvict(value = "khoa", allEntries = true)
    public KhoaDTO create(KhoaDTO dto) {
        log.debug("Tạo khoa mới: {}", dto);
        if (khoaRepository.existsById(dto.getMaKhoa())) {
            throw new RuntimeException("Mã khoa đã tồn tại");
        }
        Khoa khoa = toEntity(dto);
        return toDTO(khoaRepository.save(khoa));
    }

    @Transactional
    @CacheEvict(value = "khoa", allEntries = true)
    public KhoaDTO update(String maKhoa, KhoaDTO dto) {
        log.debug("Cập nhật khoa với ID {}: {}", maKhoa, dto);
        Khoa existing = khoaRepository.findById(maKhoa)
                .orElseThrow(() -> new ResourceNotFoundException("Khoa", "mã khoa", maKhoa));

        existing.setTenKhoa(dto.getTenKhoa());
        existing.setIsActive(dto.getIsActive());

        return toDTO(khoaRepository.save(existing));
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "khoa", key = "#maKhoa"),
            @CacheEvict(value = "khoa", allEntries = true)
    })
    @Override
    public void softDelete(String maKhoa) {
        log.debug("Xóa mềm khoa với ID: {}", maKhoa);
        super.softDelete(maKhoa);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "khoa", key = "#maKhoa"),
            @CacheEvict(value = "khoa", allEntries = true)
    })
    @Override
    public void restore(String maKhoa) {
        log.debug("Khôi phục khoa với ID: {}", maKhoa);
        super.restore(maKhoa);
    }

    // Mapper
    @Override
    protected KhoaDTO toDTO(Khoa khoa) {
        return KhoaDTO.builder()
                .maKhoa(khoa.getMaKhoa())
                .tenKhoa(khoa.getTenKhoa())
                .isActive(khoa.getIsActive())
                .build();
    }

    @Override
    protected Khoa toEntity(KhoaDTO dto) {
        return Khoa.builder()
                .maKhoa(dto.getMaKhoa())
                .tenKhoa(dto.getTenKhoa())
                .isActive(dto.getIsActive())
                .build();
    }

    @Cacheable(value = "khoa", key = "'maKhoa:' + #maKhoa")
    public KhoaDTO getByMaKhoa(String maKhoa) {
        log.debug("Tìm khoa theo mã khoa {} từ database", maKhoa);
        return toDTO(khoaRepository.findById(maKhoa)
                .orElseThrow(() -> new ResourceNotFoundException("Khoa", "mã khoa", maKhoa)));
    }
}
