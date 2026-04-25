package com.yas.media.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.media.model.Media;
import com.yas.media.model.dto.MediaDto;
import com.yas.media.service.MediaService;
import com.yas.media.viewmodel.MediaPostVm;
import com.yas.media.viewmodel.MediaVm;
import com.yas.media.viewmodel.NoFileMediaVm;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class MediaControllerTest {

    @Mock
    private MediaService mediaService;

    private MediaController mediaController;

    @BeforeEach
    void setUp() {
        mediaController = new MediaController(mediaService);
    }

    @Test
    void create_whenServiceReturnsMedia_thenReturnsNoFileMediaVm() {
        Media media = new Media();
        media.setId(1L);
        media.setCaption("caption");
        media.setFileName("file.png");
        media.setMediaType("image/png");
        when(mediaService.saveMedia(org.mockito.ArgumentMatchers.any(MediaPostVm.class))).thenReturn(media);

        MockMultipartFile multipartFile = new MockMultipartFile("file", "file.png", "image/png", new byte[] {1});
        var response = mediaController.create(new MediaPostVm("caption", multipartFile, "file.png"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertThat(response.getBody()).isInstanceOf(NoFileMediaVm.class);
        NoFileMediaVm body = (NoFileMediaVm) response.getBody();
        assertNotNull(body);
        assertEquals(1L, body.id());
        assertEquals("caption", body.caption());
        assertEquals("file.png", body.fileName());
        assertEquals("image/png", body.mediaType());
        verify(mediaService).saveMedia(org.mockito.ArgumentMatchers.any(MediaPostVm.class));
    }

    @Test
    void delete_whenServiceCompletes_thenReturnsNoContent() {
        var response = mediaController.delete(1L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());
        verify(mediaService).removeMedia(1L);
    }

    @Test
    void get_whenServiceReturnsMedia_thenReturnsOk() {
        MediaVm mediaVm = new MediaVm(1L, "caption", "file.png", "image/png", "/media/medias/1/file/file.png");
        when(mediaService.getMediaById(1L)).thenReturn(mediaVm);

        var response = mediaController.get(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("caption", response.getBody().getCaption());
        assertEquals("file.png", response.getBody().getFileName());
        assertEquals("image/png", response.getBody().getMediaType());
        verify(mediaService).getMediaById(1L);
    }

    @Test
    void get_whenServiceReturnsNull_thenReturnsNotFound() {
        when(mediaService.getMediaById(1L)).thenReturn(null);

        var response = mediaController.get(1L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
        verify(mediaService).getMediaById(1L);
    }

    @Test
    void getByIds_whenServiceReturnsMedias_thenReturnsOk() {
        MediaVm first = new MediaVm(1L, "first", "first.png", "image/png", "/media/medias/1/file/first.png");
        MediaVm second = new MediaVm(2L, "second", "second.png", "image/png", "/media/medias/2/file/second.png");
        when(mediaService.getMediaByIds(List.of(1L, 2L))).thenReturn(List.of(first, second));

        var response = mediaController.getByIds(List.of(1L, 2L));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody()).extracting(MediaVm::getCaption)
            .containsExactlyInAnyOrder("first", "second");
        verify(mediaService).getMediaByIds(List.of(1L, 2L));
    }

    @Test
    void getByIds_whenServiceReturnsEmptyList_thenReturnsNotFound() {
        when(mediaService.getMediaByIds(List.of(1L, 2L))).thenReturn(List.of());

        var response = mediaController.getByIds(List.of(1L, 2L));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
        verify(mediaService).getMediaByIds(List.of(1L, 2L));
    }

    @Test
    void getFile_whenServiceReturnsMediaDto_thenReturnsAttachmentResponse() throws IOException {
        byte[] fileContent = "file-content".getBytes(StandardCharsets.UTF_8);
        MediaDto mediaDto = MediaDto.builder()
            .content(new ByteArrayInputStream(fileContent))
            .mediaType(MediaType.valueOf("image/png"))
            .build();
        when(mediaService.getFile(1L, "file.png")).thenReturn(mediaDto);

        var response = mediaController.getFile(1L, "file.png");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("attachment; filename=\"file.png\"", response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION));
        assertEquals(MediaType.valueOf("image/png"), response.getHeaders().getContentType());
        assertNotNull(response.getBody());
        assertArrayEquals(fileContent, response.getBody().getInputStream().readAllBytes());
        verify(mediaService).getFile(1L, "file.png");
    }
}