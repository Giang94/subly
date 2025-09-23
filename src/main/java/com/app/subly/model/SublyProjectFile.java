package com.app.subly.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SublyProjectFile {

    private String fileName;
    private SublySettings settings;
    private Map<Integer, Chapter> chapters;
}
