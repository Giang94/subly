package com.app.subly.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Subtitle {
    private Integer id;
    private String primaryText;
    private String secondaryText;
}
