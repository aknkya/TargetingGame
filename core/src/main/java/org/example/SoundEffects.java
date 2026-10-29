package org.example;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Procedural Audio Synthesizer for Sudoku Master.
 * Generates high-fidelity 44.1kHz 16-bit sound effects in memory.
 */
public class SoundEffects {

    private Sound placeSound;
    private Sound pencilSound;
    private Sound eraseSound;
    private Sound errorSound;
    private Sound victorySound;
    private Sound clickSound;
    private Sound hoverSound;
    private boolean soundEnabled = true;

    public void init() {
        try {
            FileHandle dir = Gdx.files.local("sounds");
            if (!dir.exists()) {
                dir.mkdirs();
            }

            FileHandle placeFile = Gdx.files.local("sounds/place.wav");
            FileHandle pencilFile = Gdx.files.local("sounds/pencil.wav");
            FileHandle eraseFile = Gdx.files.local("sounds/erase.wav");
            FileHandle errorFile = Gdx.files.local("sounds/error.wav");
            FileHandle victoryFile = Gdx.files.local("sounds/victory.wav");
            FileHandle clickFile = Gdx.files.local("sounds/click.wav");
            FileHandle hoverFile = Gdx.files.local("sounds/hover.wav");

            placeFile.writeBytes(synthesizePlaceWav(), false);
            pencilFile.writeBytes(synthesizePencilWav(), false);
            eraseFile.writeBytes(synthesizeEraseWav(), false);
            errorFile.writeBytes(synthesizeErrorWav(), false);
            victoryFile.writeBytes(synthesizeVictoryWav(), false);
            clickFile.writeBytes(synthesizeClickWav(), false);
            hoverFile.writeBytes(synthesizeHoverWav(), false);

            placeSound = Gdx.audio.newSound(placeFile);
            pencilSound = Gdx.audio.newSound(pencilFile);
            eraseSound = Gdx.audio.newSound(eraseFile);
            errorSound = Gdx.audio.newSound(errorFile);
            victorySound = Gdx.audio.newSound(victoryFile);
            clickSound = Gdx.audio.newSound(clickFile);
            hoverSound = Gdx.audio.newSound(hoverFile);
        } catch (Exception e) {
            Gdx.app.error("SoundEffects", "Error synthesizing audio effects", e);
        }
    }

    public boolean isSoundEnabled() { return soundEnabled; }
    public void setSoundEnabled(boolean enabled) { this.soundEnabled = enabled; }
    public void toggleSound() { this.soundEnabled = !this.soundEnabled; }

    public void playPlace() {
        if (!soundEnabled || placeSound == null) return;
        placeSound.play(0.85f, 1.0f, 0f);
    }

    public void playPencil() {
        if (!soundEnabled || pencilSound == null) return;
        pencilSound.play(0.55f, 1.15f, 0f);
    }

    public void playErase() {
        if (!soundEnabled || eraseSound == null) return;
        eraseSound.play(0.6f, 0.95f, 0f);
    }

    public void playError() {
        if (!soundEnabled || errorSound == null) return;
        errorSound.play(0.75f, 1.0f, 0f);
    }

    public void playVictory() {
        if (!soundEnabled || victorySound == null) return;
        victorySound.play(1.0f, 1.0f, 0f);
    }

    public void playClick() {
        if (!soundEnabled || clickSound == null) return;
        clickSound.play(0.7f, 1.0f, 0f);
    }

    public void playHover() {
        if (!soundEnabled || hoverSound == null) return;
        hoverSound.play(0.35f, 1.0f, 0f);
    }

    public void dispose() {
        if (placeSound != null) placeSound.dispose();
        if (pencilSound != null) pencilSound.dispose();
        if (eraseSound != null) eraseSound.dispose();
        if (errorSound != null) errorSound.dispose();
        if (victorySound != null) victorySound.dispose();
        if (clickSound != null) clickSound.dispose();
        if (hoverSound != null) hoverSound.dispose();
    }

    // --- Procedural Audio Synthesizers ---

    private static byte[] buildWav(short[] pcmSamples, int sampleRate) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        int totalAudioLen = pcmSamples.length * 2;
        int totalDataLen = totalAudioLen + 36;

        dos.writeBytes("RIFF");
        dos.writeInt(Integer.reverseBytes(totalDataLen));
        dos.writeBytes("WAVE");
        dos.writeBytes("fmt ");
        dos.writeInt(Integer.reverseBytes(16));
        dos.writeShort(Short.reverseBytes((short) 1));
        dos.writeShort(Short.reverseBytes((short) 1));
        dos.writeInt(Integer.reverseBytes(sampleRate));
        dos.writeInt(Integer.reverseBytes(sampleRate * 2));
        dos.writeShort(Short.reverseBytes((short) 2));
        dos.writeShort(Short.reverseBytes((short) 16));
        dos.writeBytes("data");
        dos.writeInt(Integer.reverseBytes(totalAudioLen));

