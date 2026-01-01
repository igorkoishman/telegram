package com.koishman.telegram.translation.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubtitleSegment {
    private int index;
    private double startTime;
    private double endTime;
    private String text;

    public String toSRT() {
        return String.format("%d\n%s --> %s\n%s\n",
                index,
                formatTime(startTime),
                formatTime(endTime),
                text);
    }

    private String formatTime(double seconds) {
        int hours = (int) (seconds / 3600);
        int minutes = (int) ((seconds % 3600) / 60);
        int secs = (int) (seconds % 60);
        int millis = (int) ((seconds - Math.floor(seconds)) * 1000);
        return String.format("%02d:%02d:%02d,%03d", hours, minutes, secs, millis);
    }
}
