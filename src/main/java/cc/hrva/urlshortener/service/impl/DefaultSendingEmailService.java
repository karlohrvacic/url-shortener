package cc.hrva.urlshortener.service.impl;

import cc.hrva.urlshortener.configuration.properties.AppProperties;
import cc.hrva.urlshortener.model.EmailLog;
import cc.hrva.urlshortener.repository.EmailLogRepository;
import jakarta.mail.MessagingException;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import cc.hrva.urlshortener.model.Email;
import cc.hrva.urlshortener.model.ResetToken;
import cc.hrva.urlshortener.model.Url;
import cc.hrva.urlshortener.model.User;
import cc.hrva.urlshortener.service.SendingEmailService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@Slf4j
@RequiredArgsConstructor
public class DefaultSendingEmailService implements SendingEmailService {

    private final AppProperties appProperties;
    private final TemplateEngine templateEngine;
    private final DefaultEmailService emailService;
    private final EmailLogRepository emailLogRepository;

    @Override
    @Async
    public void sendEmailForgotPassword(final User user, final ResetToken resetToken) {
        final var ctx = getContext(user);
        ctx.setVariable("request_date", resetToken.getCreateDate().truncatedTo(ChronoUnit.SECONDS).format(DateTimeFormatter.ISO_LOCAL_TIME));
        ctx.setVariable("token", resetToken.getToken());
        ctx.setVariable("full_password_reset_link", MessageFormat.format("{0}/reset-password/{1}", appProperties.getFrontendUrl(), resetToken.getToken()));

        final var htmlContent = templateEngine.process("reset_password", ctx);

        final var email = Email.builder()
                .sender(appProperties.getEmailSenderAddress())
                .receivers(new String[]{user.getEmail()})
                .subject("Password reset token")
                .text(htmlContent)
                .build();

        tryToSendEmail(email);
    }

    @Override
    @Async
    public void sendEmailAccountDeactivated(final User user) {
        final var ctx = getContext(user);

        final var htmlContent = templateEngine.process("account_deactivated", ctx);

        final var email = Email.builder()
                .sender(appProperties.getEmailSenderAddress())
                .receivers(new String[]{user.getEmail()})
                .subject("Account deactivated")
                .text(htmlContent)
                .build();

        tryToSendEmail(email);
    }

    @Override
    @Async
    public void sendWelcomeEmail(final User user) {
        final var ctx = getContext(user);

        final var htmlContent = templateEngine.process("welcome", ctx);

        final var email = Email.builder()
                .sender(appProperties.getEmailSenderAddress())
                .receivers(new String[]{user.getEmail()})
                .subject("Welcome")
                .text(htmlContent)
                .build();

        tryToSendEmail(email);
    }

    @Override
    @Async
    public void sendEmailUrlMalwareDetected(final User user, final Url url, final String threatType) {
        final var ctx = getContext(user);
        ctx.setVariable("shortUrl", url.getShortUrl());
        ctx.setVariable("longUrl", url.getLongUrl());
        ctx.setVariable("threatType", threatType);

        final var htmlContent = templateEngine.process("url_malware_detected", ctx);

        final var email = Email.builder()
                .sender(appProperties.getEmailSenderAddress())
                .receivers(new String[]{user.getEmail()})
                .subject("URL deactivated — malware detected")
                .text(htmlContent)
                .build();

        tryToSendEmail(email);
    }

    public void tryToSendEmail(final Email email) {
        final var logEntry = emailLogRepository.save(EmailLog.builder()
                .recipient(Arrays.toString(email.getReceivers()))
                .subject(email.getSubject())
                .status("SENDING")
                .build());

        log.info("Sending {} email to {}", email.getSubject(), Arrays.toString(email.getReceivers()));
        sendEmailWithRetries(email, logEntry);
    }

    private void sendEmailWithRetries(final Email email, final EmailLog logEntry) {
        int attempts = 0;
        while (attempts < 3) {
            final var exception = attemptSendEmail(email);
            if (exception.isEmpty()) {
                logEntry.setStatus("SENT");
                logEntry.setSentAt(LocalDateTime.now());
                emailLogRepository.save(logEntry);
                return;
            }

            attempts++;
            if (attempts >= 3) {
                log.error("Failed to send email after {} attempts", attempts, exception.get());
                logEntry.setStatus("FAILED");
                logEntry.setErrorMessage(exception.get().getMessage());
                emailLogRepository.save(logEntry);
            } else {
                log.warn("Failed to send email (attempt {}/3), retrying...", attempts);
                if (!sleepBeforeRetry(attempts)) {
                    return;
                }
            }
        }
    }

    private Optional<MessagingException> attemptSendEmail(final Email email) {
        try {
            emailService.sendEmail(email, null);
            return Optional.empty();
        } catch (final MessagingException e) {
            return Optional.of(e);
        }
    }

    private boolean sleepBeforeRetry(final int attemptNumber) {
        try {
            Thread.sleep((long) Math.pow(2, attemptNumber) * 100L);
            return true;
        } catch (final InterruptedException ie) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private Context getContext(final User user) {
        final var ctx = new Context();
        ctx.setVariable("name", user.getEmail());
        ctx.setVariable("number_of_api_keys", user.getApiKeySlots().toString());
        ctx.setVariable("app_name", appProperties.getAppName());
        ctx.setVariable("contact_email", appProperties.getContactEmail());
        ctx.setVariable("login_page", MessageFormat.format("{0}/login/", appProperties.getFrontendUrl()));
        ctx.setVariable("api_documentation", MessageFormat.format("{0}/swagger-ui/index.html", appProperties.getServerUrl()));
        ctx.setVariable("days_of_inactivity", appProperties.getDeactivateUserAccountAfterDays().toString());
        ctx.setVariable("password_reset_link", MessageFormat.format("{0}/reset-password", appProperties.getFrontendUrl()));
        ctx.setVariable("token_expiration", appProperties.getResetTokenExpirationInHours().toString());

        return ctx;
    }

}
