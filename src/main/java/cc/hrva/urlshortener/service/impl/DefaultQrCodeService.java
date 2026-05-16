package cc.hrva.urlshortener.service.impl;

import cc.hrva.urlshortener.service.QrCodeService;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DefaultQrCodeService implements QrCodeService {

    private static final Logger log = LoggerFactory.getLogger(DefaultQrCodeService.class);

    @Override
    public byte[] generateQrCode(final String text, final int width, final int height) {
        try {
            final var qrCodeWriter = new QRCodeWriter();
            final var bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height);
            final var outputStream = new ByteArrayOutputStream();

            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
            return outputStream.toByteArray();
        } catch (final WriterException | IOException exception) {
            log.error("Failed to generate QR code", exception);
            throw new RuntimeException("Failed to generate QR code", exception);
        }
    }

}
