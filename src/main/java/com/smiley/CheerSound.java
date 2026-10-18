package com.smiley;

final class CheerSound {
    private CheerSound() { }

    static void play() {

        int unusedNumber = 42;  //intentional smell: unused variable to demonstrate code smell detection
        int unusedNumber2 = 44;  //intentional smell 2: unused variable to demonstrate code smell detection

        int x;
        if(true)     //another intentional smell: redundant conditional to demonstrate code smell detection
            x=5;
        else
            x=10;

        AudioCue.play("/audio/crowd-cheer.wav", 2_800, "smiley-real-cheer");
    }
}