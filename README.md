# Sudoku Master - Modern Glass Edition

A modern, high-performance 9x9 Sudoku game built with **Java 21** and the **LibGDX** ecosystem. Features dynamic 2D lighting, real-time rigid body physics celebration, anti-aliased vector rendering, procedural audio synthesis, candidate pencil notes, and multiple difficulty levels.

---

## 🎮 Game Controls & Features

### 🕹️ Controls
- **Cell Selection**: Click any cell on the 9x9 board, or navigate using **Arrow Keys [↑ ↓ ← →]**.
- **Number Input**: Click the on-screen **NumPad (1-9)** or press **[1] - [9]** on your keyboard.
- **Pencil Notes (Not Modu)**: Toggle on-screen **✎ Not** button, or press **[Space]** / **[N]** to write candidate mini-numbers (1-9) within empty cells.
- **Erase (Sil)**: Click **⌫ Sil** or press **[Backspace]** / **[Del]** to clear the selected cell or its notes.
- **Undo (Geri Al)**: Click **↺ Geri Al** or press **[Z]** / **[U]** to undo previous moves and notes.
- **Hint (İpucu)**: Click **💡 İpucu** to reveal the correct solution for the selected cell.
- **Pause & Menu**: Press **[ESC]** or click **[⏸ MENÜ]** to pause, restart, or return to the main menu.
- **Audio Toggle**: Press **[M]** or click **[🔊 SES]** to mute/unmute.

### 🌟 Gameplay Features
- **4 Difficulty Levels**:
  - **Kolay (Easy)**: Relaxed puzzle with ~36 clues removed.
  - **Orta (Medium)**: Balanced challenge with ~46 clues removed.
  - **Zor (Hard)**: Advanced techniques required with ~52 clues removed.
  - **Uzman (Expert)**: For master solvers with ~56 clues removed.
- **Smart Highlighting**:
  - Peer cell highlighting (row, column, 3x3 block).
  - Identical number highlighting across the entire board.
  - Real-time conflict checking (invalid placements highlighted in red).
- **Mistakes Limit**: 3 strikes system with game-over protection and retry options.
- **Dynamic Timer**: Running timer in `00:00` format.
- **Remaining Count Badges**: NumPad displays remaining occurrences needed for each digit (1-9).

---

## 🛠️ Libraries Used & Roles

| Library | Role in Sudoku Master |
| :--- | :--- |
| **LibGDX Core** (`com.badlogicgames.gdx:gdx`) | Game engine lifecycle, viewport handling, input processing, and sprite batching. |
| **LWJGL3 Backend** (`gdx-backend-lwjgl3`) | Desktop window manager, 60 FPS V-Sync, and 4x MSAA Anti-Aliasing. |
| **ShapeDrawer** (`space.earlygrey:shapedrawer`) | Renders the 9x9 grid, thick 3x3 block borders, rounded buttons, glass cards, and selection glows. |
| **Box2D Physics** (`gdx-box2d`) | Simulates the dynamic **Victory Celebration**: 65+ physical confetti cubes tumbling and bouncing with realistic gravity! |
| **Box2DLights** (`box2dlights`) | Real-time 2D **cursor lighting**: a soft neon PointLight tracks mouse movement and cell selection with dynamic shadows. |
| **Gdx FreeType** (`gdx-freetype`) | Rasterizes crisp TrueType typography (`segoeui.ttf`) for digits, candidate notes, and HUD typography with full Turkish character support. |
| **Procedural Audio** (`SoundEffects.java`) | 44.1kHz 16-bit sound synthesizer in code: tactile number placement pop, pencil tick, erase whoosh, conflict warning buzz, and victory fanfare. |

---

## 🚀 How to Run & Download Builds

### 📦 Automatic Builds from GitHub Actions
Whenever code is pushed, GitHub Actions automatically builds and uploads release packages:
1. Go to the [GitHub Actions tab](https://github.com/aknkya/TargetingGame/actions).
2. Click on the latest workflow run.
3. Under **Artifacts** at the bottom, download:
   - **`SudokuMaster-Android-APK`**: Installable `.apk` for Android devices.
   - **`SudokuMaster-macOS-App`**: Self-contained native macOS `.app` bundle (no Java installation required, includes Apple Silicon & Intel support).
   - **`SudokuMaster-Universal-Desktop-JAR`**: Cross-platform executable `.jar` for Windows, macOS, and Linux.

---

### 💻 Running Locally (Development)
- **Windows**: Double-click [run.bat](run.bat) or run `.\gradlew desktop:run`.
- **macOS / Linux**: Run `./gradlew desktop:run`.
- **IntelliJ IDEA**: Select **DesktopLauncher** from the top Run configurations dropdown and click **Run ▶**.
