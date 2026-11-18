package com.fileconverter.core.model;

public enum FileFormat {
    // Current valid file formats
    PNG("png"),
    JPG("jpg"),
    BMP("bmp"),
    WEBP("webp");

    // TODO Add more formats later

    private final String extension;

    FileFormat(String extension) {
        this.extension = extension;
    }

    public String getExtension() {
        return extension;
    }
}
