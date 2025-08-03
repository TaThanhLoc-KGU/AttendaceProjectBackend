package com.tathanhloc.faceattendance.Service;

import com.tathanhloc.faceattendance.DTO.NganhMonHocDTO;
import com.tathanhloc.faceattendance.Model.NganhMonHoc;
import com.tathanhloc.faceattendance.Model.NganhMonHocId;
import com.tathanhloc.faceattendance.Repository.MonHocRepository;
import com.tathanhloc.faceattendance.Repository.NganhMonHocRepository;
import com.tathanhloc.faceattendance.Repository.NganhRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NganhMonHocService {

    private final NganhMonHocRepository nganhMonHocRepository;
    private final NganhRepository nganhRepository;
    private final MonHocRepository monHocRepository;

    public List<NganhMonHocDTO> getAll() {
        return nganhMonHocRepository.findAll().stream()
                .map(e -> new NganhMonHocDTO(e.getId().getMaNganh(), e.getId().getMaMh(), e.getIsActive()))
                .toList();
    }

    @Transactional
    public NganhMonHocDTO create(NganhMonHocDTO dto) {
        log.debug("Creating NganhMonHoc relation: {} - {}", dto.getMaNganh(), dto.getMaMh());

        try {
            // Create composite key
            NganhMonHocId id = NganhMonHocId.builder()
                    .maNganh(dto.getMaNganh())
                    .maMh(dto.getMaMh())
                    .build();

            // Check if already exists (including inactive)
            Optional<NganhMonHoc> existing = nganhMonHocRepository.findById(id);
            if (existing.isPresent()) {
                NganhMonHoc existingRelation = existing.get();
                // Nếu đã tồn tại và đang active thì báo lỗi
                if (Boolean.TRUE.equals(existingRelation.getIsActive())) {
                    log.warn("Active relation already exists: {} - {}", dto.getMaNganh(), dto.getMaMh());
                    throw new RuntimeException("Mối quan hệ đã tồn tại");
                } else {
                    // Nếu tồn tại nhưng inactive, khôi phục nó
                    existingRelation.setIsActive(true);
                    NganhMonHoc saved = nganhMonHocRepository.save(existingRelation);
                    log.debug("✅ NganhMonHoc relation restored: {} - {}", dto.getMaNganh(), dto.getMaMh());

                    return NganhMonHocDTO.builder()
                            .maNganh(saved.getId().getMaNganh())
                            .maMh(saved.getId().getMaMh())
                            .isActive(saved.getIsActive())
                            .build();
                }
            }

            // Verify entities exist
            if (!nganhRepository.existsById(dto.getMaNganh())) {
                throw new RuntimeException("Nganh not found: " + dto.getMaNganh());
            }
            if (!monHocRepository.existsById(dto.getMaMh())) {
                throw new RuntimeException("MonHoc not found: " + dto.getMaMh());
            }

            // Create new entity
            NganhMonHoc entity = NganhMonHoc.builder()
                    .id(id)
                    .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                    .build();

            NganhMonHoc saved = nganhMonHocRepository.save(entity);
            log.debug("✅ NganhMonHoc relation created: {} - {}", dto.getMaNganh(), dto.getMaMh());

            return NganhMonHocDTO.builder()
                    .maNganh(saved.getId().getMaNganh())
                    .maMh(saved.getId().getMaMh())
                    .isActive(saved.getIsActive())
                    .build();

        } catch (Exception e) {
            log.error("❌ Error creating NganhMonHoc relation: {} - {}", dto.getMaNganh(), dto.getMaMh(), e);
            throw e;
        }
    }

    /**
     * Xóa mềm NganhMonHoc relation (soft delete)
     * Set isActive = false thay vì xóa khỏi database
     */
    @Transactional
    public void softDelete(String maNganh, String maMh) {
        log.debug("Soft deleting NganhMonHoc relation: {} - {}", maNganh, maMh);

        try {
            NganhMonHocId id = NganhMonHocId.builder()
                    .maNganh(maNganh)
                    .maMh(maMh)
                    .build();

            Optional<NganhMonHoc> existing = nganhMonHocRepository.findById(id);
            if (existing.isEmpty()) {
                log.warn("Relation not found for soft deletion: {} - {}", maNganh, maMh);
                return;
            }

            NganhMonHoc relation = existing.get();
            relation.setIsActive(false);
            nganhMonHocRepository.save(relation);

            log.debug("✅ NganhMonHoc relation soft deleted: {} - {}", maNganh, maMh);

        } catch (Exception e) {
            log.error("❌ Error soft deleting NganhMonHoc relation: {} - {}", maNganh, maMh, e);
            throw e;
        }
    }

    /**
     * Khôi phục NganhMonHoc relation đã xóa mềm
     */
    @Transactional
    public void restore(String maNganh, String maMh) {
        log.debug("Restoring NganhMonHoc relation: {} - {}", maNganh, maMh);

        try {
            NganhMonHocId id = NganhMonHocId.builder()
                    .maNganh(maNganh)
                    .maMh(maMh)
                    .build();

            Optional<NganhMonHoc> existing = nganhMonHocRepository.findById(id);
            if (existing.isEmpty()) {
                log.warn("Relation not found for restoration: {} - {}", maNganh, maMh);
                return;
            }

            NganhMonHoc relation = existing.get();
            relation.setIsActive(true);
            nganhMonHocRepository.save(relation);

            log.debug("✅ NganhMonHoc relation restored: {} - {}", maNganh, maMh);

        } catch (Exception e) {
            log.error("❌ Error restoring NganhMonHoc relation: {} - {}", maNganh, maMh, e);
            throw e;
        }
    }

    /**
     * Xóa vĩnh viễn NganhMonHoc relation (hard delete)
     * Xóa hoàn toàn khỏi database
     */
    @Transactional
    public void hardDelete(String maNganh, String maMh) {
        log.debug("Hard deleting NganhMonHoc relation: {} - {}", maNganh, maMh);

        try {
            NganhMonHocId id = NganhMonHocId.builder()
                    .maNganh(maNganh)
                    .maMh(maMh)
                    .build();

            if (!nganhMonHocRepository.existsById(id)) {
                log.warn("Relation not found for hard deletion: {} - {}", maNganh, maMh);
                return;
            }

            nganhMonHocRepository.deleteById(id);
            log.debug("✅ NganhMonHoc relation hard deleted: {} - {}", maNganh, maMh);

        } catch (Exception e) {
            log.error("❌ Error hard deleting NganhMonHoc relation: {} - {}", maNganh, maMh, e);
            throw e;
        }
    }

    /**
     * Compatibility method - sử dụng soft delete as default
     */
    @Transactional
    public void delete(String maNganh, String maMh) {
        softDelete(maNganh, maMh);
    }

    /**
     * Tìm relations theo mã ngành (chỉ active)
     */
    public List<NganhMonHocDTO> findByMaNganh(String maNganh) {
        try {
            return nganhMonHocRepository.findByNganhMaNganh(maNganh)
                    .stream()
                    .filter(e -> Boolean.TRUE.equals(e.getIsActive())) // Chỉ lấy active
                    .map(e -> NganhMonHocDTO.builder()
                            .maNganh(e.getId().getMaNganh())
                            .maMh(e.getId().getMaMh())
                            .isActive(e.getIsActive())
                            .build())
                    .toList();
        } catch (Exception e) {
            log.error("Error finding NganhMonHoc by Nganh: {}", maNganh, e);
            return List.of();
        }
    }

    /**
     * Tìm relations theo mã môn học (chỉ active)
     */
    public List<NganhMonHocDTO> findByMaMh(String maMh) {
        try {
            return nganhMonHocRepository.findByMonHocMaMh(maMh)
                    .stream()
                    .filter(e -> Boolean.TRUE.equals(e.getIsActive())) // Chỉ lấy active
                    .map(e -> NganhMonHocDTO.builder()
                            .maNganh(e.getId().getMaNganh())
                            .maMh(e.getId().getMaMh())
                            .isActive(e.getIsActive())
                            .build())
                    .toList();
        } catch (Exception e) {
            log.error("Error finding NganhMonHoc by MonHoc: {}", maMh, e);
            return List.of();
        }
    }

    /**
     * Tìm relations theo mã ngành (bao gồm cả inactive) - dùng cho restore/hard delete
     */
    public List<NganhMonHocDTO> findByMaNganhIncludeInactive(String maNganh) {
        try {
            return nganhMonHocRepository.findByNganhMaNganh(maNganh)
                    .stream()
                    .map(e -> NganhMonHocDTO.builder()
                            .maNganh(e.getId().getMaNganh())
                            .maMh(e.getId().getMaMh())
                            .isActive(e.getIsActive())
                            .build())
                    .toList();
        } catch (Exception e) {
            log.error("Error finding NganhMonHoc by Nganh (include inactive): {}", maNganh, e);
            return List.of();
        }
    }

    /**
     * Tìm relations theo mã môn học (bao gồm cả inactive) - dùng cho restore/hard delete
     */
    public List<NganhMonHocDTO> findByMaMhIncludeInactive(String maMh) {
        try {
            return nganhMonHocRepository.findByMonHocMaMh(maMh)
                    .stream()
                    .map(e -> NganhMonHocDTO.builder()
                            .maNganh(e.getId().getMaNganh())
                            .maMh(e.getId().getMaMh())
                            .isActive(e.getIsActive())
                            .build())
                    .toList();
        } catch (Exception e) {
            log.error("Error finding NganhMonHoc by MonHoc (include inactive): {}", maMh, e);
            return List.of();
        }
    }
}