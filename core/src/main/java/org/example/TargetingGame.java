package org.example;

import box2dLight.PointLight;
import box2dLight.RayHandler;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Preferences;
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
 * Sudoku Master - Multi-Theme Edition (Paper Notebook & Modern Glass)
 * Complete Sudoku game powered by LibGDX, Box2D, Box2DLights, ShapeDrawer, and FreeType.
 */
public class TargetingGame extends ApplicationAdapter {

    public static final float SCREEN_WIDTH = 1280f;
    public static final float SCREEN_HEIGHT = 720f;
    public static final float PPM = 32f; // Pixels Per Meter for Box2D

    public enum GameTheme {
        PAPER("Paper Notebook", "Ruled Paper & Handwriting"),
        MODERN_GLASS("Modern Glass", "Dark Glass & Neon Lights");

        public final String title;
        public final String description;

        GameTheme(String title, String description) {
            this.title = title;
            this.description = description;
        }
    }

    public enum GameState {
        MAIN_MENU,
        PLAYING,
        HOW_TO_PLAY,
        SETTINGS,
        PAUSED,
        VICTORY,
        GAME_OVER
    }

    // Settings & Theme State (Default: PAPER)
    private GameTheme currentTheme = GameTheme.PAPER;
    private GameState state = GameState.MAIN_MENU;
    private GameState stateBeforeSettings = GameState.MAIN_MENU;
    private Preferences prefs;

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
        public Color textColor;
        public Color hoverTextColor;
        public float hoverAnim = 0f;
        public boolean isHovered = false;
        public Runnable onClick;

        public UIElement(float x, float y, float width, float height, String text,
                         Color baseColor, Color hoverColor, Color borderColor, Color hoverBorderColor,
                         Runnable onClick) {
            this(x, y, width, height, text, null, baseColor, hoverColor, borderColor, hoverBorderColor,
                    new Color(0.95f, 0.97f, 1.0f, 1.0f), new Color(1.0f, 1.0f, 1.0f, 1.0f), onClick);
        }

        public UIElement(float x, float y, float width, float height, String text, String subText,
                         Color baseColor, Color hoverColor, Color borderColor, Color hoverBorderColor,
                         Color textColor, Color hoverTextColor,
                         Runnable onClick) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.text = text;
            this.subText = subText;
            this.baseColor = new Color(baseColor);
            this.hoverColor = new Color(hoverColor);
            this.borderColor = new Color(borderColor);
            this.hoverBorderColor = new Color(hoverBorderColor);
            this.textColor = new Color(textColor);
            this.hoverTextColor = new Color(hoverTextColor);
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
    private final List<UIElement> hudButtons = new ArrayList<>();
    private final List<UIElement> actionButtons = new ArrayList<>();
    private final List<UIElement> numPadButtons = new ArrayList<>();
    private final List<UIElement> pauseButtons = new ArrayList<>();
    private final List<UIElement> settingsButtons = new ArrayList<>();
    private final List<UIElement> modalButtons = new ArrayList<>();
    private final List<UIElement> victoryButtons = new ArrayList<>();
    private final List<UIElement> gameOverButtons = new ArrayList<>();

    private UIElement diffPillButton;
    private UIElement notesButton;

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

        prefs = Gdx.app.getPreferences("SudokuMasterPrefs");
        String savedTheme = prefs.getString("theme", GameTheme.PAPER.name());
        try {
            currentTheme = GameTheme.valueOf(savedTheme);
        } catch (Exception e) {
            currentTheme = GameTheme.PAPER;
        }

        initShapeDrawer();
        initFonts();

        sfx = new SoundEffects();
        sfx.init();
        boolean savedSound = prefs.getBoolean("sound", true);
        if (!savedSound && sfx.isSoundEnabled()) {
            sfx.toggleSound();
        }

        sudoku = new SudokuEngine();

