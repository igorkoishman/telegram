package com.koishman.telegram.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class MediaTrack {
    private Integer index;
    private String type;
    private String codec;
    private String lang;

    @JsonProperty("default")
    private Integer defaultTrack;

    private Integer forced;
    private String title;
}
