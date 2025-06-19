package com.tathanhloc.faceattendance.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;

    public void sendResetPasswordEmail(String to, String tempPassword) {
        String subject = "Mật khẩu mới cho tài khoản của bạn";
        String content = String.format("""
                <p>Xin chào,</p>
                <p>Bạn đã yêu cầu đặt lại mật khẩu. Dưới đây là mật khẩu tạm thời:</p>
                <h3 style="color: blue;">%s</h3>
                <p>Hãy đăng nhập và đổi lại mật khẩu ngay sau đó.</p>
                <p>Trân trọng,<br>Hệ thống điểm danh phòng máy</p>
                """, tempPassword);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Không thể gửi email: " + e.getMessage());
        }
    }
}
