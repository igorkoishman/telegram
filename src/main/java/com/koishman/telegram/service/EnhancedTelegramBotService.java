package com.koishman.telegram.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.koishman.telegram.config.TelegramBotConfig;
import com.koishman.telegram.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnhancedTelegramBotService {

    private final TelegramApiClient telegramApi;
    private final TranslationApiClient translationApi;
    private final SessionManager sessionManager;
    private final TelegramBotConfig config;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final Set<String> completedJobs = Collections.synchronizedSet(new HashSet<>());
    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    // Supported languages
    private static final Map<String, String> LANGUAGES = Map.of(
            "en", "English",
            "es", "Spanish",
            "fr", "French",
            "de", "German",
            "it", "Italian",
            "ru", "Russian",
            "he", "Hebrew",
            "ar", "Arabic"
    );

    // Whisper models
    private static final List<String> WHISPER_MODELS = Arrays.asList("tiny", "base", "small", "medium", "large");

    public void processUpdate(String updateJson) {
        try {
            JsonNode update = objectMapper.readTree(updateJson);

            if (update.has("callback_query")) {
                handleCallbackQuery(update.get("callback_query"));
            } else if (update.has("message")) {
                JsonNode message = update.get("message");
                Long chatId = message.get("chat").get("id").asLong();

                if (message.has("text")) {
                    handleTextMessage(chatId, message);
                } else if (message.has("video") || message.has("document")) {
                    handleMediaMessage(chatId, message);
                }
            }
        } catch (Exception e) {
            log.error("Error processing update: {}", updateJson, e);
        }
    }

    private void handleTextMessage(Long chatId, JsonNode message) {
        String text = message.get("text").asText();
        String firstName = message.get("from").has("first_name")
                ? message.get("from").get("first_name").asText()
                : "there";

        log.info("Received message from {}: {}", firstName, text);

        switch (text.toLowerCase()) {
            case "/start":
                sendWelcomeMessage(chatId, firstName);
                break;
            case "/help":
                sendHelpMessage(chatId);
                break;
            case "/cancel":
                sessionManager.clearSession(chatId);
                telegramApi.sendMessage(chatId, "Session cancelled. Send /start to begin again.");
                break;
            default:
                telegramApi.sendMessage(chatId, "Please send a video file or use /help for available commands.");
                break;
        }
    }

    private void handleMediaMessage(Long chatId, JsonNode message) {
        try {
            String fileId;
            String fileName;

            if (message.has("video")) {
                JsonNode video = message.get("video");
                fileId = video.get("file_id").asText();
                fileName = video.has("file_name") ? video.get("file_name").asText() : "video.mp4";
            } else {
                JsonNode document = message.get("document");
                fileId = document.get("file_id").asText();
                fileName = document.has("file_name") ? document.get("file_name").asText() : "file";
            }

            UserSession session = sessionManager.getOrCreateSession(chatId);
            session.setFileId(fileId);
            session.setFileName(fileName);
            session.setState(UserSession.SessionState.ANALYZING_MEDIA);
            sessionManager.updateSession(chatId, session);

            telegramApi.sendMessage(chatId, "📥 File received: " + fileName + "\n\n⏳ Analyzing media file...");

            // Download and analyze asynchronously
            analyzeMediaAsync(chatId, fileId, fileName);

        } catch (Exception e) {
            log.error("Error handling media message", e);
            telegramApi.sendMessage(chatId, "❌ Error processing file. Please try again.");
        }
    }

    @Async
    private void analyzeMediaAsync(Long chatId, String fileId, String fileName) {
        try {
            // Download file from Telegram
            String downloadPath = config.getTempDownloadDir() + "/" + chatId + "/" + fileName;
            File videoFile = telegramApi.downloadFile(fileId, downloadPath);

            // Analyze media
            MediaAnalysis analysis = translationApi.analyzeMedia(videoFile);

            UserSession session = sessionManager.getSession(chatId);
            session.setMediaAnalysis(analysis);
            session.setState(UserSession.SessionState.FILE_UPLOADED);
            sessionManager.updateSession(chatId, session);

            // Send analysis results
            sendMediaAnalysisResults(chatId, analysis);

        } catch (Exception e) {
            log.error("Error analyzing media", e);
            telegramApi.sendMessage(chatId, "❌ Error analyzing file: " + e.getMessage());
            sessionManager.clearSession(chatId);
        }
    }

    private void sendMediaAnalysisResults(Long chatId, MediaAnalysis analysis) {
        StringBuilder message = new StringBuilder("📊 Media Analysis Results:\n\n");

        List<MediaTrack> audioTracks = analysis.getTracks().stream()
                .filter(t -> "audio".equals(t.getType()))
                .collect(Collectors.toList());

        List<MediaTrack> subtitleTracks = analysis.getTracks().stream()
                .filter(t -> "subtitle".equals(t.getType()))
                .collect(Collectors.toList());

        message.append("🎵 Audio tracks: ").append(audioTracks.size()).append("\n");
        audioTracks.forEach(track -> {
            message.append("  - Track #").append(track.getIndex())
                    .append(" (").append(track.getCodec()).append(")")
                    .append(" Language: ").append(track.getLang())
                    .append("\n");
        });

        message.append("\n📝 Subtitle tracks: ").append(subtitleTracks.size()).append("\n");
        if (!subtitleTracks.isEmpty()) {
            subtitleTracks.forEach(track -> {
                message.append("  - Track #").append(track.getIndex())
                        .append(" Language: ").append(track.getLang())
                        .append("\n");
            });
        } else {
            message.append("  - No existing subtitles found\n");
        }

        message.append("\n🔄 What would you like to do?");

        Map<String, Object> keyboard = createMainOptionsKeyboard(!subtitleTracks.isEmpty());
        telegramApi.sendMessage(chatId, message.toString(), keyboard);
    }

    private Map<String, Object> createMainOptionsKeyboard(boolean hasSubtitles) {
        List<List<Map<String, String>>> buttons = new ArrayList<>();

        if (hasSubtitles) {
            buttons.add(Collections.singletonList(
                    createButton("📝 Use Existing Subtitles", "use_existing_subs")
            ));
        }

        buttons.add(Collections.singletonList(
                createButton("🎙️ Transcribe Audio (New Subtitles)", "transcribe_new")
        ));

        Map<String, Object> keyboard = new HashMap<>();
        keyboard.put("inline_keyboard", buttons);
        return keyboard;
    }

    private void handleCallbackQuery(JsonNode callbackQuery) {
        try {
            String queryId = callbackQuery.get("id").asText();
            String data = callbackQuery.get("data").asText();
            Long chatId = callbackQuery.get("message").get("chat").get("id").asLong();
            Integer messageId = callbackQuery.get("message").get("message_id").asInt();

            log.info("Callback from chatId {}: {}", chatId, data);

            UserSession session = sessionManager.getSession(chatId);
            if (session == null) {
                telegramApi.answerCallbackQuery(queryId, "Session expired. Please send the file again.");
                return;
            }

            telegramApi.answerCallbackQuery(queryId, null);

            if (data.startsWith("use_existing_subs")) {
                handleUseExistingSubtitles(chatId, messageId);
            } else if (data.startsWith("transcribe_new")) {
                handleTranscribeNew(chatId, messageId);
            } else if (data.startsWith("whisper_model:")) {
                handleWhisperModelSelection(chatId, messageId, data);
            } else if (data.startsWith("whisper_backend:")) {
                handleWhisperBackendSelection(chatId, messageId, data);
            } else if (data.startsWith("align_output:")) {
                handleAlignOutputSelection(chatId, messageId, data);
            } else if (data.startsWith("source_lang:")) {
                handleSourceLanguageSelection(chatId, messageId, data);
            } else if (data.startsWith("select_lang:")) {
                handleLanguageSelection(chatId, messageId, data);
            } else if (data.startsWith("done_langs")) {
                handleLanguagesDone(chatId, messageId);
            } else if (data.startsWith("translation_model:")) {
                handleTranslationModelSelection(chatId, messageId, data);
            } else if (data.startsWith("burn_type:")) {
                handleBurnTypeSelection(chatId, messageId, data);
            } else if (data.startsWith("start_processing")) {
                handleStartProcessing(chatId, messageId);
            }

        } catch (Exception e) {
            log.error("Error handling callback query", e);
        }
    }

    private void handleUseExistingSubtitles(Long chatId, Integer messageId) {
        UserSession session = sessionManager.getSession(chatId);
        session.setUseExistingSubtitles(true);
        session.setState(UserSession.SessionState.SELECTING_TRANSLATION_OPTIONS);
        sessionManager.updateSession(chatId, session);

        showLanguageSelection(chatId, messageId);
    }

    private void handleTranscribeNew(Long chatId, Integer messageId) {
        UserSession session = sessionManager.getSession(chatId);
        session.setUseExistingSubtitles(false);
        session.setState(UserSession.SessionState.SELECTING_TRANSCRIPTION_OPTIONS);
        sessionManager.updateSession(chatId, session);

        showWhisperModelSelection(chatId, messageId);
    }

    private void showWhisperModelSelection(Long chatId, Integer messageId) {
        String text = "🎙️ Select Whisper Model:\n\n" +
                "Larger models are more accurate but take longer to process.\n\n" +
                "• tiny - Fastest, least accurate\n" +
                "• base - Fast\n" +
                "• small - Balanced\n" +
                "• medium - Good accuracy\n" +
                "• large - Best accuracy (recommended)";

        List<List<Map<String, String>>> buttons = new ArrayList<>();
        for (String model : WHISPER_MODELS) {
            buttons.add(Collections.singletonList(
                    createButton(model.toUpperCase(), "whisper_model:" + model)
            ));
        }

        Map<String, Object> keyboard = new HashMap<>();
        keyboard.put("inline_keyboard", buttons);

        telegramApi.editMessageText(chatId, messageId, text, keyboard);
    }

    private void handleWhisperModelSelection(Long chatId, Integer messageId, String data) {
        String model = data.split(":")[1];
        UserSession session = sessionManager.getSession(chatId);
        session.setWhisperModel(model);
        sessionManager.updateSession(chatId, session);

        showWhisperBackendSelection(chatId, messageId);
    }

    private void showWhisperBackendSelection(Long chatId, Integer messageId) {
        String text = "🔧 Select Transcription Backend:\n\n" +
                "• Faster-Whisper - Optimized for speed (recommended)\n" +
                "• OpenAI-Whisper - Original implementation\n\n" +
                "Both support high-quality transcription.";

        List<List<Map<String, String>>> buttons = Arrays.asList(
                Collections.singletonList(createButton("⚡ Faster-Whisper", "whisper_backend:faster-whisper")),
                Collections.singletonList(createButton("🔬 OpenAI-Whisper", "whisper_backend:openai-whisper"))
        );

        Map<String, Object> keyboard = new HashMap<>();
        keyboard.put("inline_keyboard", buttons);

        telegramApi.editMessageText(chatId, messageId, text, keyboard);
    }

    private void handleWhisperBackendSelection(Long chatId, Integer messageId, String data) {
        String backend = data.split(":")[1];
        UserSession session = sessionManager.getSession(chatId);
        session.setWhisperBackend(backend);
        sessionManager.updateSession(chatId, session);

        showAlignOutputSelection(chatId, messageId);
    }

    private void showAlignOutputSelection(Long chatId, Integer messageId) {
        String text = "🎯 Enable Subtitle Alignment?\n\n" +
                "Alignment improves subtitle timing accuracy using WhisperX.\n\n" +
                "• Enabled - Better timing (recommended)\n" +
                "• Disabled - Faster processing";

        List<List<Map<String, String>>> buttons = Arrays.asList(
                Collections.singletonList(createButton("✅ Enable Alignment", "align_output:true")),
                Collections.singletonList(createButton("⏩ Disable Alignment", "align_output:false"))
        );

        Map<String, Object> keyboard = new HashMap<>();
        keyboard.put("inline_keyboard", buttons);

        telegramApi.editMessageText(chatId, messageId, text, keyboard);
    }

    private void handleAlignOutputSelection(Long chatId, Integer messageId, String data) {
        boolean alignOutput = data.split(":")[1].equals("true");
        UserSession session = sessionManager.getSession(chatId);
        session.setAlignOutput(alignOutput);
        sessionManager.updateSession(chatId, session);

        showSourceLanguageSelection(chatId, messageId);
    }

    private void showSourceLanguageSelection(Long chatId, Integer messageId) {
        String text = "🎤 Source Language (Optional):\n\n" +
                "If you know the language of the video, select it to improve transcription accuracy and speed.\n\n" +
                "Or skip to auto-detect.";

        List<List<Map<String, String>>> buttons = new ArrayList<>();

        // Auto-detect button
        buttons.add(Collections.singletonList(
                createButton("🔍 Auto-Detect", "source_lang:auto")
        ));

        // Language buttons in rows of 2
        List<Map.Entry<String, String>> langList = new ArrayList<>(LANGUAGES.entrySet());
        for (int i = 0; i < langList.size(); i += 2) {
            List<Map<String, String>> row = new ArrayList<>();
            Map.Entry<String, String> lang1 = langList.get(i);
            row.add(createButton(lang1.getValue(), "source_lang:" + lang1.getKey()));
            if (i + 1 < langList.size()) {
                Map.Entry<String, String> lang2 = langList.get(i + 1);
                row.add(createButton(lang2.getValue(), "source_lang:" + lang2.getKey()));
            }
            buttons.add(row);
        }

        Map<String, Object> keyboard = new HashMap<>();
        keyboard.put("inline_keyboard", buttons);

        telegramApi.editMessageText(chatId, messageId, text, keyboard);
    }

    private void handleSourceLanguageSelection(Long chatId, Integer messageId, String data) {
        String sourceLang = data.split(":")[1];
        UserSession session = sessionManager.getSession(chatId);

        // If auto-detect, set to null, otherwise set the language code
        if ("auto".equals(sourceLang)) {
            session.setOriginalLanguage(null);
        } else {
            session.setOriginalLanguage(sourceLang);
        }
        sessionManager.updateSession(chatId, session);

        showLanguageSelection(chatId, messageId);
    }

    private void showLanguageSelection(Long chatId, Integer messageId) {
        UserSession session = sessionManager.getSession(chatId);

        StringBuilder text = new StringBuilder("🌍 Select Target Languages:\n\n");
        text.append("Choose one or more languages for translation.\n\n");
        text.append("Selected: ");
        if (session.getTargetLanguages().isEmpty()) {
            text.append("None");
        } else {
            text.append(session.getTargetLanguages().stream()
                    .map(LANGUAGES::get)
                    .collect(Collectors.joining(", ")));
        }

        List<List<Map<String, String>>> buttons = new ArrayList<>();

        // Language buttons in rows of 2
        List<Map.Entry<String, String>> langList = new ArrayList<>(LANGUAGES.entrySet());
        for (int i = 0; i < langList.size(); i += 2) {
            List<Map<String, String>> row = new ArrayList<>();
            row.add(createLanguageButton(langList.get(i), session));
            if (i + 1 < langList.size()) {
                row.add(createLanguageButton(langList.get(i + 1), session));
            }
            buttons.add(row);
        }

        // Done button
        if (!session.getTargetLanguages().isEmpty()) {
            buttons.add(Collections.singletonList(
                    createButton("✅ Done", "done_langs")
            ));
        }

        Map<String, Object> keyboard = new HashMap<>();
        keyboard.put("inline_keyboard", buttons);

        telegramApi.editMessageText(chatId, messageId, text.toString(), keyboard);
    }

    private Map<String, String> createLanguageButton(Map.Entry<String, String> lang, UserSession session) {
        boolean selected = session.getTargetLanguages().contains(lang.getKey());
        String label = (selected ? "✓ " : "") + lang.getValue();
        return createButton(label, "select_lang:" + lang.getKey());
    }

    private void handleLanguageSelection(Long chatId, Integer messageId, String data) {
        String langCode = data.split(":")[1];
        UserSession session = sessionManager.getSession(chatId);

        if (session.getTargetLanguages().contains(langCode)) {
            session.getTargetLanguages().remove(langCode);
        } else {
            session.getTargetLanguages().add(langCode);
        }

        sessionManager.updateSession(chatId, session);
        showLanguageSelection(chatId, messageId);
    }

    private void handleLanguagesDone(Long chatId, Integer messageId) {
        showTranslationModelSelection(chatId, messageId);
    }

    private void showTranslationModelSelection(Long chatId, Integer messageId) {
        String text = "🔤 Select Translation Model:\n\n" +
                "• M2M100 - Balanced speed and quality (recommended)\n" +
                "• NLLB - Better quality for many languages\n\n" +
                "Both support all selected languages.";

        List<List<Map<String, String>>> buttons = Arrays.asList(
                Collections.singletonList(createButton("⚡ M2M100", "translation_model:m2m100")),
                Collections.singletonList(createButton("🎯 NLLB", "translation_model:nllb"))
        );

        Map<String, Object> keyboard = new HashMap<>();
        keyboard.put("inline_keyboard", buttons);

        telegramApi.editMessageText(chatId, messageId, text, keyboard);
    }

    private void handleTranslationModelSelection(Long chatId, Integer messageId, String data) {
        String model = data.split(":")[1];
        UserSession session = sessionManager.getSession(chatId);
        session.setTranslationModel(model);
        sessionManager.updateSession(chatId, session);

        showBurnTypeSelection(chatId, messageId);
    }

    private void showBurnTypeSelection(Long chatId, Integer messageId) {
        String text = "🎬 Select Subtitle Type:\n\n" +
                "• Hard - Subtitles burned into video (compatible with all players)\n" +
                "• Soft - Subtitles as separate tracks (can be toggled on/off)\n" +
                "• Both - Generate both versions";

        List<List<Map<String, String>>> buttons = Arrays.asList(
                Collections.singletonList(createButton("Hard Burn", "burn_type:hard")),
                Collections.singletonList(createButton("Soft Subtitles", "burn_type:soft")),
                Collections.singletonList(createButton("Both", "burn_type:both"))
        );

        Map<String, Object> keyboard = new HashMap<>();
        keyboard.put("inline_keyboard", buttons);

        telegramApi.editMessageText(chatId, messageId, text, keyboard);
    }

    private void handleBurnTypeSelection(Long chatId, Integer messageId, String data) {
        String burnType = data.split(":")[1];
        UserSession session = sessionManager.getSession(chatId);
        session.setSubtitleBurnType(burnType);
        sessionManager.updateSession(chatId, session);

        showProcessingSummary(chatId, messageId);
    }

    private void showProcessingSummary(Long chatId, Integer messageId) {
        UserSession session = sessionManager.getSession(chatId);

        StringBuilder text = new StringBuilder("📋 Processing Summary:\n\n");
        text.append("📁 File: ").append(session.getFileName()).append("\n\n");

        if (session.isUseExistingSubtitles()) {
            text.append("📝 Mode: Use existing subtitles\n");
        } else {
            text.append("🎙️ Transcription:\n");
            text.append("  • Model: ").append(session.getWhisperModel().toUpperCase()).append("\n");
            text.append("  • Backend: ").append(session.getWhisperBackend() != null ?
                    session.getWhisperBackend() : "faster-whisper").append("\n");
            text.append("  • Alignment: ").append(session.isAlignOutput() ? "Enabled" : "Disabled").append("\n");
            if (session.getOriginalLanguage() != null) {
                text.append("  • Source language: ").append(LANGUAGES.get(session.getOriginalLanguage())).append("\n");
            } else {
                text.append("  • Source language: Auto-detect\n");
            }
        }

        text.append("\n🔤 Translation:\n");
        text.append("  • Model: ").append(session.getTranslationModel() != null ?
                session.getTranslationModel().toUpperCase() : "M2M100").append("\n");
        text.append("  • Target languages: ").append(
                session.getTargetLanguages().stream()
                        .map(LANGUAGES::get)
                        .collect(Collectors.joining(", "))
        ).append("\n");

        text.append("\n🎬 Subtitle type: ").append(session.getSubtitleBurnType().toUpperCase()).append("\n\n");
        text.append("Ready to process?");

        List<List<Map<String, String>>> buttons = Arrays.asList(
                Collections.singletonList(createButton("▶️ Start Processing", "start_processing")),
                Collections.singletonList(createButton("❌ Cancel", "cancel"))
        );

        Map<String, Object> keyboard = new HashMap<>();
        keyboard.put("inline_keyboard", buttons);

        telegramApi.editMessageText(chatId, messageId, text.toString(), keyboard);
    }

    private void handleStartProcessing(Long chatId, Integer messageId) {
        UserSession session = sessionManager.getSession(chatId);
        session.setState(UserSession.SessionState.PROCESSING);
        sessionManager.updateSession(chatId, session);

        telegramApi.editMessageText(chatId, messageId, "⏳ Processing started...\nThis may take several minutes.", null);

        startProcessingAsync(chatId);
    }

    @Async
    private void startProcessingAsync(Long chatId) {
        try {
            UserSession session = sessionManager.getSession(chatId);

            // Get downloaded file
            String downloadPath = config.getTempDownloadDir() + "/" + chatId + "/" + session.getFileName();
            File videoFile = new File(downloadPath);

            // Upload to translation service
            String targetLangs = String.join(" ", session.getTargetLanguages());

            JobResponse jobResponse = translationApi.uploadVideo(
                    videoFile,
                    targetLangs,
                    session.getWhisperModel(),
                    session.getWhisperModelType(),
                    session.getWhisperBackend(),
                    session.isAlignOutput(),
                    session.getTranslationModel(),
                    session.getSubtitleBurnType(),
                    session.isUseExistingSubtitles(),
                    session.getOriginalLanguage(),
                    session.getAudioTrackIndex(),
                    session.getSubtitleTrackIndex(),
                    chatId
            );

            session.setJobId(jobResponse.getJobId());
            sessionManager.updateSession(chatId, session);

            telegramApi.sendMessage(chatId, "✅ Job submitted successfully!\nJob ID: " + jobResponse.getJobId() +
                    "\n\n⏳ Processing... I'll notify you when it's done.");

            // Start polling for status
            pollJobStatus(chatId, jobResponse.getJobId());

        } catch (Exception e) {
            log.error("Error starting processing", e);
            telegramApi.sendMessage(chatId, "❌ Error starting processing: " + e.getMessage());
            sessionManager.clearSession(chatId);
        }
    }

    private void pollJobStatus(Long chatId, String jobId) {
        log.info("Starting job status polling for job {} (chatId: {})", jobId, chatId);

        ScheduledFuture<?> task = scheduler.scheduleAtFixedRate(() -> {
            try {
                // Check if job was already completed
                if (completedJobs.contains(jobId)) {
                    log.info("Job {} already completed, canceling polling task", jobId);
                    cancelPollingTask(jobId);
                    return;
                }

                log.info("Polling status for job {}...", jobId);
                JobStatus status = translationApi.getJobStatus(jobId);
                log.info("Job {} status: {}", jobId, status.getStatus());

                if ("done".equals(status.getStatus())) {
                    // Mark as completed first to prevent duplicate processing
                    if (!completedJobs.add(jobId)) {
                        log.info("Job {} already being processed by another thread", jobId);
                        cancelPollingTask(jobId);
                        return;
                    }

                    log.info("Job {} completed! Sending files to chatId {}", jobId, chatId);
                    handleJobCompleted(chatId, status);
                    log.info("Job {} files sent successfully", jobId);

                    // Cancel the polling task
                    cancelPollingTask(jobId);
                } else if ("failed".equals(status.getStatus())) {
                    log.error("Job {} failed: {}", jobId, status.getError());
                    telegramApi.sendMessage(chatId, "❌ Processing failed: " + status.getError());
                    cancelPollingTask(jobId);
                } else {
                    log.info("Job {} still processing (status: {})...", jobId, status.getStatus());
                }
            } catch (Exception e) {
                log.error("Error polling job status for job {}: {}", jobId, e.getMessage(), e);
                telegramApi.sendMessage(chatId, "❌ Error checking job status: " + e.getMessage());
                // Don't cancel on transient errors, keep retrying
            }
        }, 0, 10, TimeUnit.SECONDS);

        // Store the task so we can cancel it later
        scheduledTasks.put(jobId, task);
        log.info("Polling task scheduled for job {} (polling every 10 seconds)", jobId);
    }

    private void cancelPollingTask(String jobId) {
        ScheduledFuture<?> task = scheduledTasks.remove(jobId);
        if (task != null) {
            task.cancel(false);
            log.info("Polling task canceled for job {}", jobId);
        }
    }

    private void handleJobCompleted(Long chatId, JobStatus status) {
        try {
            telegramApi.sendMessage(chatId, "✅ Processing completed in " + status.getDurationSeconds() + " seconds!");

            // Download and send results
            Map<String, String> outputs = status.getOutputs();

            for (Map.Entry<String, String> output : outputs.entrySet()) {
                String key = output.getKey();
                String filename = output.getValue();

                if (filename.endsWith(".mp4") || filename.endsWith(".mkv")) {
                    String downloadPath = config.getTempDownloadDir() + "/" + chatId + "/" + filename;
                    File resultFile = translationApi.downloadFile(filename, downloadPath);

                    String caption = "📹 " + key.toUpperCase() + " version";
                    if (resultFile.length() < 50 * 1024 * 1024) { // 50MB limit
                        telegramApi.sendVideo(chatId, resultFile, caption);
                    } else {
                        telegramApi.sendDocument(chatId, resultFile, caption + " (sent as document due to size)");
                    }

                    resultFile.delete();
                } else if (filename.endsWith(".srt")) {
                    String downloadPath = config.getTempDownloadDir() + "/" + chatId + "/" + filename;
                    File srtFile = translationApi.downloadFile(filename, downloadPath);
                    telegramApi.sendDocument(chatId, srtFile, "📝 " + key.toUpperCase() + " subtitles");
                    srtFile.delete();
                }
            }

            telegramApi.sendMessage(chatId, "✨ All files sent! Send another video to process.");
            sessionManager.clearSession(chatId);

        } catch (Exception e) {
            log.error("Error handling completed job", e);
            telegramApi.sendMessage(chatId, "❌ Error downloading results: " + e.getMessage());
        }
    }

    private Map<String, String> createButton(String text, String callbackData) {
        Map<String, String> button = new HashMap<>();
        button.put("text", text);
        button.put("callback_data", callbackData);
        return button;
    }

    private void sendWelcomeMessage(Long chatId, String firstName) {
        String message = String.format(
                "👋 Hello %s!\n\n" +
                        "I'm a video subtitle translation bot powered by AI.\n\n" +
                        "🎬 Send me a video file and I can:\n" +
                        "• Generate subtitles using Whisper AI\n" +
                        "• Translate to multiple languages\n" +
                        "• Burn subtitles into video or add as separate tracks\n\n" +
                        "Just send me a video to get started!",
                firstName
        );
        telegramApi.sendMessage(chatId, message);
    }

    private void sendHelpMessage(Long chatId) {
        String message = "📖 Help & Commands:\n\n" +
                "/start - Start the bot\n" +
                "/help - Show this message\n" +
                "/cancel - Cancel current session\n\n" +
                "🎥 How to use:\n" +
                "1. Send a video file\n" +
                "2. I'll analyze it and show available options\n" +
                "3. Choose transcription model and target languages\n" +
                "4. Select subtitle type (hard/soft/both)\n" +
                "5. Wait for processing\n" +
                "6. Download your translated videos!\n\n" +
                "Supported languages:\n" +
                LANGUAGES.values().stream().collect(Collectors.joining(", "));

        telegramApi.sendMessage(chatId, message);
    }
}
