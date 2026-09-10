package com.example.resumeats.service;

import com.example.resumeats.exception.ResumeParsingException;
import com.example.resumeats.exception.UnsupportedFileTypeException;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Extracts plain text from resume files (PDF or DOCX).
 * Validates file type and size before processing.
 * Temporary files are deleted after parsing.
 */
@Service
@Slf4j
public class ResumeParserService {

    private static final List<String> SUPPORTED_EXTENSIONS = List.of("pdf", "docx");

    @Value("${resume.max-file-size-mb:10}")
    private int maxFileSizeMb;

    /**
     * Parses a resume file and returns extracted text.
     *
     * @param file      the temporary file to parse
     * @param fileName  original file name (used to detect extension)
     * @return extracted plain text
     */
    public String parseResume(File file, String fileName) {
        validateFile(file, fileName);

        String extension = getExtension(fileName);
        log.debug("Parsing resume file: {} (extension: {})", fileName, extension);

        try {
            String text = switch (extension) {
                case "pdf" -> parsePdf(file);
                case "docx" -> parseDocx(file);
                default -> throw new UnsupportedFileTypeException("Unsupported format: " + extension);
            };

            if (text == null || text.isBlank()) {
                throw new ResumeParsingException("The uploaded file appears to be empty or unreadable.");
            }

            log.debug("Successfully extracted {} characters from resume", text.length());
            return text.trim();

        } finally {
            // Always delete temp file after processing
            if (file.exists() && !file.delete()) {
                log.warn("Could not delete temporary file: {}", file.getAbsolutePath());
            }
        }
    }

    /**
     * Validates file extension and size.
     */
    private void validateFile(File file, String fileName) {
        String extension = getExtension(fileName);

        if (!SUPPORTED_EXTENSIONS.contains(extension)) {
            throw new UnsupportedFileTypeException(
                    "Unsupported file type: ." + extension + ". Please upload a PDF or DOCX file."
            );
        }

        long maxBytes = (long) maxFileSizeMb * 1024 * 1024;
        if (file.length() > maxBytes) {
            throw new ResumeParsingException(
                    "File size exceeds " + maxFileSizeMb + "MB limit."
            );
        }
    }

    /**
     * Extracts text from a PDF file using Apache PDFBox.
     */
    private String parsePdf(File file) {
        try (PDDocument document = Loader.loadPDF(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        } catch (IOException e) {
            log.error("PDF parsing failed: {}", e.getMessage());
            throw new ResumeParsingException("Could not read the PDF file. It may be corrupted or password-protected.", e);
        }
    }

    /**
     * Extracts text from a DOCX file using Apache POI.
     */
    private String parseDocx(File file) {
        try (FileInputStream fis = new FileInputStream(file);
             XWPFDocument doc = new XWPFDocument(fis)) {

            List<XWPFParagraph> paragraphs = doc.getParagraphs();
            return paragraphs.stream()
                    .map(XWPFParagraph::getText)
                    .filter(text -> text != null && !text.isBlank())
                    .collect(Collectors.joining("\n"));

        } catch (IOException e) {
            log.error("DOCX parsing failed: {}", e.getMessage());
            throw new ResumeParsingException("Could not read the DOCX file. It may be corrupted.", e);
        }
    }

    /**
     * Returns the lowercase extension of a filename.
     */
    private String getExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    }
}
