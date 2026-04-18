package com.example.demo.controller;

import com.example.demo.service.FileService;
import io.minio.StatObjectResponse;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;

@RestController
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @GetMapping("/files/{filename}")
    public ResponseEntity<InputStreamResource> getFile(
            @PathVariable String filename,
            @RequestHeader(value = "Range", required = false) String rangeHeader
    ) throws Exception {

        StatObjectResponse stat = fileService.stat(filename);
        long fileSize = stat.size();

        String contentType = stat.contentType();
        if (contentType == null || contentType.isBlank()) {
            contentType = "video/mp4";
        }

        if (rangeHeader == null || !rangeHeader.startsWith("bytes=")) {
            InputStream stream = fileService.download(filename);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, contentType)
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(fileSize))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .body(new InputStreamResource(stream));
        }

        String rangeValue = rangeHeader.replace("bytes=", "");
        String[] ranges = rangeValue.split("-");

        long start = Long.parseLong(ranges[0]);
        long end = (ranges.length > 1 && !ranges[1].isBlank()) ? Long.parseLong(ranges[1]) : fileSize - 1;

        if (end >= fileSize) {
            end = fileSize - 1;
        }

        long contentLength = end - start + 1;

        InputStream stream = fileService.downloadRange(filename, start, contentLength);

        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(contentLength))
                .header(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + fileSize)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .body(new InputStreamResource(stream));
    }
}