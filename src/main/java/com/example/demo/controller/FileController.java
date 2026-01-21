package com.example.demo.controller;

import com.example.demo.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    // ✅ Upload: POST /files/upload  (form-data: file)
    @PostMapping("/upload")
    public String upload(@RequestParam MultipartFile file) throws Exception {
        return fileService.upload(file);
    }

    // ✅ Download: GET /files/{fileName}
    @GetMapping("/{fileName}")
    public ResponseEntity<byte[]> download(@PathVariable String fileName) throws Exception {
        try (InputStream is = fileService.download(fileName)) {
            byte[] bytes = is.readAllBytes();

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(bytes);
        }
    }
}
