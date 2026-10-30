package org.example;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Random;

/**
 * Procedural Audio Synthesizer for Sudoku Master.
 * Features high-fidelity DSP synthesis for Paper Notebook and Modern Glass themes.
 * Paper soundscape delivers authentic stationery acoustics:
 *  - Tactile pencil writing strokes on textured paper
 *  - Delicate graphite pencil note marks
 *  - Realistic rubber eraser friction
 *  - Natural pencil scribble / cross-out mistake feedback
 *  - Notebook page turn flutter & warm acoustic celebration chime
 *  - Tactile paper card taps and subtle paper rustle hovers
 */
public class SoundEffects {

    // Paper Theme Sounds
    private Sound paperPlaceSound;
    private Sound paperPencilSound;
    private Sound paperEraseSound;
    private Sound paperErrorSound;
    private Sound paperVictorySound;
    private Sound paperClickSound;
    private Sound paperHoverSound;

    // Modern Glass Theme Sounds
    private Sound glassPlaceSound;
    private Sound glassPencilSound;
    private Sound glassEraseSound;
    private Sound glassErrorSound;
    private Sound glassVictorySound;
    private Sound glassClickSound;
    private Sound glassHoverSound;

    private boolean isPaperTheme = true;
    private boolean soundEnabled = true;

    public void init() {
        try {
            ensureDirectory("sounds");
            ensureDirectory("../assets/sounds");

            // --- 1. Synthesize Paper Theme Audio ---
            byte[] paperPlaceWav = synthesizePaperPlaceWav();
            byte[] paperPencilWav = synthesizePaperPencilWav();
            byte[] paperEraseWav = synthesizePaperEraseWav();
            byte[] paperErrorWav = synthesizePaperErrorWav();
            byte[] paperVictoryWav = synthesizePaperVictoryWav();
            byte[] paperClickWav = synthesizePaperClickWav();
            byte[] paperHoverWav = synthesizePaperHoverWav();

            // --- 2. Synthesize Modern Glass Theme Audio ---
            byte[] glassPlaceWav = synthesizeGlassPlaceWav();
            byte[] glassPencilWav = synthesizeGlassPencilWav();
            byte[] glassEraseWav = synthesizeGlassEraseWav();
            byte[] glassErrorWav = synthesizeGlassErrorWav();
            byte[] glassVictoryWav = synthesizeGlassVictoryWav();
            byte[] glassClickWav = synthesizeGlassClickWav();
            byte[] glassHoverWav = synthesizeGlassHoverWav();

            // Write files to local storage (both theme-specific and default files)
            FileHandle pPlaceFile = writeBytesSafely("sounds/paper_place.wav", paperPlaceWav);
            FileHandle pPencilFile = writeBytesSafely("sounds/paper_pencil.wav", paperPencilWav);
            FileHandle pEraseFile = writeBytesSafely("sounds/paper_erase.wav", paperEraseWav);
            FileHandle pErrorFile = writeBytesSafely("sounds/paper_error.wav", paperErrorWav);
            FileHandle pVictoryFile = writeBytesSafely("sounds/paper_victory.wav", paperVictoryWav);
            FileHandle pClickFile = writeBytesSafely("sounds/paper_click.wav", paperClickWav);
            FileHandle pHoverFile = writeBytesSafely("sounds/paper_hover.wav", paperHoverWav);

            // Default filenames point to Paper Theme audio
            writeBytesSafely("sounds/place.wav", paperPlaceWav);
            writeBytesSafely("sounds/pencil.wav", paperPencilWav);
            writeBytesSafely("sounds/erase.wav", paperEraseWav);
            writeBytesSafely("sounds/error.wav", paperErrorWav);
            writeBytesSafely("sounds/victory.wav", paperVictoryWav);
            writeBytesSafely("sounds/click.wav", paperClickWav);
            writeBytesSafely("sounds/hover.wav", paperHoverWav);

            // Also mirror to assets/sounds if accessible
            mirrorToAssets("place.wav", paperPlaceWav);
            mirrorToAssets("pencil.wav", paperPencilWav);
            mirrorToAssets("erase.wav", paperEraseWav);
            mirrorToAssets("error.wav", paperErrorWav);
            mirrorToAssets("victory.wav", paperVictoryWav);
            mirrorToAssets("click.wav", paperClickWav);
            mirrorToAssets("hover.wav", paperHoverWav);

            FileHandle gPlaceFile = writeBytesSafely("sounds/glass_place.wav", glassPlaceWav);
            FileHandle gPencilFile = writeBytesSafely("sounds/glass_pencil.wav", glassPencilWav);
            FileHandle gEraseFile = writeBytesSafely("sounds/glass_erase.wav", glassEraseWav);
            FileHandle gErrorFile = writeBytesSafely("sounds/glass_error.wav", glassErrorWav);
            FileHandle gVictoryFile = writeBytesSafely("sounds/glass_victory.wav", glassVictoryWav);
            FileHandle gClickFile = writeBytesSafely("sounds/glass_click.wav", glassClickWav);
            FileHandle gHoverFile = writeBytesSafely("sounds/glass_hover.wav", glassHoverWav);

            // Instantiate Sound instances
            paperPlaceSound = Gdx.audio.newSound(pPlaceFile);
            paperPencilSound = Gdx.audio.newSound(pPencilFile);
            paperEraseSound = Gdx.audio.newSound(pEraseFile);
            paperErrorSound = Gdx.audio.newSound(pErrorFile);
            paperVictorySound = Gdx.audio.newSound(pVictoryFile);
            paperClickSound = Gdx.audio.newSound(pClickFile);
            paperHoverSound = Gdx.audio.newSound(pHoverFile);

            glassPlaceSound = Gdx.audio.newSound(gPlaceFile);
            glassPencilSound = Gdx.audio.newSound(gPencilFile);
            glassEraseSound = Gdx.audio.newSound(gEraseFile);
            glassErrorSound = Gdx.audio.newSound(gErrorFile);
            glassVictorySound = Gdx.audio.newSound(gVictoryFile);
            glassClickSound = Gdx.audio.newSound(gClickFile);
            glassHoverSound = Gdx.audio.newSound(gHoverFile);

        } catch (Exception e) {
            Gdx.app.error("SoundEffects", "Error synthesizing audio effects", e);
        }
    }

