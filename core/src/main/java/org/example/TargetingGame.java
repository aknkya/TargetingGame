package org.example;

import box2dLight.PointLight;
import box2dLight.RayHandler;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import space.earlygrey.shapedrawer.ShapeDrawer;

import java.util.ArrayList;
import java.util.List;

/**
 * Sudoku Master - Modern Glass Edition
 * Complete Sudoku game refactored using LibGDX, Box2D, Box2DLights, ShapeDrawer, and FreeType.
 */
public class TargetingGame extends ApplicationAdapter {

    public static final float SCREEN_WIDTH = 1280f;
    public static final float SCREEN_HEIGHT = 720f;
    public static final float PPM = 32f; // Pixels Per Meter for Box2D

    public enum GameState {
        MAIN_MENU,
        PLAYING,
        HOW_TO_PLAY,
        PAUSED,
        VICTORY,
        GAME_OVER
    }

    // Core Rendering
    private OrthographicCamera camera;
    private OrthographicCamera box2dCamera;
    private Viewport viewport;
    private SpriteBatch batch;
    private ShapeDrawer drawer;
    private Texture pixelTexture;
    private final GlyphLayout layout = new GlyphLayout();

    // Box2D Physics & Confetti Celebration
    private World world;
    private Body groundBody;
    private final Array<Body> confettiBodies = new Array<>();
    private final List<Color> confettiColors = new ArrayList<>();

    // Box2DLights
    private RayHandler rayHandler;
    private PointLight cursorLight;
    private PointLight boardAmbientLight;

    // Procedural Audio
    private SoundEffects sfx;

    // Sudoku Engine
    private SudokuEngine sudoku;
    private GameState state = GameState.MAIN_MENU;

    // Grid Layout
    private final float boardSize = 540f;
    private final float cellSize = boardSize / 9f; // 60px
    private final float boardX = 130f;
    private final float boardY = 85f;

    // Selection & Interaction
    private int selectedRow = 4;
    private int selectedCol = 4;

    // Fonts
    private BitmapFont titleFont;
    private BitmapFont headerFont;
    private BitmapFont digitFont;
    private BitmapFont noteFont;
    private BitmapFont buttonFont;
    private BitmapFont smallFont;

    // Interactive Buttons
    public static class UIElement {
        public float x, y, width, height;
        public String text;
        public String subText;
        public Color baseColor;
        public Color hoverColor;
        public Color borderColor;
        public Color hoverBorderColor;
        public float hoverAnim = 0f;
        public boolean isHovered = false;
        public Runnable onClick;

        public UIElement(float x, float y, float width, float height, String text,
                         Color baseColor, Color hoverColor, Color borderColor, Color hoverBorderColor,
                         Runnable onClick) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.text = text;
            this.baseColor = new Color(baseColor);
            this.hoverColor = new Color(hoverColor);
            this.borderColor = new Color(borderColor);
            this.hoverBorderColor = new Color(hoverBorderColor);
            this.onClick = onClick;
        }

        public boolean contains(float mx, float my) {
            return mx >= x && mx <= x + width && my >= y && my <= y + height;
        }

        public void update(float mx, float my, float dt, SoundEffects sfx) {
            boolean hovered = contains(mx, my);
            if (hovered && !isHovered && sfx != null) {
                sfx.playHover();
            }
            isHovered = hovered;
            float target = isHovered ? 1f : 0f;
            hoverAnim += (target - hoverAnim) * Math.min(1f, 15f * dt);
        }

