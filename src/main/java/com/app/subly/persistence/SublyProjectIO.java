package com.app.subly.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Public static API preserved for existing callers (ProjectFileManager).
 * Now treats *.subly as a zip archive (project.json + media/).
 */
public final class SublyProjectIO {

    private static final ObjectMapper MAPPER;
    private static final ProjectArchiveIO ARCHIVER;
    private static final Map<Path, Path> EXTRACTIONS = new ConcurrentHashMap<>();

    static {
        MAPPER = new ObjectMapper()
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        ARCHIVER = new ProjectArchiveIO(MAPPER);
    }

    private SublyProjectIO() {
    }

    /**
     * Saves project as archive (.subly).
     */
    public static void save(Object projectModel, Path targetFile) throws IOException {
        Objects.requireNonNull(projectModel, "projectModel");
        Objects.requireNonNull(targetFile, "targetFile");
        ensureParent(targetFile);
        ARCHIVER.save(projectModel, targetFile);
    }

    /**
     * Loads project from archive (.subly). If a legacy plain JSON is detected
     * (not a zip), it is read directly (backward compatibility).
     */
    public static <T> T load(Path file) throws IOException {
        return load(file, (Class<T>) Object.class);
    }

    public static <T> T load(Path file, Class<T> type) throws IOException {
        Objects.requireNonNull(file, "file");
        Objects.requireNonNull(type, "type");
        if (!Files.isRegularFile(file)) {
            throw new IOException("File not found: " + file);
        }
        if (isZip(file)) {
            var loaded = ARCHIVER.load(file, type);
            EXTRACTIONS.put(file.toAbsolutePath(), loaded.extractionRoot());
            return loaded.projectModel();
        } else {
            return MAPPER.readValue(Files.readAllBytes(file), type);
        }
    }

    /**
     * Optional: remove extracted temp directory for a loaded archive.
     */
    public static void cleanupExtraction(Path archiveFile) throws IOException {
        Path root = EXTRACTIONS.remove(archiveFile.toAbsolutePath());
        if (root != null && Files.exists(root)) {
            try (var walk = Files.walk(root)) {
                walk.sorted(java.util.Comparator.reverseOrder())
                        .forEach(p -> {
                            try {
                                Files.deleteIfExists(p);
                            } catch (IOException ignored) {
                            }
                        });
            }
        }
    }

    private static void ensureParent(Path target) throws IOException {
        Path parent = target.toAbsolutePath().getParent();
        if (parent != null) Files.createDirectories(parent);
    }

    private static boolean isZip(Path file) {
        try {
            if (Files.size(file) < 4) return false;
            byte[] sig = new byte[4];
            try (var in = Files.newInputStream(file)) {
                if (in.read(sig) != 4) return false;
            }
            // ZIP magic: PK\003\004
            return sig[0] == 'P' && sig[1] == 'K';
        } catch (IOException e) {
            return false;
        }
    }
}