    private void ensureDirectory(String dirPath) {
        try {
            FileHandle dir = Gdx.files.local(dirPath);
            if (!dir.exists()) {
                dir.mkdirs();
            }
        } catch (Exception ignored) {}
    }

    private FileHandle writeBytesSafely(String path, byte[] data) {
        FileHandle file = Gdx.files.local(path);
        file.writeBytes(data, false);
        return file;
    }

    private void mirrorToAssets(String name, byte[] data) {
        try {
            FileHandle f = Gdx.files.local("../assets/sounds/" + name);
            if (f.parent().exists()) {
                f.writeBytes(data, false);
            }
        } catch (Exception ignored) {}
    }

    public boolean isSoundEnabled() { return soundEnabled; }
    public void setSoundEnabled(boolean enabled) { this.soundEnabled = enabled; }
    public void toggleSound() { this.soundEnabled = !this.soundEnabled; }

    public boolean isPaperTheme() { return isPaperTheme; }
    public void setPaperTheme(boolean paperTheme) { this.isPaperTheme = paperTheme; }

    /**
     * Tactile number placement (Pencil stroke on paper or glass chime).
     */
    public void playPlace() {
        if (!soundEnabled) return;
        float pitchVar = 0.97f + (float) (Math.random() * 0.06f);
        if (isPaperTheme) {
            if (paperPlaceSound != null) paperPlaceSound.play(0.92f, pitchVar, 0f);
        } else {
            if (glassPlaceSound != null) glassPlaceSound.play(0.85f, 1.0f, 0f);
        }
    }

