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
 * Procedural Audio Synthesizer for TargetingGame.
 * Generates custom 44.1kHz 16-bit sound effects entirely through code.
 */
public class SoundEffects {

    private Sound launchSound;
    private Sound impactSound;
    private Sound resetSound;

    public void init() {
        try {
            FileHandle dir = Gdx.files.local("sounds");
            if (!dir.exists()) {
                dir.mkdirs();
            }

            FileHandle launchFile = Gdx.files.local("sounds/launch.wav");
            FileHandle impactFile = Gdx.files.local("sounds/impact.wav");
            FileHandle resetFile = Gdx.files.local("sounds/reset.wav");

            launchFile.writeBytes(synthesizeLaunchWav(), false);
            impactFile.writeBytes(synthesizeImpactWav(), false);
            resetFile.writeBytes(synthesizeResetWav(), false);

            launchSound = Gdx.audio.newSound(launchFile);
            impactSound = Gdx.audio.newSound(impactFile);
            resetSound = Gdx.audio.newSound(resetFile);
        } catch (Exception e) {
            Gdx.app.error("SoundEffects", "Error synthesizing audio effects", e);
        }
    }

    public void playLaunch() {
        if (launchSound != null) {
            launchSound.play(0.85f, 1.0f, -0.3f); // Pan slightly left where launcher is
        }
    }

    public void playImpact(float volume, float pitch, float pan) {
        if (impactSound != null) {
            float clampedVol = Math.max(0.15f, Math.min(volume, 1.0f));
            float clampedPitch = Math.max(0.7f, Math.min(pitch, 1.4f));
            float clampedPan = Math.max(-1.0f, Math.min(pan, 1.0f));
            impactSound.play(clampedVol, clampedPitch, clampedPan);
        }
    }

    public void playReset() {
        if (resetSound != null) {
            resetSound.play(0.6f, 1.1f, -0.3f);
        }
    }

    public void dispose() {
        if (launchSound != null) launchSound.dispose();
        if (impactSound != null) impactSound.dispose();
        if (resetSound != null) resetSound.dispose();
    }

    // --- Procedural WAV Generation ---

    private static byte[] buildWav(short[] pcmSamples, int sampleRate) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        int totalAudioLen = pcmSamples.length * 2;
        int totalDataLen = totalAudioLen + 36;

        // RIFF header
        dos.writeBytes("RIFF");
        dos.writeInt(Integer.reverseBytes(totalDataLen));
        dos.writeBytes("WAVE");

        // Format chunk
        dos.writeBytes("fmt ");
        dos.writeInt(Integer.reverseBytes(16)); // Chunk size
        dos.writeShort(Short.reverseBytes((short) 1)); // Audio format (PCM)
        dos.writeShort(Short.reverseBytes((short) 1)); // Channels (1 = Mono)
        dos.writeInt(Integer.reverseBytes(sampleRate)); // Sample rate
        dos.writeInt(Integer.reverseBytes(sampleRate * 2)); // Byte rate
        dos.writeShort(Short.reverseBytes((short) 2)); // Block align
        dos.writeShort(Short.reverseBytes((short) 16)); // Bits per sample

        // Data chunk
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
     * Synthesizes an energetic 'Whoosh / Launch' slingshot sound.
     */
    private static byte[] synthesizeLaunchWav() throws IOException {
        int sampleRate = 44100;
        float duration = 0.28f;
        int totalSamples = (int) (sampleRate * duration);
        short[] samples = new short[totalSamples];

        for (int i = 0; i < totalSamples; i++) {
            float t = (float) i / sampleRate;
            float progress = (float) i / totalSamples;

            // Pitch sweeps upwards from 140Hz to 480Hz
            float freq = 140f + 340f * (float) Math.pow(progress, 1.5);
            float sine = (float) Math.sin(2.0 * Math.PI * freq * t);

            // Add slight air noise
            float noise = (float) (Math.random() * 2.0 - 1.0) * 0.25f;

            // Amplitude envelope (fast rise, smooth fade out)
            float envelope = (float) (Math.sin(progress * Math.PI));

            float sampleVal = (sine * 0.75f + noise * 0.25f) * envelope * 24000f;
            samples[i] = (short) Math.max(-32767, Math.min(32767, sampleVal));
        }

        return buildWav(samples, sampleRate);
    }

    /**
     * Synthesizes a punchy, heavy 'Thud / Impact' wooden-stone collision sound.
     */
    private static byte[] synthesizeImpactWav() throws IOException {
        int sampleRate = 44100;
        float duration = 0.32f;
        int totalSamples = (int) (sampleRate * duration);
        short[] samples = new short[totalSamples];

        for (int i = 0; i < totalSamples; i++) {
            float t = (float) i / sampleRate;
            float progress = (float) i / totalSamples;

            // Low frequency punch (starts at 180Hz and rapidly drops to 45Hz)
            float freq = 45f + 135f * (float) Math.exp(-progress * 14.0);
            float bass = (float) Math.sin(2.0 * Math.PI * freq * t);

            // Crunchy wood splinter crackle noise at the impact onset
            float crackle = (progress < 0.2f) ? (float) (Math.random() * 2.0 - 1.0) * 0.5f : 0f;

            // Exponential decay envelope
            float envelope = (float) Math.exp(-progress * 9.0);

            float sampleVal = (bass * 0.75f + crackle * 0.25f) * envelope * 29000f;
            samples[i] = (short) Math.max(-32767, Math.min(32767, sampleVal));
        }

        return buildWav(samples, sampleRate);
    }

    /**
     * Synthesizes a subtle, pleasant 'Pop / Reset' sound.
     */
    private static byte[] synthesizeResetWav() throws IOException {
        int sampleRate = 44100;
        float duration = 0.16f;
        int totalSamples = (int) (sampleRate * duration);
        short[] samples = new short[totalSamples];

        for (int i = 0; i < totalSamples; i++) {
            float t = (float) i / sampleRate;
            float progress = (float) i / totalSamples;

            // Tone drops slightly from 600Hz to 400Hz
            float freq = 400f + 200f * (1.0f - progress);
            float sine = (float) Math.sin(2.0 * Math.PI * freq * t);

            float envelope = (float) Math.exp(-progress * 15.0);

            float sampleVal = sine * envelope * 18000f;
            samples[i] = (short) Math.max(-32767, Math.min(32767, sampleVal));
        }

        return buildWav(samples, sampleRate);
    }
}
