package com.fileconverter.core;

import com.fileconverter.core.converter.ImageFileConverter;
import com.fileconverter.core.model.ConversionRequest;
import com.fileconverter.core.model.ConversionResult;
import com.fileconverter.core.model.FileFormat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ConversionManagerTest {

    @TempDir
    Path tempDir;

    private ConversionManager manager;
    private Path input;

    @BeforeEach
    void setUp() throws IOException {
        manager = new ConversionManager(List.of(new ImageFileConverter()));

        // Create a valid PNG image for conversion tests.
        input = tempDir.resolve("test.png");
        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        assertTrue(ImageIO.write(image, "png", input.toFile()));
    }

    private ConversionRequest createRequest(String outputName, FileFormat target) {
        return new ConversionRequest(input, tempDir.resolve(outputName), FileFormat.PNG, target);
    }

    @Test
    void testSingleConversion() {
        ConversionRequest request = createRequest("output.jpg", FileFormat.JPG);
        ConversionResult result = manager.convertSingle(request);

        assertTrue(result.successful(), result.errorMessage());
        assertTrue(Files.exists(request.outputPath()));
    }

    @Test
    void testMultipleConversions() {
        List<ConversionRequest> requests = List.of(
                createRequest("output1.jpg", FileFormat.JPG),
                createRequest("output2.bmp", FileFormat.BMP)
        );

        List<ConversionResult> results = manager.convertBatch(requests);

        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(ConversionResult::successful));
    }

    @Test
    void testFailedConversionDoesNotStopBatch() {
        ConversionRequest first = createRequest("first.jpg", FileFormat.JPG);
        ConversionRequest invalid = createRequest("invalid.webp", FileFormat.WEBP);
        ConversionRequest last = createRequest("last.bmp", FileFormat.BMP);

        List<ConversionResult> results = manager.convertBatch(List.of(first, invalid, last));

        assertEquals(3, results.size());
        assertTrue(results.get(0).successful());
        assertFalse(results.get(1).successful());
        assertTrue(results.get(2).successful());

        assertTrue(Files.exists(first.outputPath()));
        assertFalse(Files.exists(invalid.outputPath()));
        assertTrue(Files.exists(last.outputPath()));
    }

    @Test
    void testEmptyBatchRejected() {
        assertThrows(IllegalArgumentException.class, () -> manager.convertBatch(List.of()));
    }

    @Test
    void testTwentyFilesAccepted() {
        List<ConversionRequest> requests = new ArrayList<>();

        for (int i = 0; i < 20; i++) {
            requests.add(createRequest("output" + i + ".jpg", FileFormat.JPG));
        }

        List<ConversionResult> results = manager.convertBatch(requests);

        assertEquals(20, results.size());
        assertEquals(20, results.stream().filter(ConversionResult::successful).count());
    }

    @Test
    void testTwentyOneFilesRejected() {
        List<ConversionRequest> requests = new ArrayList<>();

        for (int i = 0; i < 21; i++) {
            requests.add(createRequest("output" + i + ".jpg", FileFormat.JPG));
        }

        assertThrows(IllegalArgumentException.class, () -> manager.convertBatch(requests));

        // No files should be created when the batch itself is invalid.
        assertFalse(Files.exists(tempDir.resolve("output0.jpg")));
    }

    @Test
    void testMissingInputDoesNotStopBatch() {
        Path missing = tempDir.resolve("missing.png");
        ConversionRequest invalid = new ConversionRequest(missing, tempDir.resolve("missing.jpg"), FileFormat.PNG, FileFormat.JPG);
        ConversionRequest valid = createRequest("valid.jpg", FileFormat.JPG);

        List<ConversionResult> results = manager.convertBatch(List.of(invalid, valid));

        assertFalse(results.get(0).successful());
        assertTrue(results.get(1).successful());
    }

    @Test
    void testExistingOutputRejected() throws IOException {
        Path output = tempDir.resolve("existing.jpg");
        Files.writeString(output, "Original data");

        ConversionResult result = manager.convertSingle(createRequest("existing.jpg", FileFormat.JPG));

        assertFalse(result.successful());
        assertEquals("Original data", Files.readString(output));
    }
}
