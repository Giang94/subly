package com.app.subly.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.*;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Packs/unpacks a .subly archive (zip) containing:
 * project.json
 * media/<hash>.<ext>
 */
final class ProjectArchiveIO {

    static final String JSON_ENTRY = "project.json";
    static final String MEDIA_DIR = "media/";
    private static final Set<String> IMAGE_EXT = Set.of(".png", ".jpg", ".jpeg", ".gif", ".bmp", ".webp");

    private final ObjectMapper mapper;

    ProjectArchiveIO(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    void save(Object projectModel, Path targetArchive) throws IOException {
        Objects.requireNonNull(projectModel, "projectModel");
        Objects.requireNonNull(targetArchive, "targetArchive");

        Path tmpDir = Files.createTempDirectory("subly-pack-");
        Path mediaDir = tmpDir.resolve(MEDIA_DIR);
        Files.createDirectories(mediaDir);

        JsonNode root = mapper.valueToTree(projectModel);

        // cache originalAbsPath -> relative media/<hash>.<ext>
        Map<String, String> relPathCache = new HashMap<>();
        rewriteImagesForSave(root, mediaDir, relPathCache);

        Path jsonFile = tmpDir.resolve(JSON_ENTRY);
        try (OutputStream out = Files.newOutputStream(jsonFile)) {
            mapper.writerWithDefaultPrettyPrinter().writeValue(out, root);
        }

        zipDirectory(tmpDir, targetArchive);
        deleteRecursive(tmpDir);
    }

    LoadedArchive load(Path archiveFile) throws IOException {
        Objects.requireNonNull(archiveFile, "archiveFile");
        Path extractRoot = Files.createTempDirectory("subly-unpack-");
        boolean success = false;
        try {
            unzip(archiveFile, extractRoot);
            Path json = extractRoot.resolve(JSON_ENTRY);
            if (!Files.isRegularFile(json)) {
                throw new IOException("Missing " + JSON_ENTRY + " in archive: " + archiveFile);
            }
            JsonNode root;
            try (InputStream in = Files.newInputStream(json)) {
                root = mapper.readTree(in);
            }

            rewriteMediaPathsForLoad(root, extractRoot);

            Object project = mapper.treeToValue(root, Object.class);
            success = true;
            return new LoadedArchive(project, extractRoot);
        } finally {
            if (!success) {
                deleteRecursive(extractRoot);
            }
        }
    }

    record LoadedArchive(Object projectModel, Path extractionRoot) {
    }

    /* ---------------- Rewrite helpers ---------------- */

    // Convert absolute image paths to copied hashed relative paths in media/
    private void rewriteImagesForSave(JsonNode node,
                                      Path mediaDir,
                                      Map<String, String> relCache) throws IOException {
        if (node == null) return;
        if (node.isObject()) {
            ObjectNode obj = (ObjectNode) node;
            Iterator<Map.Entry<String, JsonNode>> it = obj.fields();
            List<Map.Entry<String, JsonNode>> fields = new ArrayList<>();
            it.forEachRemaining(fields::add);
            for (Map.Entry<String, JsonNode> e : fields) {
                JsonNode child = e.getValue();
                if (child.isTextual()) {
                    String v = child.textValue();
                    if (shouldProcessImage(v)) {
                        Path src = toExistingPath(v);
                        if (src != null) {
                            String rel = relCache.computeIfAbsent(src.toString(), k -> {
                                try {
                                    return copyHashed(src, mediaDir);
                                } catch (IOException ex) {
                                    throw new UncheckedIOException(ex);
                                }
                            });
                            obj.set(e.getKey(), TextNode.valueOf(rel));
                            continue;
                        }
                    }
                }
                rewriteImagesForSave(child, mediaDir, relCache);
            }
        } else if (node.isArray()) {
            ArrayNode arr = (ArrayNode) node;
            for (int i = 0; i < arr.size(); i++) {
                JsonNode child = arr.get(i);
                if (child.isTextual()) {
                    String v = child.textValue();
                    if (shouldProcessImage(v)) {
                        Path src = toExistingPath(v);
                        if (src != null) {
                            String rel = relCache.computeIfAbsent(src.toString(), k -> {
                                try {
                                    return copyHashed(src, mediaDir);
                                } catch (IOException ex) {
                                    throw new UncheckedIOException(ex);
                                }
                            });
                            arr.set(i, TextNode.valueOf(rel));
                            continue;
                        }
                    }
                }
                rewriteImagesForSave(child, mediaDir, relCache);
            }
        }
    }

    // Convert relative media/ paths to absolute extracted file paths
    private void rewriteMediaPathsForLoad(JsonNode node, Path extractRoot) {
        if (node == null) return;
        if (node.isObject()) {
            ObjectNode obj = (ObjectNode) node;
            Iterator<Map.Entry<String, JsonNode>> it = obj.fields();
            List<Map.Entry<String, JsonNode>> fields = new ArrayList<>();
            it.forEachRemaining(fields::add);
            for (Map.Entry<String, JsonNode> e : fields) {
                JsonNode child = e.getValue();
                if (child.isTextual()) {
                    String v = child.textValue();
                    if (v != null && v.startsWith(MEDIA_DIR)) {
                        Path file = extractRoot.resolve(v).normalize();
                        if (Files.isRegularFile(file)) {
                            obj.set(e.getKey(), TextNode.valueOf(file.toString()));
                            continue;
                        }
                    }
                }
                rewriteMediaPathsForLoad(child, extractRoot);
            }
        } else if (node.isArray()) {
            ArrayNode arr = (ArrayNode) node;
            for (int i = 0; i < arr.size(); i++) {
                JsonNode child = arr.get(i);
                if (child.isTextual()) {
                    String v = child.textValue();
                    if (v != null && v.startsWith(MEDIA_DIR)) {
                        Path file = extractRoot.resolve(v).normalize();
                        if (Files.isRegularFile(file)) {
                            arr.set(i, TextNode.valueOf(file.toString()));
                            continue;
                        }
                    }
                }
                rewriteMediaPathsForLoad(child, extractRoot);
            }
        }
    }

    private boolean shouldProcessImage(String s) {
        if (s == null || s.isBlank()) return false;
        return isLikelyImagePath(s);
    }

    private boolean isLikelyImagePath(String s) {
        String lower = s.toLowerCase(Locale.ROOT);
        for (String ext : IMAGE_EXT) {
            if (lower.endsWith(ext)) return true;
        }
        return false;
    }

    private Path toExistingPath(String s) {
        try {
            Path p;
            if (s.startsWith("file:/")) {
                p = Paths.get(java.net.URI.create(s));
            } else {
                p = Paths.get(s);
            }
            return Files.isRegularFile(p) ? p : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String copyHashed(Path src, Path mediaDir) throws IOException {
        String hash = sha256(src).substring(0, 32);
        String ext = extension(src.getFileName().toString());
        String fileName = hash + ext;
        Path dest = mediaDir.resolve(fileName);
        if (!Files.exists(dest)) {
            Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
        }
        return MEDIA_DIR + fileName;
    }

    private String extension(String name) {
        int i = name.lastIndexOf('.');
        if (i >= 0) {
            String ext = name.substring(i).toLowerCase(Locale.ROOT);
            if (IMAGE_EXT.contains(ext)) return ext;
        }
        return ".img";
    }

    private String sha256(Path file) throws IOException {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            try (InputStream in = Files.newInputStream(file);
                 DigestInputStream dis = new DigestInputStream(in, md)) {
                byte[] buf = new byte[8192];
                while (dis.read(buf) != -1) { /* digest */ }
            }
            byte[] d = md.digest();
            StringBuilder sb = new StringBuilder(d.length * 2);
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IOException("Hash failed: " + file, e);
        }
    }

    private void zipDirectory(Path dir, Path dest) throws IOException {
        if (Files.exists(dest)) Files.delete(dest);
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(dest))) {
            Files.walk(dir).forEach(p -> {
                if (Files.isDirectory(p)) return;
                Path rel = dir.relativize(p);
                try (InputStream in = Files.newInputStream(p)) {
                    ZipEntry entry = new ZipEntry(rel.toString().replace('\\', '/'));
                    zos.putNextEntry(entry);
                    in.transferTo(zos);
                    zos.closeEntry();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (UncheckedIOException u) {
            throw u.getCause();
        }
    }

    private void unzip(Path archive, Path destDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(archive))) {
            ZipEntry e;
            while ((e = zis.getNextEntry()) != null) {
                Path out = destDir.resolve(e.getName()).normalize();
                if (!out.startsWith(destDir))
                    throw new IOException("Illegal entry: " + e.getName());
                if (e.isDirectory()) {
                    Files.createDirectories(out);
                } else {
                    Files.createDirectories(out.getParent());
                    try (OutputStream o = Files.newOutputStream(out)) {
                        zis.transferTo(o);
                    }
                }
            }
        }
    }

    private void deleteRecursive(Path root) throws IOException {
        if (root == null || !Files.exists(root)) return;
        try (var stream = Files.walk(root)) {
            stream.sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (IOException ignored) {
                        }
                    });
        }
    }
}