package com.koishman.telegram.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class UserSession {
    private Long chatId;
    private SessionState state;
    private String fileId;
    private String fileName;
    private MediaAnalysis mediaAnalysis;

    // User selections
    private String whisperModel = "large";
    private String whisperModelType = "faster-whisper";
    private String whisperBackend = "faster-whisper"; // faster-whisper or openai-whisper
    private boolean alignOutput = true; // Enable subtitle alignment by default
    private List<String> targetLanguages = new ArrayList<>();
    private String translationModel = "m2m100"; // m2m100, nllb, or helsinki
    private String subtitleBurnType = "hard";
    private boolean useExistingSubtitles = false;
    private Integer audioTrackIndex;
    private Integer subtitleTrackIndex;
    private String originalLanguage;

    // Processing
    private String jobId;

    public enum SessionState {
        IDLE,
        FILE_UPLOADED,
        ANALYZING_MEDIA,
        SELECTING_TRANSCRIPTION_OPTIONS,
        SELECTING_TRANSLATION_OPTIONS,
        PROCESSING,
        COMPLETED
    }
}
