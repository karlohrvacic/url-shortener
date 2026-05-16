package cc.hrva.urlshortener.service;

import cc.hrva.urlshortener.dto.LinkPreviewResponse;

public interface LinkPreviewService {

    LinkPreviewResponse fetchPreview(String url);

}