        initBox2D();
        initLighting();
        updateThemeLighting();
        buildUI();
        setupInput();
        updateWindowTitle();
    }

    private void updateWindowTitle() {
        Gdx.graphics.setTitle(currentTheme == GameTheme.PAPER ?
                "Sudoku Master - Paper Edition" :
                "Sudoku Master - Modern Glass Edition");
    }

    public void setTheme(GameTheme theme) {
        if (this.currentTheme == theme) return;
        this.currentTheme = theme;
        initFonts();
        updateThemeLighting();
        buildUI();
        updateWindowTitle();
        if (prefs != null) {
            prefs.putString("theme", currentTheme.name());
            prefs.flush();
        }
    }

    private void toggleTheme() {
        setTheme(currentTheme == GameTheme.PAPER ? GameTheme.MODERN_GLASS : GameTheme.PAPER);
        if (sfx != null) sfx.playClick();
    }

    private void toggleAudio() {
        sfx.toggleSound();
        if (prefs != null) {
            prefs.putBoolean("sound", sfx.isSoundEnabled());
            prefs.flush();
        }
        buildUI();
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

    private void disposeFonts() {
        if (titleFont != null) { titleFont.dispose(); titleFont = null; }
        if (headerFont != null) { headerFont.dispose(); headerFont = null; }
        if (digitFont != null) { digitFont.dispose(); digitFont = null; }
        if (noteFont != null) { noteFont.dispose(); noteFont = null; }
        if (buttonFont != null) { buttonFont.dispose(); buttonFont = null; }
        if (smallFont != null) { smallFont.dispose(); smallFont = null; }
    }

    private void initFonts() {
        disposeFonts();

        String fontFileStr = (currentTheme == GameTheme.PAPER) ? "fonts/handwriting.ttf" : "fonts/segoeui.ttf";
        FileHandle fontFile = Gdx.files.internal(fontFileStr);
        if (!fontFile.exists()) {
            fontFile = Gdx.files.internal("fonts/segoeui.ttf");
        }

        if (fontFile.exists()) {
            FreeTypeFontGenerator gen = new FreeTypeFontGenerator(fontFile);
            FreeTypeFontParameter param = new FreeTypeFontParameter();
            param.minFilter = Texture.TextureFilter.Linear;
            param.magFilter = Texture.TextureFilter.Linear;
            param.characters = FreeTypeFontGenerator.DEFAULT_CHARS
                    + "abcçdefgğhıijklmnoöprsştuüvyzABCÇDEFGĞHIİJKLMNOÖPRSŞTUÜVYZ"
                    + "0123456789.,:;!?()[]{}/+-*=%&'\"_#<>|~^ ";

            param.size = (currentTheme == GameTheme.PAPER) ? 38 : 38;
            titleFont = gen.generateFont(param);

            param.size = (currentTheme == GameTheme.PAPER) ? 24 : 24;
            headerFont = gen.generateFont(param);

            param.size = (currentTheme == GameTheme.PAPER) ? 30 : 28;
            digitFont = gen.generateFont(param);

            param.size = (currentTheme == GameTheme.PAPER) ? 12 : 12;
            noteFont = gen.generateFont(param);

            param.size = (currentTheme == GameTheme.PAPER) ? 18 : 18;
            buttonFont = gen.generateFont(param);

            param.size = (currentTheme == GameTheme.PAPER) ? 13 : 14;
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
        rayHandler.setBlurNum(2);

        cursorLight = new PointLight(rayHandler, 48, new Color(0.25f, 0.80f, 1.0f, 0.75f), 10.5f, 0, 0);
        cursorLight.setSoft(true);

        boardAmbientLight = new PointLight(rayHandler, 64, new Color(1.0f, 0.80f, 0.40f, 0.45f), 18f,
                (boardX + boardSize / 2f) / PPM, (boardY + boardSize / 2f) / PPM);
        boardAmbientLight.setSoft(true);
    }

    private void updateThemeLighting() {
        if (rayHandler == null) return;
        if (currentTheme == GameTheme.PAPER) {
            rayHandler.setAmbientLight(0.96f, 0.94f, 0.89f, 0.90f);
            if (cursorLight != null) {
                cursorLight.setColor(new Color(1.0f, 0.92f, 0.78f, 0.22f));
            }
            if (boardAmbientLight != null) {
                boardAmbientLight.setColor(new Color(1.0f, 0.96f, 0.88f, 0.18f));
            }
        } else {
            rayHandler.setAmbientLight(0.25f, 0.28f, 0.35f, 0.70f);
            if (cursorLight != null) {
                cursorLight.setColor(new Color(0.25f, 0.80f, 1.0f, 0.75f));
            }
            if (boardAmbientLight != null) {
                boardAmbientLight.setColor(new Color(1.0f, 0.80f, 0.40f, 0.45f));
            }
        }
    }

    private void triggerVictoryCelebration() {
        for (Body b : confettiBodies) {
            world.destroyBody(b);
        }
        confettiBodies.clear();
        confettiColors.clear();

        Color[] palette = (currentTheme == GameTheme.PAPER) ? new Color[] {
                new Color(0.85f, 0.72f, 0.55f, 1f), // Kraft brown
                new Color(0.55f, 0.75f, 0.92f, 1f), // Pastel blue
                new Color(0.94f, 0.65f, 0.72f, 1f), // Pastel pink
                new Color(0.65f, 0.88f, 0.68f, 1f), // Pastel mint
                new Color(0.98f, 0.82f, 0.40f, 1f)  // Canary yellow
        } : new Color[] {
                new Color(0.2f, 0.85f, 1.0f, 1f),
                new Color(1.0f, 0.75f, 0.2f, 1f),
                new Color(0.2f, 1.0f, 0.6f, 1f),
                new Color(1.0f, 0.3f, 0.5f, 1f),
                new Color(0.8f, 0.4f, 1.0f, 1f)
        };

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
        hudButtons.clear();
        actionButtons.clear();
        numPadButtons.clear();
        pauseButtons.clear();
        settingsButtons.clear();
        modalButtons.clear();
        victoryButtons.clear();
        gameOverButtons.clear();

        boolean isPaper = (currentTheme == GameTheme.PAPER);

        // --- 0. DIFFICULTY SELECTOR BUTTONS (Main Menu) ---
        float diffW = 86f;
        float diffH = 36f;
        float cardW = 460f;
        float cardH = 570f;
        float cx = (SCREEN_WIDTH - cardW) / 2f;
        float cy = (SCREEN_HEIGHT - cardH) / 2f;
        float diffStartX = cx + (cardW - (diffW * 4 + 24f)) / 2f;
        float diffY = cy + cardH - 145f;

        for (SudokuEngine.Difficulty diff : SudokuEngine.Difficulty.values()) {
            final SudokuEngine.Difficulty d = diff;
            float dx = diffStartX + diff.ordinal() * (diffW + 8f);

            Color base = isPaper ? new Color(0.94f, 0.91f, 0.85f, 0.95f) : new Color(0.10f, 0.14f, 0.20f, 0.85f);
            Color hover = isPaper ? new Color(0.88f, 0.84f, 0.76f, 1.0f) : new Color(0.18f, 0.35f, 0.55f, 0.95f);
            Color border = isPaper ? new Color(0.70f, 0.66f, 0.60f, 0.85f) : new Color(0.24f, 0.35f, 0.50f, 0.60f);
            Color hoverBorder = isPaper ? new Color(0.35f, 0.32f, 0.28f, 1.0f) : new Color(0.40f, 0.75f, 1.0f, 0.90f);
            Color txt = isPaper ? new Color(0.20f, 0.20f, 0.24f, 1.0f) : new Color(0.90f, 0.95f, 1.0f, 1.0f);
            Color hoverTxt = isPaper ? new Color(0.10f, 0.10f, 0.14f, 1.0f) : new Color(1.0f, 1.0f, 1.0f, 1.0f);

            difficultyButtons.add(new UIElement(
                    dx, diffY, diffW, diffH, d.label, null,
                    base, hover, border, hoverBorder, txt, hoverTxt,
                    () -> {
                        selectedDifficulty = d;
                        sfx.playClick();
                    }
            ));
        }

        // --- 1. MAIN MENU BUTTONS ---
        float menuW = 340f;
        float menuH = 44f;
        float menuX = (SCREEN_WIDTH - menuW) / 2f;
        float startY = diffY - 56f;
        float gap = 50f;

        // PLAY
        Color playBase = isPaper ? new Color(0.84f, 0.92f, 0.84f, 0.95f) : new Color(0.10f, 0.32f, 0.30f, 0.90f);
        Color playHov = isPaper ? new Color(0.75f, 0.88f, 0.75f, 1.0f) : new Color(0.14f, 0.70f, 0.56f, 0.95f);
        Color playBrd = isPaper ? new Color(0.42f, 0.65f, 0.45f, 0.90f) : new Color(0.25f, 0.85f, 0.70f, 0.70f);
        Color playHovBrd = isPaper ? new Color(0.25f, 0.50f, 0.28f, 1.0f) : new Color(0.40f, 1.0f, 0.85f, 1.0f);
        Color playTxt = isPaper ? new Color(0.10f, 0.32f, 0.15f, 1.0f) : new Color(0.95f, 0.97f, 1.0f, 1.0f);

        menuButtons.add(new UIElement(
                menuX, startY, menuW, menuH, "PLAY", null,
                playBase, playHov, playBrd, playHovBrd, playTxt, playTxt,
                () -> {
                    sudoku.startNewGame(selectedDifficulty);
                    if (diffPillButton != null) diffPillButton.text = selectedDifficulty.label;
                    state = GameState.PLAYING;
                }
        ));

        // HOW TO PLAY
        Color secBase = isPaper ? new Color(0.94f, 0.91f, 0.85f, 0.95f) : new Color(0.11f, 0.16f, 0.24f, 0.90f);
        Color secHov = isPaper ? new Color(0.88f, 0.84f, 0.76f, 1.0f) : new Color(0.18f, 0.32f, 0.48f, 0.95f);
        Color secBrd = isPaper ? new Color(0.68f, 0.64f, 0.58f, 0.85f) : new Color(0.28f, 0.52f, 0.78f, 0.65f);
        Color secHovBrd = isPaper ? new Color(0.38f, 0.35f, 0.30f, 1.0f) : new Color(0.45f, 0.75f, 1.0f, 0.95f);
        Color secTxt = isPaper ? new Color(0.18f, 0.18f, 0.22f, 1.0f) : new Color(0.95f, 0.97f, 1.0f, 1.0f);

        menuButtons.add(new UIElement(
                menuX, startY - gap, menuW, menuH, "HOW TO PLAY", null,
                secBase, secHov, secBrd, secHovBrd, secTxt, secTxt,
                () -> state = GameState.HOW_TO_PLAY
        ));

        // SETTINGS
        menuButtons.add(new UIElement(
                menuX, startY - gap * 2, menuW, menuH, "SETTINGS & THEME", null,
                secBase, secHov, secBrd, secHovBrd, secTxt, secTxt,
                () -> {
                    stateBeforeSettings = GameState.MAIN_MENU;
                    state = GameState.SETTINGS;
                }
        ));

        // AUDIO TOGGLE
        menuButtons.add(new UIElement(
                menuX, startY - gap * 3, menuW, menuH, sfx.isSoundEnabled() ? "SOUND: ON" : "SOUND: OFF", null,
                secBase, secHov, secBrd, secHovBrd, secTxt, secTxt,
                this::toggleAudio
        ));

        // QUIT
        Color quitBase = isPaper ? new Color(0.94f, 0.85f, 0.85f, 0.95f) : new Color(0.18f, 0.10f, 0.12f, 0.85f);
        Color quitHov = isPaper ? new Color(0.88f, 0.74f, 0.74f, 1.0f) : new Color(0.65f, 0.16f, 0.22f, 0.92f);
        Color quitBrd = isPaper ? new Color(0.75f, 0.45f, 0.45f, 0.90f) : new Color(0.60f, 0.22f, 0.28f, 0.60f);
        Color quitHovBrd = isPaper ? new Color(0.60f, 0.22f, 0.24f, 1.0f) : new Color(1.0f, 0.35f, 0.45f, 0.95f);
        Color quitTxt = isPaper ? new Color(0.48f, 0.12f, 0.14f, 1.0f) : new Color(1.0f, 0.85f, 0.88f, 1.0f);

        menuButtons.add(new UIElement(
                menuX, startY - gap * 4, menuW, menuH, "QUIT", null,
                quitBase, quitHov, quitBrd, quitHovBrd, quitTxt, quitTxt,
                () -> Gdx.app.exit()
        ));

        // --- 2. IN-GAME TOP HUD PILLS ---
        // Menu Button
        hudButtons.add(new UIElement(
                25f, SCREEN_HEIGHT - 54f, 105f, 38f, "MENU", null,
                secBase, secHov, secBrd, secHovBrd, secTxt, secTxt,
                () -> state = GameState.PAUSED
        ));

        // Settings Button in HUD
        hudButtons.add(new UIElement(
                140f, SCREEN_HEIGHT - 54f, 115f, 38f, "SETTINGS", null,
                secBase, secHov, secBrd, secHovBrd, secTxt, secTxt,
                () -> {
                    stateBeforeSettings = GameState.PAUSED;
                    state = GameState.SETTINGS;
                }
        ));

        // Sound Button in HUD
        hudButtons.add(new UIElement(
                SCREEN_WIDTH - 275f, SCREEN_HEIGHT - 54f, 120f, 38f, sfx.isSoundEnabled() ? "SOUND: ON" : "SOUND: OFF", null,
                secBase, secHov, secBrd, secHovBrd, secTxt, secTxt,
                this::toggleAudio
        ));

        // New Game Button in HUD
        hudButtons.add(new UIElement(
                SCREEN_WIDTH - 145f, SCREEN_HEIGHT - 54f, 120f, 38f, "+ NEW", null,
                playBase, playHov, playBrd, playHovBrd, playTxt, playTxt,
                () -> {
                    sudoku.startNewGame(selectedDifficulty);
                    if (diffPillButton != null) diffPillButton.text = selectedDifficulty.label;
                }
        ));

        // Clickable In-Game Difficulty Pill
        float midX = SCREEN_WIDTH / 2f;
        float headerY = SCREEN_HEIGHT - 32f;
        Color pillBase = isPaper ? new Color(0.85f, 0.90f, 0.96f, 0.95f) : new Color(0.14f, 0.26f, 0.40f, 0.85f);
        Color pillHov = isPaper ? new Color(0.75f, 0.84f, 0.94f, 1.0f) : new Color(0.20f, 0.42f, 0.65f, 0.95f);
        Color pillBrd = isPaper ? new Color(0.35f, 0.55f, 0.80f, 0.90f) : new Color(0.35f, 0.70f, 1.0f, 0.85f);
        Color pillHovBrd = isPaper ? new Color(0.20f, 0.40f, 0.70f, 1.0f) : new Color(0.55f, 0.85f, 1.0f, 1.0f);
        Color pillTxt = isPaper ? new Color(0.12f, 0.28f, 0.55f, 1.0f) : new Color(0.95f, 0.97f, 1.0f, 1.0f);

        diffPillButton = new UIElement(
                midX - 220f, headerY - 14f, 96f, 28f, selectedDifficulty.label, null,
                pillBase, pillHov, pillBrd, pillHovBrd, pillTxt, pillTxt,
                () -> {
                    int nextIdx = (selectedDifficulty.ordinal() + 1) % SudokuEngine.Difficulty.values().length;
                    selectedDifficulty = SudokuEngine.Difficulty.values()[nextIdx];
                    sudoku.startNewGame(selectedDifficulty);
                    if (diffPillButton != null) diffPillButton.text = selectedDifficulty.label;
                    sfx.playClick();
                }
        );
        hudButtons.add(diffPillButton);

        // --- 3. CONTROL PANEL & NUMPAD (Right Side: x = 735 to 1180) ---
        float panelX = 735f;
        float actBtnW = 100f;
        float actBtnH = 50f;
        float actGap = 12f;
        float actY = 560f;

        // Undo
        actionButtons.add(new UIElement(
                panelX, actY, actBtnW, actBtnH, "UNDO", null,
                secBase, secHov, secBrd, secHovBrd, secTxt, secTxt,
                () -> {
                    if (sudoku.undo()) sfx.playPlace();
                }
        ));

        // Pencil / Note Mode Toggle
        Color noteBase = isPaper ? new Color(0.94f, 0.91f, 0.85f, 0.95f) : new Color(0.10f, 0.14f, 0.22f, 0.85f);
        Color noteHov = isPaper ? new Color(0.92f, 0.86f, 0.72f, 1.0f) : new Color(0.28f, 0.45f, 0.68f, 0.95f);
        Color noteBrd = isPaper ? new Color(0.68f, 0.64f, 0.58f, 0.85f) : new Color(0.28f, 0.45f, 0.68f, 0.60f);
        Color noteHovBrd = isPaper ? new Color(0.80f, 0.55f, 0.15f, 1.0f) : new Color(1.0f, 0.75f, 0.25f, 1.0f);
        notesButton = new UIElement(
                panelX + (actBtnW + actGap), actY, actBtnW, actBtnH, "NOTES", null,
                noteBase, noteHov, noteBrd, noteHovBrd, secTxt, secTxt,
                () -> {
                    sudoku.togglePencilMode();
                    sfx.playClick();
                }
        );
        actionButtons.add(notesButton);

        // Erase
        actionButtons.add(new UIElement(
                panelX + (actBtnW + actGap) * 2, actY, actBtnW, actBtnH, "ERASE", null,
                quitBase, quitHov, quitBrd, quitHovBrd, quitTxt, quitTxt,
                () -> {
                    if (sudoku.erase(selectedRow, selectedCol)) sfx.playErase();
                }
        ));

        // Hint
        Color hintBase = isPaper ? new Color(0.96f, 0.92f, 0.80f, 0.95f) : new Color(0.20f, 0.18f, 0.10f, 0.85f);
        Color hintHov = isPaper ? new Color(0.92f, 0.85f, 0.65f, 1.0f) : new Color(0.55f, 0.45f, 0.14f, 0.95f);
        Color hintBrd = isPaper ? new Color(0.75f, 0.60f, 0.25f, 0.90f) : new Color(0.70f, 0.55f, 0.20f, 0.60f);
        Color hintHovBrd = isPaper ? new Color(0.85f, 0.68f, 0.15f, 1.0f) : new Color(1.0f, 0.82f, 0.25f, 0.95f);
        Color hintTxt = isPaper ? new Color(0.45f, 0.30f, 0.05f, 1.0f) : new Color(1.0f, 0.92f, 0.65f, 1.0f);
        actionButtons.add(new UIElement(
                panelX + (actBtnW + actGap) * 3, actY, actBtnW, actBtnH, "HINT", null,
                hintBase, hintHov, hintBrd, hintHovBrd, hintTxt, hintTxt,
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

        Color numBase = isPaper ? new Color(0.95f, 0.93f, 0.88f, 0.95f) : new Color(0.09f, 0.13f, 0.20f, 0.88f);
        Color numHov = isPaper ? new Color(0.88f, 0.85f, 0.78f, 1.0f) : new Color(0.15f, 0.35f, 0.55f, 0.96f);
        Color numBrd = isPaper ? new Color(0.70f, 0.66f, 0.60f, 0.85f) : new Color(0.22f, 0.40f, 0.62f, 0.65f);
        Color numHovBrd = isPaper ? new Color(0.35f, 0.32f, 0.28f, 1.0f) : new Color(0.35f, 0.75f, 1.0f, 0.95f);
        Color numTxt = isPaper ? new Color(0.12f, 0.15f, 0.22f, 1.0f) : new Color(0.95f, 0.97f, 1.0f, 1.0f);

        for (int i = 0; i < 9; i++) {
            final int digit = i + 1;
            int r = i / 3;
            int c = i % 3;
            float nx = panelX + c * (numW + numGapX);
            float ny = numStartY - r * (numH + numGapY);

            numPadButtons.add(new UIElement(
                    nx, ny, numW, numH, String.valueOf(digit), null,
                    numBase, numHov, numBrd, numHovBrd, numTxt, numTxt,
                    () -> handleNumberInput(digit)
            ));
        }

        // --- 4. PAUSE MODAL BUTTONS ---
        float pauseCardW = 380f;
        float pauseCardH = 370f;
        float pauseCx = (SCREEN_WIDTH - pauseCardW) / 2f;
        float pauseCy = (SCREEN_HEIGHT - pauseCardH) / 2f;
        float pauseBtnW = 300f;
        float pauseBtnH = 46f;
        float pauseBx = pauseCx + 40f;

        pauseButtons.add(new UIElement(
                pauseBx, pauseCy + 235f, pauseBtnW, pauseBtnH, "RESUME", null,
                playBase, playHov, playBrd, playHovBrd, playTxt, playTxt,
                () -> state = GameState.PLAYING
        ));
        pauseButtons.add(new UIElement(
                pauseBx, pauseCy + 175f, pauseBtnW, pauseBtnH, "SETTINGS & THEME", null,
                secBase, secHov, secBrd, secHovBrd, secTxt, secTxt,
                () -> {
                    stateBeforeSettings = GameState.PAUSED;
                    state = GameState.SETTINGS;
                }
        ));
        pauseButtons.add(new UIElement(
                pauseBx, pauseCy + 115f, pauseBtnW, pauseBtnH, "+ NEW GAME", null,
                secBase, secHov, secBrd, secHovBrd, secTxt, secTxt,
                () -> {
                    sudoku.startNewGame(selectedDifficulty);
                    if (diffPillButton != null) diffPillButton.text = selectedDifficulty.label;
                    state = GameState.PLAYING;
                }
        ));
        pauseButtons.add(new UIElement(
                pauseBx, pauseCy + 55f, pauseBtnW, pauseBtnH, "MAIN MENU", null,
                quitBase, quitHov, quitBrd, quitHovBrd, quitTxt, quitTxt,
                () -> state = GameState.MAIN_MENU
        ));

        // --- 5. SETTINGS MODAL BUTTONS ---
        float setCardW = 600f;
        float setCardH = 490f;
        float setCx = (SCREEN_WIDTH - setCardW) / 2f;
        float setCy = (SCREEN_HEIGHT - setCardH) / 2f;

        // Theme 1: Paper Notebook Card Button
        boolean isPaperSel = (currentTheme == GameTheme.PAPER);
        Color paperBase = isPaperSel ?
                new Color(0.95f, 0.91f, 0.80f, 1.0f) :
                new Color(0.91f, 0.88f, 0.82f, 0.85f);
        Color paperHov = new Color(0.98f, 0.95f, 0.86f, 1.0f);
        Color paperBrd = isPaperSel ?
                new Color(0.22f, 0.20f, 0.18f, 1.0f) :
                new Color(0.68f, 0.64f, 0.58f, 0.75f);
        Color paperHovBrd = new Color(0.10f, 0.10f, 0.12f, 1.0f);
        Color paperTxt = new Color(0.15f, 0.15f, 0.18f, 1.0f);

        settingsButtons.add(new UIElement(
                setCx + 40f, setCy + setCardH - 185f, 245f, 75f,
                "PAPER NOTEBOOK",
                isPaperSel ? "[ ACTIVE ]" : "Ruled Paper & Handwriting",
                paperBase, paperHov, paperBrd, paperHovBrd, paperTxt, paperTxt,
                () -> setTheme(GameTheme.PAPER)
        ));

        // Theme 2: Modern Glass Card Button
        boolean isGlassSel = (currentTheme == GameTheme.MODERN_GLASS);
        Color glassBase = isGlassSel ?
                new Color(0.14f, 0.28f, 0.44f, 0.95f) :
                new Color(0.08f, 0.12f, 0.18f, 0.85f);
        Color glassHov = new Color(0.18f, 0.38f, 0.58f, 1.0f);
        Color glassBrd = isGlassSel ?
                new Color(0.40f, 0.88f, 1.0f, 1.0f) :
                new Color(0.24f, 0.38f, 0.55f, 0.65f);
        Color glassHovBrd = new Color(0.55f, 0.95f, 1.0f, 1.0f);
        Color glassTxt = new Color(0.95f, 0.98f, 1.0f, 1.0f);

        settingsButtons.add(new UIElement(
                setCx + 315f, setCy + setCardH - 185f, 245f, 75f,
                "MODERN GLASS",
                isGlassSel ? "[ ACTIVE ]" : "Dark Glass & Neon Lights",
                glassBase, glassHov, glassBrd, glassHovBrd, glassTxt, glassTxt,
                () -> setTheme(GameTheme.MODERN_GLASS)
        ));

        // Sound Toggle Button in Settings
        settingsButtons.add(new UIElement(
                setCx + 40f, setCy + 130f, 520f, 46f,
                sfx.isSoundEnabled() ? "SOUND EFFECTS: ON" : "SOUND EFFECTS: OFF", null,
                secBase, secHov, secBrd, secHovBrd, secTxt, secTxt,
                this::toggleAudio
        ));

        // Back Button in Settings
        settingsButtons.add(new UIElement(
                setCx + 150f, setCy + 45f, 300f, 46f,
                "< BACK", null,
                playBase, playHov, playBrd, playHovBrd, playTxt, playTxt,
                () -> state = stateBeforeSettings
        ));

        // --- 6. HOW TO PLAY MODAL BUTTON ---
        modalButtons.add(new UIElement(
                (SCREEN_WIDTH - 240f) / 2f, 175f, 240f, 46f, "GOT IT, PLAY!", null,
                playBase, playHov, playBrd, playHovBrd, playTxt, playTxt,
                () -> {
                    sudoku.startNewGame(selectedDifficulty);
                    state = GameState.PLAYING;
                }
        ));

        // --- 7. VICTORY MODAL BUTTONS ---
        victoryButtons.add(new UIElement(
                (SCREEN_WIDTH - 260f) / 2f, 110f, 260f, 48f, "PLAY AGAIN", null,
                playBase, playHov, playBrd, playHovBrd, playTxt, playTxt,
                () -> {
                    sudoku.startNewGame(selectedDifficulty);
                    state = GameState.PLAYING;
                }
        ));
        victoryButtons.add(new UIElement(
                (SCREEN_WIDTH - 260f) / 2f, 50f, 260f, 44f, "MAIN MENU", null,
                secBase, secHov, secBrd, secHovBrd, secTxt, secTxt,
                () -> state = GameState.MAIN_MENU
        ));

        // --- 8. GAME OVER MODAL BUTTONS ---
        gameOverButtons.add(new UIElement(
                (SCREEN_WIDTH - 260f) / 2f, 150f, 260f, 48f, "TRY AGAIN", null,
                quitBase, quitHov, quitBrd, quitHovBrd, quitTxt, quitTxt,
                () -> {
                    sudoku.startNewGame(selectedDifficulty);
                    state = GameState.PLAYING;
                }
        ));
        gameOverButtons.add(new UIElement(
                (SCREEN_WIDTH - 260f) / 2f, 90f, 260f, 44f, "MAIN MENU", null,
                secBase, secHov, secBrd, secHovBrd, secTxt, secTxt,
                () -> state = GameState.MAIN_MENU
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

                if (state == GameState.SETTINGS) {
                    for (UIElement btn : settingsButtons) {
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
                    for (UIElement btn : pauseButtons) {
                        if (btn.handleClick(mx, my, sfx)) return true;
                    }
                    return true;
                }

                if (state == GameState.VICTORY) {
                    for (UIElement btn : victoryButtons) {
                        if (btn.handleClick(mx, my, sfx)) return true;
                    }
                    return true;
                }

                if (state == GameState.GAME_OVER) {
                    for (UIElement btn : gameOverButtons) {
                        if (btn.handleClick(mx, my, sfx)) return true;
                    }
                    return true;
                }

                if (state == GameState.PLAYING) {
                    for (UIElement btn : hudButtons) {
                        if (btn.handleClick(mx, my, sfx)) return true;
                    }
                    for (UIElement btn : actionButtons) {
                        if (btn.handleClick(mx, my, sfx)) return true;
                    }
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
                    else if (state == GameState.SETTINGS) state = stateBeforeSettings;
                    return true;
                }

                // Quick Theme Toggle shortcut [T]
                if (keycode == Input.Keys.T) {
                    toggleTheme();
                    return true;
                }

                // Sound Toggle shortcut [M]
                if (keycode == Input.Keys.M) {
                    toggleAudio();
                    return true;
                }

                if (state == GameState.PLAYING) {
                    if (keycode == Input.Keys.UP && selectedRow > 0) { selectedRow--; return true; }
                    if (keycode == Input.Keys.DOWN && selectedRow < 8) { selectedRow++; return true; }
                    if (keycode == Input.Keys.LEFT && selectedCol > 0) { selectedCol--; return true; }
                    if (keycode == Input.Keys.RIGHT && selectedCol < 8) { selectedCol++; return true; }

                    if (keycode >= Input.Keys.NUM_1 && keycode <= Input.Keys.NUM_9) {
                        handleNumberInput(keycode - Input.Keys.NUM_1 + 1);
                        return true;
                    }
                    if (keycode >= Input.Keys.NUMPAD_1 && keycode <= Input.Keys.NUMPAD_9) {
                        handleNumberInput(keycode - Input.Keys.NUMPAD_1 + 1);
                        return true;
                    }

                    if (keycode == Input.Keys.BACKSPACE || keycode == Input.Keys.DEL) {
                        if (sudoku.erase(selectedRow, selectedCol)) sfx.playErase();
                        return true;
                    }

                    if (keycode == Input.Keys.SPACE || keycode == Input.Keys.N) {
                        sudoku.togglePencilMode();
                        sfx.playClick();
                        return true;
                    }

                    if (keycode == Input.Keys.Z || keycode == Input.Keys.U) {
                        if (sudoku.undo()) sfx.playPlace();
                        return true;
                    }
                }

                return false;
            }
        });
    }

    private void updateHover(float mx, float my, float dt) {
        if (state == GameState.MAIN_MENU) {
            for (UIElement btn : difficultyButtons) btn.update(mx, my, dt, sfx);
            for (UIElement btn : menuButtons) btn.update(mx, my, dt, sfx);
        } else if (state == GameState.SETTINGS) {
            for (UIElement btn : settingsButtons) btn.update(mx, my, dt, sfx);
        } else if (state == GameState.PLAYING) {
            for (UIElement btn : hudButtons) btn.update(mx, my, dt, sfx);
            for (UIElement btn : actionButtons) btn.update(mx, my, dt, sfx);
            for (UIElement btn : numPadButtons) btn.update(mx, my, dt, sfx);
        } else if (state == GameState.HOW_TO_PLAY) {
            for (UIElement btn : modalButtons) btn.update(mx, my, dt, sfx);
        } else if (state == GameState.PAUSED) {
            for (UIElement btn : pauseButtons) btn.update(mx, my, dt, sfx);
        } else if (state == GameState.VICTORY) {
            for (UIElement btn : victoryButtons) btn.update(mx, my, dt, sfx);
        } else if (state == GameState.GAME_OVER) {
            for (UIElement btn : gameOverButtons) btn.update(mx, my, dt, sfx);
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

        if (state == GameState.VICTORY) {
            world.step(1 / 60f, 6, 2);
        }

        cursorLight.setPosition(mouseCoords.x / PPM, mouseCoords.y / PPM);

        // Clear screen with active theme color
        if (currentTheme == GameTheme.PAPER) {
            Gdx.gl.glClearColor(0.96f, 0.94f, 0.89f, 1f); // Warm parchment
        } else {
            Gdx.gl.glClearColor(0.06f, 0.08f, 0.12f, 1f); // Modern glass dark slate
        }
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        viewport.apply();
        batch.setProjectionMatrix(camera.combined);

        // --- 1. WORLD & BOARD RENDERING (ShapeDrawer Batch) ---
        batch.begin();

        renderBackgroundPattern();

        if (state != GameState.MAIN_MENU) {
            renderGameHeader();
            renderSudokuBoard();
            renderControlPanel();
        }

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
        } else if (state == GameState.SETTINGS) {
            renderSettingsModal();
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

    private void renderBackgroundPattern() {
        if (currentTheme == GameTheme.PAPER) {
            // Authentic ruled notebook lines across screen
            drawer.setColor(new Color(0.72f, 0.80f, 0.88f, 0.38f));
            for (float y = 25f; y < SCREEN_HEIGHT; y += 32f) {
                drawer.line(0, y, SCREEN_WIDTH, y, 1.2f);
            }
            // Classic notebook vertical margin line (soft red pencil)
            drawer.setColor(new Color(0.92f, 0.60f, 0.60f, 0.50f));
            drawer.line(80f, 0, 80f, SCREEN_HEIGHT, 2.0f);
        } else {
            // Modern Glass ambient grid lines
            drawer.setColor(new Color(0.12f, 0.16f, 0.24f, 0.35f));
            for (int i = 0; i < 15; i++) {
                drawer.line(i * 90f, 0, i * 90f, SCREEN_HEIGHT, 1.5f);
                drawer.line(0, i * 50f, SCREEN_WIDTH, i * 50f, 1.5f);
            }
        }
    }

    private void renderGameHeader() {
        boolean isPaper = (currentTheme == GameTheme.PAPER);

        // Top Bar Background
        if (isPaper) {
            drawer.setColor(new Color(0.93f, 0.90f, 0.84f, 0.96f));
            drawer.filledRectangle(0, SCREEN_HEIGHT - 65f, SCREEN_WIDTH, 65f);
            drawer.setColor(new Color(0.76f, 0.72f, 0.66f, 0.90f));
            drawer.line(0, SCREEN_HEIGHT - 65f, SCREEN_WIDTH, SCREEN_HEIGHT - 65f, 2f);
        } else {
            drawer.setColor(new Color(0.08f, 0.11f, 0.16f, 0.90f));
            drawer.filledRectangle(0, SCREEN_HEIGHT - 65f, SCREEN_WIDTH, 65f);
            drawer.setColor(new Color(0.20f, 0.35f, 0.55f, 0.55f));
            drawer.line(0, SCREEN_HEIGHT - 65f, SCREEN_WIDTH, SCREEN_HEIGHT - 65f, 2f);
        }

        // Render In-Game top HUD buttons
        for (int i = 0; i < hudButtons.size(); i++) {
            renderButton(hudButtons.get(i), i == 4 ? smallFont : buttonFont);
        }

        // Center HUD: Timer + Mistakes
        float midX = SCREEN_WIDTH / 2f;
        float headerY = SCREEN_HEIGHT - 32f;

        // Timer
        Color timeTxtColor = isPaper ? new Color(0.18f, 0.18f, 0.22f, 1.0f) : new Color(0.95f, 0.95f, 1.0f, 1.0f);
        headerFont.setColor(timeTxtColor);
        String timeStr = "Time: " + sudoku.getFormattedTime();
        layout.setText(headerFont, timeStr);
        headerFont.draw(batch, timeStr, midX - layout.width / 2f, headerY + layout.height / 2f);

        // Mistakes Pill
        String mistakeStr = "Mistakes: " + sudoku.getMistakes() + "/" + sudoku.getMaxMistakes();
        if (isPaper) {
            drawer.setColor(new Color(0.91f, 0.87f, 0.80f, 0.95f));
            drawer.filledRectangle(midX + 130f, headerY - 14f, 130f, 28f);
            drawer.setColor(sudoku.getMistakes() > 0 ? new Color(0.85f, 0.22f, 0.22f, 0.90f) : new Color(0.68f, 0.64f, 0.58f, 0.85f));
            drawer.rectangle(midX + 130f, headerY - 14f, 130f, 28f, 1.5f);
            smallFont.setColor(sudoku.getMistakes() > 0 ? new Color(0.85f, 0.18f, 0.18f, 1.0f) : new Color(0.22f, 0.22f, 0.25f, 1.0f));
        } else {
            drawer.setColor(new Color(0.24f, 0.12f, 0.14f, 0.85f));
            drawer.filledRectangle(midX + 130f, headerY - 14f, 130f, 28f);
            drawer.setColor(sudoku.getMistakes() > 0 ? new Color(1.0f, 0.35f, 0.40f, 0.85f) : new Color(0.45f, 0.60f, 0.75f, 0.60f));
            drawer.rectangle(midX + 130f, headerY - 14f, 130f, 28f, 1.5f);
            smallFont.setColor(sudoku.getMistakes() > 0 ? new Color(1.0f, 0.45f, 0.50f, 1.0f) : new Color(0.75f, 0.85f, 0.95f, 1.0f));
        }
        layout.setText(smallFont, mistakeStr);
        smallFont.draw(batch, mistakeStr, midX + 130f + (130f - layout.width) / 2f, headerY + layout.height / 2f);
    }

    private void renderSudokuBoard() {
        boolean isPaper = (currentTheme == GameTheme.PAPER);

        if (isPaper) {
            // Paper Board Card with soft drop-shadow
            drawer.setColor(new Color(0.78f, 0.74f, 0.68f, 0.60f));
            drawer.filledRectangle(boardX + 4f, boardY - 4f, boardSize, boardSize);

            drawer.setColor(new Color(0.995f, 0.995f, 0.99f, 1.0f));
            drawer.filledRectangle(boardX, boardY, boardSize, boardSize);

            drawer.setColor(new Color(0.25f, 0.24f, 0.22f, 0.95f));
            drawer.rectangle(boardX, boardY, boardSize, boardSize, 2.5f);
        } else {
            // Modern Glass Board Outer Glowing Frame
            drawer.setColor(new Color(0.12f, 0.35f, 0.60f, 0.20f));
            drawer.filledRectangle(boardX - 6f, boardY - 6f, boardSize + 12f, boardSize + 12f);

            drawer.setColor(new Color(0.08f, 0.11f, 0.17f, 0.95f));
            drawer.filledRectangle(boardX, boardY, boardSize, boardSize);
        }

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
                    if (isPaper) {
                        drawer.setColor(new Color(0.96f, 0.94f, 0.84f, 0.55f)); // Soft yellow highlighter wash
                    } else {
                        drawer.setColor(new Color(0.14f, 0.20f, 0.30f, 0.65f));
                    }
                    drawer.filledRectangle(cx, cy, cellSize, cellSize);
                }

                // Highlight Same Numbers Across Board
                if (isSameNumber && !isSelected) {
                    if (isPaper) {
                        drawer.setColor(new Color(0.84f, 0.91f, 0.93f, 0.70f)); // Soft cyan highlighter wash
                    } else {
                        drawer.setColor(new Color(0.25f, 0.45f, 0.65f, 0.55f));
                    }
                    drawer.filledRectangle(cx, cy, cellSize, cellSize);
                }

                // Highlight Conflicting Cell
                if (isConflict) {
                    if (isPaper) {
                        drawer.setColor(new Color(0.96f, 0.78f, 0.78f, 0.75f)); // Red error wash
                    } else {
                        drawer.setColor(new Color(0.65f, 0.15f, 0.22f, 0.55f));
                    }
                    drawer.filledRectangle(cx, cy, cellSize, cellSize);
                }

                // Highlight Selected Cell
                if (isSelected) {
                    if (isPaper) {
                        drawer.setColor(new Color(0.92f, 0.88f, 0.75f, 0.80f));
                        drawer.filledRectangle(cx, cy, cellSize, cellSize);
                        drawer.setColor(new Color(0.25f, 0.25f, 0.28f, 0.95f));
                        drawer.rectangle(cx + 1f, cy + 1f, cellSize - 2f, cellSize - 2f, 2.5f);
                    } else {
                        drawer.setColor(new Color(0.18f, 0.48f, 0.75f, 0.85f));
                        drawer.filledRectangle(cx, cy, cellSize, cellSize);
                        drawer.setColor(new Color(0.40f, 0.88f, 1.0f, 0.95f));
                        drawer.rectangle(cx + 1f, cy + 1f, cellSize - 2f, cellSize - 2f, 2.5f);
                    }
                }

                // Draw Digits or Candidate Notes
                if (val > 0) {
                    if (isPaper) {
                        if (isConflict) {
                            digitFont.setColor(0.85f, 0.18f, 0.18f, 1.0f); // Red correction pen
                        } else if (sudoku.getInitialCell(r, c) > 0) {
                            digitFont.setColor(0.12f, 0.14f, 0.18f, 1.0f); // Bold black printed clue
                        } else {
                            digitFont.setColor(0.12f, 0.35f, 0.78f, 1.0f); // Blue ballpoint pen ink!
                        }
                    } else {
                        if (isConflict) {
                            digitFont.setColor(1.0f, 0.35f, 0.35f, 1.0f); // Conflict neon red
                        } else if (sudoku.getInitialCell(r, c) > 0) {
                            digitFont.setColor(0.95f, 0.97f, 1.0f, 1.0f); // Clue silver
                        } else {
                            digitFont.setColor(0.35f, 0.90f, 1.0f, 1.0f); // Player neon cyan
                        }
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

                            if (isPaper) {
                                noteFont.setColor(0.42f, 0.44f, 0.48f, 0.95f); // Graphite pencil grey
                            } else {
                                noteFont.setColor(0.95f, 0.78f, 0.35f, 0.90f); // Neon amber
                            }
                            String noteStr = String.valueOf(n);
                            layout.setText(noteFont, noteStr);
                            noteFont.draw(batch, noteStr, nx + (subCell - layout.width) / 2f, ny + (subCell + layout.height) / 2f);
                        }
                    }
                }
            }
        }

        // Draw Thin Grid Lines (1px)
        if (isPaper) {
            drawer.setColor(new Color(0.72f, 0.70f, 0.66f, 0.85f));
        } else {
            drawer.setColor(new Color(0.20f, 0.28f, 0.40f, 0.70f));
        }
        for (int i = 1; i < 9; i++) {
            if (i % 3 != 0) {
                float pos = i * cellSize;
                drawer.line(boardX + pos, boardY, boardX + pos, boardY + boardSize, 1.2f);
                drawer.line(boardX, boardY + pos, boardX + boardSize, boardY + pos, 1.2f);
            }
        }

        // Draw Thick 3x3 Block Borders (3.2px)
        if (isPaper) {
            drawer.setColor(new Color(0.22f, 0.22f, 0.25f, 0.95f));
        } else {
            drawer.setColor(new Color(0.35f, 0.65f, 0.95f, 0.85f));
        }
        for (int i = 0; i <= 3; i++) {
            float pos = i * (cellSize * 3);
            drawer.line(boardX + pos, boardY, boardX + pos, boardY + boardSize, 3.2f);
            drawer.line(boardX, boardY + pos, boardX + boardSize, boardY + pos, 3.2f);
        }
    }

    private void renderControlPanel() {
        boolean isPaper = (currentTheme == GameTheme.PAPER);

        // Control Panel Action Buttons (Undo, Notes, Erase, Hint)
        for (int i = 0; i < actionButtons.size(); i++) {
            UIElement btn = actionButtons.get(i);
            // Highlight notes button if active
            if (i == 1 && sudoku.isPencilMode()) {
                if (isPaper) {
                    btn.borderColor.set(0.80f, 0.55f, 0.15f, 1.0f);
                    btn.baseColor.set(0.96f, 0.90f, 0.74f, 0.95f);
                } else {
                    btn.borderColor.set(1.0f, 0.75f, 0.25f, 1.0f);
                    btn.baseColor.set(0.35f, 0.25f, 0.10f, 0.90f);
                }
            } else if (i == 1) {
                if (isPaper) {
                    btn.borderColor.set(0.68f, 0.64f, 0.58f, 0.85f);
                    btn.baseColor.set(0.94f, 0.91f, 0.85f, 0.95f);
                } else {
                    btn.borderColor.set(0.28f, 0.45f, 0.68f, 0.60f);
                    btn.baseColor.set(0.10f, 0.14f, 0.22f, 0.85f);
                }
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
            if (isPaper) {
                smallFont.setColor(remaining > 0 ? new Color(0.40f, 0.42f, 0.48f, 0.95f) : new Color(0.15f, 0.55f, 0.25f, 0.95f));
            } else {
                smallFont.setColor(remaining > 0 ? new Color(0.60f, 0.75f, 0.90f, 0.85f) : new Color(0.35f, 0.85f, 0.55f, 0.90f));
            }
            layout.setText(smallFont, remText);
            smallFont.draw(batch, remText, btn.x + (btn.width - layout.width) / 2f, btn.y + 22f);
        }
    }

    private void renderButton(UIElement btn, BitmapFont font) {
        boolean isPaper = (currentTheme == GameTheme.PAPER);
        float lift = btn.hoverAnim * 2f;
        float dy = btn.y + lift;

        Color bg = btn.baseColor.cpy().lerp(btn.hoverColor, btn.hoverAnim);
        Color border = btn.borderColor.cpy().lerp(btn.hoverBorderColor, btn.hoverAnim);

        // Shadow / Glow
        if (isPaper) {
            drawer.setColor(new Color(0.75f, 0.70f, 0.65f, 0.35f + btn.hoverAnim * 0.20f));
            drawer.filledRectangle(btn.x + 2f, dy - 2f, btn.width, btn.height);
        } else {
            if (btn.hoverAnim > 0.05f) {
                Color glow = btn.hoverBorderColor.cpy();
                glow.a = btn.hoverAnim * 0.25f;
                drawer.setColor(glow);
                drawer.filledRectangle(btn.x - 3f, dy - 3f, btn.width + 6f, btn.height + 6f);
            }
        }

        drawer.setColor(bg);
        drawer.filledRectangle(btn.x, dy, btn.width, btn.height);

        drawer.setColor(border);
        drawer.rectangle(btn.x, dy, btn.width, btn.height, 2f);

        // Left accent indicator on hover
        if (btn.hoverAnim > 0.05f) {
            drawer.setColor(border);
            float pillH = (btn.height - 14f) * btn.hoverAnim;
            drawer.filledRectangle(btn.x + 4f, dy + (btn.height - pillH) / 2f, 4f, pillH);
        }

        // Draw Text / Subtext
        Color tc = btn.textColor.cpy().lerp(btn.hoverTextColor, btn.hoverAnim);
        if (btn.subText != null && !btn.subText.isEmpty()) {
            font.setColor(tc);
            layout.setText(font, btn.text);
            font.draw(batch, btn.text, btn.x + (btn.width - layout.width) / 2f, dy + btn.height - 18f);

            Color subCol = isPaper ? new Color(0.35f, 0.35f, 0.40f, 0.90f) : new Color(0.65f, 0.85f, 1.0f, 0.85f);
            smallFont.setColor(subCol);
            layout.setText(smallFont, btn.subText);
            smallFont.draw(batch, btn.subText, btn.x + (btn.width - layout.width) / 2f, dy + 22f);
        } else {
            font.setColor(tc);
            layout.setText(font, btn.text);
            font.draw(batch, btn.text, btn.x + (btn.width - layout.width) / 2f, dy + (btn.height + layout.height) / 2f);
        }
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
        boolean isPaper = (currentTheme == GameTheme.PAPER);
        float cardW = 460f;
        float cardH = 570f;
        float cx = (SCREEN_WIDTH - cardW) / 2f;
        float cy = (SCREEN_HEIGHT - cardH) / 2f;

        if (isPaper) {
            // Paper Sheet Shadow & Card
            drawer.setColor(new Color(0.75f, 0.70f, 0.65f, 0.50f));
            drawer.filledRectangle(cx + 6f, cy - 6f, cardW, cardH);

            drawer.setColor(new Color(0.98f, 0.96f, 0.92f, 0.98f));
            drawer.filledRectangle(cx, cy, cardW, cardH);

            drawer.setColor(new Color(0.35f, 0.32f, 0.30f, 0.95f));
            drawer.rectangle(cx, cy, cardW, cardH, 2.5f);

            smallFont.setColor(0.55f, 0.40f, 0.25f, 1.0f);
            layout.setText(smallFont, "PAPER NOTEBOOK EDITION");
            smallFont.draw(batch, "PAPER NOTEBOOK EDITION", cx + (cardW - layout.width) / 2f, cy + cardH - 32f);

            titleFont.setColor(0.12f, 0.22f, 0.45f, 1.0f);
            layout.setText(titleFont, "SUDOKU MASTER");
            titleFont.draw(batch, "SUDOKU MASTER", cx + (cardW - layout.width) / 2f, cy + cardH - 60f);

            drawer.setColor(new Color(0.70f, 0.65f, 0.60f, 0.70f));
            drawer.line(cx + 35f, cy + cardH - 95f, cx + cardW - 35f, cy + cardH - 95f, 2f);
        } else {
            // Glass Glow & Card
            drawer.setColor(new Color(0.15f, 0.45f, 0.75f, 0.22f));
            drawer.filledRectangle(cx - 6f, cy - 6f, cardW + 12f, cardH + 12f);

            drawer.setColor(new Color(0.07f, 0.10f, 0.16f, 0.92f));
            drawer.filledRectangle(cx, cy, cardW, cardH);

            drawer.setColor(new Color(0.25f, 0.65f, 0.95f, 0.85f));
            drawer.rectangle(cx, cy, cardW, cardH, 2.5f);

            smallFont.setColor(0.40f, 0.85f, 1.0f, 1.0f);
            layout.setText(smallFont, "MODERN GLASS EDITION");
            smallFont.draw(batch, "MODERN GLASS EDITION", cx + (cardW - layout.width) / 2f, cy + cardH - 32f);

            titleFont.setColor(1.0f, 0.78f, 0.25f, 1.0f);
            layout.setText(titleFont, "SUDOKU MASTER");
            titleFont.draw(batch, "SUDOKU MASTER", cx + (cardW - layout.width) / 2f, cy + cardH - 60f);

            drawer.setColor(new Color(0.35f, 0.65f, 0.95f, 0.45f));
            drawer.line(cx + 35f, cy + cardH - 95f, cx + cardW - 35f, cy + cardH - 95f, 2f);
        }

        // Render Difficulty Buttons
        for (UIElement btn : difficultyButtons) {
            boolean isSel = btn.text.equals(selectedDifficulty.label);
            if (isPaper) {
                if (isSel) {
                    btn.baseColor.set(0.80f, 0.88f, 0.95f, 1.0f);
                    btn.borderColor.set(0.25f, 0.50f, 0.80f, 1.0f);
                    btn.textColor.set(0.10f, 0.25f, 0.55f, 1.0f);
                } else {
                    btn.baseColor.set(0.94f, 0.91f, 0.85f, 0.95f);
                    btn.borderColor.set(0.70f, 0.66f, 0.60f, 0.85f);
                    btn.textColor.set(0.20f, 0.20f, 0.24f, 1.0f);
                }
            } else {
                if (isSel) {
                    btn.baseColor.set(0.18f, 0.55f, 0.75f, 0.95f);
                    btn.borderColor.set(0.45f, 0.90f, 1.0f, 1.0f);
                } else {
                    btn.baseColor.set(0.10f, 0.14f, 0.20f, 0.85f);
                    btn.borderColor.set(0.24f, 0.35f, 0.50f, 0.60f);
                }
            }
            renderButton(btn, smallFont);
        }

        // Render Menu Buttons
        for (UIElement btn : menuButtons) {
            renderButton(btn, buttonFont);
        }

        // Shortcut footer hint
        Color hintCol = isPaper ? new Color(0.45f, 0.45f, 0.48f, 0.85f) : new Color(0.40f, 0.60f, 0.80f, 0.75f);
        smallFont.setColor(hintCol);
        String shortcutHint = "[T] Switch Theme  |  [M] Toggle Audio";
        layout.setText(smallFont, shortcutHint);
        smallFont.draw(batch, shortcutHint, cx + (cardW - layout.width) / 2f, cy + 22f);
    }

    private void renderSettingsModal() {
        boolean isPaper = (currentTheme == GameTheme.PAPER);
        float setCardW = 600f;
        float setCardH = 490f;
        float cx = (SCREEN_WIDTH - setCardW) / 2f;
        float cy = (SCREEN_HEIGHT - setCardH) / 2f;

        renderModalCard(setCardW, setCardH, "SETTINGS");

        // Category 1: Select Theme
        Color catTitleCol = isPaper ? new Color(0.15f, 0.22f, 0.45f, 1.0f) : new Color(1.0f, 0.78f, 0.25f, 1.0f);
        headerFont.setColor(catTitleCol);
        String themeHeader = "SELECT THEME";
        layout.setText(headerFont, themeHeader);
        headerFont.draw(batch, themeHeader, cx + 40f, cy + setCardH - 85f);

        // Render Theme Selection Buttons (0 & 1)
        for (int i = 0; i < 2; i++) {
            renderButton(settingsButtons.get(i), buttonFont);
        }

        // Active Theme Style Description Box
        float descY = cy + setCardH - 200f;
        Color descBg = isPaper ? new Color(0.93f, 0.89f, 0.82f, 0.90f) : new Color(0.09f, 0.14f, 0.22f, 0.85f);
        Color descBrd = isPaper ? new Color(0.75f, 0.70f, 0.64f, 0.75f) : new Color(0.24f, 0.38f, 0.55f, 0.65f);
        drawer.setColor(descBg);
        drawer.filledRectangle(cx + 40f, descY - 45f, 520f, 44f);
        drawer.setColor(descBrd);
        drawer.rectangle(cx + 40f, descY - 45f, 520f, 44f, 1.5f);

        Color descTxtCol = isPaper ? new Color(0.25f, 0.25f, 0.28f, 1.0f) : new Color(0.80f, 0.90f, 1.0f, 1.0f);
        smallFont.setColor(descTxtCol);
        String descStr = isPaper ?
                "Active Theme: Paper Notebook — Ruled texture, fountain blue ink, pencil notes and handwriting font." :
                "Active Theme: Modern Glass — Dark glass interface, neon blue glow and modern typeface.";
        layout.setText(smallFont, descStr);
        smallFont.draw(batch, descStr, cx + 55f, descY - 20f);

        // Category 2: Audio
        headerFont.setColor(catTitleCol);
        String audioHeader = "AUDIO OPTIONS";
        layout.setText(headerFont, audioHeader);
        headerFont.draw(batch, audioHeader, cx + 40f, cy + 205f);

        // Render Sound and Back Buttons (2 & 3)
        for (int i = 2; i < settingsButtons.size(); i++) {
            renderButton(settingsButtons.get(i), buttonFont);
        }
    }

    private void renderHowToPlayModal() {
        boolean isPaper = (currentTheme == GameTheme.PAPER);
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
        renderGuideCard(col1, row2, cardW, cardH, "HINTS & UNDO", "Stuck? Tap the Hint button for a clue.", "Undo mistakes anytime with the [Z] key.");
        renderGuideCard(col2, row2, cardW, cardH, "KEYBOARD CONTROLS", "Navigate with Arrow keys, enter 1-9 digits.", "[T] toggles theme, [ESC] pauses game.");

        renderButton(modalButtons.get(0), buttonFont);
    }

    private void renderGuideCard(float x, float y, float w, float h, String title, String l1, String l2) {
        boolean isPaper = (currentTheme == GameTheme.PAPER);

        if (isPaper) {
            drawer.setColor(new Color(0.95f, 0.93f, 0.88f, 0.95f));
            drawer.filledRectangle(x, y, w, h);
            drawer.setColor(new Color(0.75f, 0.72f, 0.66f, 0.85f));
            drawer.rectangle(x, y, w, h, 1.5f);

            buttonFont.setColor(0.12f, 0.25f, 0.55f, 1.0f);
            buttonFont.draw(batch, title, x + 14f, y + h - 14f);

            smallFont.setColor(0.22f, 0.22f, 0.25f, 0.95f);
            smallFont.draw(batch, l1, x + 14f, y + h - 40f);
            smallFont.draw(batch, l2, x + 14f, y + h - 62f);
        } else {
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
    }

    private void renderPauseModal() {
        boolean isPaper = (currentTheme == GameTheme.PAPER);
        float cardW = 380f;
        float cardH = 370f;
        float cx = (SCREEN_WIDTH - cardW) / 2f;
        float cy = (SCREEN_HEIGHT - cardH) / 2f;

        renderModalCard(cardW, cardH, "GAME PAUSED");

        for (UIElement btn : pauseButtons) {
            renderButton(btn, buttonFont);
        }
    }

    private void renderVictoryModal() {
        boolean isPaper = (currentTheme == GameTheme.PAPER);
        float cardW = 440f;
        float cardH = 370f;
        float cx = (SCREEN_WIDTH - cardW) / 2f;
        float cy = (SCREEN_HEIGHT - cardH) / 2f;

        renderModalCard(cardW, cardH, "CONGRATULATIONS!");

        Color victoryTitleCol = isPaper ? new Color(0.12f, 0.50f, 0.25f, 1.0f) : new Color(0.25f, 1.0f, 0.65f, 1.0f);
        headerFont.setColor(victoryTitleCol);
        String sub = "SUDOKU SOLVED!";
        layout.setText(headerFont, sub);
        headerFont.draw(batch, sub, cx + (cardW - layout.width) / 2f, cy + cardH - 85f);

        // Stats
        String timeInfo = "Completion Time: " + sudoku.getFormattedTime();
        String diffInfo = "Difficulty: " + sudoku.getDifficulty().label;
        String mistInfo = "Mistakes: " + sudoku.getMistakes() + "/" + sudoku.getMaxMistakes();

        Color statCol = isPaper ? new Color(0.22f, 0.22f, 0.25f, 1.0f) : new Color(0.85f, 0.92f, 1.0f, 1.0f);
        buttonFont.setColor(statCol);
        layout.setText(buttonFont, timeInfo);
        buttonFont.draw(batch, timeInfo, cx + (cardW - layout.width) / 2f, cy + 225f);
        layout.setText(buttonFont, diffInfo);
        buttonFont.draw(batch, diffInfo, cx + (cardW - layout.width) / 2f, cy + 190f);
        layout.setText(buttonFont, mistInfo);
        buttonFont.draw(batch, mistInfo, cx + (cardW - layout.width) / 2f, cy + 155f);

        for (UIElement btn : victoryButtons) {
            renderButton(btn, buttonFont);
        }
    }

    private void renderGameOverModal() {
        boolean isPaper = (currentTheme == GameTheme.PAPER);
        float cardW = 420f;
        float cardH = 310f;
        float cx = (SCREEN_WIDTH - cardW) / 2f;
        float cy = (SCREEN_HEIGHT - cardH) / 2f;

        renderModalCard(cardW, cardH, "GAME OVER");

        Color msgCol = isPaper ? new Color(0.75f, 0.20f, 0.20f, 1.0f) : new Color(1.0f, 0.40f, 0.45f, 1.0f);
        headerFont.setColor(msgCol);
        String info = "3 Mistakes Limit Reached!";
        layout.setText(headerFont, info);
        headerFont.draw(batch, info, cx + (cardW - layout.width) / 2f, cy + cardH - 95f);

        for (UIElement btn : gameOverButtons) {
            renderButton(btn, buttonFont);
        }
    }

    private void renderModalCard(float w, float h, String title) {
        boolean isPaper = (currentTheme == GameTheme.PAPER);
        float cx = (SCREEN_WIDTH - w) / 2f;
        float cy = (SCREEN_HEIGHT - h) / 2f;

        if (isPaper) {
            drawer.setColor(new Color(0.20f, 0.18f, 0.15f, 0.40f));
            drawer.filledRectangle(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);

            drawer.setColor(new Color(0.75f, 0.70f, 0.65f, 0.45f));
            drawer.filledRectangle(cx + 6f, cy - 6f, w, h);

            drawer.setColor(new Color(0.98f, 0.96f, 0.92f, 0.98f));
            drawer.filledRectangle(cx, cy, w, h);
            drawer.setColor(new Color(0.35f, 0.32f, 0.30f, 0.95f));
            drawer.rectangle(cx, cy, w, h, 2.5f);

            titleFont.setColor(0.15f, 0.22f, 0.45f, 1.0f);
            layout.setText(titleFont, title);
            titleFont.draw(batch, title, cx + (w - layout.width) / 2f, cy + h - 35f);

            drawer.setColor(new Color(0.70f, 0.65f, 0.60f, 0.70f));
            drawer.line(cx + 35f, cy + h - 65f, cx + w - 35f, cy + h - 65f, 2f);
        } else {
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
            drawer.line(cx + 35f, cy + h - 65f, cx + w - 35f, cy + h - 65f, 2f);
        }
    }

    @Override
    public void dispose() {
        if (pixelTexture != null) pixelTexture.dispose();
        if (batch != null) batch.dispose();
        disposeFonts();
        if (sfx != null) sfx.dispose();
        if (rayHandler != null) rayHandler.dispose();
        if (world != null) world.dispose();
    }
}
