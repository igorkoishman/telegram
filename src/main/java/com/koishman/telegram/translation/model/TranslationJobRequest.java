package com.koishman.telegram.translation.model;

import lombok.Data;

import java.util.List;

@Data
public class TranslationJobRequest {
    private List<String> targetLanguages;
    private String whisperModel = "large";
    private String whisperModelType = "faster-whisper";
    private String whisperBackend = "faster-whisper"; // faster-whisper or openai-whisper
    private boolean alignOutput = true; // Enable WhisperX alignment
    private String translationModel = "m2m100"; // m2m100 or nllb
    private String subtitleBurnType = "hard"; // hard, soft, both
    private Boolean useSubtitlesOnly = false;
    private String originalLanguage;
    private Integer audioTrack;
    private Integer subtitleTrack;
    private Boolean align = true;
}
