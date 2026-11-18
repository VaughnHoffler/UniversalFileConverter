package com.fileconverter.core.validation;

import com.fileconverter.core.converter.FileConverter;
import com.fileconverter.core.model.ConversionRequest;
import com.fileconverter.core.model.FileFormat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public class ConversionValidator {
    public static final int MAX_BATCH_SIZE = 20;
    public static final long MAX_FILE_SIZE = 1_000_000_000L;

    private final List<FileConverter> converters;

    public ConversionValidator(List<FileConverter> converters) {
        Objects.requireNonNull(converters, "Converter list cannot be null");
        this.converters = List.copyOf(converters);
    }

    // Validates an individual conversion request.
    public void validate(ConversionRequest request) throws IOException {
        Objects.requireNonNull(request, "Conversion request cannot be null");

        Path input = request.inputPath();
        Path output = request.outputPath();

        if (!Files.isRegularFile(input)) {
            throw new IOException("Input file does not exist or is not a regular file");
        }

        long fileSize = Files.size(input);

        // Image and document files are limited to 1 GB.
        if (fileSize > MAX_FILE_SIZE) {
            throw new IOException("File exceeds the 1 GB size limit");
        }

        if (!Files.isReadable(input)) {
            throw new IOException("Input file cannot be read");
        }

        if (!Files.isDirectory(output.toAbsolutePath().getParent())) {
            throw new IOException("Output directory does not exist");
        }

        if (Files.exists(output)) {
            throw new IOException("Output file already exists");
        }

        // Ensure the input extension matches its declared format.
        String filename = input.getFileName().toString().toLowerCase();
        String extension = "." + request.sourceFormat().getExtension();

        boolean validExtension = filename.endsWith(extension);

        if (request.sourceFormat() == FileFormat.JPG) {
            validExtension = filename.endsWith(".jpg") || filename.endsWith(".jpeg");
        }

        if (!validExtension) {
            throw new IOException("Input file extension does not match the source format");
        }

        // Ensure the output filename matches the requested target format.
        String outputName = output.getFileName().toString().toLowerCase();
        String targetExtension = "." + request.targetFormat().getExtension();

        boolean validOutputExtension = outputName.endsWith(targetExtension);

        if (request.targetFormat() == FileFormat.JPG) {
            validOutputExtension = outputName.endsWith(".jpg") || outputName.endsWith(".jpeg");
        }

        if (!validOutputExtension) {
            throw new IOException("Output file extension does not match the target format");
        }

        boolean supported = false;

        for (FileConverter converter : converters) {
            if (converter.supports(request.sourceFormat(), request.targetFormat())) {
                supported = true;
                break;
            }
        }

        if (!supported) {
            throw new IOException("Unsupported conversion: " + request.sourceFormat() + " to " + request.targetFormat());
        }
    }

    // Checks the number of files before processing a batch.
    public void validateBatch(List<ConversionRequest> requests) {
        Objects.requireNonNull(requests, "Batch cannot be null");

        if (requests.isEmpty()) {
            throw new IllegalArgumentException("Batch must contain at least one file");
        }

        if (requests.size() > MAX_BATCH_SIZE) {
            throw new IllegalArgumentException("Batch cannot contain more than " + MAX_BATCH_SIZE + " files");
        }
    }
}