        ByteBuffer buffer = ByteBuffer.allocate(pcmSamples.length * 2).order(ByteOrder.LITTLE_ENDIAN);
        for (short sample : pcmSamples) {
            buffer.putShort(sample);
        }
        dos.write(buffer.array());
        dos.flush();
        return baos.toByteArray();
    }

    /**
     * Tactile number placement pop (pleasant clean marimba chime).
     */
    private static byte[] synthesizePlaceWav() throws IOException {
        int sampleRate = 44100;
        float duration = 0.12f;
        int totalSamples = (int) (sampleRate * duration);
        short[] samples = new short[totalSamples];

        for (int i = 0; i < totalSamples; i++) {
            float t = (float) i / sampleRate;
            float freq1 = 523.25f; // C5
            float freq2 = 1046.5f; // C6 overtone
            float env = (float) Math.exp(-t * 32.0);
            float wave = (float) (Math.sin(2.0 * Math.PI * freq1 * t) * 0.7 +
                                  Math.sin(2.0 * Math.PI * freq2 * t) * 0.3);
            samples[i] = (short) (wave * env * 24000);
        }
        return buildWav(samples, sampleRate);
    }

    /**
     * Subtle, high-frequency pencil note tick.
     */
    private static byte[] synthesizePencilWav() throws IOException {
        int sampleRate = 44100;
        float duration = 0.04f;
        int totalSamples = (int) (sampleRate * duration);
        short[] samples = new short[totalSamples];

        for (int i = 0; i < totalSamples; i++) {
            float t = (float) i / sampleRate;
            float freq = 1480f - (t / duration) * 400f;
            float env = (float) Math.exp(-t * 95.0);
            float wave = (float) Math.sin(2.0 * Math.PI * freq * t);
            samples[i] = (short) (wave * env * 18000);
        }
        return buildWav(samples, sampleRate);
    }

    /**
     * Subtle whoosh for erase.
     */
    private static byte[] synthesizeEraseWav() throws IOException {
        int sampleRate = 44100;
        float duration = 0.10f;
        int totalSamples = (int) (sampleRate * duration);
        short[] samples = new short[totalSamples];

        for (int i = 0; i < totalSamples; i++) {
            float t = (float) i / sampleRate;
            float noise = (float) (Math.random() * 2.0 - 1.0);
            float env = (float) (Math.sin(Math.PI * (t / duration)) * Math.exp(-t * 15.0));
            samples[i] = (short) (noise * env * 14000);
        }
        return buildWav(samples, sampleRate);
    }

    /**
     * Soft warning buzz for sudoku conflict/mistake.
     */
    private static byte[] synthesizeErrorWav() throws IOException {
        int sampleRate = 44100;
        float duration = 0.22f;
        int totalSamples = (int) (sampleRate * duration);
        short[] samples = new short[totalSamples];

        for (int i = 0; i < totalSamples; i++) {
            float t = (float) i / sampleRate;
            float freq = 150f;
            float env = (float) Math.exp(-t * 12.0);
            // Square-ish wave for buzz
            float wave = Math.sin(2.0 * Math.PI * freq * t) > 0 ? 0.6f : -0.6f;
            samples[i] = (short) (wave * env * 18000);
        }
        return buildWav(samples, sampleRate);
    }

    /**
     * Arpeggio victory celebration fanfare.
     */
    private static byte[] synthesizeVictoryWav() throws IOException {
        int sampleRate = 44100;
        float duration = 0.65f;
        int totalSamples = (int) (sampleRate * duration);
        short[] samples = new short[totalSamples];

        float[] freqs = { 523.25f, 659.25f, 783.99f, 1046.50f }; // C5, E5, G5, C6
        float noteDur = 0.14f;

        for (int i = 0; i < totalSamples; i++) {
            float t = (float) i / sampleRate;
            int noteIdx = Math.min(freqs.length - 1, (int) (t / noteDur));
            float noteT = t - (noteIdx * noteDur);
            float env = (float) Math.exp(-noteT * 10.0);
            float wave = (float) Math.sin(2.0 * Math.PI * freqs[noteIdx] * t);
            samples[i] = (short) (wave * env * 22000);
        }
        return buildWav(samples, sampleRate);
    }

    private static byte[] synthesizeClickWav() throws IOException {
        int sampleRate = 44100;
        float duration = 0.05f;
        int totalSamples = (int) (sampleRate * duration);
        short[] samples = new short[totalSamples];

        for (int i = 0; i < totalSamples; i++) {
            float t = (float) i / sampleRate;
            float freq = 850f - (t / duration) * 450f;
            float env = (float) Math.exp(-t * 90.0);
            float wave = (float) Math.sin(2.0 * Math.PI * freq * t);
            samples[i] = (short) (wave * env * 22000);
        }
        return buildWav(samples, sampleRate);
    }

    private static byte[] synthesizeHoverWav() throws IOException {
        int sampleRate = 44100;
        float duration = 0.02f;
        int totalSamples = (int) (sampleRate * duration);
        short[] samples = new short[totalSamples];

        for (int i = 0; i < totalSamples; i++) {
            float t = (float) i / sampleRate;
            float freq = 1200f;
            float env = (float) Math.exp(-t * 220.0);
            float wave = (float) Math.sin(2.0 * Math.PI * freq * t);
            samples[i] = (short) (wave * env * 11000);
        }
        return buildWav(samples, sampleRate);
    }
}
