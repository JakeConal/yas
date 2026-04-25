package com.yas.media.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ConstraintValidatorContext.ConstraintViolationBuilder;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class FileTypeValidatorTest {

    @Mock
    private ValidFileType validFileType;

    @Mock
    private ConstraintValidatorContext context;

    @Mock
    private ConstraintViolationBuilder constraintViolationBuilder;

    private FileTypeValidator validator;

    @BeforeEach
    void setUp() {
        when(validFileType.allowedTypes()).thenReturn(new String[] {"image/png"});
        when(validFileType.message()).thenReturn("File type not allowed");
        lenient().when(context.buildConstraintViolationWithTemplate("File type not allowed"))
            .thenReturn(constraintViolationBuilder);
        lenient().when(constraintViolationBuilder.addConstraintViolation()).thenReturn(context);

        validator = new FileTypeValidator();
        validator.initialize(validFileType);
    }

    @Test
    void isValid_whenFileIsNull_thenReturnsFalseAndAddsViolation() {
        assertFalse(validator.isValid(null, context));

        verify(context).disableDefaultConstraintViolation();
        verify(context).buildConstraintViolationWithTemplate("File type not allowed");
        verify(constraintViolationBuilder).addConstraintViolation();
    }

    @Test
    void isValid_whenFileTypeIsNotAllowed_thenReturnsFalseAndAddsViolation() {
        MultipartFile multipartFile = new MockMultipartFile(
            "file",
            "example.txt",
            "text/plain",
            "not-an-image".getBytes()
        );

        assertFalse(validator.isValid(multipartFile, context));

        verify(context).disableDefaultConstraintViolation();
        verify(context).buildConstraintViolationWithTemplate("File type not allowed");
        verify(constraintViolationBuilder).addConstraintViolation();
    }

    @Test
    void isValid_whenSupportedImageIsValid_thenReturnsTrue() throws Exception {
        MultipartFile multipartFile = createImageFile();

        assertTrue(validator.isValid(multipartFile, context));
        verifyNoInteractions(context);
    }

    @Test
    void isValid_whenSupportedImageCannotBeRead_thenReturnsFalse() {
        MultipartFile multipartFile = new MockMultipartFile(
            "file",
            "example.png",
            "image/png",
            "not-a-real-image".getBytes()
        );

        assertFalse(validator.isValid(multipartFile, context));
        verifyNoInteractions(context);
    }

    private static MultipartFile createImageFile() throws Exception {
        BufferedImage bufferedImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        bufferedImage.setRGB(0, 0, Color.RED.getRGB());

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage, "png", outputStream);

        return new MockMultipartFile("file", "example.png", "image/png", outputStream.toByteArray());
    }
}