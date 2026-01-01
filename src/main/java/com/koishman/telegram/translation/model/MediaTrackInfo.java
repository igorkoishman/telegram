package com.koishman.telegram.translation.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MediaTrackInfo {
    private Integer index;
    private String type;
    private String codec;
    private String lang;

    @JsonProperty("default")
    private Integer defaultTrack;

    private Integer forced;
    private String title;
}
