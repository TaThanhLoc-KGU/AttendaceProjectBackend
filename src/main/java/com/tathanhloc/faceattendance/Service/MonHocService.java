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
                deleteAllRelations(id);
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

    @Transactional
    public void delete(String id) {
        log.debug("Deleting MonHoc: {}", id);

        try {
            if (!monHocRepository.existsById(id)) {
                throw new ResourceNotFoundException("MonHoc", "maMh", id);
            }

            deleteAllRelations(id);
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

    private void deleteAllRelations(String maMh) {
        try {
            List<NganhMonHocDTO> relations = nganhMonHocService.findByMaMh(maMh);
            for (NganhMonHocDTO relation : relations) {
                try {
                    nganhMonHocService.delete(relation.getMaNganh(), relation.getMaMh());
                } catch (Exception e) {
                    log.warn("Failed to delete relation: {} - {}", relation.getMaNganh(), relation.getMaMh(), e);
                }
            }
        } catch (Exception e) {
            log.warn("Error deleting relations for MonHoc: {}", maMh, e);
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