        public boolean handleClick(float mx, float my, SoundEffects sfx) {
            if (contains(mx, my)) {
                if (sfx != null) sfx.playClick();
                if (onClick != null) onClick.run();
                return true;
            }
            return false;
        }
    }

    private final List<UIElement> menuButtons = new ArrayList<>();
    private final List<UIElement> difficultyButtons = new ArrayList<>();
    private final List<UIElement> inGameButtons = new ArrayList<>();
    private final List<UIElement> numPadButtons = new ArrayList<>();
    private final List<UIElement> modalButtons = new ArrayList<>();

    private SudokuEngine.Difficulty selectedDifficulty = SudokuEngine.Difficulty.MEDIUM;
    private float globalTime = 0f;

    @Override
    public void create() {
        camera = new OrthographicCamera();
        viewport = new FitViewport(SCREEN_WIDTH, SCREEN_HEIGHT, camera);
        viewport.apply();
        camera.position.set(SCREEN_WIDTH / 2f, SCREEN_HEIGHT / 2f, 0);
        camera.update();

        box2dCamera = new OrthographicCamera();
        box2dCamera.setToOrtho(false, SCREEN_WIDTH / PPM, SCREEN_HEIGHT / PPM);

        batch = new SpriteBatch();

        initShapeDrawer();
        initFonts();

        sfx = new SoundEffects();
        sfx.init();

        sudoku = new SudokuEngine();

        initBox2D();
        initLighting();
        buildUI();
        setupInput();
    }

    private void initShapeDrawer() {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        pixelTexture = new Texture(pixmap);
        pixmap.dispose();

        TextureRegion region = new TextureRegion(pixelTexture, 0, 0, 1, 1);
        drawer = new ShapeDrawer(batch, region);
    }

    private void initFonts() {
        FileHandle fontFile = Gdx.files.internal("fonts/segoeui.ttf");
        if (fontFile.exists()) {
            FreeTypeFontGenerator gen = new FreeTypeFontGenerator(fontFile);
            FreeTypeFontParameter param = new FreeTypeFontParameter();
            param.minFilter = Texture.TextureFilter.Linear;
            param.magFilter = Texture.TextureFilter.Linear;
            param.characters = FreeTypeFontGenerator.DEFAULT_CHARS
                    + "abcçdefgğhıijklmnoöprsştuüvyzABCÇDEFGĞHIİJKLMNOÖPRSŞTUÜVYZ"
                    + "0123456789.,:;!?()[]{}/+-*=%&'\"_# ";

            param.size = 38;
            titleFont = gen.generateFont(param);

            param.size = 24;
            headerFont = gen.generateFont(param);

            param.size = 28;
            digitFont = gen.generateFont(param);

            param.size = 12;
            noteFont = gen.generateFont(param);

            param.size = 18;
            buttonFont = gen.generateFont(param);

            param.size = 14;
            smallFont = gen.generateFont(param);

            gen.dispose();
        } else {
            titleFont = new BitmapFont();
            headerFont = new BitmapFont();
            digitFont = new BitmapFont();
            noteFont = new BitmapFont();
            buttonFont = new BitmapFont();
            smallFont = new BitmapFont();
        }
    }

    private void initBox2D() {
        world = new World(new Vector2(0, -26f), true);

        // Ground barrier at bottom for victory confetti
        BodyDef bdef = new BodyDef();
        bdef.type = BodyDef.BodyType.StaticBody;
        bdef.position.set(SCREEN_WIDTH / 2f / PPM, 15f / PPM);
        groundBody = world.createBody(bdef);

        PolygonShape shape = new PolygonShape();
        shape.setAsBox((SCREEN_WIDTH / 2f) / PPM, 15f / PPM);
        groundBody.createFixture(shape, 0f);
        shape.dispose();
    }

    private void initLighting() {
        RayHandler.useDiffuseLight(true);
        rayHandler = new RayHandler(world);
        rayHandler.setAmbientLight(0.25f, 0.28f, 0.35f, 0.70f);
        rayHandler.setBlurNum(2);

        // Dynamic light attached to mouse / selected cell
        cursorLight = new PointLight(rayHandler, 48, new Color(0.25f, 0.80f, 1.0f, 0.75f), 10.5f, 0, 0);
        cursorLight.setSoft(true);

        // Ambient board spotlight
        boardAmbientLight = new PointLight(rayHandler, 64, new Color(1.0f, 0.80f, 0.40f, 0.45f), 18f,
                (boardX + boardSize / 2f) / PPM, (boardY + boardSize / 2f) / PPM);
        boardAmbientLight.setSoft(true);
    }

    private void triggerVictoryCelebration() {
        // Clear previous confetti
        for (Body b : confettiBodies) {
            world.destroyBody(b);
        }
        confettiBodies.clear();
        confettiColors.clear();

        Color[] palette = {
                new Color(0.2f, 0.85f, 1.0f, 1f),
                new Color(1.0f, 0.75f, 0.2f, 1f),
                new Color(0.2f, 1.0f, 0.6f, 1f),
                new Color(1.0f, 0.3f, 0.5f, 1f),
                new Color(0.8f, 0.4f, 1.0f, 1f)
        };

        // Spawn 65 dynamic Box2D confetti cubes
        for (int i = 0; i < 65; i++) {
            BodyDef bdef = new BodyDef();
            bdef.type = BodyDef.BodyType.DynamicBody;
            float spawnX = MathUtils.random(boardX - 40f, boardX + boardSize + 40f);
            float spawnY = MathUtils.random(SCREEN_HEIGHT - 60f, SCREEN_HEIGHT + 150f);
            bdef.position.set(spawnX / PPM, spawnY / PPM);

            Body body = world.createBody(bdef);

            PolygonShape box = new PolygonShape();
            float halfSize = MathUtils.random(6f, 12f) / PPM;
            box.setAsBox(halfSize, halfSize);

            FixtureDef fdef = new FixtureDef();
            fdef.shape = box;
            fdef.density = 1.0f;
            fdef.friction = 0.5f;
            fdef.restitution = 0.65f;
            body.createFixture(fdef);
            box.dispose();

            body.setLinearVelocity(MathUtils.random(-8f, 8f), MathUtils.random(-5f, 2f));
            body.setAngularVelocity(MathUtils.random(-12f, 12f));

            confettiBodies.add(body);
            confettiColors.add(palette[i % palette.length]);
        }

        sfx.playVictory();
    }

    private void buildUI() {
        menuButtons.clear();
        difficultyButtons.clear();
        inGameButtons.clear();
        numPadButtons.clear();
        modalButtons.clear();

        // --- 0. DIFFICULTY SELECTOR BUTTONS (Main Menu) ---
        float diffW = 86f;
        float diffH = 36f;
        float cardW = 460f;
        float cardH = 560f;
        float cx = (SCREEN_WIDTH - cardW) / 2f;
        float cy = (SCREEN_HEIGHT - cardH) / 2f;
        float diffStartX = cx + (cardW - (diffW * 4 + 24f)) / 2f;
        float diffY = cy + cardH - 155f;

        for (SudokuEngine.Difficulty diff : SudokuEngine.Difficulty.values()) {
            final SudokuEngine.Difficulty d = diff;
            float dx = diffStartX + diff.ordinal() * (diffW + 8f);
            difficultyButtons.add(new UIElement(
                    dx, diffY, diffW, diffH, d.label,
                    new Color(0.10f, 0.14f, 0.20f, 0.85f),
                    new Color(0.18f, 0.35f, 0.55f, 0.95f),
                    new Color(0.24f, 0.35f, 0.50f, 0.60f),
                    new Color(0.40f, 0.75f, 1.0f, 0.90f),
                    () -> {
                        selectedDifficulty = d;
                        sfx.playClick();
                    }
            ));
        }

        // --- 1. MAIN MENU BUTTONS ---
        float menuW = 340f;
        float menuH = 48f;
        float menuX = (SCREEN_WIDTH - menuW) / 2f;
        float startY = 320f;
        float gap = 60f;

        // PLAY
        menuButtons.add(new UIElement(
                menuX, startY, menuW, menuH, "PLAY",
                new Color(0.10f, 0.32f, 0.30f, 0.90f), new Color(0.14f, 0.70f, 0.56f, 0.95f),
                new Color(0.25f, 0.85f, 0.70f, 0.70f), new Color(0.40f, 1.0f, 0.85f, 1.0f),
                () -> {
                    sudoku.startNewGame(selectedDifficulty);
                    if (inGameButtons.size() > 3) {
                        inGameButtons.get(3).text = selectedDifficulty.label;
                    }
                    state = GameState.PLAYING;
                }
        ));

        // HOW TO PLAY
        menuButtons.add(new UIElement(
                menuX, startY - gap, menuW, menuH, "HOW TO PLAY",
                new Color(0.11f, 0.16f, 0.24f, 0.90f), new Color(0.18f, 0.32f, 0.48f, 0.95f),
                new Color(0.28f, 0.52f, 0.78f, 0.65f), new Color(0.45f, 0.75f, 1.0f, 0.95f),
                () -> state = GameState.HOW_TO_PLAY
        ));

        // AUDIO TOGGLE
        menuButtons.add(new UIElement(
                menuX, startY - gap * 2, menuW, menuH, sfx.isSoundEnabled() ? "SOUND: ON" : "SOUND: OFF",
                new Color(0.11f, 0.16f, 0.24f, 0.90f), new Color(0.22f, 0.30f, 0.44f, 0.95f),
                new Color(0.35f, 0.45f, 0.62f, 0.65f), new Color(0.55f, 0.70f, 0.95f, 0.95f),
                this::toggleAudio
        ));

        // QUIT
        menuButtons.add(new UIElement(
                menuX, startY - gap * 3, menuW, menuH, "QUIT",
                new Color(0.18f, 0.10f, 0.12f, 0.85f), new Color(0.65f, 0.16f, 0.22f, 0.92f),
                new Color(0.60f, 0.22f, 0.28f, 0.60f), new Color(1.0f, 0.35f, 0.45f, 0.95f),
                () -> Gdx.app.exit()
        ));

        // --- 2. IN-GAME TOP HUD PILLS ---
        inGameButtons.add(new UIElement(
                25f, SCREEN_HEIGHT - 54f, 120f, 38f, "MENU",
                new Color(0.08f, 0.12f, 0.18f, 0.85f), new Color(0.18f, 0.30f, 0.45f, 0.95f),
                new Color(0.28f, 0.48f, 0.70f, 0.60f), new Color(0.40f, 0.75f, 1.0f, 0.90f),
                () -> state = GameState.PAUSED
        ));

        inGameButtons.add(new UIElement(
                SCREEN_WIDTH - 280f, SCREEN_HEIGHT - 54f, 120f, 38f, sfx.isSoundEnabled() ? "SOUND: ON" : "SOUND: OFF",
                new Color(0.08f, 0.12f, 0.18f, 0.85f), new Color(0.18f, 0.30f, 0.45f, 0.95f),
                new Color(0.28f, 0.48f, 0.70f, 0.60f), new Color(0.40f, 0.75f, 1.0f, 0.90f),
                this::toggleAudio
        ));

        inGameButtons.add(new UIElement(
                SCREEN_WIDTH - 145f, SCREEN_HEIGHT - 54f, 120f, 38f, "+ NEW",
                new Color(0.10f, 0.28f, 0.26f, 0.85f), new Color(0.14f, 0.62f, 0.50f, 0.95f),
                new Color(0.22f, 0.75f, 0.62f, 0.60f), new Color(0.35f, 0.95f, 0.80f, 0.90f),
                () -> {
                    sudoku.startNewGame(selectedDifficulty);
                    if (inGameButtons.size() > 3) {
                        inGameButtons.get(3).text = selectedDifficulty.label;
                    }
                }
        ));

        // Clickable In-Game Difficulty Pill (cycles difficulty!)
        float midX = SCREEN_WIDTH / 2f;
        float headerY = SCREEN_HEIGHT - 32f;
        inGameButtons.add(new UIElement(
                midX - 220f, headerY - 14f, 96f, 28f, selectedDifficulty.label,
                new Color(0.14f, 0.26f, 0.40f, 0.85f), new Color(0.20f, 0.42f, 0.65f, 0.95f),
                new Color(0.35f, 0.70f, 1.0f, 0.85f), new Color(0.55f, 0.85f, 1.0f, 1.0f),
                () -> {
                    int nextIdx = (selectedDifficulty.ordinal() + 1) % SudokuEngine.Difficulty.values().length;
                    selectedDifficulty = SudokuEngine.Difficulty.values()[nextIdx];
                    sudoku.startNewGame(selectedDifficulty);
                    if (inGameButtons.size() > 3) {
                        inGameButtons.get(3).text = selectedDifficulty.label;
                    }
                    sfx.playClick();
                }
        ));

        // --- 3. CONTROL PANEL & NUMPAD (Right Side: x = 740 to 1180) ---
        float panelX = 735f;
        float actBtnW = 100f;
        float actBtnH = 50f;
        float actGap = 12f;
        float actY = 560f;

        // Undo
        inGameButtons.add(new UIElement(
                panelX, actY, actBtnW, actBtnH, "UNDO",
                new Color(0.10f, 0.14f, 0.22f, 0.85f), new Color(0.18f, 0.28f, 0.44f, 0.95f),
                new Color(0.28f, 0.45f, 0.68f, 0.60f), new Color(0.45f, 0.75f, 1.0f, 0.95f),
                () -> {
                    if (sudoku.undo()) sfx.playPlace();
                }
        ));

        // Pencil / Note Mode Toggle
        inGameButtons.add(new UIElement(
                panelX + (actBtnW + actGap), actY, actBtnW, actBtnH, "NOTES",
                new Color(0.10f, 0.14f, 0.22f, 0.85f), new Color(0.28f, 0.45f, 0.68f, 0.95f),
                new Color(0.28f, 0.45f, 0.68f, 0.60f), new Color(1.0f, 0.75f, 0.25f, 1.0f),
                () -> {
                    sudoku.togglePencilMode();
                    sfx.playClick();
                }
        ));

        // Erase
        inGameButtons.add(new UIElement(
                panelX + (actBtnW + actGap) * 2, actY, actBtnW, actBtnH, "ERASE",
                new Color(0.16f, 0.11f, 0.14f, 0.85f), new Color(0.45f, 0.18f, 0.25f, 0.95f),
                new Color(0.55f, 0.25f, 0.35f, 0.60f), new Color(0.95f, 0.40f, 0.55f, 0.95f),
                () -> {
                    if (sudoku.erase(selectedRow, selectedCol)) sfx.playErase();
                }
        ));

        // Hint
        inGameButtons.add(new UIElement(
                panelX + (actBtnW + actGap) * 3, actY, actBtnW, actBtnH, "HINT",
                new Color(0.20f, 0.18f, 0.10f, 0.85f), new Color(0.55f, 0.45f, 0.14f, 0.95f),
                new Color(0.70f, 0.55f, 0.20f, 0.60f), new Color(1.0f, 0.82f, 0.25f, 0.95f),
                () -> {
                    if (sudoku.giveHint(selectedRow, selectedCol)) sfx.playVictory();
                }
        ));

        // 3x3 NumPad (Digits 1-9)
        float numW = 132f;
        float numH = 92f;
        float numGapX = 20f;
        float numGapY = 16f;
        float numStartY = 410f;

        for (int i = 0; i < 9; i++) {
            final int digit = i + 1;
            int r = i / 3;
            int c = i % 3;
            float nx = panelX + c * (numW + numGapX);
            float ny = numStartY - r * (numH + numGapY);

            numPadButtons.add(new UIElement(
                    nx, ny, numW, numH, String.valueOf(digit),
                    new Color(0.09f, 0.13f, 0.20f, 0.88f), new Color(0.15f, 0.35f, 0.55f, 0.96f),
                    new Color(0.22f, 0.40f, 0.62f, 0.65f), new Color(0.35f, 0.75f, 1.0f, 0.95f),
                    () -> handleNumberInput(digit)
            ));
        }

        // Modal Close Button
        modalButtons.add(new UIElement(
                (SCREEN_WIDTH - 240f) / 2f, 175f, 240f, 46f, "GOT IT, PLAY!",
                new Color(0.10f, 0.32f, 0.28f, 0.92f), new Color(0.14f, 0.70f, 0.56f, 0.96f),
                new Color(0.25f, 0.85f, 0.70f, 0.75f), new Color(0.45f, 1.0f, 0.88f, 1.0f),
                () -> {
                    sudoku.startNewGame(selectedDifficulty);
                    state = GameState.PLAYING;
                }
        ));
    }

    private void handleNumberInput(int digit) {
        if (state != GameState.PLAYING) return;
        boolean wasPencil = sudoku.isPencilMode();
        boolean placed = sudoku.setNumber(selectedRow, selectedCol, digit);

        if (placed) {
            if (wasPencil) {
                sfx.playPencil();
            } else {
                if (sudoku.isConflict(selectedRow, selectedCol)) {
                    sfx.playError();
                } else {
                    sfx.playPlace();
                }
            }

            if (sudoku.isCompleted()) {
                state = GameState.VICTORY;
                triggerVictoryCelebration();
            } else if (sudoku.isGameOver()) {
                state = GameState.GAME_OVER;
                sfx.playError();
            }
        }
    }

    private void toggleAudio() {
        sfx.toggleSound();
        String lbl = sfx.isSoundEnabled() ? "SOUND: ON" : "SOUND: OFF";
        if (menuButtons.size() > 2) menuButtons.get(2).text = lbl;
        if (inGameButtons.size() > 1) inGameButtons.get(1).text = lbl;
    }

    private void setupInput() {
        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean mouseMoved(int screenX, int screenY) {
                Vector3 worldCoords = viewport.unproject(new Vector3(screenX, screenY, 0));
                updateHover(worldCoords.x, worldCoords.y, Gdx.graphics.getDeltaTime());
                return false;
            }

            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                Vector3 worldCoords = viewport.unproject(new Vector3(screenX, screenY, 0));
                float mx = worldCoords.x;
                float my = worldCoords.y;

                if (state == GameState.MAIN_MENU) {
                    for (UIElement btn : difficultyButtons) {
                        if (btn.handleClick(mx, my, sfx)) return true;
                    }
                    for (UIElement btn : menuButtons) {
                        if (btn.handleClick(mx, my, sfx)) return true;
                    }
                    return true;
                }

                if (state == GameState.HOW_TO_PLAY) {
                    for (UIElement btn : modalButtons) {
                        if (btn.handleClick(mx, my, sfx)) return true;
                    }
                    return true;
                }

                if (state == GameState.PAUSED) {
                    handlePauseClick(mx, my);
                    return true;
                }

                if (state == GameState.VICTORY || state == GameState.GAME_OVER) {
                    handleEndgameClick(mx, my);
                    return true;
                }

                if (state == GameState.PLAYING) {
                    // Check HUD buttons
                    for (UIElement btn : inGameButtons) {
                        if (btn.handleClick(mx, my, sfx)) return true;
                    }

                    // Check NumPad
                    for (UIElement btn : numPadButtons) {
                        if (btn.handleClick(mx, my, sfx)) return true;
                    }

                    // Check Sudoku Board Cell Click
                    if (mx >= boardX && mx <= boardX + boardSize && my >= boardY && my <= boardY + boardSize) {
                        int col = (int) ((mx - boardX) / cellSize);
                        int row = (int) ((boardSize - (my - boardY)) / cellSize); // Invert Y
                        if (row >= 0 && row < 9 && col >= 0 && col < 9) {
                            selectedRow = row;
                            selectedCol = col;
                            sfx.playHover();
                            return true;
                        }
                    }
                }

                return false;
            }

            @Override
            public boolean keyDown(int keycode) {
                if (keycode == Input.Keys.ESCAPE) {
                    if (state == GameState.PLAYING) state = GameState.PAUSED;
                    else if (state == GameState.PAUSED) state = GameState.PLAYING;
                    else if (state == GameState.HOW_TO_PLAY) state = GameState.MAIN_MENU;
                    return true;
                }

                if (keycode == Input.Keys.M) {
                    toggleAudio();
                    return true;
                }

                if (state == GameState.PLAYING) {
                    // Keyboard Grid Navigation
                    if (keycode == Input.Keys.UP && selectedRow > 0) { selectedRow--; return true; }
                    if (keycode == Input.Keys.DOWN && selectedRow < 8) { selectedRow++; return true; }
                    if (keycode == Input.Keys.LEFT && selectedCol > 0) { selectedCol--; return true; }
                    if (keycode == Input.Keys.RIGHT && selectedCol < 8) { selectedCol++; return true; }

                    // Number Input
                    if (keycode >= Input.Keys.NUM_1 && keycode <= Input.Keys.NUM_9) {
                        handleNumberInput(keycode - Input.Keys.NUM_1 + 1);
                        return true;
                    }
                    if (keycode >= Input.Keys.NUMPAD_1 && keycode <= Input.Keys.NUMPAD_9) {
                        handleNumberInput(keycode - Input.Keys.NUMPAD_1 + 1);
                        return true;
                    }

                    // Erase
                    if (keycode == Input.Keys.BACKSPACE || keycode == Input.Keys.DEL) {
                        if (sudoku.erase(selectedRow, selectedCol)) sfx.playErase();
                        return true;
                    }

                    // Toggle Pencil mode with SPACE or N
                    if (keycode == Input.Keys.SPACE || keycode == Input.Keys.N) {
                        sudoku.togglePencilMode();
                        sfx.playClick();
                        return true;
                    }

                    // Undo with Z or U
                    if (keycode == Input.Keys.Z || keycode == Input.Keys.U) {
                        if (sudoku.undo()) sfx.playPlace();
                        return true;
                    }
                }

                return false;
            }
        });
    }

    private void handlePauseClick(float mx, float my) {
        float cardW = 380f;
        float cardH = 340f;
        float cx = (SCREEN_WIDTH - cardW) / 2f;
        float cy = (SCREEN_HEIGHT - cardH) / 2f;

        // Resume button
        if (mx >= cx + 40f && mx <= cx + cardW - 40f && my >= cy + 180f && my <= cy + 230f) {
            state = GameState.PLAYING;
            sfx.playClick();
        }
        // New Game
        else if (mx >= cx + 40f && mx <= cx + cardW - 40f && my >= cy + 115f && my <= cy + 165f) {
            sudoku.startNewGame(selectedDifficulty);
            state = GameState.PLAYING;
            sfx.playClick();
        }
        // Main Menu
        else if (mx >= cx + 40f && mx <= cx + cardW - 40f && my >= cy + 50f && my <= cy + 100f) {
            state = GameState.MAIN_MENU;
            sfx.playClick();
        }
    }

    private void handleEndgameClick(float mx, float my) {
        float btnW = 240f;
        float btnH = 48f;
        float bx = (SCREEN_WIDTH - btnW) / 2f;
        float by = 200f;

        if (mx >= bx && mx <= bx + btnW && my >= by && my <= by + btnH) {
            sudoku.startNewGame(selectedDifficulty);
            state = GameState.PLAYING;
            sfx.playClick();
        }
    }

    private void updateHover(float mx, float my, float dt) {
        if (state == GameState.MAIN_MENU) {
            for (UIElement btn : difficultyButtons) btn.update(mx, my, dt, sfx);
            for (UIElement btn : menuButtons) btn.update(mx, my, dt, sfx);
        } else if (state == GameState.PLAYING) {
            for (UIElement btn : inGameButtons) btn.update(mx, my, dt, sfx);
            for (UIElement btn : numPadButtons) btn.update(mx, my, dt, sfx);
        } else if (state == GameState.HOW_TO_PLAY) {
            for (UIElement btn : modalButtons) btn.update(mx, my, dt, sfx);
        }
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        box2dCamera.setToOrtho(false, width / PPM, height / PPM);
    }

    @Override
    public void render() {
        float dt = Gdx.graphics.getDeltaTime();
        if (dt > 0.1f) dt = 0.1f;
        globalTime += dt;

        Vector3 mouseCoords = viewport.unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));
        updateHover(mouseCoords.x, mouseCoords.y, dt);

        if (state == GameState.PLAYING) {
            sudoku.updateTimer(dt);
        }

        // Step Box2D simulation for victory celebration
        if (state == GameState.VICTORY) {
            world.step(1 / 60f, 6, 2);
        }

        // Move cursor PointLight
        cursorLight.setPosition(mouseCoords.x / PPM, mouseCoords.y / PPM);

        // Clear screen
        Gdx.gl.glClearColor(0.06f, 0.08f, 0.12f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        viewport.apply();
        batch.setProjectionMatrix(camera.combined);

        // --- 1. WORLD & BOARD RENDERING (ShapeDrawer Batch) ---
        batch.begin();

        if (state == GameState.MAIN_MENU) {
            renderMainMenuBackground();
        } else {
            renderGameHeader();
            renderSudokuBoard();
            renderControlPanel();
        }

        // Render falling Box2D confetti cubes
        if (state == GameState.VICTORY) {
            renderConfetti();
        }

        batch.end();

        // --- 2. RENDER BOX2DLIGHTS ---
        box2dCamera.position.set(camera.position.x / PPM, camera.position.y / PPM, 0);
        box2dCamera.update();
        rayHandler.setCombinedMatrix(box2dCamera);
        rayHandler.updateAndRender();

        // --- 3. UI OVERLAYS & MODALS ---
        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        if (state == GameState.MAIN_MENU) {
            renderMainMenuCard();
        } else if (state == GameState.HOW_TO_PLAY) {
            renderHowToPlayModal();
        } else if (state == GameState.PAUSED) {
            renderPauseModal();
        } else if (state == GameState.VICTORY) {
            renderVictoryModal();
        } else if (state == GameState.GAME_OVER) {
            renderGameOverModal();
        }

        batch.end();
    }

    private void renderMainMenuBackground() {
        // Draw subtle ambient grid lines
        drawer.setColor(new Color(0.12f, 0.16f, 0.24f, 0.35f));
        for (int i = 0; i < 15; i++) {
            drawer.line(i * 90f, 0, i * 90f, SCREEN_HEIGHT, 1.5f);
            drawer.line(0, i * 50f, SCREEN_WIDTH, i * 50f, 1.5f);
        }
    }

    private void renderGameHeader() {
        // Top Bar Background
        drawer.setColor(new Color(0.08f, 0.11f, 0.16f, 0.90f));
        drawer.filledRectangle(0, SCREEN_HEIGHT - 65f, SCREEN_WIDTH, 65f);
        drawer.setColor(new Color(0.20f, 0.35f, 0.55f, 0.55f));
        drawer.line(0, SCREEN_HEIGHT - 65f, SCREEN_WIDTH, SCREEN_HEIGHT - 65f, 2f);

        // Render In-Game top HUD buttons (Menu, Sound, New, Difficulty Pill)
        for (int i = 0; i < inGameButtons.size() && i < 4; i++) {
            renderButton(inGameButtons.get(i), i == 3 ? smallFont : buttonFont);
        }

        // Center HUD: Timer + Mistakes
        float midX = SCREEN_WIDTH / 2f;
        float headerY = SCREEN_HEIGHT - 32f;

        // Timer
        headerFont.setColor(0.95f, 0.95f, 1.0f, 1.0f);
        String timeStr = "Time: " + sudoku.getFormattedTime();
        layout.setText(headerFont, timeStr);
        headerFont.draw(batch, timeStr, midX - layout.width / 2f, headerY + layout.height / 2f);

        // Mistakes Pill
        String mistakeStr = "Mistakes: " + sudoku.getMistakes() + "/" + sudoku.getMaxMistakes();
        drawer.setColor(new Color(0.24f, 0.12f, 0.14f, 0.85f));
        drawer.filledRectangle(midX + 130f, headerY - 14f, 130f, 28f);
        drawer.setColor(sudoku.getMistakes() > 0 ? new Color(1.0f, 0.35f, 0.40f, 0.85f) : new Color(0.45f, 0.60f, 0.75f, 0.60f));
        drawer.rectangle(midX + 130f, headerY - 14f, 130f, 28f, 1.5f);
        smallFont.setColor(sudoku.getMistakes() > 0 ? new Color(1.0f, 0.45f, 0.50f, 1.0f) : new Color(0.75f, 0.85f, 0.95f, 1.0f));
        layout.setText(smallFont, mistakeStr);
        smallFont.draw(batch, mistakeStr, midX + 130f + (130f - layout.width) / 2f, headerY + layout.height / 2f);
    }

    private void renderSudokuBoard() {
        // Board Outer Glowing Frame
        drawer.setColor(new Color(0.12f, 0.35f, 0.60f, 0.20f));
        drawer.filledRectangle(boardX - 6f, boardY - 6f, boardSize + 12f, boardSize + 12f);

        drawer.setColor(new Color(0.08f, 0.11f, 0.17f, 0.95f));
        drawer.filledRectangle(boardX, boardY, boardSize, boardSize);

        int selectedVal = sudoku.getCell(selectedRow, selectedCol);

        // Cell Highlighting & Content
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                float cx = boardX + c * cellSize;
                float cy = boardY + (8 - r) * cellSize; // Invert Y
                int val = sudoku.getCell(r, c);

                boolean isSelected = (r == selectedRow && c == selectedCol);
                boolean isSamePeer = (r == selectedRow || c == selectedCol ||
                        (r / 3 == selectedRow / 3 && c / 3 == selectedCol / 3));
                boolean isSameNumber = (selectedVal > 0 && val == selectedVal);
                boolean isConflict = sudoku.isConflict(r, c);

                // Highlight Peer Cells (Row, Col, Box)
                if (isSamePeer && !isSelected) {
                    drawer.setColor(new Color(0.14f, 0.20f, 0.30f, 0.65f));
                    drawer.filledRectangle(cx, cy, cellSize, cellSize);
                }

                // Highlight Same Numbers Across Board
                if (isSameNumber && !isSelected) {
                    drawer.setColor(new Color(0.25f, 0.45f, 0.65f, 0.55f));
                    drawer.filledRectangle(cx, cy, cellSize, cellSize);
                }

                // Highlight Conflicting Cell
                if (isConflict) {
                    drawer.setColor(new Color(0.65f, 0.15f, 0.22f, 0.55f));
                    drawer.filledRectangle(cx, cy, cellSize, cellSize);
                }

                // Highlight Selected Cell
                if (isSelected) {
                    drawer.setColor(new Color(0.18f, 0.48f, 0.75f, 0.85f));
                    drawer.filledRectangle(cx, cy, cellSize, cellSize);
                    drawer.setColor(new Color(0.40f, 0.88f, 1.0f, 0.95f));
                    drawer.rectangle(cx + 1f, cy + 1f, cellSize - 2f, cellSize - 2f, 2.5f);
                }

                // Draw Digits or Candidate Notes
                if (val > 0) {
                    // Number placed
                    if (isConflict) {
                        digitFont.setColor(1.0f, 0.35f, 0.35f, 1.0f); // Conflict red
                    } else if (sudoku.getInitialCell(r, c) > 0) {
                        digitFont.setColor(0.95f, 0.97f, 1.0f, 1.0f); // Given clue silver
                    } else {
                        digitFont.setColor(0.35f, 0.90f, 1.0f, 1.0f); // Player number neon cyan
                    }

                    String digitStr = String.valueOf(val);
                    layout.setText(digitFont, digitStr);
                    digitFont.draw(batch, digitStr, cx + (cellSize - layout.width) / 2f, cy + (cellSize + layout.height) / 2f);
                } else {
                    // Render Pencil Notes (3x3 mini grid)
                    float subCell = cellSize / 3f;
                    for (int n = 1; n <= 9; n++) {
                        if (sudoku.hasNote(r, c, n)) {
                            int nr = (n - 1) / 3;
                            int nc = (n - 1) % 3;
                            float nx = cx + nc * subCell;
                            float ny = cy + (2 - nr) * subCell;

                            noteFont.setColor(0.95f, 0.78f, 0.35f, 0.90f);
                            String noteStr = String.valueOf(n);
                            layout.setText(noteFont, noteStr);
                            noteFont.draw(batch, noteStr, nx + (subCell - layout.width) / 2f, ny + (subCell + layout.height) / 2f);
                        }
                    }
                }
            }
        }

        // Draw Thin Grid Lines (1px)
        drawer.setColor(new Color(0.20f, 0.28f, 0.40f, 0.70f));
        for (int i = 1; i < 9; i++) {
            if (i % 3 != 0) {
                float pos = i * cellSize;
                drawer.line(boardX + pos, boardY, boardX + pos, boardY + boardSize, 1.2f);
                drawer.line(boardX, boardY + pos, boardX + boardSize, boardY + pos, 1.2f);
            }
        }

        // Draw Thick 3x3 Block Borders (3.5px)
        drawer.setColor(new Color(0.35f, 0.65f, 0.95f, 0.85f));
        for (int i = 0; i <= 3; i++) {
            float pos = i * (cellSize * 3);
            drawer.line(boardX + pos, boardY, boardX + pos, boardY + boardSize, 3.5f);
            drawer.line(boardX, boardY + pos, boardX + boardSize, boardY + pos, 3.5f);
        }
    }

    private void renderControlPanel() {
        // Control Panel Action Buttons (Undo, Pencil, Erase, Hint)
        for (int i = 3; i < inGameButtons.size(); i++) {
            UIElement btn = inGameButtons.get(i);
            // Highlight pencil button if active
            if (i == 4 && sudoku.isPencilMode()) {
                btn.borderColor.set(1.0f, 0.75f, 0.25f, 1.0f);
                btn.baseColor.set(0.35f, 0.25f, 0.10f, 0.90f);
            } else if (i == 4) {
                btn.borderColor.set(0.28f, 0.45f, 0.68f, 0.60f);
                btn.baseColor.set(0.10f, 0.14f, 0.22f, 0.85f);
            }
            renderButton(btn, buttonFont);
        }

        // Render 3x3 NumPad
        for (int i = 0; i < numPadButtons.size(); i++) {
            UIElement btn = numPadButtons.get(i);
            int digit = i + 1;
            int remaining = sudoku.getRemainingCount(digit);

            renderButton(btn, digitFont);

            // Remaining count badge
            String remText = remaining > 0 ? remaining + " left" : "Done";
            smallFont.setColor(remaining > 0 ? new Color(0.60f, 0.75f, 0.90f, 0.85f) : new Color(0.35f, 0.85f, 0.55f, 0.90f));
            layout.setText(smallFont, remText);
            smallFont.draw(batch, remText, btn.x + (btn.width - layout.width) / 2f, btn.y + 22f);
        }
    }

    private void renderButton(UIElement btn, BitmapFont font) {
        float lift = btn.hoverAnim * 2f;
        float dy = btn.y + lift;

        Color bg = btn.baseColor.cpy().lerp(btn.hoverColor, btn.hoverAnim);
        Color border = btn.borderColor.cpy().lerp(btn.hoverBorderColor, btn.hoverAnim);

        // Outer glow
        if (btn.hoverAnim > 0.05f) {
            Color glow = btn.hoverBorderColor.cpy();
            glow.a = btn.hoverAnim * 0.25f;
            drawer.setColor(glow);
            drawer.filledRectangle(btn.x - 3f, dy - 3f, btn.width + 6f, btn.height + 6f);
        }

        drawer.setColor(bg);
        drawer.filledRectangle(btn.x, dy, btn.width, btn.height);

        drawer.setColor(border);
        drawer.rectangle(btn.x, dy, btn.width, btn.height, 2f);

        // Futuristic accent pill on left side when hovered
        if (btn.hoverAnim > 0.05f) {
            drawer.setColor(border);
            float pillH = (btn.height - 14f) * btn.hoverAnim;
            drawer.filledRectangle(btn.x + 4f, dy + (btn.height - pillH) / 2f, 4f, pillH);
        }

        // Center Text
        font.setColor(0.95f, 0.97f, 1.0f, 1.0f);
        layout.setText(font, btn.text);
        font.draw(batch, btn.text, btn.x + (btn.width - layout.width) / 2f, dy + (btn.height + layout.height) / 2f);
    }

    private void renderConfetti() {
        for (int i = 0; i < confettiBodies.size; i++) {
            Body b = confettiBodies.get(i);
            Vector2 pos = b.getPosition().scl(PPM);
            float angle = b.getAngle();
            Color c = confettiColors.get(i % confettiColors.size());
            drawer.setColor(c);
            drawer.filledRectangle(pos.x, pos.y, 14f, 14f, angle);
        }
    }

    // --- MODAL DIALOGS ---

    private void renderMainMenuCard() {
        float cardW = 460f;
        float cardH = 560f;
        float cx = (SCREEN_WIDTH - cardW) / 2f;
        float cy = (SCREEN_HEIGHT - cardH) / 2f;

        // Card Glow & Body
        drawer.setColor(new Color(0.15f, 0.45f, 0.75f, 0.22f));
        drawer.filledRectangle(cx - 6f, cy - 6f, cardW + 12f, cardH + 12f);

        drawer.setColor(new Color(0.07f, 0.10f, 0.16f, 0.92f));
        drawer.filledRectangle(cx, cy, cardW, cardH);

        drawer.setColor(new Color(0.25f, 0.65f, 0.95f, 0.85f));
        drawer.rectangle(cx, cy, cardW, cardH, 2.5f);

        // Tag
        smallFont.setColor(0.40f, 0.85f, 1.0f, 1.0f);
        layout.setText(smallFont, "MODERN GLASS EDITION");
        smallFont.draw(batch, "MODERN GLASS EDITION", cx + (cardW - layout.width) / 2f, cy + cardH - 32f);

        // Main Title
        titleFont.setColor(1.0f, 0.78f, 0.25f, 1.0f);
        layout.setText(titleFont, "SUDOKU MASTER");
        titleFont.draw(batch, "SUDOKU MASTER", cx + (cardW - layout.width) / 2f, cy + cardH - 60f);

        // Divider
        drawer.setColor(new Color(0.35f, 0.65f, 0.95f, 0.45f));
        drawer.line(cx + 35f, cy + cardH - 105f, cx + cardW - 35f, cy + cardH - 105f, 2f);

        // Render Difficulty Buttons
        for (UIElement btn : difficultyButtons) {
            boolean isSel = btn.text.equals(selectedDifficulty.label);
            if (isSel) {
                btn.baseColor.set(0.18f, 0.55f, 0.75f, 0.95f);
                btn.borderColor.set(0.45f, 0.90f, 1.0f, 1.0f);
            } else {
                btn.baseColor.set(0.10f, 0.14f, 0.20f, 0.85f);
                btn.borderColor.set(0.24f, 0.35f, 0.50f, 0.60f);
            }
            renderButton(btn, smallFont);
        }

        // Render Menu Buttons
        for (UIElement btn : menuButtons) {
            renderButton(btn, buttonFont);
        }
    }

    private void renderHowToPlayModal() {
        renderModalCard(720f, 480f, "HOW TO PLAY");

        float modalW = 720f;
        float modalH = 480f;
        float cx = (SCREEN_WIDTH - modalW) / 2f;
        float cy = (SCREEN_HEIGHT - modalH) / 2f;

        float cardW = 310f;
        float cardH = 90f;
        float col1 = cx + 35f;
        float col2 = cx + modalW - cardW - 35f;
        float row1 = cy + modalH - 180f;
        float row2 = row1 - 105f;

        renderGuideCard(col1, row1, cardW, cardH, "9x9 SUDOKU RULES", "Each row, column, and 3x3 box", "must contain digits 1-9 with no repeats.");
        renderGuideCard(col2, row1, cardW, cardH, "NOTES MODE (PENCIL)", "Write small candidate notes in empty cells.", "Toggle with Space bar or [N] key.");
        renderGuideCard(col1, row2, cardW, cardH, "HINTS & UNDO", "Stuck? Tap the Hint button for a clue.", "Undo wrong moves anytime with [Z].");
        renderGuideCard(col2, row2, cardW, cardH, "KEYBOARD CONTROLS", "Navigate with Arrow keys, enter 1-9 digits.", "Erase with [Backspace], pause with [ESC].");

        renderButton(modalButtons.get(0), buttonFont);
    }

    private void renderGuideCard(float x, float y, float w, float h, String title, String l1, String l2) {
        drawer.setColor(new Color(0.10f, 0.15f, 0.23f, 0.85f));
        drawer.filledRectangle(x, y, w, h);
        drawer.setColor(new Color(0.24f, 0.40f, 0.60f, 0.65f));
        drawer.rectangle(x, y, w, h, 1.5f);

        buttonFont.setColor(1.0f, 0.78f, 0.25f, 1.0f);
        buttonFont.draw(batch, title, x + 14f, y + h - 14f);

        smallFont.setColor(0.90f, 0.93f, 0.98f, 0.95f);
        smallFont.draw(batch, l1, x + 14f, y + h - 40f);
        smallFont.draw(batch, l2, x + 14f, y + h - 62f);
    }

    private void renderPauseModal() {
        float cardW = 380f;
        float cardH = 340f;
        float cx = (SCREEN_WIDTH - cardW) / 2f;
        float cy = (SCREEN_HEIGHT - cardH) / 2f;

        drawer.setColor(new Color(0.04f, 0.06f, 0.10f, 0.75f));
        drawer.filledRectangle(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);

        drawer.setColor(new Color(0.08f, 0.12f, 0.18f, 0.95f));
        drawer.filledRectangle(cx, cy, cardW, cardH);
        drawer.setColor(new Color(0.35f, 0.70f, 1.0f, 0.85f));
        drawer.rectangle(cx, cy, cardW, cardH, 2.5f);

        headerFont.setColor(0.40f, 0.85f, 1.0f, 1.0f);
        layout.setText(headerFont, "GAME PAUSED");
        headerFont.draw(batch, "GAME PAUSED", cx + (cardW - layout.width) / 2f, cy + cardH - 35f);

        // Buttons: Resume, New Game, Main Menu
        float btnW = 300f;
        float btnH = 50f;
        float bx = cx + 40f;

        drawSimpleModalBtn(bx, cy + 180f, btnW, btnH, "RESUME", new Color(0.14f, 0.65f, 0.52f, 0.95f));
        drawSimpleModalBtn(bx, cy + 115f, btnW, btnH, "+ NEW GAME", new Color(0.18f, 0.35f, 0.52f, 0.95f));
        drawSimpleModalBtn(bx, cy + 50f, btnW, btnH, "MAIN MENU", new Color(0.55f, 0.20f, 0.35f, 0.92f));
    }

    private void renderVictoryModal() {
        float cardW = 440f;
        float cardH = 360f;
        float cx = (SCREEN_WIDTH - cardW) / 2f;
        float cy = (SCREEN_HEIGHT - cardH) / 2f;

        drawer.setColor(new Color(0.04f, 0.06f, 0.10f, 0.60f));
        drawer.filledRectangle(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);

        drawer.setColor(new Color(0.08f, 0.14f, 0.18f, 0.96f));
        drawer.filledRectangle(cx, cy, cardW, cardH);
        drawer.setColor(new Color(0.20f, 1.0f, 0.65f, 0.90f));
        drawer.rectangle(cx, cy, cardW, cardH, 3.0f);

        titleFont.setColor(0.25f, 1.0f, 0.65f, 1.0f);
        layout.setText(titleFont, "CONGRATULATIONS!");
        titleFont.draw(batch, "CONGRATULATIONS!", cx + (cardW - layout.width) / 2f, cy + cardH - 35f);

        headerFont.setColor(Color.WHITE);
        layout.setText(headerFont, "SUDOKU SOLVED!");
        headerFont.draw(batch, "SUDOKU SOLVED!", cx + (cardW - layout.width) / 2f, cy + cardH - 85f);

        // Stats
        String timeInfo = "Completion Time: " + sudoku.getFormattedTime();
        String diffInfo = "Difficulty: " + sudoku.getDifficulty().label;
        String mistInfo = "Mistakes: " + sudoku.getMistakes() + "/" + sudoku.getMaxMistakes();

        buttonFont.setColor(0.85f, 0.92f, 1.0f, 1.0f);
        layout.setText(buttonFont, timeInfo);
        buttonFont.draw(batch, timeInfo, cx + (cardW - layout.width) / 2f, cy + 220f);
        layout.setText(buttonFont, diffInfo);
        buttonFont.draw(batch, diffInfo, cx + (cardW - layout.width) / 2f, cy + 185f);
        layout.setText(buttonFont, mistInfo);
        buttonFont.draw(batch, mistInfo, cx + (cardW - layout.width) / 2f, cy + 150f);

        drawSimpleModalBtn((SCREEN_WIDTH - 240f) / 2f, 200f - 120f, 240f, 48f, "START NEW GAME", new Color(0.14f, 0.70f, 0.56f, 0.95f));
    }

    private void renderGameOverModal() {
        float cardW = 420f;
        float cardH = 300f;
        float cx = (SCREEN_WIDTH - cardW) / 2f;
        float cy = (SCREEN_HEIGHT - cardH) / 2f;

        drawer.setColor(new Color(0.04f, 0.06f, 0.10f, 0.80f));
        drawer.filledRectangle(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);

        drawer.setColor(new Color(0.14f, 0.08f, 0.10f, 0.96f));
        drawer.filledRectangle(cx, cy, cardW, cardH);
        drawer.setColor(new Color(1.0f, 0.35f, 0.40f, 0.90f));
        drawer.rectangle(cx, cy, cardW, cardH, 3.0f);

        titleFont.setColor(1.0f, 0.35f, 0.40f, 1.0f);
        layout.setText(titleFont, "GAME OVER");
        titleFont.draw(batch, "GAME OVER", cx + (cardW - layout.width) / 2f, cy + cardH - 35f);

        headerFont.setColor(Color.WHITE);
        String info = "3 Mistakes Limit Reached!";
        layout.setText(headerFont, info);
        headerFont.draw(batch, info, cx + (cardW - layout.width) / 2f, cy + cardH - 95f);

        drawSimpleModalBtn((SCREEN_WIDTH - 240f) / 2f, 200f - 70f, 240f, 48f, "PLAY AGAIN", new Color(0.70f, 0.20f, 0.30f, 0.95f));
    }

    private void drawSimpleModalBtn(float x, float y, float w, float h, String text, Color col) {
        drawer.setColor(col);
        drawer.filledRectangle(x, y, w, h);
        drawer.setColor(new Color(col.r * 1.3f, col.g * 1.3f, col.b * 1.3f, 1f));
        drawer.rectangle(x, y, w, h, 2f);

        buttonFont.setColor(Color.WHITE);
        layout.setText(buttonFont, text);
        buttonFont.draw(batch, text, x + (w - layout.width) / 2f, y + (h + layout.height) / 2f);
    }

    private void renderModalCard(float w, float h, String title) {
        float cx = (SCREEN_WIDTH - w) / 2f;
        float cy = (SCREEN_HEIGHT - h) / 2f;

        drawer.setColor(new Color(0.04f, 0.06f, 0.10f, 0.80f));
        drawer.filledRectangle(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);

        drawer.setColor(new Color(0.07f, 0.10f, 0.16f, 0.96f));
        drawer.filledRectangle(cx, cy, w, h);
        drawer.setColor(new Color(0.25f, 0.70f, 0.95f, 0.85f));
        drawer.rectangle(cx, cy, w, h, 2.5f);

        titleFont.setColor(0.35f, 0.90f, 0.85f, 1.0f);
        layout.setText(titleFont, title);
        titleFont.draw(batch, title, cx + (w - layout.width) / 2f, cy + h - 35f);

        drawer.setColor(new Color(0.25f, 0.70f, 0.95f, 0.45f));
        drawer.line(cx + 35f, cy + h - 70f, cx + w - 35f, cy + h - 70f, 2f);
    }

    @Override
    public void dispose() {
        if (pixelTexture != null) pixelTexture.dispose();
        if (batch != null) batch.dispose();
        if (titleFont != null) titleFont.dispose();
        if (headerFont != null) headerFont.dispose();
        if (digitFont != null) digitFont.dispose();
        if (noteFont != null) noteFont.dispose();
        if (buttonFont != null) buttonFont.dispose();
        if (smallFont != null) smallFont.dispose();
        if (sfx != null) sfx.dispose();
        if (rayHandler != null) rayHandler.dispose();
        if (world != null) world.dispose();
    }
}
