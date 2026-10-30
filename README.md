# Sudoku Master - Paper Edition

A tactile, elegant, high-performance 9x9 Sudoku game built with **Java 21** and the **LibGDX** ecosystem. Features an authentic **Paper & Pencil stationery theme** (cream parchment, graphite pencil borders, warm desk lamp lighting, handwritten ink typography, and fluttering paper confetti), powered by the Universal Tween Engine, Scene2D, Preferences save system, Box2D physics, and cross-platform desktop/Android support.

---

## 🎮 Game Controls & Features

### 🕹️ Controls
- **Cell Selection**: Click/tap any cell on the 9x9 board, or navigate using **Arrow Keys [↑ ↓ ← →]**.
- **Number Input**: Click the on-screen **NumPad (1-9)** or press **[1] - [9]** on your keyboard.
- **Pencil Notes (Notes Mode)**: Toggle the on-screen **NOTES** button, or press **[Space]** / **[N]** to write candidate mini-numbers (1-9) within empty cells.
- **Erase**: Click **ERASE** or press **[Backspace]** / **[Del]** to clear the selected cell or its candidate notes.
- **Undo**: Click **UNDO** or press **[Z]** / **[U]** to revert previous moves and notes.
- **Hint**: Click **HINT** to reveal the correct solution for the currently selected cell.
- **Pause & Menu**: Press **[ESC]** or click **[MENU]** to pause the game, adjust settings, or return to the main menu.
- **Audio Toggle**: Press **[M]** or click **[SOUND: ON/OFF]** in the HUD or Settings screen.

### 🌟 Gameplay Features
- **4 Difficulty Levels**:
  - **Easy**: Relaxed puzzle with ~36 clues removed.
  - **Medium**: Balanced challenge with ~46 clues removed.
  - **Hard**: Advanced logic required with ~52 clues removed.
  - **Expert**: Master-level challenge with ~56 clues removed.
- **Universal Tween Engine Animations**:
  - Smooth paper elevation & tint when selecting cells.
  - Tactile "pencil writing" pop animation (`0.90 -> 1.05 -> 1.00`) when placing digits.
  - Rapid shake and red ink color transition on conflicting/invalid numbers.
  - Smooth parchment fade transition across screen states.
- **Save & Resume System (Preferences)**:
  - Automatically saves active game state (board digits, candidate notes, elapsed time, difficulty, mistakes).
  - Main menu offers a seamless **RESUME** button to continue where you left off.
  - Player statistics: Total games completed, best time per difficulty, and total cumulative play time.
  - Persistent user preferences (sound effects toggle, desk lamp toggle).
- **Scene2D Paper Theme UI**:
  - Main Menu, Settings, and Statistics screens rendered with programmatic paper card drawables, fine pencil outlines, and drop shadows.
- **Atmospheric Lighting & Confetti**:
  - **Warm Desk Lamp (Box2DLights)**: Soft amber/yellow light tracking cursor/touch over cream parchment.
  - **Origami Paper Confetti (Box2D)**: Pastel, matte paper squares fluttering with realistic physics on victory.
- **Cross-Platform Compatibility**:
  - Full desktop (Windows, macOS, Linux) and Android support with responsive touch down/up handling and `FitViewport` multi-aspect ratio scaling.

---

## 🛠️ Complete Libraries & Dependencies

All libraries and dependencies used in the project, including the latest additions:

