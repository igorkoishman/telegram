package com.koishman.telegram.web;

import com.koishman.telegram.service.TelegramBotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/telegram")
@RequiredArgsConstructor
public class TelegramWebhookController {

    private final TelegramBotService botService;

    @PostMapping("/webhook")
    @ResponseStatus(HttpStatus.OK)
    public void onUpdate(@RequestBody String rawJson,
                         @RequestHeader(value = "X-Forwarded-For", required = false) String forwardedFor,
                         @RequestHeader(value = "X-Real-IP", required = false) String realIp) {
        log.info("=== Webhook received ===");
        log.info("X-Forwarded-For: {}", forwardedFor);
        log.info("X-Real-IP: {}", realIp);
        log.info("Update payload: {}", rawJson);
        log.info("=======================");
        botService.processUpdate(rawJson);
    }
}
