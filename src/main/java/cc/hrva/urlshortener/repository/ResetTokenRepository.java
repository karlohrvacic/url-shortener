package cc.hrva.urlshortener.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import cc.hrva.urlshortener.model.ResetToken;
import cc.hrva.urlshortener.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ResetTokenRepository extends JpaRepository<ResetToken, Long> {

    List<ResetToken> findByActiveTrue();
    List<ResetToken> findByUserAndActiveTrue(User user);
    Optional<ResetToken> findResetTokenByUserAndTokenAndActiveTrue(User user, String token);
    List<ResetToken> findByExpirationDateIsLessThanEqualAndActiveTrue(LocalDateTime expirationDate);
    List<ResetToken> findByExpirationDateIsLessThanEqualAndActiveFalse(LocalDateTime expirationDate);
    void deleteByUser(User user);

}
