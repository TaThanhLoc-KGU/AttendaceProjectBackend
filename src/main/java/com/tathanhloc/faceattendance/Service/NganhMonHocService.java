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

            // Check if already exists
            if (nganhMonHocRepository.existsById(id)) {
                log.warn("Relation already exists: {} - {}", dto.getMaNganh(), dto.getMaMh());
                throw new RuntimeException("Mối quan hệ đã tồn tại");
            }

            // Verify entities exist
            if (!nganhRepository.existsById(dto.getMaNganh())) {
                throw new RuntimeException("Nganh not found: " + dto.getMaNganh());
            }
            if (!monHocRepository.existsById(dto.getMaMh())) {
                throw new RuntimeException("MonHoc not found: " + dto.getMaMh());
            }

            // Create entity - DON'T set nganh and monHoc to avoid circular issues
            NganhMonHoc entity = NganhMonHoc.builder()
                    .id(id)
                    .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                    // Do NOT set nganh and monHoc - they are read-only joins
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

    @Transactional
    public void delete(String maNganh, String maMh) {
        log.debug("Deleting NganhMonHoc relation: {} - {}", maNganh, maMh);

        try {
            NganhMonHocId id = NganhMonHocId.builder()
                    .maNganh(maNganh)
                    .maMh(maMh)
                    .build();

            if (!nganhMonHocRepository.existsById(id)) {
                log.warn("Relation not found for deletion: {} - {}", maNganh, maMh);
                return;
            }

            nganhMonHocRepository.deleteById(id);
            log.debug("✅ NganhMonHoc relation deleted: {} - {}", maNganh, maMh);

        } catch (Exception e) {
            log.error("❌ Error deleting NganhMonHoc relation: {} - {}", maNganh, maMh, e);
            throw e;
        }
    }

    public List<NganhMonHocDTO> findByMaNganh(String maNganh) {
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
            log.error("Error finding NganhMonHoc by Nganh: {}", maNganh, e);
            return List.of();
        }
    }

    public List<NganhMonHocDTO> findByMaMh(String maMh) {
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
            log.error("Error finding NganhMonHoc by MonHoc: {}", maMh, e);
            return List.of();
        }
    }
}