    /**
     * Subtle graphite pencil note tick or digital blip.
     */
    public void playPencil() {
        if (!soundEnabled) return;
        float pitchVar = 0.96f + (float) (Math.random() * 0.08f);
        if (isPaperTheme) {
            if (paperPencilSound != null) paperPencilSound.play(0.70f, pitchVar, 0f);
        } else {
            if (glassPencilSound != null) glassPencilSound.play(0.55f, 1.15f, 0f);
        }
    }

    /**
     * Rubber eraser rubbing on paper fibers or smooth whoosh.
     */
    public void playErase() {
        if (!soundEnabled) return;
        float pitchVar = 0.97f + (float) (Math.random() * 0.06f);
        if (isPaperTheme) {
            if (paperEraseSound != null) paperEraseSound.play(0.85f, pitchVar, 0f);
        } else {
            if (glassEraseSound != null) glassEraseSound.play(0.60f, 0.95f, 0f);
        }
    }

    /**
     * Frustrated pencil scribble / scratch-out error on paper or electronic buzz.
     */
    public void playError() {
        if (!soundEnabled) return;
        float pitchVar = 0.98f + (float) (Math.random() * 0.04f);
        if (isPaperTheme) {
            if (paperErrorSound != null) paperErrorSound.play(0.90f, pitchVar, 0f);
        } else {
            if (glassErrorSound != null) glassErrorSound.play(0.75f, 1.0f, 0f);
        }
    }

    /**
     * Notebook page turn flutter & warm acoustic celebration or arpeggio fanfare.
     */
    public void playVictory() {
        if (!soundEnabled) return;
        if (isPaperTheme) {
            if (paperVictorySound != null) paperVictorySound.play(1.0f, 1.0f, 0f);
        } else {
            if (glassVictorySound != null) glassVictorySound.play(1.0f, 1.0f, 0f);
        }
    }

    /**
     * Tactile paper card/tab tap or modern interface click.
     */
    public void playClick() {
        if (!soundEnabled) return;
        float pitchVar = 0.98f + (float) (Math.random() * 0.04f);
        if (isPaperTheme) {
            if (paperClickSound != null) paperClickSound.play(0.75f, pitchVar, 0f);
        } else {
            if (glassClickSound != null) glassClickSound.play(0.70f, 1.0f, 0f);
        }
    }

    /**
     * Whisper of paper rustle / brush or subtle modern hover blip.
     */
    public void playHover() {
        if (!soundEnabled) return;
        if (isPaperTheme) {
            if (paperHoverSound != null) paperHoverSound.play(0.40f, 1.0f, 0f);
        } else {
            if (glassHoverSound != null) glassHoverSound.play(0.35f, 1.0f, 0f);
        }
    }

    public void dispose() {
        if (paperPlaceSound != null) paperPlaceSound.dispose();
        if (paperPencilSound != null) paperPencilSound.dispose();
        if (paperEraseSound != null) paperEraseSound.dispose();
        if (paperErrorSound != null) paperErrorSound.dispose();
        if (paperVictorySound != null) paperVictorySound.dispose();
        if (paperClickSound != null) paperClickSound.dispose();
        if (paperHoverSound != null) paperHoverSound.dispose();

        if (glassPlaceSound != null) glassPlaceSound.dispose();
        if (glassPencilSound != null) glassPencilSound.dispose();
        if (glassEraseSound != null) glassEraseSound.dispose();
        if (glassErrorSound != null) glassErrorSound.dispose();
        if (glassVictorySound != null) glassVictorySound.dispose();
        if (glassClickSound != null) glassClickSound.dispose();
        if (glassHoverSound != null) glassHoverSound.dispose();
    }

    // =========================================================================
    // DSP Filter & Wav Utility
    // =========================================================================

