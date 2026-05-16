package cc.hrva.urlshortener.service;

public interface QrCodeService {

    byte[] generateQrCode(String text, int width, int height);

}
