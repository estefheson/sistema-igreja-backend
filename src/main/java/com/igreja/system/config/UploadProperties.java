package com.igreja.system.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;

@Component
@ConfigurationProperties(prefix = "app.uploads")
public class UploadProperties {

    private String rootDir = "./uploads";
    private String roomsDir = "rooms";
    private String listsDir = "lists";
    private String publicPath = "/uploads";

    public String getRootDir() {
        return rootDir;
    }

    public void setRootDir(String rootDir) {
        this.rootDir = rootDir;
    }

    public String getRoomsDir() {
        return roomsDir;
    }

    public void setRoomsDir(String roomsDir) {
        this.roomsDir = roomsDir;
    }

    public String getPublicPath() {
        return publicPath;
    }

    public void setPublicPath(String publicPath) {
        this.publicPath = publicPath;
    }

    public String getListsDir() {
        return listsDir;
    }

    public void setListsDir(String listsDir) {
        this.listsDir = listsDir;
    }

    public Path getResolvedRootDir() {
        return Paths.get(rootDir).toAbsolutePath().normalize();
    }

    public Path getResolvedRoomsDir() {
        return getResolvedRootDir().resolve(roomsDir).normalize();
    }

    public Path getResolvedListsDir() {
        return getResolvedRootDir().resolve(listsDir).normalize();
    }
}
