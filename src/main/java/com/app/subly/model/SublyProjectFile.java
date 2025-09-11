package com.app.subly.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SublyProjectFile {

    private String fileName;
    private SublySettings settings;
    private List<Subtitle> subtitles;
}
