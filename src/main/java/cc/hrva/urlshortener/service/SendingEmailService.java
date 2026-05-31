package cc.hrva.urlshortener.service;

import cc.hrva.urlshortener.model.ResetToken;
import cc.hrva.urlshortener.model.Url;
import cc.hrva.urlshortener.model.User;

public interface SendingEmailService {

    void sendWelcomeEmail(User user);
    void sendNewUserNotificationToAdmin(User user);
    void sendEmailUrlMalwareDetected(User user, Url url, String threatType);
    void sendEmailAccountDeactivated(User user);
    void sendEmailForgotPassword(User user, ResetToken resetToken);

}
