package com.fileconverter.core;

import com.fileconverter.core.converter.ImageFileConverter;
import com.fileconverter.core.model.ConversionRequest;
import com.fileconverter.core.model.FileFormat;
import com.fileconverter.core.validation.ConversionValidator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ConversionValidatorTest {

    @TempDir
    Path tempDir;

    private ConversionValidator validator;
    private Path input;

    @BeforeEach
    void setUp() throws IOException {
        validator = new ConversionValidator(List.of(new ImageFileConverter()));

        // Create a small file with a valid PNG extension.
        input = tempDir.resolve("test.png");
        Files.write(input, new byte[]{1, 2, 3, 4});
    }

    // Creates a request using the default test input file.
    private ConversionRequest createRequest(FileFormat source, FileFormat target) {
        Path output = tempDir.resolve("output." + target.getExtension());
        return new ConversionRequest(input, output, source, target);
    }

    @Test
    void testValidConversion() {
        ConversionRequest request = createRequest(FileFormat.PNG, FileFormat.JPG);

        assertDoesNotThrow(() -> validator.validate(request));
    }

    @Test
    void testMissingInputFile() {
        Path missing = tempDir.resolve("missing.png");
        Path output = tempDir.resolve("output.jpg");
        ConversionRequest request = new ConversionRequest(missing, output, FileFormat.PNG, FileFormat.JPG);

        assertThrows(IOException.class, () -> validator.validate(request));
    }

    @Test
    void testInputExtensionMismatch() {
        ConversionRequest request = createRequest(FileFormat.JPG, FileFormat.BMP);

        assertThrows(IOException.class, () -> validator.validate(request));
    }

    @Test
    void testOutputExtensionMismatch() {
        Path output = tempDir.resolve("output.bmp");
        ConversionRequest request = new ConversionRequest(input, output, FileFormat.PNG, FileFormat.JPG);

        assertThrows(IOException.class, () -> validator.validate(request));
    }

    @Test
    void testUnsupportedWebpConversion() {
        ConversionRequest request = createRequest(FileFormat.PNG, FileFormat.WEBP);

        assertThrows(IOException.class, () -> validator.validate(request));
    }

    @Test
    void testSameFormatConversion() {
        ConversionRequest request = createRequest(FileFormat.PNG, FileFormat.PNG);

        assertThrows(IOException.class, () -> validator.validate(request));
    }

    @Test
    void testExistingOutputFile() throws IOException {
        Path output = tempDir.resolve("output.jpg");
        Files.writeString(output, "Existing file");
        ConversionRequest request = new ConversionRequest(input, output, FileFormat.PNG, FileFormat.JPG);

        assertThrows(IOException.class, () -> validator.validate(request));
    }

    @Test
    void testFileExactlyAtSizeLimit() throws IOException {
        createSparseFile(input, ConversionValidator.MAX_FILE_SIZE);
        ConversionRequest request = createRequest(FileFormat.PNG, FileFormat.JPG);

        assertDoesNotThrow(() -> validator.validate(request));
    }

    @Test
    void testFileExceedsSizeLimit() throws IOException {
        createSparseFile(input, ConversionValidator.MAX_FILE_SIZE + 1);
        ConversionRequest request = createRequest(FileFormat.PNG, FileFormat.JPG);

        IOException exception = assertThrows(IOException.class, () -> validator.validate(request));
        assertTrue(exception.getMessage().contains("1 GB"));
    }

    @Test
    void testEmptyBatch() {
        assertThrows(IllegalArgumentException.class, () -> validator.validateBatch(List.of()));
    }

    @Test
    void testBatchExactlyTwentyFiles() {
        List<ConversionRequest> requests = createBatch(20);

        assertDoesNotThrow(() -> validator.validateBatch(requests));
    }

    @Test
    void testBatchExceedsTwentyFiles() {
        List<ConversionRequest> requests = createBatch(21);

        assertThrows(IllegalArgumentException.class, () -> validator.validateBatch(requests));
    }

    // Creates a file with a specific logical size without writing gigabytes of data.
    private void createSparseFile(Path path, long size) throws IOException {
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
            channel.position(size - 1);
            channel.write(ByteBuffer.wrap(new byte[]{0}));
        }
    }

    // Creates requests for batch boundary testing.
    private List<ConversionRequest> createBatch(int count) {
        List<ConversionRequest> requests = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            Path output = tempDir.resolve("output" + i + ".jpg");
            requests.add(new ConversionRequest(input, output, FileFormat.PNG, FileFormat.JPG));
        }

        return requests;
    }
}
