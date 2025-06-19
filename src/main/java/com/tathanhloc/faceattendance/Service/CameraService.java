package com.tathanhloc.faceattendance.Service;

import com.tathanhloc.faceattendance.DTO.CameraDTO;
import com.tathanhloc.faceattendance.Model.Camera;
import com.tathanhloc.faceattendance.Model.PhongHoc;
import com.tathanhloc.faceattendance.Repository.CameraRepository;
import com.tathanhloc.faceattendance.Repository.PhongHocRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class CameraService {

    @Autowired
    private CameraRepository cameraRepository;

    @Autowired
    private PhongHocRepository phongHocRepository;

    public List<CameraDTO> getAll() {
        return cameraRepository.findAll()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public CameraDTO getById(Long id) {
        return cameraRepository.findById(id)
                .map(this::toDTO)
                .orElse(null);
    }

    public CameraDTO create(CameraDTO dto) {
        Camera camera = new Camera();
        mapToEntity(dto, camera);
        Camera saved = cameraRepository.save(camera);
        return toDTO(saved);
    }

    public CameraDTO update(Long id, CameraDTO dto) {
        Optional<Camera> cameraOpt = cameraRepository.findById(id);
        if (cameraOpt.isPresent()) {
            Camera existing = cameraOpt.get();
            mapToEntity(dto, existing);
            Camera updated = cameraRepository.save(existing);
            return toDTO(updated);
        }
        return null;
    }

    public boolean delete(Long id) {
        if (cameraRepository.existsById(id)) {
            cameraRepository.deleteById(id);
            return true;
        }
        return false;
    }

    private CameraDTO toDTO(Camera camera) {
        return CameraDTO.builder()
                .id(camera.getId())
                .tenCamera(camera.getTenCamera())
                .ipAddress(camera.getIpAddress())
                .maPhong(camera.getMaPhong() != null ? camera.getMaPhong().getMaPhong() : null)
                .vungIn(camera.getVungIn())
                .vungOut(camera.getVungOut())
                .active(camera.getIsActive())
                .password(camera.getPassword()) // thêm dòng này
                .build();
    }

    private void mapToEntity(CameraDTO dto, Camera camera) {
        camera.setTenCamera(dto.getTenCamera());
        camera.setIpAddress(dto.getIpAddress());
        camera.setVungIn(dto.getVungIn());
        camera.setVungOut(dto.getVungOut());
        camera.setIsActive(dto.getActive());
        camera.setPassword(dto.getPassword()); // thêm dòng này

        if (dto.getMaPhong() != null) {
            PhongHoc phong = phongHocRepository.findById(dto.getMaPhong())
                    .orElseThrow(() -> new RuntimeException("Phòng học không tồn tại: " + dto.getMaPhong()));
            camera.setMaPhong(phong);
        } else {
            camera.setMaPhong(null);
        }
    }
}