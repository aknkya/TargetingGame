# TargetingGame - 2D Physics Demolition Game

A 2D physics-based destruction game built with **Java 21** and the **LibGDX** framework. It features slingshot mechanics, realistic rigid body physics, real-time dynamic 2D lighting and shadows, high-performance batch rendering, and visual effects like impact particles and camera shake.

---

## 🎮 Game Controls & Mechanics

- **Aim & Launch**: Click and hold the ball, drag backwards to aim, then release to launch.
- **Demolition**: Demolish multi-tier stone and wooden fortress towers and hit the golden crown block.
- **Auto-Respawn**: Once the ball stops moving for 1 second, it automatically resets to the launch pedestal so you can fire again.
- **Rebuild [R]**: Press `[R]` at any time to rebuild all towers from scratch and reset the ball.

---

## 🛠️ Libraries Used in This Project

| Library | Group / Artifact | Version | Role & Functionality |
| :--- | :--- | :--- | :--- |
| **LibGDX Core** | `com.badlogicgames.gdx:gdx` | `1.13.1` | The core game engine framework providing math utilities, asset management, input handling, and OpenGL graphics abstraction. |
| **LWJGL3 Backend** | `com.badlogicgames.gdx:gdx-backend-lwjgl3` | `1.13.1` | Desktop platform backend using LWJGL3/GLFW. Configured with **4x MSAA (Anti-Aliasing)** and V-Sync for smooth graphics. |
| **Box2D Physics** | `com.badlogicgames.gdx:gdx-box2d` | `1.13.1` | Industry-standard 2D rigid-body physics engine. Simulates gravity, collision restitution (bounce), friction, rotational torque, and domino collapse of tower blocks. |
| **Box2D Contact Listener** | *Built-in Box2D feature* | — | Detects high-velocity impacts between the ball and blocks, triggering procedural spark/dust particles and screen shake. |
| **Box2DLights** | `com.badlogicgames.box2dlights:box2dlights` | `1.5` | Dynamic 2D ray-cast lighting engine. Provides an amber `PointLight` attached to the ball and moonlight illuminating the fort, casting **real-time dynamic shadows** behind blocks. |
| **ShapeDrawer** | `space.earlygrey:shapedrawer` | `2.6.0` | Ultra-fast shape and vector rendering integrated directly into `SpriteBatch`. Eliminates costly OpenGL state switching caused by standard `ShapeRenderer`. |
| **Gdx FreeType** | `com.badlogicgames.gdx:gdx-freetype` | `1.13.1` | Vector TrueType (`.ttf`) font rasterizer. Renders crisp, anti-aliased HUD typography at any resolution without pixelation. |
| **Procedural Audio** | *Custom Audio Synthesizer* | — | Built-in 44.1kHz 16-bit procedural WAV synthesizer ([`SoundEffects.java`](src/main/java/org/example/SoundEffects.java)). Generates dynamic whoosh launches, stereo-panned physics impact thuds, and respawn chimes without external audio files. |

---

## 📚 Other Popular LibGDX Libraries (Not Yet Used in This Project)

Here are the most widely used libraries in the LibGDX ecosystem for further enhancing graphics, audio, UI, and architecture:

### 1. Visual & Post-Processing Effects
- **[gdx-vfx](https://github.com/crashinvaders/gdx-vfx)**: A flexible post-processing shader pipeline. Add cinematic effects like **Bloom (glow)**, Motion Blur, Vignette, Chromatic Aberration, and CRT retro scanlines with one line of code.
- **[Universal Tween Engine](https://github.com/AurelienRibon/universal-tween-engine)**: Smoothly animates any numeric property (positions, scale, alpha, colors) with over 30 easing functions (Elastic, Bounce, Cubic, Sine). Ideal for UI pop-ups and ball squash & stretch.
- **[Spine Runtimes (spine-libgdx)](https://esotericsoftware.com/)**: The industry standard for 2D skeletal character animation. Smooth 60fps animations with mesh deformation without heavy sprite sheet memory.

### 2. UI & Scene Management
- **[VisUI](https://github.com/kotcrab/vis-ui)**: A complete, modern flat UI toolkit built on top of LibGDX `Scene2D.UI`. Offers ready-made dialogs, file pickers, color pickers, tooltips, and tabbed panes.
- **[Scene2D Actions](https://libgdx.com/wiki/graphics/2d/scene2d/actions)** *(Built-in)*: Composite and sequence action triggers (`Actions.sequence(...)`, `Actions.fadeIn(...)`, `Actions.moveBy(...)`).

### 3. Artificial Intelligence & Architecture
- **[Ashley](https://github.com/libgdx/ashley)**: An official Entity-Component-System (ECS) framework. Excellent for scaling game architecture by decoupling game data (Components) from game logic (Systems).
- **[gdx-ai](https://github.com/libgdx/gdx-ai)**: Artificial intelligence module providing A* pathfinding, steering behaviors (flocking, seek, arrive), state machines, and behavior trees.

### 4. Audio & Sound Design
- **[gdx-soloud](https://github.com/raeleus/gdx-soloud)** or **[gdx-fmod](https://github.com/fmod/libgdx-fmod)**: High-performance 3D positional audio and sound effects with dynamic filters (echo, low-pass, environmental reverb).

### 5. Networking & Multiplayer
- **[KryoNet](https://github.com/EsotericSoftware/kryonet)**: High-performance TCP/UDP client-server networking library tailored for Java games.

---

## 🚀 How to Run & Build

### 📱 Android APK (Download from GitHub Actions)
1. Go to the **Actions** tab on GitHub: [TargetingGame Actions](https://github.com/aknkya/TargetingGame/actions).
2. Click on the latest workflow run.
3. Under the **Artifacts** section at the bottom, download **`TargetingGame-Debug-APK`**.
4. Transfer and install the `.apk` on your Android phone!

### 💻 Desktop (Windows / Mac / Linux)
- **Option 1**: Double-click `run.bat` (Windows).
- **Option 2**: Run `./gradlew desktop:run` in terminal.
- **Option 3**: Open in IntelliJ IDEA and run `desktop/src/main/java/org/example/DesktopLauncher.java`.
