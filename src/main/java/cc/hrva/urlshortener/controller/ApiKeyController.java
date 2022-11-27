package cc.hrva.urlshortener.controller;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import cc.hrva.urlshortener.dto.ApiKeyUpdateDto;
import cc.hrva.urlshortener.exception.ApiKeyDoesntExistException;
import cc.hrva.urlshortener.exception.UserDoesntExistException;
import cc.hrva.urlshortener.model.ApiKey;
import cc.hrva.urlshortener.service.ApiKeyService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/key")
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    @GetMapping("/new")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ApiKey generateNewApiKey() throws UserDoesntExistException {
        return apiKeyService.generateNewApiKey();
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('ROLE_USER')")
    public List<ApiKey> fetchMyApiKeys() throws UserDoesntExistException {
        return apiKeyService.fetchMyApiKeys();
    }

    @GetMapping()
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public List<ApiKey> fetchAllApiKeys() {
        return apiKeyService.fetchAllApiKeys();
    }

    @PutMapping()
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ApiKey updateApiKey(@Valid @RequestBody final ApiKeyUpdateDto apiKeyUpdateDto) {
        return apiKeyService.updateKey(apiKeyUpdateDto);
    }

    @GetMapping("/revoke/{id}")
    public ApiKey revokeApiKey(@PathVariable("id") final Long id) throws UserDoesntExistException, ApiKeyDoesntExistException {
        return apiKeyService.revokeApiKey(id);
    }

}
