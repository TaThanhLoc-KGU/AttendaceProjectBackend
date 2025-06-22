package com.tathanhloc.faceattendance.Service;

import com.tathanhloc.faceattendance.DTO.SystemLogDTO;
import com.tathanhloc.faceattendance.Model.SystemLog;
import com.tathanhloc.faceattendance.Model.TaiKhoan;
import com.tathanhloc.faceattendance.Repository.SystemLogRepository;
import com.tathanhloc.faceattendance.Repository.TaiKhoanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SystemLogService {

    private final SystemLogRepository systemLogRepository;
    private final TaiKhoanRepository taiKhoanRepository;

    public List<SystemLogDTO> getAll() {
        return systemLogRepository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    public SystemLogDTO getById(Long id) {
        return toDTO(systemLogRepository.findById(id).orElseThrow());
    }

    public SystemLogDTO create(SystemLogDTO dto) {
        SystemLog log = toEntity(dto);
        log.setId(null); // để auto-generate
        return toDTO(systemLogRepository.save(log));
    }

    public void delete(Long id) {
        systemLogRepository.deleteById(id);
    }

    private SystemLogDTO toDTO(SystemLog log) {
        return SystemLogDTO.builder()
                .id(log.getId())
                .action(log.getAction())
                .ipAddress(log.getIpAddress())
                .createdAt(log.getCreatedAt())
                .userId(log.getTaiKhoan().getId())
                .build();
    }

    private SystemLog toEntity(SystemLogDTO dto) {
        TaiKhoan tk = taiKhoanRepository.findById(dto.getUserId()).orElseThrow();
        return SystemLog.builder()
                .id(dto.getId())
                .action(dto.getAction())
                .ipAddress(dto.getIpAddress())
                .createdAt(dto.getCreatedAt())
                .taiKhoan(tk)
                .build();
    }

    public List<SystemLogDTO> getByUserId(Long userId) {
        return systemLogRepository.findByTaiKhoanId(userId).stream()
                .map(this::toDTO).toList();
    }

}
