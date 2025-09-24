package com.app.subly.persistence;

import com.app.subly.model.SublyProjectFile;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class SublyProjectIO {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .enable(SerializationFeature.INDENT_OUTPUT)
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, true)
            .configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, true);

    private SublyProjectIO() {
    }

    public static void save(SublyProjectFile project, Path file) throws IOException {
        if (project == null) throw new IllegalArgumentException("project is null");
        if (file == null) throw new IllegalArgumentException("file is null");
        project.setSchemaVersion(project.getSchemaVersion() == null ? 1 : project.getSchemaVersion());
        project.normalize();
        if (file.getParent() != null) Files.createDirectories(file.getParent());
        MAPPER.writeValue(file.toFile(), project);
    }

    public static SublyProjectFile load(Path file) throws IOException {
        if (file == null || !Files.exists(file)) {
            throw new IOException("Project file not found: " + file);
        }
        SublyProjectFile p = MAPPER.readValue(file.toFile(), SublyProjectFile.class);
        if (p.getSchemaVersion() == null) p.setSchemaVersion(1);
        p.normalize();
        return p;
    }
}