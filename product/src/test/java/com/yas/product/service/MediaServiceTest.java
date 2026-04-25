package com.yas.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.config.ServiceUrlConfig;
import com.yas.product.viewmodel.NoFileMediaVm;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.RequestBodyUriSpec;
import org.springframework.web.client.RestClient.RequestHeadersUriSpec;
import org.springframework.web.client.RestClient.ResponseSpec;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class MediaServiceTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private RestClient restClient;

    @Mock(answer = Answers.RETURNS_SELF)
    private RequestBodyUriSpec requestBodyUriSpec;

    @Mock(answer = Answers.RETURNS_SELF)
    private RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private ResponseSpec responseSpec;

    @Mock
    private ServiceUrlConfig serviceUrlConfig;

    private MediaService mediaService;

    @BeforeEach
    void setUp() {
        mediaService = new MediaService(restClient, serviceUrlConfig);
        lenient().when(serviceUrlConfig.media()).thenReturn("https://media-service.example");
        setAuthentication("jwt-token-123");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getMedia_whenIdIsNull_returnsDefaultResponse() {
        NoFileMediaVm result = mediaService.getMedia(null);

        assertThat(result.id()).isNull();
        assertThat(result.url()).isEmpty();
    }

    @Test
    void getMedia_whenIdIsPresent_returnsRestClientResponse() {
        NoFileMediaVm expected = new NoFileMediaVm(1L, "caption", "file.png", "image/png", "https://cdn/1");
        doReturn(requestHeadersUriSpec).when(restClient).get();
        doReturn(requestHeadersUriSpec).when(requestHeadersUriSpec).uri(any(URI.class));
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(NoFileMediaVm.class)).thenReturn(expected);

        NoFileMediaVm result = mediaService.getMedia(1L);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void saveFile_whenCalled_returnsCreatedMedia() {
        MultipartFile multipartFile = new MockMultipartFile(
            "multipartFile",
            "sample.png",
            "image/png",
            "binary-content".getBytes(StandardCharsets.UTF_8)
        );
        NoFileMediaVm expected = new NoFileMediaVm(2L, "caption", "sample.png", "image/png", "https://cdn/2");
        doReturn(requestBodyUriSpec).when(restClient).post();
        doReturn(requestBodyUriSpec).when(requestBodyUriSpec).uri(any(URI.class));
        when(requestBodyUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(NoFileMediaVm.class)).thenReturn(expected);

        NoFileMediaVm result = mediaService.saveFile(multipartFile, "caption", "sample.png");

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void removeMedia_whenCalled_completesWithoutError() {
        doReturn(requestHeadersUriSpec).when(restClient).delete();
        doReturn(requestHeadersUriSpec).when(requestHeadersUriSpec).uri(any(URI.class));
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(eq(Void.class))).thenReturn(null);

        mediaService.removeMedia(3L);
    }

    private void setAuthentication(String tokenValue) {
        Jwt jwt = Jwt.withTokenValue(tokenValue)
            .header("alg", "none")
            .claim("sub", "user-1")
            .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }
}