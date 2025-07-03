package com.tathanhloc.faceattendance.Security;

import com.tathanhloc.faceattendance.Enum.VaiTroEnum;
import com.tathanhloc.faceattendance.Model.TaiKhoan;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import org.springframework.security.core.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@AllArgsConstructor
public class CustomUserDetails implements UserDetails {

    private final TaiKhoan taiKhoan;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        @NotNull(message = "Vai trò không được để trống") VaiTroEnum role = taiKhoan.getVaiTro(); // Giả sử vai trò được lưu trong taiKhoan
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }


    @Override
    public String getPassword() {
        return taiKhoan.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return taiKhoan.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return taiKhoan.getIsActive();
    }

    public TaiKhoan getTaiKhoan() {
        return taiKhoan;
    }
}
