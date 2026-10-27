package com.bookloop.sale;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import net.sourceforge.tess4j.util.LoadLibs;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Runs English-only local OCR on supported images and bounded-size PDFs. */
@Service
public class SaleBillOcrService {

    private final String tessdataPath;
    private final int maxPdfPages;

    public SaleBillOcrService(
            @Value("${bookloop.ocr.data-path:}") String tessdataPath,
            @Value("${bookloop.sale-bills.max-pdf-pages:10}") int maxPdfPages
    ) {
        this.tessdataPath = tessdataPath == null ? "" : tessdataPath.trim();
        this.maxPdfPages = maxPdfPages;
    }

    /** Converts an uploaded image or PDF pages into text using the local Tesseract engine. */
    public String recognizeBillText(byte[] fileData, String contentType) {
        try {
            Tesseract engine = new Tesseract();
            engine.setDatapath(resolveEnglishDataPath());
            engine.setLanguage("eng");
            if ("application/pdf".equals(contentType)) {
                return recognizePdf(engine, fileData);
            }
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(fileData));
            if (image == null) {
                throw new IllegalArgumentException("The uploaded image could not be decoded.");
            }
            return engine.doOCR(image);
        } catch (TesseractException | IOException exception) {
            throw new IllegalStateException("Local bill OCR could not process this file. Check the OCR setup or upload a clearer copy.", exception);
        } catch (LinkageError error) {
            throw new IllegalStateException("The local Tesseract native library could not load. Install the Microsoft Visual C++ v14 Redistributable and restart BookLoop.", error);
        }
    }

    /** Renders each PDF page for OCR after enforcing the configured page limit. */
    private String recognizePdf(Tesseract engine, byte[] fileData) throws IOException, TesseractException {
        StringBuilder text = new StringBuilder();
        try (var document = Loader.loadPDF(fileData)) {
            if (document.getNumberOfPages() > maxPdfPages) {
                throw new IllegalArgumentException("PDF bills may contain at most " + maxPdfPages + " pages.");
            }
            PDFRenderer renderer = new PDFRenderer(document);
            for (int page = 0; page < document.getNumberOfPages(); page++) {
                BufferedImage image = renderer.renderImageWithDPI(page, 250, ImageType.GRAY);
                try {
                    text.append(engine.doOCR(image)).append('\n');
                } finally {
                    image.flush();
                }
            }
        }
        return text.toString();
    }

    /** Uses an explicitly configured language-data folder or extracts Tess4J's bundled English data. */
    private String resolveEnglishDataPath() {
        Path dataDirectory = tessdataPath.isBlank()
                ? LoadLibs.extractTessResources("tessdata").toPath()
                : Path.of(tessdataPath);
        if (!Files.isRegularFile(dataDirectory.resolve("eng.traineddata"))) {
            throw new IllegalStateException("English Tesseract data is unavailable. Set BOOKLOOP_TESSDATA_PATH to a folder containing eng.traineddata.");
        }
        return dataDirectory.toString();
    }
}
