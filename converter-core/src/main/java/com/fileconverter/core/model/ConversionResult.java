package com.fileconverter.core.model;

import java.util.Objects;

public record ConversionResult(
        ConversionRequest request,
        boolean successful,
        long originalSizeBytes,
        long convertedSizeBytes,
        long durationMillis,
        String errorMessage
) {
    public ConversionResult {
        Objects.requireNonNull(request, "Conversion request cannot be null");

        // File sizes and processing time cannot be negative.
        if (originalSizeBytes < 0 || convertedSizeBytes < 0 || durationMillis < 0) {
            throw new IllegalArgumentException("File sizes and duration cannot be negative");
        }

        // Successful conversions should not contain an error message.
        if (successful && errorMessage != null) {
            throw new IllegalArgumentException("Successful conversions cannot have an error message");
        }

        if (!successful && (errorMessage == null || errorMessage.isBlank())) {
            throw new IllegalArgumentException("Failed conversions must have an error message");
        }
    }

    // Creates a result for a successful conversion.
    public static ConversionResult success(ConversionRequest request, long originalSize, long convertedSize, long duration) {
        return new ConversionResult(request, true, originalSize, convertedSize, duration, null);
    }

    // Creates a result for a failed conversion.
    public static ConversionResult failure(ConversionRequest request, long originalSize, long duration, String errorMessage) {
        return new ConversionResult(request, false, originalSize, 0, duration, errorMessage);
    }
}
