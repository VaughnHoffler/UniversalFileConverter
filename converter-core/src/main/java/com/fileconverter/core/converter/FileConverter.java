package com.fileconverter.core.converter;

import com.fileconverter.core.model.ConversionRequest;
import com.fileconverter.core.model.ConversionResult;
import com.fileconverter.core.model.FileFormat;

public interface FileConverter {
    // Determines whether this converter supports the requested formats.
    boolean supports(FileFormat sourceFormat, FileFormat targetFormat);

    // Performs a conversion and returns its result.
    ConversionResult convert(ConversionRequest request);
}
