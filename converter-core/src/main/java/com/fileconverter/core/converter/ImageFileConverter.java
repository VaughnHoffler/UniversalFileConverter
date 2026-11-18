package com.fileconverter.core.converter;

import com.fileconverter.core.model.ConversionRequest;
import com.fileconverter.core.model.ConversionResult;
import com.fileconverter.core.model.FileFormat;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class ImageFileConverter implements FileConverter {
    private static final long MAX_IMAGE_SIZE = 1_000_000_000L;


    @Override
    public boolean supports(FileFormat sourceFormat, FileFormat targetFormat) {
        if (sourceFormat == null || targetFormat == null || sourceFormat == targetFormat) {
            return false;
        }

        // WebP support will be added once we install an image plugin.
        return sourceFormat != FileFormat.WEBP && targetFormat != FileFormat.WEBP;
    }

    @Override
    public ConversionResult convert(ConversionRequest request) {
        long startTime = System.nanoTime();
        long originalSize = 0;
        Path input = request.inputPath();
        Path output = request.outputPath();
        boolean outputCreated = false;

        try {
            if (!supports(request.sourceFormat(), request.targetFormat())) {
                throw new IOException("Unsupported image conversion");
            }

            if (!Files.isRegularFile(input)) {
                throw new IOException("Input file does not exist or is not a regular file");
            }

            originalSize = Files.size(input);

            // Reject oversized images before loading them into memory.
            if (originalSize > MAX_IMAGE_SIZE) {
                throw new IOException("Image exceeds the 1 GB file size limit");
            }

            // Decode the input image.
            BufferedImage image = ImageIO.read(input.toFile());

            if (image == null) {
                throw new IOException("Unable to read the input image");
            }

            if (Files.exists(output)) {
                throw new IOException("Output file already exists");
            }

            // JPG and BMP require an image without transparency.
            if (request.targetFormat() == FileFormat.JPG || request.targetFormat() == FileFormat.BMP) {
                image = removeTransparency(image);
            }

            String format = request.targetFormat().getExtension();

            // CREATE_NEW prevents existing files from being overwritten.
            try (OutputStream stream = Files.newOutputStream(output, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
                outputCreated = true;

                if (!ImageIO.write(image, format, stream)) {
                    throw new IOException("No image writer available for " + format);
                }
            }

            long convertedSize = Files.size(output);
            long duration = (System.nanoTime() - startTime) / 1_000_000;

            return ConversionResult.success(request, originalSize, convertedSize, duration);
        }
        catch (IOException e) {
            // Remove incomplete output files, but never delete pre-existing files.
            if (outputCreated) {
                try {
                    Files.deleteIfExists(output);
                }
                catch (IOException cleanupError) {
                    e.addSuppressed(cleanupError);
                }
            }

            long duration = (System.nanoTime() - startTime) / 1_000_000;
            return ConversionResult.failure(request, originalSize, duration, e.getMessage());
        }
    }

    // Replaces transparent pixels with a white background.
    private BufferedImage removeTransparency(BufferedImage image) {
        BufferedImage converted = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = converted.createGraphics();

        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
            graphics.drawImage(image, 0, 0, null);
        }
        finally {
            graphics.dispose();
        }

        return converted;
    }
}
