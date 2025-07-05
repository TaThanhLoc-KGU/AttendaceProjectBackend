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
    private final NganhMonHocService nganhMonHocService; // Sử dụng service có sẵn

    @Transactional(readOnly = true)
    public List<MonHocDTO> getAll() {
        log.debug("Getting all MonHoc - FIXED VERSION");

        try {
            List<MonHoc> monHocs = monHocRepository.findAll();

            if (monHocs.isEmpty()) {
                return Collections.emptyList();
            }

            // Convert to DTO safely - get nganh relations separately
            List<MonHocDTO> result = new ArrayList<>();

            for (MonHoc monHoc : monHocs) {
                try {
                    MonHocDTO dto = convertToDTOSafely(monHoc);
                    result.add(dto);
                } catch (Exception e) {
                    log.warn("Error converting MonHoc to DTO: {}", monHoc.getMaMh(), e);
                    // Add minimal DTO to avoid breaking the whole list
                    MonHocDTO minimalDto = MonHocDTO.builder()
                            .maMh(monHoc.getMaMh())
                            .tenMh(monHoc.getTenMh() != null ? monHoc.getTenMh() : "Unknown")
                            .soTinChi(monHoc.getSoTinChi() != null ? monHoc.getSoTinChi() : 0)
                            .isActive(monHoc.getIsActive() != null ? monHoc.getIsActive() : false)
                            .maNganhs(Collections.emptySet())
                            .build();
                    result.add(minimalDto);
                }
            }

            return result;

        } catch (Exception e) {
            log.error("Error in getAll MonHoc", e);
            return Collections.emptyList();
        }
    }

    @Transactional(readOnly = true)
    public MonHocDTO getById(String id) {
        log.debug("Getting MonHoc by ID: {}", id);

        MonHoc monHoc = monHocRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MonHoc", "maMh", id));

        return convertToDTOSafely(monHoc);
    }

    @Transactional
    public MonHocDTO create(MonHocDTO dto) {
        log.debug("Creating new MonHoc: {}", dto.getMaMh());

        try {
            // Validate input
            validateMonHocDTO(dto);

            // Check if already exists
            if (monHocRepository.existsById(dto.getMaMh())) {
                throw new IllegalArgumentException("Mã môn học đã tồn tại: " + dto.getMaMh());
            }

            // Create MonHoc entity WITHOUT nganhs to avoid circular reference
            MonHoc monHoc = MonHoc.builder()
                    .maMh(dto.getMaMh())
                    .tenMh(dto.getTenMh())
                    .soTinChi(dto.getSoTinChi())
                    .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                    .build();

            // Save MonHoc first
            MonHoc savedMonHoc = monHocRepository.save(monHoc);
            log.debug("✅ MonHoc saved: {}", savedMonHoc.getMaMh());

            // Handle nganh relations using NganhMonHocService
            Set<String> successfulNganhs = new HashSet<>();
            if (dto.getMaNganhs() != null && !dto.getMaNganhs().isEmpty()) {
                successfulNganhs = createNganhMonHocRelations(savedMonHoc.getMaMh(), dto.getMaNganhs());
            }

            // Return DTO with successful relations
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

            // Update basic fields
            existing.setTenMh(dto.getTenMh());
            existing.setSoTinChi(dto.getSoTinChi());
            existing.setIsActive(dto.getIsActive());

            MonHoc updated = monHocRepository.save(existing);
            log.debug("✅ MonHoc updated: {}", updated.getMaMh());

            // Update nganh relations
            Set<String> successfulNganhs = new HashSet<>();
            if (dto.getMaNganhs() != null) {
                // First delete existing relations
                deleteNganhMonHocRelations(id);

                // Then create new relations
                successfulNganhs = createNganhMonHocRelations(id, dto.getMaNganhs());
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

    @Transactional
    public void delete(String id) {
        log.debug("Deleting MonHoc: {}", id);

        try {
            if (!monHocRepository.existsById(id)) {
                throw new ResourceNotFoundException("MonHoc", "maMh", id);
            }

            // Delete nganh relations first
            deleteNganhMonHocRelations(id);

            // Then delete MonHoc
            monHocRepository.deleteById(id);
            log.debug("✅ MonHoc deleted: {}", id);

        } catch (Exception e) {
            log.error("❌ Error deleting MonHoc: {}", id, e);
            throw e;
        }
    }

    public MonHocDTO getByMaMh(String maMh) {
        return getById(maMh);
    }

    // ===== HELPER METHODS =====

    /**
     * Convert MonHoc to DTO safely - get nganh relations separately
     */
    private MonHocDTO convertToDTOSafely(MonHoc monHoc) {
        try {
            // Get nganh relations using NganhMonHocService
            Set<String> nganhIds = getNganhIdsByMonHoc(monHoc.getMaMh());

            return MonHocDTO.builder()
                    .maMh(monHoc.getMaMh())
                    .tenMh(monHoc.getTenMh())
                    .soTinChi(monHoc.getSoTinChi())
                    .isActive(monHoc.getIsActive())
                    .maNganhs(nganhIds)
                    .build();

        } catch (Exception e) {
            log.warn("Error converting MonHoc to DTO, returning minimal version: {}", monHoc.getMaMh(), e);
            return MonHocDTO.builder()
                    .maMh(monHoc.getMaMh())
                    .tenMh(monHoc.getTenMh())
                    .soTinChi(monHoc.getSoTinChi())
                    .isActive(monHoc.getIsActive())
                    .maNganhs(Collections.emptySet())
                    .build();
        }
    }

    /**
     * Get nganh IDs for a MonHoc using NganhMonHocService
     */
    private Set<String> getNganhIdsByMonHoc(String maMh) {
        try {
            List<NganhMonHocDTO> relations = nganhMonHocService.findByMaMh(maMh);
            return relations.stream()
                    .filter(r -> r.getIsActive() != null && r.getIsActive())
                    .map(NganhMonHocDTO::getMaNganh)
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            log.warn("Error getting nganh relations for MonHoc: {}", maMh, e);
            return Collections.emptySet();
        }
    }

    /**
     * Create nganh-monhoc relations using NganhMonHocService
     */
    private Set<String> createNganhMonHocRelations(String maMh, Set<String> nganhIds) {
        Set<String> successful = new HashSet<>();

        for (String nganhId : nganhIds) {
            try {
                // Validate nganh exists
                if (!nganhRepository.existsById(nganhId)) {
                    log.warn("⚠️ Nganh not found: {}", nganhId);
                    continue;
                }

                // Create relation using NganhMonHocService
                NganhMonHocDTO relationDTO = NganhMonHocDTO.builder()
                        .maNganh(nganhId)
                        .maMh(maMh)
                        .isActive(true)
                        .build();

                nganhMonHocService.create(relationDTO);
                successful.add(nganhId);
                log.debug("✅ Created relation: MonHoc {} - Nganh {}", maMh, nganhId);

            } catch (Exception e) {
                log.warn("⚠️ Failed to create relation MonHoc: {} - Nganh: {}", maMh, nganhId, e);
                // Continue with other nganhs instead of failing completely
            }
        }

        log.debug("Created {}/{} nganh relations for MonHoc: {}",
                successful.size(), nganhIds.size(), maMh);

        return successful;
    }

    /**
     * Delete all nganh-monhoc relations for a MonHoc
     */
    private void deleteNganhMonHocRelations(String maMh) {
        try {
            List<NganhMonHocDTO> existingRelations = nganhMonHocService.findByMaMh(maMh);

            for (NganhMonHocDTO relation : existingRelations) {
                try {
                    nganhMonHocService.delete(relation.getMaNganh(), relation.getMaMh());
                    log.debug("✅ Deleted relation: MonHoc {} - Nganh {}",
                            relation.getMaMh(), relation.getMaNganh());
                } catch (Exception e) {
                    log.warn("⚠️ Failed to delete relation: MonHoc {} - Nganh {}",
                            relation.getMaMh(), relation.getMaNganh(), e);
                }
            }

        } catch (Exception e) {
            log.warn("⚠️ Error deleting nganh relations for MonHoc: {}", maMh, e);
        }
    }

    /**
     * Validate MonHocDTO
     */
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

        if (dto.getSoTinChi() > 10) {
            throw new IllegalArgumentException("Số tín chỉ không được vượt quá 10");
        }
    }
}

// ===== ALTERNATIVE SIMPLE APPROACH =====
// Nếu vẫn có vấn đề, sử dụng approach này (không dùng Many-to-Many)

/*
@Service
@RequiredArgsConstructor
@Slf4j
public class SimpleMonHocService {

    private final MonHocRepository monHocRepository;
    private final NganhRepository nganhRepository;

    @Transactional(readOnly = true)
    public List<MonHocDTO> getAll() {
        try {
            return monHocRepository.findAll().stream()
                    .map(this::convertToSimpleDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error getting all MonHoc", e);
            return Collections.emptyList();
        }
    }

    @Transactional
    public MonHocDTO create(MonHocDTO dto) {
        // Just create MonHoc without relations first
        MonHoc monHoc = MonHoc.builder()
                .maMh(dto.getMaMh())
                .tenMh(dto.getTenMh())
                .soTinChi(dto.getSoTinChi())
                .isActive(true)
                .build();

        MonHoc saved = monHocRepository.save(monHoc);
        return convertToSimpleDTO(saved);
    }

    private MonHocDTO convertToSimpleDTO(MonHoc monHoc) {
        return MonHocDTO.builder()
                .maMh(monHoc.getMaMh())
                .tenMh(monHoc.getTenMh())
                .soTinChi(monHoc.getSoTinChi())
                .isActive(monHoc.getIsActive())
                .maNganhs(Collections.emptySet()) // Empty for now
                .build();
    }
}
*/