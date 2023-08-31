package com.example.mapbackend.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.logging.Logger;

@RestController
public class FileController {
    @Value("${map.file_save_path}")
    String fileSavePath;

    @GetMapping("/download/{fileName}")
    public ResponseEntity<InputStreamResource> download(@PathVariable String fileName) throws FileNotFoundException {
        Path savingAbsolutePath = Paths.get(fileSavePath).toAbsolutePath().normalize();
        File file = savingAbsolutePath.resolve(fileName).toFile();
        InputStreamResource fileResource = new InputStreamResource(new FileInputStream(file));

        return ResponseEntity.ok()
            .header("Content-Disposition", "attachment;filename*=UTF-8''" + fileName)
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .contentLength(file.length())
            .body(fileResource);
    }
}
