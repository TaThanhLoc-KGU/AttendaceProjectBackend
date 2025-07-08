package com.tathanhloc.faceattendance.Service;

import com.tathanhloc.faceattendance.DTO.HocKyNamHocDTO;
import com.tathanhloc.faceattendance.DTO.HocKyDTO;
import com.tathanhloc.faceattendance.Model.HocKy;
import com.tathanhloc.faceattendance.Model.HocKyNamHoc;
import com.tathanhloc.faceattendance.Model.NamHoc;
import com.tathanhloc.faceattendance.Repository.HocKyNamHocRepository;
import com.tathanhloc.faceattendance.Repository.HocKyRepository;
import com.tathanhloc.faceattendance.Repository.NamHocRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class HocKyNamHocService {

    private final HocKyRepository hocKyRepository;
    private final NamHocRepository namHocRepository;
    private final HocKyNamHocRepository hocKyNamHocRepository;

    public List<HocKyNamHocDTO> getAll() {
        return hocKyNamHocRepository.findAll().stream()
                .map(this::toDTO)
                .toList();
    }

    public HocKyNamHocDTO getById(Integer id) {
        return toDTO(hocKyNamHocRepository.findById(id).orElseThrow());
    }

    /**
     * Tạo học kỳ trong năm học
     */
    @Transactional
    public HocKyNamHocDTO createHocKyInNamHoc(String maNamHoc, HocKyDTO hocKyDTO) {
        log.info("Tạo học kỳ {} trong năm học {}", hocKyDTO.getMaHocKy(), maNamHoc);

        // Kiểm tra năm học tồn tại
        NamHoc namHoc = namHocRepository.findById(maNamHoc)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy năm học: " + maNamHoc));

        // Validate thời gian học kỳ nằm trong năm học
        validateHocKyTimeInNamHoc(hocKyDTO, namHoc);

        // Tạo học kỳ
        HocKy hocKy = HocKy.builder()
                .maHocKy(hocKyDTO.getMaHocKy())
                .tenHocKy(hocKyDTO.getTenHocKy())
                .ngayBatDau(hocKyDTO.getNgayBatDau())
                .ngayKetThuc(hocKyDTO.getNgayKetThuc())
                .moTa(hocKyDTO.getMoTa())
                .isActive(true)
                .isCurrent(false)
                .build();

        hocKy = hocKyRepository.save(hocKy);

        // Tính thứ tự học kỳ trong năm
        Long soHocKyHienCo = hocKyNamHocRepository.countByNamHoc(maNamHoc);
        Integer thuTu = hocKyDTO.getThuTu() != null ? hocKyDTO.getThuTu() : (soHocKyHienCo.intValue() + 1);

        // Tạo liên kết
        HocKyNamHoc hocKyNamHoc = HocKyNamHoc.builder()
                .hocKy(hocKy)
                .namHoc(namHoc)
                .thuTu(thuTu)
                .isActive(true)
                .build();

        hocKyNamHoc = hocKyNamHocRepository.save(hocKyNamHoc);

        log.info("✅ Đã tạo học kỳ {} (thứ tự {}) trong năm học {}",
                hocKy.getMaHocKy(), thuTu, maNamHoc);

        return toDTO(hocKyNamHoc);
    }

    /**
     * Lấy danh sách học kỳ theo năm học
     */
    public List<HocKyDTO> getHocKyByNamHoc(String maNamHoc) {
        return hocKyNamHocRepository.findByNamHocOrderByThuTu(maNamHoc).stream()
                .map(hknh -> toHocKyDTO(hknh.getHocKy(), hknh))
                .toList();
    }

    /**
     * Lấy thông tin năm học của một học kỳ
     */
    public HocKyNamHocDTO getNamHocByHocKy(String maHocKy) {
        return hocKyNamHocRepository.findByHocKyMaHocKyAndIsActiveTrue(maHocKy)
                .map(this::toDTO)
                .orElse(null);
    }

    @Transactional
    public HocKyNamHocDTO create(HocKyNamHocDTO dto) {
        HocKy hocKy = hocKyRepository.findById(dto.getMaHocKy()).orElseThrow();
        NamHoc namHoc = namHocRepository.findById(dto.getMaNamHoc()).orElseThrow();

        HocKyNamHoc entity = HocKyNamHoc.builder()
                .hocKy(hocKy)
                .namHoc(namHoc)
                .thuTu(dto.getThuTu())
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .build();

        return toDTO(hocKyNamHocRepository.save(entity));
    }

    public void delete(Integer id) {
        hocKyNamHocRepository.deleteById(id);
    }

    /**
     * Tạo học kỳ mặc định cho năm học
     */
    @Transactional
    public List<HocKyNamHocDTO> createDefaultSemestersForNamHoc(String maNamHoc) {
        NamHoc namHoc = namHocRepository.findById(maNamHoc)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy năm học: " + maNamHoc));

        LocalDate startDate = namHoc.getNgayBatDau();
        LocalDate endDate = namHoc.getNgayKetThuc();

        // Tính toán thời gian cho 2 học kỳ
        long totalDays = ChronoUnit.DAYS.between(startDate, endDate);
        long semester1Days = totalDays * 45 / 100; // 45% cho học kỳ 1
        long breakDays = totalDays * 10 / 100;     // 10% nghỉ giữa kỳ

        // Học kỳ 1
        LocalDate sem1Start = startDate;
        LocalDate sem1End = startDate.plusDays(semester1Days);

        HocKyDTO semester1 = HocKyDTO.builder()
                .maHocKy(maNamHoc + "_HK1")
                .tenHocKy("Học kỳ 1 - " + namHoc.getTenNamHoc())
                .ngayBatDau(sem1Start)
                .ngayKetThuc(sem1End)
                .moTa("Học kỳ 1 của " + namHoc.getTenNamHoc())
                .thuTu(1)
                .build();

        // Học kỳ 2
        LocalDate sem2Start = sem1End.plusDays(breakDays + 1);
        LocalDate sem2End = endDate;

        HocKyDTO semester2 = HocKyDTO.builder()
                .maHocKy(maNamHoc + "_HK2")
                .tenHocKy("Học kỳ 2 - " + namHoc.getTenNamHoc())
                .ngayBatDau(sem2Start)
                .ngayKetThuc(sem2End)
                .moTa("Học kỳ 2 của " + namHoc.getTenNamHoc())
                .thuTu(2)
                .build();

        List<HocKyNamHocDTO> result = List.of(
                createHocKyInNamHoc(maNamHoc, semester1),
                createHocKyInNamHoc(maNamHoc, semester2)
        );

        log.info("✅ Đã tạo {} học kỳ mặc định cho năm học {}", result.size(), maNamHoc);
        return result;
    }

    // Helper methods
    private void validateHocKyTimeInNamHoc(HocKyDTO hocKyDTO, NamHoc namHoc) {
        LocalDate namHocStart = namHoc.getNgayBatDau();
        LocalDate namHocEnd = namHoc.getNgayKetThuc();
        LocalDate hocKyStart = hocKyDTO.getNgayBatDau();
        LocalDate hocKyEnd = hocKyDTO.getNgayKetThuc();

        if (hocKyStart.isBefore(namHocStart) || hocKyEnd.isAfter(namHocEnd)) {
            throw new RuntimeException(
                    String.format("Thời gian học kỳ (%s - %s) phải nằm trong năm học (%s - %s)",
                            hocKyStart, hocKyEnd, namHocStart, namHocEnd)
            );
        }
    }

    private HocKyNamHocDTO toDTO(HocKyNamHoc entity) {
        return HocKyNamHocDTO.builder()
                .id(entity.getId())
                .maHocKy(entity.getHocKy().getMaHocKy())
                .maNamHoc(entity.getNamHoc().getMaNamHoc())
                .thuTu(entity.getThuTu())
                .isActive(entity.getIsActive())
                .tenHocKy(entity.getHocKy().getTenHocKy())
                .tenNamHoc(entity.getNamHoc().getTenNamHoc())
                .build();
    }

    private HocKyDTO toHocKyDTO(HocKy hocKy, HocKyNamHoc hocKyNamHoc) {
        LocalDate now = LocalDate.now();

        // Calculate status
        String trangThai;
        if (hocKy.isUpcoming()) {
            trangThai = "Chưa bắt đầu";
        } else if (hocKy.isOngoing()) {
            trangThai = "Đang diễn ra";
        } else {
            trangThai = "Đã kết thúc";
        }

        // Calculate progress
        Integer soNgayConLai = null;
        Integer tongSoNgay = null;
        Double tiLePhanTram = null;

        if (hocKy.getNgayBatDau() != null && hocKy.getNgayKetThuc() != null) {
            tongSoNgay = (int) ChronoUnit.DAYS.between(hocKy.getNgayBatDau(), hocKy.getNgayKetThuc()) + 1;

            if (hocKy.isOngoing()) {
                soNgayConLai = (int) ChronoUnit.DAYS.between(now, hocKy.getNgayKetThuc());
                long ngayDaQua = ChronoUnit.DAYS.between(hocKy.getNgayBatDau(), now) + 1;
                tiLePhanTram = (double) ngayDaQua / tongSoNgay * 100;
            } else if (hocKy.isUpcoming()) {
                soNgayConLai = (int) ChronoUnit.DAYS.between(now, hocKy.getNgayBatDau());
                tiLePhanTram = 0.0;
            } else {
                soNgayConLai = 0;
                tiLePhanTram = 100.0;
            }
        }

        return HocKyDTO.builder()
                .maHocKy(hocKy.getMaHocKy())
                .tenHocKy(hocKy.getTenHocKy())
                .ngayBatDau(hocKy.getNgayBatDau())
                .ngayKetThuc(hocKy.getNgayKetThuc())
                .moTa(hocKy.getMoTa())
                .isActive(hocKy.getIsActive())
                .isCurrent(hocKy.getIsCurrent())
                .maNamHoc(hocKyNamHoc.getNamHoc().getMaNamHoc())
                .tenNamHoc(hocKyNamHoc.getNamHoc().getTenNamHoc())
                .thuTu(hocKyNamHoc.getThuTu())
                .trangThai(trangThai)
                .soNgayConLai(soNgayConLai)
                .tongSoNgay(tongSoNgay)
                .tiLePhanTram(tiLePhanTram)
                .build();
    }
}