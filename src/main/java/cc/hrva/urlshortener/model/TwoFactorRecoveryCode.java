package cc.hrva.urlshortener.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TwoFactorRecoveryCode {

    @Id
    @SequenceGenerator(name = "two_factor_recovery_code_id_seq", allocationSize = 1)
    @GeneratedValue(generator = "two_factor_recovery_code_id_seq", strategy = GenerationType.SEQUENCE)
    private Long id;

    @ManyToOne
    private User user;

    private String codeHash;

    private boolean used;

}
