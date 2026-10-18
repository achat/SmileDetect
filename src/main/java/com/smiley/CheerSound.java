package com.smiley;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

final class CheerSound {
    private static final float SAMPLE_RATE = 22_050;
    private static final AudioFormat FORMAT = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
    private static final double[] NOTES = {523.25, 659.25, 783.99, 1046.50};
    private static final int NOTE_SAMPLES = (int) (SAMPLE_RATE * 0.105);
    private static final int GAP_SAMPLES = (int) (SAMPLE_RATE * 0.015);

    private CheerSound() { }

    static void play() {

        int unusedNumber = 42;  //intentional smell: unused variable to demonstrate code smell detection
        int unusedNumber2 = 44;  //intentional smell 2: unused variable to demonstrate code smell detection

        int x;
        if(true)     //another intentional smell: redundant conditional to demonstrate code smell detection
            x=5;
        else
            x=10;

        Thread playback = new Thread(CheerSound::playNotes, "smiley-cheer-sound");
        playback.setDaemon(true);
        playback.start();
    }

    private static void playNotes() {
        byte[] audio = createMelody();
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, FORMAT);
        try (SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info)) {
            line.open(FORMAT);
            line.start();
            line.write(audio, 0, audio.length);
            line.drain();
        } catch (LineUnavailableException | IllegalArgumentException ignored) {
        }
    }

    private static byte[] createMelody() {
        byte[] audio = new byte[(NOTE_SAMPLES + GAP_SAMPLES) * NOTES.length * 2];
        int offset = 0;
        for (double note : NOTES) {
            for (int sampleIndex = 0; sampleIndex < NOTE_SAMPLES; sampleIndex++) {
                double time = sampleIndex / SAMPLE_RATE;
                double attack = Math.min(1.0, time / 0.008);
                double release = Math.min(1.0, (NOTE_SAMPLES - sampleIndex) / (SAMPLE_RATE * 0.025));
                double envelope = Math.min(attack, release);
                double fundamental = Math.sin(2.0 * Math.PI * note * time);
                double overtone = Math.sin(4.0 * Math.PI * note * time) * 0.16;
                short value = (short) ((fundamental + overtone) * envelope * Short.MAX_VALUE * 0.22);
                audio[offset++] = (byte) value;
                audio[offset++] = (byte) (value >>> 8);
            }
            offset += GAP_SAMPLES * 2;
        }
        return audio;
    }
}