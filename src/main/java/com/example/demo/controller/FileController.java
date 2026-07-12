package com.example.demo.controller;

import com.example.demo.service.FileService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRange;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.util.List;
import java.util.Locale;

@RestController
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @RequestMapping(value = "/files/{filename}", method = {RequestMethod.GET, RequestMethod.HEAD})
    public ResponseEntity<InputStreamResource> getFile(
            @PathVariable String filename,
            @RequestHeader(value = "Range", required = false) String rangeHeader,
            HttpMethod method
    ) throws Exception {

        boolean isHeadRequest = HttpMethod.HEAD.equals(method);

        if (!isHeadRequest && isImage(filename) && rangeHeader == null) {
            InputStream stream = fileService.download(filename);
            MediaType mediaType = MediaTypeFactory.getMediaType(filename)
                    .orElse(MediaType.APPLICATION_OCTET_STREAM);

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000, immutable")
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .body(new InputStreamResource(stream));
        }

        FileService.FileMetadata metadata = fileService.metadata(filename);
        long fileSize = metadata.size();

        String contentType = metadata.contentType();
        if (contentType == null || contentType.isBlank()) {
            contentType = MediaTypeFactory.getMediaType(filename)
                    .orElse(MediaType.APPLICATION_OCTET_STREAM)
                    .toString();
        }

        ResponseEntity.BodyBuilder response = ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(fileSize))
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=3600")
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"");

        if (metadata.etag() != null && !metadata.etag().isBlank()) {
            response.header(HttpHeaders.ETAG, quoteEtag(metadata.etag()));
        }

        if (isHeadRequest) {
            return response.build();
        }

        if (rangeHeader == null) {
            InputStream stream = fileService.download(filename);
            return response.body(new InputStreamResource(stream));
        }

        ByteRange byteRange;
        try {
            byteRange = parseRange(rangeHeader, fileSize);
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                    .header(HttpHeaders.CONTENT_RANGE, "bytes */" + fileSize)
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .build();
        }

        InputStream stream = fileService.downloadRange(filename, byteRange.start(), byteRange.length());

        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(byteRange.length()))
                .header(HttpHeaders.CONTENT_RANGE, "bytes " + byteRange.start() + "-" + byteRange.end() + "/" + fileSize)
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=3600")
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .body(new InputStreamResource(stream));
    }

    static ByteRange parseRange(String rangeHeader, long fileSize) {
        if (fileSize <= 0) {
            throw new IllegalArgumentException("Cannot range an empty file");
        }

        List<HttpRange> ranges = HttpRange.parseRanges(rangeHeader);
        if (ranges.size() != 1) {
            throw new IllegalArgumentException("Only one byte range is supported");
        }

        HttpRange range = ranges.get(0);
        long start = range.getRangeStart(fileSize);
        long end = range.getRangeEnd(fileSize);
        if (start < 0 || start >= fileSize || end < start) {
            throw new IllegalArgumentException("Range is outside the file");
        }

        return new ByteRange(start, end);
    }

    private String quoteEtag(String etag) {
        return etag.startsWith("\"") ? etag : "\"" + etag + "\"";
    }

    record ByteRange(long start, long end) {
        long length() {
            return end - start + 1;
        }
    }

    private boolean isImage(String filename) {
        String lowerName = filename.toLowerCase(Locale.ROOT);
        return lowerName.endsWith(".jpg")
                || lowerName.endsWith(".jpeg")
                || lowerName.endsWith(".png")
                || lowerName.endsWith(".webp")
                || lowerName.endsWith(".gif")
                || lowerName.endsWith(".avif");
    }
}
