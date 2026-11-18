package com.fileconverter.core;

import com.fileconverter.core.converter.ImageFileConverter;
import com.fileconverter.core.model.ConversionRequest;
import com.fileconverter.core.model.ConversionResult;
import com.fileconverter.core.model.FileFormat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class ImageFileConverterTest {

    @TempDir
    Path tempDir;

    private ImageFileConverter converter;

    @BeforeEach
    void setUp() {
        converter = new ImageFileConverter();
    }

    // Creates a small sample image for testing.
    private Path createTestImage(String filename, String format) throws IOException {
        Path path = tempDir.resolve(filename);
        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();

        try {
            graphics.setColor(Color.BLUE);
            graphics.fillRect(0, 0, 100, 100);
        }
        finally {
            graphics.dispose();
        }

        assertTrue(ImageIO.write(image, format, path.toFile()));
        return path;
    }

    @Test
    void testPngToJpg() throws IOException {
        Path input = createTestImage("test.png", "png");
        Path output = tempDir.resolve("test.jpg");

        ConversionRequest request = new ConversionRequest(input, output, FileFormat.PNG, FileFormat.JPG);
        ConversionResult result = converter.convert(request);

        assertTrue(result.successful());
        assertTrue(Files.exists(output));
        assertTrue(Files.size(output) > 0);
        assertNotNull(ImageIO.read(output.toFile()));
    }

    @Test
    void testJpgToPng() throws IOException {
        Path input = createTestImage("test.jpg", "jpg");
        Path output = tempDir.resolve("test.png");

        ConversionRequest request = new ConversionRequest(input, output, FileFormat.JPG, FileFormat.PNG);
        ConversionResult result = converter.convert(request);

        assertTrue(result.successful());
        assertTrue(Files.exists(output));
        assertNotNull(ImageIO.read(output.toFile()));
    }

    @Test
    void testPngToBmp() throws IOException {
        Path input = createTestImage("test.png", "png");
        Path output = tempDir.resolve("test.bmp");

        ConversionRequest request = new ConversionRequest(input, output, FileFormat.PNG, FileFormat.BMP);
        ConversionResult result = converter.convert(request);

        assertTrue(result.successful());
        assertTrue(Files.exists(output));
        assertNotNull(ImageIO.read(output.toFile()));
    }

    @Test
    void testUnsupportedWebpConversion() {
        assertFalse(converter.supports(FileFormat.WEBP, FileFormat.PNG));
        assertFalse(converter.supports(FileFormat.PNG, FileFormat.WEBP));
    }

    @Test
    void testSameFormatRejected() {
        assertFalse(converter.supports(FileFormat.PNG, FileFormat.PNG));
    }

    @Test
    void testMissingInputFile() {
        Path input = tempDir.resolve("missing.png");
        Path output = tempDir.resolve("output.jpg");

        ConversionRequest request = new ConversionRequest(input, output, FileFormat.PNG, FileFormat.JPG);
        ConversionResult result = converter.convert(request);

        assertFalse(result.successful());
        assertNotNull(result.errorMessage());
        assertFalse(Files.exists(output));
    }

    @Test
    void testExistingOutputNotOverwritten() throws IOException {
        Path input = createTestImage("input.png", "png");
        Path output = tempDir.resolve("existing.jpg");
        Files.writeString(output, "Original data");

        ConversionRequest request = new ConversionRequest(input, output, FileFormat.PNG, FileFormat.JPG);
        ConversionResult result = converter.convert(request);

        assertFalse(result.successful());
        assertEquals("Original data", Files.readString(output));
    }

    @Test
    void testInvalidInputData() throws IOException {
        Path input = tempDir.resolve("invalid.png");
        Path output = tempDir.resolve("output.jpg");
        Files.writeString(input, "This is not an image");

        ConversionRequest request = new ConversionRequest(input, output, FileFormat.PNG, FileFormat.JPG);
        ConversionResult result = converter.convert(request);

        assertFalse(result.successful());
        assertFalse(Files.exists(output));
    }

    @Test
    void testImageExceedsSizeLimit() throws IOException {
        Path input = tempDir.resolve("oversized.png");
        Path output = tempDir.resolve("output.jpg");

        // Creates a sparse file without filling 1 GB of disk space on supported file systems.
        try (var file = java.nio.channels.FileChannel.open(input, java.nio.file.StandardOpenOption.CREATE_NEW, java.nio.file.StandardOpenOption.WRITE)) {
            file.position(1_000_000_000L);
            file.write(java.nio.ByteBuffer.wrap(new byte[]{0}));
        }

        ConversionRequest request = new ConversionRequest(input, output, FileFormat.PNG, FileFormat.JPG);
        ConversionResult result = converter.convert(request);

        assertFalse(result.successful());
        assertTrue(result.errorMessage().contains("1 GB"));
        assertFalse(Files.exists(output));
    }
}