package com.tathanhloc.faceattendance.Service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Lớp service cơ sở cung cấp các phương thức chung cho các service khác
 * @param <T> Entity type
 * @param <ID> ID type
 * @param <D> DTO type
 */
public abstract class BaseService<T, ID, D> {

    protected abstract JpaRepository<T, ID> getRepository();

    protected abstract D toDTO(T entity);

    protected abstract T toEntity(D dto);

    protected abstract void setActive(T entity, boolean active);

    protected abstract boolean isActive(T entity);

    /**
     * Lấy tất cả các bản ghi đang hoạt động
     */
    public List<D> getAllActive() {
        return getRepository().findAll().stream()
                .filter(this::isActive)
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Lấy tất cả các bản ghi (bao gồm cả không hoạt động)
     */
    public List<D> getAll() {
        return getRepository().findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Lấy tất cả các bản ghi với phân trang
     */
    public Page<D> getAllWithPagination(Pageable pageable) {
        return getRepository().findAll(pageable)
                .map(this::toDTO);
    }

    /**
     * Lấy bản ghi theo ID (chỉ trả về nếu đang hoạt động)
     */
    public Optional<D> getActiveById(ID id) {
        return getRepository().findById(id)
                .filter(this::isActive)
                .map(this::toDTO);
    }

    /**
     * Lấy bản ghi theo ID (bao gồm cả không hoạt động)
     */
    public D getById(ID id) {
        return getRepository().findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new com.tathanhloc.faceattendance.Exception.ResourceNotFoundException("Không tìm thấy bản ghi với ID: " + id));
    }

    /**
     * Xóa mềm bản ghi (đặt isActive = false)
     */
    public void softDelete(ID id) {
        getRepository().findById(id).ifPresent(entity -> {
            setActive(entity, false);
            getRepository().save(entity);
        });
    }

    /**
     * Khôi phục bản ghi đã xóa mềm (đặt isActive = true)
     */
    public void restore(ID id) {
        getRepository().findById(id).ifPresent(entity -> {
            setActive(entity, true);
            getRepository().save(entity);
        });
    }
}
