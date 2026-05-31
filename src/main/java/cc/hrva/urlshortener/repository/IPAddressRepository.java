package cc.hrva.urlshortener.repository;

import java.time.LocalDateTime;
import java.util.List;
import cc.hrva.urlshortener.model.IPAddress;
import cc.hrva.urlshortener.model.Url;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IPAddressRepository extends JpaRepository<IPAddress, Long> {

    List<IPAddress> findAllByUrl(Url url);
    void deleteByUrlIn(List<Url> urls);
    List<IPAddress> findByUrlAndActiveTrue(Url url);
    List<IPAddress> findByCreateDateIsLessThanEqualAndActiveTrue(LocalDateTime createDate);
    List<IPAddress> findByCreateDateIsLessThanEqualAndActiveFalse(LocalDateTime createDate);

}
