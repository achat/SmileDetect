package com.smiley;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

final class SadSound {
    private static final float SAMPLE_RATE = 22_050;
    private static final AudioFormat FORMAT = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
    private static final double[] NOTES = {440.0, 370.0, 329.63};
    private static final int NOTE_SAMPLES = (int) (SAMPLE_RATE * 0.16);
    private static final int GAP_SAMPLES = (int) (SAMPLE_RATE * 0.025);

    private SadSound() { }

    static void play() {
        Thread playback = new Thread(SadSound::playNotes, "smiley-sad-sound");
        playback.setDaemon(true);
        playback.start();
    }

    private static void playNotes() {
        byte[] audio = createCry();
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, FORMAT);
        try (SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info)) {
            line.open(FORMAT);
            line.start();
            line.write(audio, 0, audio.length);
            line.drain();
        } catch (LineUnavailableException | IllegalArgumentException ignored) {
        }
    }

    private static byte[] createCry() {
        byte[] audio = new byte[(NOTE_SAMPLES + GAP_SAMPLES) * NOTES.length * 2];
        int offset = 0;
        for (double note : NOTES) {
            for (int sampleIndex = 0; sampleIndex < NOTE_SAMPLES; sampleIndex++) {
                double time = sampleIndex / SAMPLE_RATE;
                double wobble = 0.18 * Math.sin(2.0 * Math.PI * 5.5 * time);
                double envelope = Math.min(1.0, Math.min(time / 0.012,
                        (NOTE_SAMPLES - sampleIndex) / (SAMPLE_RATE * 0.04)));
                double wave = Math.sin(2.0 * Math.PI * note * time + wobble);
                short value = (short) (wave * envelope * Short.MAX_VALUE * 0.2);
                audio[offset++] = (byte) value;
                audio[offset++] = (byte) (value >>> 8);
            }
            offset += GAP_SAMPLES * 2;
        }
        return audio;
    }
}