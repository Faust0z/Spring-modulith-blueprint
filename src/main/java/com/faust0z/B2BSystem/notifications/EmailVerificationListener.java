package com.faust0z.B2BSystem.notifications;

import com.faust0z.B2BSystem.tenant.TenantEmailVerificationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
class EmailVerificationListener {

    private static final String VALIDATE_PATH = "/validar-correo?token=";

    private final EmailService emailService;
    private final String frontendUrl;

    EmailVerificationListener(
            EmailService emailService,
            @Value("${app.frontend.url}") String frontendUrl) {
        this.emailService = emailService;
        this.frontendUrl = frontendUrl;
    }

    @ApplicationModuleListener
    void onEmailVerificationRequested(TenantEmailVerificationEvent event) {
        log.info("Sending email verification link to {}", event.email());

        var validateUrl = frontendUrl + VALIDATE_PATH + event.token();

        var htmlBody = """
                <!DOCTYPE html>
                <html lang="es">
                <body style="font-family: Arial, sans-serif; background-color: #f4f4f5; padding: 40px 0; margin: 0;">
                    <table align="center" width="600" style="background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 4px 6px rgba(0,0,0,0.1); border-collapse: collapse;">
                        <tr>
                            <td style="background-color: #0f172a; padding: 30px; text-align: center;">
                                <h1 style="color: #ffffff; margin: 0; font-size: 24px;">Confirma tu correo</h1>
                            </td>
                        </tr>
                        <tr>
                            <td style="padding: 40px 30px; color: #334155; line-height: 1.6; font-size: 16px;">
                                <p style="margin-top: 0;">Hola,</p>
                                <p>Para poder usar tu cuenta de <strong>%s</strong>, confirmá que este correo electrónico es tuyo haciendo clic en el siguiente botón:</p>
                                <div style="text-align: center; margin: 40px 0;">
                                    <a href="%s" style="background-color: #2563eb; color: #ffffff; text-decoration: none; padding: 14px 28px; border-radius: 6px; font-weight: bold; display: inline-block;">Validar mi correo</a>
                                </div>
                                <p style="font-size: 14px; color: #64748b;">El enlace vence en 24 horas. Si no lo usás en ese plazo, podés solicitar uno nuevo desde la plataforma.</p>
                            </td>
                        </tr>
                        <tr>
                            <td style="background-color: #f8fafc; padding: 20px; text-align: center; font-size: 12px; color: #94a3b8; border-top: 1px solid #e2e8f0;">
                                <p style="margin: 0;">Este es un mensaje automático, por favor no respondas a este correo.</p>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """.formatted(frontendUrl, validateUrl);

        emailService.sendEmail(event.email(), "Confirmá tu correo electrónico", htmlBody);
    }
}