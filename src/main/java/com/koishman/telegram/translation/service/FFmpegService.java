package com.koishman.telegram.translation.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.koishman.telegram.translation.model.MediaTrackInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FFmpegService {

    private final ObjectMapper objectMapper;

    public List<MediaTrackInfo> analyzeMedia(File mediaFile) {
        List<MediaTrackInfo> tracks = new ArrayList<>();

        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "ffprobe",
                    "-v", "quiet",
                    "-print_format", "json",
                    "-show_streams",
                    mediaFile.getAbsolutePath()
            );

            Process process = pb.start();
            StringBuilder output = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("ffprobe failed with exit code: " + exitCode);
            }

            JsonNode root = objectMapper.readTree(output.toString());
            JsonNode streams = root.get("streams");

            int index = 0;
            for (JsonNode stream : streams) {
                MediaTrackInfo track = new MediaTrackInfo();
                track.setIndex(index++);
                track.setType(stream.has("codec_type") ? stream.get("codec_type").asText() : "unknown");
                track.setCodec(stream.has("codec_name") ? stream.get("codec_name").asText() : "unknown");

                JsonNode tags = stream.get("tags");
                if (tags != null && tags.has("language")) {
                    track.setLang(tags.get("language").asText());
                } else {
                    track.setLang("und");
                }

                track.setDefaultTrack(stream.has("disposition") &&
                        stream.get("disposition").has("default") ?
                        stream.get("disposition").get("default").asInt() : 0);
                track.setForced(0);
                track.setTitle(tags != null && tags.has("title") ? tags.get("title").asText() : "");

                tracks.add(track);
            }

            log.info("Analyzed media file: {} tracks found", tracks.size());

        } catch (Exception e) {
            log.error("Failed to analyze media file", e);
            throw new RuntimeException("Media analysis failed", e);
        }

        return tracks;
    }

    public File extractAudio(File videoFile, File outputAudioFile, Integer audioTrackIndex) {
        try {
            List<String> command = new ArrayList<>();
            command.add("ffmpeg");
            command.add("-i");
            command.add(videoFile.getAbsolutePath());
            command.add("-y"); // Overwrite output file

            if (audioTrackIndex != null) {
                command.add("-map");
                command.add("0:a:" + audioTrackIndex);
            }

            command.add("-vn"); // No video
            command.add("-acodec");
            command.add("pcm_s16le"); // WAV format for Whisper
            command.add("-ar");
            command.add("16000"); // 16kHz sample rate
            command.add("-ac");
            command.add("1"); // Mono
            command.add(outputAudioFile.getAbsolutePath());

            executeFFmpeg(command);
            log.info("Audio extracted to: {}", outputAudioFile.getAbsolutePath());

            return outputAudioFile;

        } catch (Exception e) {
            log.error("Failed to extract audio", e);
            throw new RuntimeException("Audio extraction failed", e);
        }
    }

    public File burnSubtitles(File videoFile, File subtitleFile, File outputFile) {
        try {
            List<String> command = new ArrayList<>();
            command.add("ffmpeg");
            command.add("-i");
            command.add(videoFile.getAbsolutePath());
            command.add("-y");
            command.add("-vf");
            command.add("subtitles=" + subtitleFile.getAbsolutePath().replace("\\", "/").replace(":", "\\:"));
            command.add("-c:a");
            command.add("copy");
            command.add(outputFile.getAbsolutePath());

            executeFFmpeg(command);
            log.info("Subtitles burned to: {}", outputFile.getAbsolutePath());

            return outputFile;

        } catch (Exception e) {
            log.error("Failed to burn subtitles", e);
            throw new RuntimeException("Subtitle burning failed", e);
        }
    }

    public File muxSoftSubtitles(File videoFile, List<File> subtitleFiles, List<String> languages, File outputFile) {
        try {
            List<String> command = new ArrayList<>();
            command.add("ffmpeg");
            command.add("-i");
            command.add(videoFile.getAbsolutePath());
            command.add("-y");

            // Add subtitle inputs
            for (File subFile : subtitleFiles) {
                command.add("-i");
                command.add(subFile.getAbsolutePath());
            }

            // Map video and audio from original
            command.add("-map");
            command.add("0:v");
            command.add("-map");
            command.add("0:a");

            // Map all subtitle tracks
            for (int i = 0; i < subtitleFiles.size(); i++) {
                command.add("-map");
                command.add((i + 1) + ":s");
                command.add("-metadata:s:s:" + i);
                command.add("language=" + (i < languages.size() ? languages.get(i) : "und"));
            }

            command.add("-c:v");
            command.add("copy");
            command.add("-c:a");
            command.add("copy");
            command.add("-c:s");
            command.add("srt");
            command.add(outputFile.getAbsolutePath());

            executeFFmpeg(command);
            log.info("Soft subtitles muxed to: {}", outputFile.getAbsolutePath());

            return outputFile;

        } catch (Exception e) {
            log.error("Failed to mux soft subtitles", e);
            throw new RuntimeException("Subtitle muxing failed", e);
        }
    }

    private void executeFFmpeg(List<String> command) throws Exception {
        log.debug("Executing FFmpeg: {}", String.join(" ", command));

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);

        Process process = pb.start();

        // Log output
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.debug("FFmpeg: {}", line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("FFmpeg command failed with exit code: " + exitCode);
        }
    }
}
