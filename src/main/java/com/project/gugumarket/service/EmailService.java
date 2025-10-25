package com.project.gugumarket.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    /**
     * 비밀번호 재설정 이메일 발송
     */
    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("[GUGU Market] 비밀번호 재설정 안내");
        message.setText(
                "안녕하세요, GUGU Market입니다.\n\n" +
                        "비밀번호 재설정을 요청하셨습니다.\n" +
                        "아래 링크를 클릭하여 비밀번호를 재설정해주세요.\n\n" +
                        resetLink + "\n\n" +
                        "이 링크는 24시간 동안 유효합니다.\n" +
                        "요청하지 않으셨다면 이 이메일을 무시하셔도 됩니다.\n\n" +
                        "감사합니다."
        );

        mailSender.send(message);
    }
}