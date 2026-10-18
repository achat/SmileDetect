package com.smiley;

final class SadSound {
    private SadSound() { }

    static void play() {
        AudioCue.play("/audio/crowd-cry.wav", 2_600, "smiley-real-sad");
    }
}