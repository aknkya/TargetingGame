package org.example;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;

public class DesktopLauncher {
    public static void main(String[] arg) {
        if (StartupHelper.startNewJvmIfRequired()) return;

        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setForegroundFPS(60);
        config.setTitle("Sudoku Master - Modern Glass Edition");
        config.setWindowedMode(1280, 720);
        config.useVsync(true);

        // Enable 4x Multi-Sample Anti-Aliasing (MSAA) for crisp, smooth vector shapes & lines
        config.setBackBufferConfig(8, 8, 8, 8, 16, 0, 4);

        new Lwjgl3Application(new TargetingGame(), config);
    }
}
