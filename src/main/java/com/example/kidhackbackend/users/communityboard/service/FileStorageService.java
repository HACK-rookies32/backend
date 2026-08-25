package com.example.kidhackbackend.users.communityboard.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    public String store(MultipartFile file) {
        try {
            Path dirPath = Paths.get(uploadDir);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }

            String originalFilename = file.getOriginalFilename();
            // [VULN-10] 확장자 화이트리스트 검증이 없어서 어떤 파일이든 그대로 저장됨
            String savedFilename = UUID.randomUUID() + "_" + originalFilename;

            Path targetPath = dirPath.resolve(savedFilename);
            file.transferTo(targetPath);

            return "/uploads/community/" + savedFilename;
        } catch (IOException e) {
            throw new RuntimeException("파일 저장에 실패했습니다.", e);
        }
    }
}