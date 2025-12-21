package com.koishman.telegram.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/telegram")
public class TelegramWebhookController {

    @PostMapping("/webhook")
    @ResponseStatus(HttpStatus.OK)
    public void onUpdate(@RequestBody String rawJson) {
        // For now just prove Telegram can reach us.
        System.out.println("Received update: " + rawJson);
    }
}
