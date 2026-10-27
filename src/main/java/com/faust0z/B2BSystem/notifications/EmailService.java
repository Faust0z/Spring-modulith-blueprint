package com.faust0z.B2BSystem.notifications;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
class EmailService {

    private final org.springframework.web.client.RestClient restClient;
    private final String fromEmail;

    EmailService(org.springframework.web.client.RestClient.Builder restClientBuilder,
                 @Value("${app.resend.api-key}") String resendApiKey,
                 @Value("${app.resend.from-email}") String fromEmail) {
        this.restClient = restClientBuilder
                .baseUrl("https://api.resend.com")
                .defaultHeader("Authorization", "Bearer " + resendApiKey)
                .build();
        this.fromEmail = fromEmail;
    }

    void sendEmail(String toEmail, String subject, String htmlBody) {
        var requestBody = new ResendEmailRequest(fromEmail, toEmail, subject, htmlBody);

        restClient.post()
                .uri("/emails")
                .body(requestBody)
                .retrieve()
                .toBodilessEntity();
    }
}

