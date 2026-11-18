package com.fileconverter.core.model;

import java.nio.file.Path;
import java.util.Objects;

public record ConversionRequest(Path inputPath, Path outputPath, FileFormat sourceFormat, FileFormat targetFormat) {
    public ConversionRequest {
        Objects.requireNonNull(inputPath, "Input path cannot be null");

        Objects.requireNonNull(outputPath, "Output path cannot be null");

        Objects.requireNonNull(sourceFormat, "Source format cannot be null");

        Objects.requireNonNull(targetFormat, "Target format cannot be null");

        if (inputPath.toAbsolutePath().normalize().equals(outputPath.toAbsolutePath().normalize())) {
            throw new IllegalArgumentException("Input and output paths cannot be the same");
        }
    }
}
