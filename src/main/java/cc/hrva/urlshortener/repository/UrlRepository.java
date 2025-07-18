package cc.hrva.urlshortener.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import cc.hrva.urlshortener.model.Url;
import cc.hrva.urlshortener.model.User;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UrlRepository extends JpaRepository<Url, Long> {

    Optional<List<Url>> findAllByOwner(User owner);
    boolean existsUrlByLongUrlAndActiveTrue(String longUrl);
    Optional<Url> findByLongUrlAndActiveTrue(String longUrl);

    @Cacheable(value = "url-exists")
    boolean existsUrlByShortUrlAndActiveTrue(String shortUrl);

    @Cacheable(value = "url")
    Optional<Url> findByShortUrlAndActiveTrue(String shortUrl);
    boolean existsUrlByLongUrlAndActiveTrueAndOwnerIsNull(String longUrl);
    List<Url> findByExpirationDateLessThanEqualAndActiveTrue(LocalDateTime expirationDate);

}