    public static class BiquadFilter {
        private double b0, b1, b2, a1, a2;
        private double x1 = 0, x2 = 0, y1 = 0, y2 = 0;

        public static BiquadFilter createBandPass(double freq, double q, double sampleRate) {
            BiquadFilter f = new BiquadFilter();
            double w0 = 2.0 * Math.PI * freq / sampleRate;
            double alpha = Math.sin(w0) / (2.0 * Math.max(0.001, q));
            double b0 = alpha;
            double b1 = 0;
            double b2 = -alpha;
            double a0 = 1.0 + alpha;
            double a1 = -2.0 * Math.cos(w0);
            double a2 = 1.0 - alpha;
            f.b0 = b0 / a0;
            f.b1 = b1 / a0;
            f.b2 = b2 / a0;
            f.a1 = a1 / a0;
            f.a2 = a2 / a0;
            return f;
        }

        public static BiquadFilter createLowPass(double freq, double q, double sampleRate) {
            BiquadFilter f = new BiquadFilter();
            double w0 = 2.0 * Math.PI * freq / sampleRate;
            double alpha = Math.sin(w0) / (2.0 * Math.max(0.001, q));
            double cosw0 = Math.cos(w0);
            double b0 = (1.0 - cosw0) / 2.0;
            double b1 = 1.0 - cosw0;
            double b2 = (1.0 - cosw0) / 2.0;
            double a0 = 1.0 + alpha;
            double a1 = -2.0 * cosw0;
            double a2 = 1.0 - alpha;
            f.b0 = b0 / a0;
            f.b1 = b1 / a0;
            f.b2 = b2 / a0;
            f.a1 = a1 / a0;
            f.a2 = a2 / a0;
            return f;
        }

        public static BiquadFilter createHighPass(double freq, double q, double sampleRate) {
            BiquadFilter f = new BiquadFilter();
            double w0 = 2.0 * Math.PI * freq / sampleRate;
            double alpha = Math.sin(w0) / (2.0 * Math.max(0.001, q));
            double cosw0 = Math.cos(w0);
            double b0 = (1.0 + cosw0) / 2.0;
            double b1 = -(1.0 + cosw0);
            double b2 = (1.0 + cosw0) / 2.0;
            double a0 = 1.0 + alpha;
            double a1 = -2.0 * cosw0;
            double a2 = 1.0 - alpha;
            f.b0 = b0 / a0;
            f.b1 = b1 / a0;
            f.b2 = b2 / a0;
            f.a1 = a1 / a0;
            f.a2 = a2 / a0;
            return f;
        }

        public double process(double in) {
            double out = b0 * in + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2;
            x2 = x1;
            x1 = in;
            y2 = y1;
            y1 = out;
            return out;
        }
    }

    private static short clampSample(double sample) {
        if (sample > 1.0) sample = 1.0;
        else if (sample < -1.0) sample = -1.0;
        return (short) (sample * 32760);
    }

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

    // =========================================================================
    // Paper Notebook Procedural Sound Synthesizers
    // =========================================================================

    /**
     * Tactile pencil writing stroke on textured notebook paper.
     */
    public static byte[] synthesizePaperPlaceWav() throws IOException {
        int sampleRate = 44100;
        float duration = 0.11f; // 110ms stroke
        int totalSamples = (int) (sampleRate * duration);
        short[] samples = new short[totalSamples];
        Random rnd = new Random(1337);

        BiquadFilter bpFriction = BiquadFilter.createBandPass(3200, 1.4, sampleRate);
        BiquadFilter bpGrain = BiquadFilter.createBandPass(5400, 2.2, sampleRate);

        for (int i = 0; i < totalSamples; i++) {
            float t = (float) i / sampleRate;

            // Initial pencil tip paper touch transient (0-15ms)
            double tipTap = Math.sin(2.0 * Math.PI * 260.0 * t) * Math.exp(-t * 160.0) * 0.45;

            // Textured friction noise
            double rawNoise = rnd.nextDouble() * 2.0 - 1.0;
            double friction = bpFriction.process(rawNoise) * 0.75 + bpGrain.process(rawNoise) * 0.55;

            // Stroke envelope with natural manual writing dynamics
            double attack = Math.min(1.0, t / 0.005);
            double strokeMod = 0.8 + 0.2 * Math.sin(2.0 * Math.PI * 35.0 * t);
            double decay = Math.exp(-Math.max(0.0, (t - 0.055) * 55.0));
            double env = attack * strokeMod * decay;

            double combined = (tipTap + friction * env) * 0.95;
            samples[i] = clampSample(combined);
        }
        return buildWav(samples, sampleRate);
    }

