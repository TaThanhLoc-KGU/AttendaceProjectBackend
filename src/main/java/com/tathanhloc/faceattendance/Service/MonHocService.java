package com.tathanhloc.faceattendance.Service;

import com.tathanhloc.faceattendance.DTO.MonHocDTO;
import com.tathanhloc.faceattendance.DTO.NganhMonHocDTO;
import com.tathanhloc.faceattendance.Exception.ResourceNotFoundException;
import com.tathanhloc.faceattendance.Model.MonHoc;
import com.tathanhloc.faceattendance.Repository.MonHocRepository;
import com.tathanhloc.faceattendance.Repository.NganhRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MonHocService {

    private final MonHocRepository monHocRepository;
    private final NganhRepository nganhRepository;
    private final NganhMonHocService nganhMonHocService;

    @Transactional(readOnly = true)
    public List<MonHocDTO> getAll() {
        log.debug("Getting all MonHoc");

        try {
            List<MonHoc> monHocs = monHocRepository.findAll();

            return monHocs.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error getting all MonHoc", e);
            return Collections.emptyList();
        }
    }

    @Transactional(readOnly = true)
    public MonHocDTO getById(String id) {
        log.debug("Getting MonHoc by ID: {}", id);

        MonHoc monHoc = monHocRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MonHoc", "maMh", id));

        return convertToDTO(monHoc);
    }

    @Transactional
    public MonHocDTO create(MonHocDTO dto) {
        log.debug("Creating new MonHoc: {}", dto.getMaMh());

        try {
            // Validate
            validateMonHocDTO(dto);

            if (monHocRepository.existsById(dto.getMaMh())) {
                throw new IllegalArgumentException("Mã môn học đã tồn tại: " + dto.getMaMh());
            }

            // Create MonHoc first
            MonHoc monHoc = MonHoc.builder()
                    .maMh(dto.getMaMh())
                    .tenMh(dto.getTenMh())
                    .soTinChi(dto.getSoTinChi())
                    .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                    .build();

            MonHoc savedMonHoc = monHocRepository.save(monHoc);
            log.debug("✅ MonHoc saved: {}", savedMonHoc.getMaMh());

            // Create relations
            Set<String> successfulNganhs = new HashSet<>();
            if (dto.getMaNganhs() != null && !dto.getMaNganhs().isEmpty()) {
                successfulNganhs = createNganhRelations(savedMonHoc.getMaMh(), dto.getMaNganhs());
            }

            return MonHocDTO.builder()
                    .maMh(savedMonHoc.getMaMh())
                    .tenMh(savedMonHoc.getTenMh())
                    .soTinChi(savedMonHoc.getSoTinChi())
                    .isActive(savedMonHoc.getIsActive())
                    .maNganhs(successfulNganhs)
                    .build();

        } catch (Exception e) {
            log.error("❌ Error creating MonHoc: {}", dto.getMaMh(), e);
            throw e;
        }
    }

    @Transactional
    public MonHocDTO update(String id, MonHocDTO dto) {
        log.debug("Updating MonHoc: {}", id);

        try {
            MonHoc existing = monHocRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("MonHoc", "maMh", id));

            existing.setTenMh(dto.getTenMh());
            existing.setSoTinChi(dto.getSoTinChi());
            existing.setIsActive(dto.getIsActive());

            MonHoc updated = monHocRepository.save(existing);

            // Update relations
            Set<String> successfulNganhs = new HashSet<>();
            if (dto.getMaNganhs() != null) {
                softDeleteAllRelations(id);
                successfulNganhs = createNganhRelations(id, dto.getMaNganhs());
            }

            return MonHocDTO.builder()
                    .maMh(updated.getMaMh())
                    .tenMh(updated.getTenMh())
                    .soTinChi(updated.getSoTinChi())
                    .isActive(updated.getIsActive())
                    .maNganhs(successfulNganhs)
                    .build();

        } catch (Exception e) {
            log.error("❌ Error updating MonHoc: {}", id, e);
            throw e;
        }
    }

    /**
     * Xóa mềm MonHoc (soft delete)
     * Set isActive = false thay vì xóa khỏi database
     */
    @Transactional
    public void softDelete(String id) {
        log.debug("Soft deleting MonHoc: {}", id);

        try {
            MonHoc monHoc = monHocRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("MonHoc", "maMh", id));

            // Set isActive = false thay vì xóa
            monHoc.setIsActive(false);
            monHocRepository.save(monHoc);

            // Soft delete tất cả relations
            softDeleteAllRelations(id);

            log.debug("✅ MonHoc soft deleted: {}", id);

        } catch (Exception e) {
            log.error("❌ Error soft deleting MonHoc: {}", id, e);
            throw e;
        }
    }

    /**
     * Khôi phục MonHoc đã xóa mềm
     */
    @Transactional
    public void restore(String id) {
        log.debug("Restoring MonHoc: {}", id);

        try {
            MonHoc monHoc = monHocRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("MonHoc", "maMh", id));

            // Set isActive = true để khôi phục
            monHoc.setIsActive(true);
            monHocRepository.save(monHoc);

            // Khôi phục tất cả relations
            restoreAllRelations(id);

            log.debug("✅ MonHoc restored: {}", id);

        } catch (Exception e) {
            log.error("❌ Error restoring MonHoc: {}", id, e);
            throw e;
        }
    }

    /**
     * Xóa vĩnh viễn MonHoc (hard delete)
     * Xóa hoàn toàn khỏi database
     */
    @Transactional
    public void hardDelete(String id) {
        log.debug("Hard deleting MonHoc: {}", id);

        try {
            if (!monHocRepository.existsById(id)) {
                throw new ResourceNotFoundException("MonHoc", "maMh", id);
            }

            // Xóa vĩnh viễn tất cả relations trước
            hardDeleteAllRelations(id);

            // Xóa vĩnh viễn MonHoc
            monHocRepository.deleteById(id);

            log.debug("✅ MonHoc hard deleted: {}", id);

        } catch (Exception e) {
            log.error("❌ Error hard deleting MonHoc: {}", id, e);
            throw e;
        }
    }

    /**
     * Compatibility method - sử dụng soft delete as default
     */
    @Transactional
    public void delete(String id) {
        softDelete(id);
    }

    public MonHocDTO getByMaMh(String maMh) {
        return getById(maMh);
    }

    // Helper methods
    private MonHocDTO convertToDTO(MonHoc monHoc) {
        try {
            Set<String> nganhIds = getNganhIds(monHoc.getMaMh());

            return MonHocDTO.builder()
                    .maMh(monHoc.getMaMh())
                    .tenMh(monHoc.getTenMh())
                    .soTinChi(monHoc.getSoTinChi())
                    .isActive(monHoc.getIsActive())
                    .maNganhs(nganhIds)
                    .build();

        } catch (Exception e) {
            log.warn("Error converting MonHoc to DTO: {}", monHoc.getMaMh(), e);
            return MonHocDTO.builder()
                    .maMh(monHoc.getMaMh())
                    .tenMh(monHoc.getTenMh())
                    .soTinChi(monHoc.getSoTinChi())
                    .isActive(monHoc.getIsActive())
                    .maNganhs(Collections.emptySet())
                    .build();
        }
    }

    private Set<String> getNganhIds(String maMh) {
        try {
            List<NganhMonHocDTO> relations = nganhMonHocService.findByMaMh(maMh);
            return relations.stream()
                    .filter(r -> Boolean.TRUE.equals(r.getIsActive()))
                    .map(NganhMonHocDTO::getMaNganh)
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            log.warn("Error getting nganh relations for MonHoc: {}", maMh, e);
            return Collections.emptySet();
        }
    }

    private Set<String> createNganhRelations(String maMh, Set<String> nganhIds) {
        Set<String> successful = new HashSet<>();

        for (String nganhId : nganhIds) {
            try {
                if (!nganhRepository.existsById(nganhId)) {
                    log.warn("Nganh not found: {}", nganhId);
                    continue;
                }

                NganhMonHocDTO relationDTO = NganhMonHocDTO.builder()
                        .maNganh(nganhId)
                        .maMh(maMh)
                        .isActive(true)
                        .build();

                nganhMonHocService.create(relationDTO);
                successful.add(nganhId);
                log.debug("✅ Created relation: MonHoc {} - Nganh {}", maMh, nganhId);

            } catch (Exception e) {
                log.warn("Failed to create relation MonHoc: {} - Nganh: {}", maMh, nganhId, e);
            }
        }

        return successful;
    }

    /**
     * Soft delete tất cả relations của MonHoc
     */
    private void softDeleteAllRelations(String maMh) {
        try {
            List<NganhMonHocDTO> relations = nganhMonHocService.findByMaMh(maMh);
            for (NganhMonHocDTO relation : relations) {
                try {
                    nganhMonHocService.softDelete(relation.getMaNganh(), relation.getMaMh());
                } catch (Exception e) {
                    log.warn("Failed to soft delete relation: {} - {}", relation.getMaNganh(), relation.getMaMh(), e);
                }
            }
        } catch (Exception e) {
            log.warn("Error soft deleting relations for MonHoc: {}", maMh, e);
        }
    }

    /**
     * Khôi phục tất cả relations của MonHoc
     */
    private void restoreAllRelations(String maMh) {
        try {
            List<NganhMonHocDTO> relations = nganhMonHocService.findByMaMhIncludeInactive(maMh);
            for (NganhMonHocDTO relation : relations) {
                try {
                    nganhMonHocService.restore(relation.getMaNganh(), relation.getMaMh());
                } catch (Exception e) {
                    log.warn("Failed to restore relation: {} - {}", relation.getMaNganh(), relation.getMaMh(), e);
                }
            }
        } catch (Exception e) {
            log.warn("Error restoring relations for MonHoc: {}", maMh, e);
        }
    }

    /**
     * Hard delete tất cả relations của MonHoc
     */
    private void hardDeleteAllRelations(String maMh) {
        try {
            List<NganhMonHocDTO> relations = nganhMonHocService.findByMaMhIncludeInactive(maMh);
            for (NganhMonHocDTO relation : relations) {
                try {
                    nganhMonHocService.hardDelete(relation.getMaNganh(), relation.getMaMh());
                } catch (Exception e) {
                    log.warn("Failed to hard delete relation: {} - {}", relation.getMaNganh(), relation.getMaMh(), e);
                }
            }
        } catch (Exception e) {
            log.warn("Error hard deleting relations for MonHoc: {}", maMh, e);
        }
    }

    private void validateMonHocDTO(MonHocDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("MonHoc data cannot be null");
        }
        if (dto.getMaMh() == null || dto.getMaMh().trim().isEmpty()) {
            throw new IllegalArgumentException("Mã môn học không được để trống");
        }
        if (dto.getTenMh() == null || dto.getTenMh().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên môn học không được để trống");
        }
        if (dto.getSoTinChi() == null || dto.getSoTinChi() <= 0) {
            throw new IllegalArgumentException("Số tín chỉ phải lớn hơn 0");
        }
    }
}