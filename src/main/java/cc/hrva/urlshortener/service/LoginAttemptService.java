package cc.hrva.urlshortener.service;

public interface LoginAttemptService {

    void loginFailed(String key);
    boolean isBlocked(String key);
    void loginSucceeded(String key);
    java.util.Map<String, Integer> getLoginAttempts();
    void clearLoginAttempts();

}
