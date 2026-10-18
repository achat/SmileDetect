package com.smiley;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.net.URL;

final class AudioCue {
    private AudioCue() { }

    static void play(String resourceName, int maxDurationMillis, String threadName) {
        Thread playback = new Thread(() -> playClip(resourceName, maxDurationMillis), threadName);
        playback.setDaemon(true);
        playback.start();
    }

    private static void playClip(String resourceName, int maxDurationMillis) {
        URL resource = AudioCue.class.getResource(resourceName);
        if (resource == null) {
            return;
        }

        try (AudioInputStream audio = AudioSystem.getAudioInputStream(resource)) {
            Clip clip = AudioSystem.getClip();
            try {
                clip.open(audio);
                clip.start();
                long clipDurationMillis = clip.getMicrosecondLength() / 1_000;
                Thread.sleep(Math.min(maxDurationMillis, clipDurationMillis));
            } finally {
                if (clip.isOpen()) {
                    clip.stop();
                    clip.close();
                }
            }
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException ignored) {
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}