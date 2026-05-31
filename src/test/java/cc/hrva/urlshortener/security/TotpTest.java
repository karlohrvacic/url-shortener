package cc.hrva.urlshortener.security;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TotpTest {

    @Test
    void shouldRoundTripBase32() {
        final var original = "Hello, TOTP!".getBytes(StandardCharsets.UTF_8);
        final var encoded = Totp.base32Encode(original);
        assertThat(Totp.base32Decode(encoded)).isEqualTo(original);
    }

    @Test
    void shouldGenerateDecodableSecret() {
        final var secret = Totp.generateSecret();
        assertThat(secret).isNotBlank();
        assertThat(Totp.base32Decode(secret)).hasSize(20);
    }

    @Test
    void shouldVerifyCurrentCode() {
        final var secret = Totp.generateSecret();
        assertThat(Totp.verify(secret, Totp.currentCode(secret))).isTrue();
    }

    @Test
    void shouldRejectWrongCode() {
        final var secret = Totp.generateSecret();
        final var current = Totp.currentCode(secret);
        final var wrong = current.equals("000000") ? "111111" : "000000";
        assertThat(Totp.verify(secret, wrong)).isFalse();
    }

    @Test
    void shouldRejectMalformedCode() {
        final var secret = Totp.generateSecret();
        assertThat(Totp.verify(secret, "abc")).isFalse();
        assertThat(Totp.verify(secret, "12345")).isFalse();
        assertThat(Totp.verify(secret, null)).isFalse();
    }

    @Test
    void shouldBuildOtpAuthUri() {
        final var uri = Totp.buildOtpAuthUri("ABC234", "user@example.com", "hrva.cc");
        assertThat(uri).startsWith("otpauth://totp/");
        assertThat(uri).contains("secret=ABC234");
        assertThat(uri).contains("issuer=hrva.cc");
    }
}
