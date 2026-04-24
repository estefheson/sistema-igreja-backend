package com.igreja.system.room.service;

import com.igreja.system.common.exception.BusinessException;
import com.igreja.system.config.UploadProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoomPhotoStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".webp", ".gif");

    private final UploadProperties uploadProperties;

    public String store(MultipartFile file) {
        validateFile(file);

        String extension = resolveExtension(file);
        String fileName = UUID.randomUUID() + extension;
        Path targetDirectory = uploadProperties.getResolvedRoomsDir();
        Path targetFile = targetDirectory.resolve(fileName).normalize();

        try {
            Files.createDirectories(targetDirectory);
            Files.copy(file.getInputStream(), targetFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new BusinessException("Nao foi possivel salvar a imagem do ambiente");
        }

        return normalizePublicPath(uploadProperties.getPublicPath()) + "/" + uploadProperties.getRoomsDir() + "/" + fileName;
    }

    public void deleteIfManaged(String imageUrl) {
        if (!isManagedRoomPhotoUrl(imageUrl)) {
            return;
        }

        String relativeFileName = imageUrl.substring(getManagedRoomPhotoPrefix().length());
        Path targetFile = uploadProperties.getResolvedRoomsDir().resolve(relativeFileName).normalize();

        try {
            Files.deleteIfExists(targetFile);
        } catch (IOException ex) {
            throw new BusinessException("Nao foi possivel remover a imagem antiga do ambiente");
        }
    }

    public boolean isManagedRoomPhotoUrl(String imageUrl) {
        return imageUrl != null && imageUrl.startsWith(getManagedRoomPhotoPrefix());
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Arquivo de imagem e obrigatorio");
        }

        String extension = resolveExtension(file);

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException("Formato de imagem nao suportado. Use JPG, PNG, WEBP ou GIF");
        }
    }

    private String resolveExtension(MultipartFile file) {
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "");
        String lowerCaseName = originalFilename.toLowerCase(Locale.ROOT);
        int extensionIndex = lowerCaseName.lastIndexOf('.');

        if (extensionIndex < 0) {
            throw new BusinessException("Arquivo de imagem deve possuir extensao valida");
        }

        return lowerCaseName.substring(extensionIndex);
    }

    private String normalizePublicPath(String publicPath) {
        if (publicPath == null || publicPath.isBlank()) {
            return "/uploads";
        }

        return publicPath.startsWith("/") ? publicPath : "/" + publicPath;
    }

    private String getManagedRoomPhotoPrefix() {
        return normalizePublicPath(uploadProperties.getPublicPath()) + "/" + uploadProperties.getRoomsDir() + "/";
    }
}
