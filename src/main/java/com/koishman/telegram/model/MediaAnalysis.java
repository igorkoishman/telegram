package com.koishman.telegram.model;

import lombok.Data;

import java.util.List;

@Data
public class MediaAnalysis {
    private List<MediaTrack> tracks;
}
