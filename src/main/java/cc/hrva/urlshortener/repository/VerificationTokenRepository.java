package cc.hrva.urlshortener.repository;

import java.util.List;
import java.util.Optional;
import cc.hrva.urlshortener.model.User;
import cc.hrva.urlshortener.model.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {

    Optional<VerificationToken> findByTokenAndActiveTrue(String token);
    List<VerificationToken> findByUserAndActiveTrue(User user);
    void deleteByUser(User user);

}