| Library / Module | Artifact / Dependency | Version | Role in Sudoku Master |
| :--- | :--- | :---: | :--- |
| **Universal Tween Engine** *(Newly Added)* | `org.mini2Dx:universal-tween-engine` | `6.3.3` | Powers smooth cell selection highlights, pencil placement pop animations, mistake shake effects, and screen fade transitions. |
| **LibGDX Core** | `com.badlogicgames.gdx:gdx` | `1.13.1` | Core game engine lifecycle, viewport management, input processing, and sprite batching. |
| **LibGDX Scene2D** *(Newly Added UI)* | `com.badlogicgames.gdx:gdx` *(Scene2D)* | `1.13.1` | Hierarchical UI widget system (`Stage`, `Table`, `Skin`, `TextButton`, `Label`) used for Main Menu, Settings, and Statistics screens. |
| **LibGDX Preferences** *(Newly Added)* | `com.badlogicgames.gdx:gdx` *(Preferences)* | `1.13.1` | Local persistent storage manager (`SaveManager`) for game progress, board state, statistics, and user settings. |
| **ShapeDrawer** | `space.earlygrey:shapedrawer` | `2.6.0` | High-performance vector rendering for notebook paper ruling lines, margin rules, 9x9 grid, 3x3 block ink borders, and card containers. |
| **Box2D Physics** | `com.badlogicgames.gdx:gdx-box2d` | `1.13.1` | Simulates the dynamic **Victory Celebration**: 70 physical pastel origami paper scraps tumbling and bouncing with realistic gravity and air resistance. |
| **Box2D Platform Natives** | `gdx-box2d-platform` (desktop/android) | `1.13.1` | Native JNI C++ physics binaries compiled for desktop and mobile architectures (x86, x64, armeabi-v7a, arm64-v8a). |
| **Box2DLights** | `com.badlogicgames.box2dlights:box2dlights` | `1.5` | Dynamic 2D **Warm Desk Lamp Lighting**: diffuse ambient room lighting and cozy amber point light tracking mouse/touch. |
| **Gdx FreeType** | `com.badlogicgames.gdx:gdx-freetype` | `1.13.1` | TrueType font rasterizer for newspaper printed clues, authentic handwriting player digits, pencil notes, and UI typography. |
| **Gdx FreeType Natives** | `gdx-freetype-platform` (desktop/android) | `1.13.1` | Native FreeType rendering binaries for desktop and Android. |
| **LWJGL3 Desktop Backend** | `com.badlogicgames.gdx:gdx-backend-lwjgl3` | `1.13.1` | Desktop windowing backend supporting 60 FPS V-Sync, hardware MSAA (4x Anti-Aliasing), and multi-OS window management. |
| **Android Backend** *(Newly Added)* | `com.badlogicgames.gdx:gdx-backend-android` | `1.13.1` | Android application backend supporting immersive mode, touch input multiplexing, and mobile lifecycle. |
| **Android Gradle Plugin** *(Newly Added)* | `com.android.tools.build:gradle` | `8.2.2` | Gradle build tools for compiling the Android module and generating release APKs. |
| **Procedural Audio Synthesizer** | `SoundEffects.java` *(Custom DSP)* | - | Custom 44.1kHz 16-bit PCM WAV audio synthesizer delivering authentic paper stationery acoustics (tactile pencil strokes, graphite note ticks, eraser friction, paper card taps, and victory fanfare). |

---

## 🚀 How to Run & Download Builds

### 💻 Running Locally (Development)
- **Windows**: Run `.\gradlew desktop:run` or double-click [run.bat](run.bat).
- **macOS / Linux**: Run `./gradlew desktop:run`.
- **IntelliJ IDEA**: Select **DesktopLauncher** from the top Run configurations dropdown and click **Run ▶**.
- **Android**: Connect an Android device or start an emulator and run `.\gradlew android:installDebug android:run`.

---

### 📦 Automatic Builds from GitHub Actions
Whenever code is pushed, GitHub Actions automatically builds and uploads release packages:
1. Go to the [GitHub Actions tab](https://github.com/aknkya/TargetingGame/actions).
2. Click on the latest workflow run.
3. Under **Artifacts** at the bottom, download:
   - **`SudokuMaster-Android-APK`**: Installable `.apk` for Android devices.
   - **`SudokuMaster-macOS-App`**: Self-contained native macOS `.app` bundle.
   - **`SudokuMaster-Universal-Desktop-JAR`**: Cross-platform executable `.jar` for Windows, macOS, and Linux.
