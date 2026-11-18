package com.fileconverter.core;

import com.fileconverter.core.converter.FileConverter;
import com.fileconverter.core.model.ConversionRequest;
import com.fileconverter.core.model.ConversionResult;
import com.fileconverter.core.validation.ConversionValidator;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ConversionManager {
    private final List<FileConverter> converters;
    private final ConversionValidator validator;

    public ConversionManager(List<FileConverter> converters) {
        Objects.requireNonNull(converters, "Converter list cannot be null");

        this.converters = List.copyOf(converters);
        this.validator = new ConversionValidator(this.converters);
    }

    // Processes a batch and returns a result for every file.
    public List<ConversionResult> convertBatch(List<ConversionRequest> requests) {
        validator.validateBatch(requests);

        if (requests.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Batch cannot contain null requests");
        }

        List<ConversionResult> results = new ArrayList<>();

        for (ConversionRequest request : requests) {
            results.add(convertSingle(request));
        }

        return results;
    }

    // Validates and processes an individual conversion.
    public ConversionResult convertSingle(ConversionRequest request) {
        Objects.requireNonNull(request, "Conversion request cannot be null");

        long startTime = System.nanoTime();

        try {
            validator.validate(request);

            // Find a converter that supports the requested formats.
            for (FileConverter converter : converters) {
                if (converter.supports(request.sourceFormat(), request.targetFormat())) {
                    return converter.convert(request);
                }
            }

            throw new IOException("No converter available for the requested formats");
        }
        catch (IOException | IllegalArgumentException e) {
            long duration = (System.nanoTime() - startTime) / 1_000_000;
            long originalSize = 0;

            // Retrieve the original size when possible for failure reporting.
            try {
                if (Files.isRegularFile(request.inputPath())) {
                    originalSize = Files.size(request.inputPath());
                }
            }
            catch (IOException | SecurityException ignored) {
                // File size is optional when reporting a failed conversion.
            }

            return ConversionResult.failure(request, originalSize, duration, e.getMessage());
        }
    }
}