    /**
     * Delicate graphite pencil tick / candidate note mark.
     */
    public static byte[] synthesizePaperPencilWav() throws IOException {
        int sampleRate = 44100;
        float duration = 0.042f; // 42ms
        int totalSamples = (int) (sampleRate * duration);
        short[] samples = new short[totalSamples];
        Random rnd = new Random(4242);

        BiquadFilter hp = BiquadFilter.createHighPass(4200, 1.0, sampleRate);
        BiquadFilter bp = BiquadFilter.createBandPass(5800, 2.5, sampleRate);

        for (int i = 0; i < totalSamples; i++) {
            float t = (float) i / sampleRate;

            // Delicate graphite contact tap
            double tap = Math.sin(2.0 * Math.PI * 480.0 * t) * Math.exp(-t * 320.0) * 0.35;

            // Light graphite scratch
            double rawNoise = rnd.nextDouble() * 2.0 - 1.0;
            double scratch = hp.process(rawNoise) * 0.4 + bp.process(rawNoise) * 0.7;

            double attack = Math.min(1.0, t / 0.002);
            double decay = Math.exp(-t * 110.0);
            double env = attack * decay;

            double combined = (tap * 0.3 + scratch * env) * 0.85;
            samples[i] = clampSample(combined);
        }
        return buildWav(samples, sampleRate);
    }

    /**
     * Realistic rubber eraser rubbing on paper fibers.
     */
    public static byte[] synthesizePaperEraseWav() throws IOException {
        int sampleRate = 44100;
        float duration = 0.24f; // 240ms (two rubs)
        int totalSamples = (int) (sampleRate * duration);
        short[] samples = new short[totalSamples];
        Random rnd = new Random(8888);

        BiquadFilter bp1 = BiquadFilter.createBandPass(1350, 1.2, sampleRate);
        BiquadFilter bp2 = BiquadFilter.createBandPass(1850, 1.4, sampleRate);

        for (int i = 0; i < totalSamples; i++) {
            float t = (float) i / sampleRate;
            double rawNoise = rnd.nextDouble() * 2.0 - 1.0;
            double env = 0;
            double filtered = 0;

            if (t < 0.10f) {
                // First rub stroke (0 to 100ms)
                double strokeT = t / 0.10f;
                double strokeEnv = Math.sin(Math.PI * strokeT);
                double wobble = 0.8 + 0.2 * Math.sin(2.0 * Math.PI * 90.0 * t);
                env = strokeEnv * wobble;
                filtered = bp1.process(rawNoise);
            } else if (t < 0.125f) {
                // Reversal pause
                env = 0.03;
                filtered = bp1.process(rawNoise) * 0.2;
            } else {
                // Second rub stroke in reverse direction
                double strokeT = (t - 0.125f) / 0.115f;
                double strokeEnv = Math.sin(Math.PI * strokeT);
                double wobble = 0.85 + 0.15 * Math.sin(2.0 * Math.PI * 105.0 * t);
                env = strokeEnv * wobble;
                filtered = bp2.process(rawNoise);
            }

            double rubberBody = Math.sin(2.0 * Math.PI * 180.0 * t) * env * 0.18;
            double combined = (filtered * env * 0.85 + rubberBody) * 0.9;
            samples[i] = clampSample(combined);
        }
        return buildWav(samples, sampleRate);
    }

