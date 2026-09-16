package com.shopflow.store;

import com.shopflow.exception.FileStorageException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path storageLocation;
    private final long maxFileSize;
    private final List<String> allowedTypes;

    public FileStorageService(
            @Value("${shopflow.file-storage.location}") String uploadDir,
            @Value("${shopflow.file-storage.max-file-size}") long maxFileSize,
            @Value("${shopflow.file-storage.allowed-types}") String allowedTypes) {
        this.storageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.maxFileSize = maxFileSize;
        this.allowedTypes = List.of(allowedTypes.split(","));
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(storageLocation);
        } catch (IOException e) {
            throw new FileStorageException("Could not create upload directory", e);
        }
    }

    public String storeFile(MultipartFile file, String subdirectory) {
        validateFile(file);

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        // Safe filename: UUID-based to prevent path traversal and collisions
        String safeFilename = UUID.randomUUID() + extension.toLowerCase();

        try {
            Path targetDir = storageLocation.resolve(subdirectory);
            Files.createDirectories(targetDir);

            Path targetLocation = targetDir.resolve(safeFilename);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return "/uploads/" + subdirectory + "/" + safeFilename;
        } catch (IOException e) {
            throw new FileStorageException("Failed to store file", e);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new FileStorageException("Cannot upload empty file");
        }

        if (file.getSize() > maxFileSize) {
            throw new FileStorageException("File size exceeds maximum allowed size of 5MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !allowedTypes.contains(contentType)) {
            throw new FileStorageException(
                    "Invalid file type. Allowed types: " + String.join(", ", allowedTypes));
        }

        // Verify extension matches content type
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null) {
            String lower = originalFilename.toLowerCase();
            boolean validExtension = lower.endsWith(".jpg") || lower.endsWith(".jpeg") ||
                    lower.endsWith(".png") || lower.endsWith(".webp");
            if (!validExtension) {
                throw new FileStorageException("Invalid file extension. Allowed: .jpg, .jpeg, .png, .webp");
            }
        }
    }
}