    /**
     * Rapid pencil scribble / cross-out of a mistake on paper.
     */
    public static byte[] synthesizePaperErrorWav() throws IOException {
        int sampleRate = 44100;
        float duration = 0.22f; // 220ms
        int totalSamples = (int) (sampleRate * duration);
        short[] samples = new short[totalSamples];
        Random rnd = new Random(5555);

        BiquadFilter bp1 = BiquadFilter.createBandPass(2800, 1.5, sampleRate);
        BiquadFilter bp2 = BiquadFilter.createBandPass(3600, 1.7, sampleRate);
        BiquadFilter bp3 = BiquadFilter.createBandPass(3100, 1.4, sampleRate);

        for (int i = 0; i < totalSamples; i++) {
            float t = (float) i / sampleRate;
            double rawNoise = rnd.nextDouble() * 2.0 - 1.0;

            double strokeEnv = 0;
            double filtered = 0;
            double thud = 0;

            if (t < 0.065f) {
                // Scratch 1
                double st = t / 0.065f;
                strokeEnv = Math.pow(Math.sin(Math.PI * st), 0.7);
                filtered = bp1.process(rawNoise);
            } else if (t < 0.135f) {
                // Scratch 2 (reverse stroke)
                double st = (t - 0.065f) / 0.070f;
                strokeEnv = Math.pow(Math.sin(Math.PI * st), 0.7);
                filtered = bp2.process(rawNoise);
            } else {
                // Scratch 3 (firm cross stroke with wood thud)
                double st = (t - 0.135f) / 0.085f;
                strokeEnv = Math.pow(Math.sin(Math.PI * st), 0.8);
                filtered = bp3.process(rawNoise);
                thud = Math.sin(2.0 * Math.PI * 220.0 * (t - 0.135f)) * Math.exp(-(t - 0.135f) * 80.0) * 0.35;
            }

            double combined = (filtered * strokeEnv * 0.85 + thud) * 0.95;
            samples[i] = clampSample(combined);
        }
        return buildWav(samples, sampleRate);
    }

    /**
     * Notebook page turn flutter + warm acoustic harmonic celebration.
     */
    public static byte[] synthesizePaperVictoryWav() throws IOException {
        int sampleRate = 44100;
        float duration = 0.78f;
        int totalSamples = (int) (sampleRate * duration);
        short[] samples = new short[totalSamples];
        Random rnd = new Random(7777);

        BiquadFilter lpPage = BiquadFilter.createLowPass(3200, 0.9, sampleRate);

        double[] noteFreqs = { 523.25, 659.25, 783.99, 1046.50, 1318.51 };
        double[] noteStart = { 0.08, 0.18, 0.28, 0.40, 0.42 };

        for (int i = 0; i < totalSamples; i++) {
            float t = (float) i / sampleRate;

            // Notebook page turn whoosh & paper flutter
            double pageSound = 0;
            if (t < 0.25f) {
                double rawNoise = rnd.nextDouble() * 2.0 - 1.0;
                double pageEnv = Math.sin(Math.PI * (t / 0.25f));
                double ripple = 0.75 + 0.25 * Math.sin(2.0 * Math.PI * 45.0 * t);
                pageSound = lpPage.process(rawNoise) * pageEnv * ripple * 0.45;
            }

            // Warm acoustic chord (marimba / kalimba rich harmonic overtones)
            double chimeSound = 0;
            for (int n = 0; n < noteFreqs.length; n++) {
                if (t >= noteStart[n]) {
                    double dt = t - noteStart[n];
                    double noteEnv = Math.exp(-dt * 6.5);
                    double freq = noteFreqs[n];
                    double wave = Math.sin(2.0 * Math.PI * freq * dt) * 0.65
                                + Math.sin(2.0 * Math.PI * (freq * 2.0) * dt) * 0.22
                                + Math.sin(2.0 * Math.PI * (freq * 3.0) * dt) * 0.09
                                + Math.sin(2.0 * Math.PI * (freq * 4.0) * dt) * 0.04;
                    chimeSound += wave * noteEnv * 0.26;
                }
            }

            double combined = pageSound + chimeSound;
            samples[i] = clampSample(combined * 0.95);
        }
        return buildWav(samples, sampleRate);
    }

    /**
     * Tactile paper card/tab tap.
     */
    public static byte[] synthesizePaperClickWav() throws IOException {
        int sampleRate = 44100;
        float duration = 0.045f;
        int totalSamples = (int) (sampleRate * duration);
        short[] samples = new short[totalSamples];
        Random rnd = new Random(1010);

        BiquadFilter bp = BiquadFilter.createBandPass(3800, 1.8, sampleRate);

        for (int i = 0; i < totalSamples; i++) {
            float t = (float) i / sampleRate;

            // Cardstock / desk tap body
            double tap = Math.sin(2.0 * Math.PI * 230.0 * t) * Math.exp(-t * 140.0) * 0.45;

            // Paper edge snap noise
            double rawNoise = rnd.nextDouble() * 2.0 - 1.0;
            double snap = bp.process(rawNoise) * Math.exp(-t * 110.0) * 0.55;

            double combined = (tap + snap) * 0.9;
            samples[i] = clampSample(combined);
        }
        return buildWav(samples, sampleRate);
    }

    /**
     * Subtle paper rustle / sheet brush hover.
     */
    public static byte[] synthesizePaperHoverWav() throws IOException {
        int sampleRate = 44100;
        float duration = 0.025f;
        int totalSamples = (int) (sampleRate * duration);
        short[] samples = new short[totalSamples];
        Random rnd = new Random(2020);

        BiquadFilter hp = BiquadFilter.createHighPass(3600, 1.0, sampleRate);

        for (int i = 0; i < totalSamples; i++) {
            float t = (float) i / sampleRate;
            double rawNoise = rnd.nextDouble() * 2.0 - 1.0;
            double filtered = hp.process(rawNoise);
            double env = Math.sin(Math.PI * (t / duration));
            double combined = filtered * env * 0.28;
            samples[i] = clampSample(combined);
        }
        return buildWav(samples, sampleRate);
    }

    // =========================================================================
    // Modern Glass Procedural Sound Synthesizers
    // =========================================================================

    private static byte[] synthesizeGlassPlaceWav() throws IOException {
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

    private static byte[] synthesizeGlassPencilWav() throws IOException {
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

    private static byte[] synthesizeGlassEraseWav() throws IOException {
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

    private static byte[] synthesizeGlassErrorWav() throws IOException {
        int sampleRate = 44100;
        float duration = 0.22f;
        int totalSamples = (int) (sampleRate * duration);
        short[] samples = new short[totalSamples];

        for (int i = 0; i < totalSamples; i++) {
            float t = (float) i / sampleRate;
            float freq = 150f;
            float env = (float) Math.exp(-t * 12.0);
            float wave = Math.sin(2.0 * Math.PI * freq * t) > 0 ? 0.6f : -0.6f;
            samples[i] = (short) (wave * env * 18000);
        }
        return buildWav(samples, sampleRate);
    }

    private static byte[] synthesizeGlassVictoryWav() throws IOException {
        int sampleRate = 44100;
        float duration = 0.65f;
        int totalSamples = (int) (sampleRate * duration);
        short[] samples = new short[totalSamples];

        float[] freqs = { 523.25f, 659.25f, 783.99f, 1046.50f };
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

    private static byte[] synthesizeGlassClickWav() throws IOException {
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

    private static byte[] synthesizeGlassHoverWav() throws IOException {
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
