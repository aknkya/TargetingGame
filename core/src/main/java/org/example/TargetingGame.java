package org.example;

import aurelienribon.tweenengine.BaseTween;
import aurelienribon.tweenengine.Timeline;
import aurelienribon.tweenengine.Tween;
import aurelienribon.tweenengine.TweenCallback;
import aurelienribon.tweenengine.TweenEquations;
import aurelienribon.tweenengine.TweenManager;
import box2dLight.PointLight;
import box2dLight.RayHandler;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import space.earlygrey.shapedrawer.ShapeDrawer;

import java.util.ArrayList;
import java.util.List;

/**
 * Sudoku Master - Paper Edition (Kâğıt & Kalem Teması)
 * Krem/kırık beyaz zemin, ince kalem çizgisi kenarlıklar, yumuşak gölgeler ve el yazısı hissi.
 * Universal Tween Engine, Preferences (SaveManager), Scene2D Menü & Ayarlar,
 * Sıcak Masa Lambası Box2DLights ve Android dokunmatik desteğiyle güçlendirilmiştir.
 */
public class TargetingGame extends ApplicationAdapter {

    public static final float SCREEN_WIDTH = 1280f;
    public static final float SCREEN_HEIGHT = 720f;
    public static final float PPM = 32f; // Box2D için Piksel/Metre oranı

    public enum GameState {
        MAIN_MENU,
        PLAYING,
        SETTINGS,
        STATS,
        HOW_TO_PLAY,
        PAUSED,
        VICTORY,
        GAME_OVER
    }

    // Durum ve Yöneticiler
    private GameState state = GameState.MAIN_MENU;
    private GameState stateBeforeSettings = GameState.MAIN_MENU;
    private SaveManager saveManager;
    private SoundEffects sfx;
    private SudokuEngine sudoku;

    // Universal Tween Engine
    private TweenManager tweenManager;
    private final Cell[][] cells = new Cell[9][9];
    private final ScreenFade screenFade = new ScreenFade();
    private boolean isTransitioning = false;

    // Çizim ve Kamera
    private OrthographicCamera camera;
    private OrthographicCamera box2dCamera;
    private Viewport viewport;
    private SpriteBatch batch;
    private ShapeDrawer drawer;
    private Texture pixelTexture;
    private final GlyphLayout layout = new GlyphLayout();

    // Scene2D Arayüz (Menü, Ayarlar, İstatistikler)
    private Stage stage;
    private PaperSkinHelper paperSkinHelper;
    private Skin skin;
    private Table menuTable;
    private Table settingsTable;
    private Table statsTable;
    private TextButton resumeMenuButton;
    private TextButton soundSettingButton;
    private TextButton lightSettingButton;
    private Label bestTimeEasyLabel;
    private Label bestTimeMediumLabel;
    private Label bestTimeHardLabel;
    private Label bestTimeExpertLabel;
    private Label totalGamesLabel;
    private Label totalPlayTimeLabel;

    // Box2D ve Pastel Kâğıt Konfetisi
    private World world;
    private Body groundBody;
    private final Array<Body> confettiBodies = new Array<>();
    private final List<Color> confettiColors = new ArrayList<>();
    private final List<Vector2> confettiSizes = new ArrayList<>();

    // Box2DLights (Sıcak Masa Lambası Efekti)
    private RayHandler rayHandler;
    private PointLight cursorLight;
    private PointLight boardAmbientLight;

    // Izgara Ölçüleri
    private final float boardSize = 540f;
    private final float cellSize = boardSize / 9f; // 60px
    private final float boardX = 130f;
    private final float boardY = 85f;

    // Seçili Hücre
    private int selectedRow = 4;
    private int selectedCol = 4;

    // Yazı Tipleri (Kâğıt Teması)
    private BitmapFont titleFont;
    private BitmapFont headerFont;
    private BitmapFont clueDigitFont;
    private BitmapFont playerDigitFont;
    private BitmapFont noteFont;
    private BitmapFont buttonFont;
    private BitmapFont smallFont;

    // Oyun İçi Etkileşim Butonları (HUD & NumPad)
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
                         Color textColor, Color hoverTextColor, Runnable onClick) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.text = text;
            this.subText = null;
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

    private final List<UIElement> hudButtons = new ArrayList<>();
    private final List<UIElement> actionButtons = new ArrayList<>();
    private final List<UIElement> numPadButtons = new ArrayList<>();
    private final List<UIElement> pauseButtons = new ArrayList<>();
    private final List<UIElement> modalButtons = new ArrayList<>();
    private final List<UIElement> victoryButtons = new ArrayList<>();
    private final List<UIElement> gameOverButtons = new ArrayList<>();
    private UIElement diffPillButton;

    private SudokuEngine.Difficulty selectedDifficulty = SudokuEngine.Difficulty.MEDIUM;
    private final Vector3 lastTouchPos = new Vector3(SCREEN_WIDTH / 2f, SCREEN_HEIGHT / 2f, 0);

    @Override
    public void create() {
        // 1. Kamera ve Viewport Kurulumu (FitViewport ile farklı ekran oranlarında kararlı ölçekleme)
        camera = new OrthographicCamera();
        viewport = new FitViewport(SCREEN_WIDTH, SCREEN_HEIGHT, camera);
        viewport.apply();
        camera.position.set(SCREEN_WIDTH / 2f, SCREEN_HEIGHT / 2f, 0);
        camera.update();

        box2dCamera = new OrthographicCamera();
        box2dCamera.setToOrtho(false, SCREEN_WIDTH / PPM, SCREEN_HEIGHT / PPM);

        batch = new SpriteBatch();

        // 2. Kayıt Sistemi & Ses Yönetimi
        saveManager = new SaveManager();
        sfx = new SoundEffects();
        sfx.init();
        sfx.setPaperTheme(true); // Kesinlikle KÂĞIT teması
        if (!saveManager.isSoundEnabled()) {
            sfx.toggleSound();
        }

        // 3. Sudoku Motoru & Tahta Durumu
        sudoku = new SudokuEngine();

        // 4. Universal Tween Engine Kurulumu & Kaydı
        tweenManager = new TweenManager();
        Tween.registerAccessor(Cell.class, new CellAccessor());
        Tween.registerAccessor(ScreenFade.class, new ScreenFadeAccessor());

        // 9x9 Hücre Modellerini Oluştur
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                cells[r][c] = new Cell(r, c);
            }
        }
        // Başlangıç hücresini seç
        cells[selectedRow][selectedCol].setSelectHighlight(1.0f);
        cells[selectedRow][selectedCol].setScale(1.04f);

        // 5. Çizim, Yazı Tipleri ve Fizik / Işık Sistemi
        initShapeDrawer();
        initFonts();
        initBox2D();
        initLighting();

        // 6. Scene2D Sahnesi ve Kâğıt Temalı Skin
        stage = new Stage(viewport, batch);
        paperSkinHelper = new PaperSkinHelper();
        skin = paperSkinHelper.createPaperSkin();
        buildScene2DUI();

        // 7. Oyun İçi HUD ve NumPad Butonları
        buildGameUI();

        // 8. Girdi İşleyicisi (InputMultiplexer ile Scene2D + Dokunmatik/Fare Hibrit Desteği)
        setupInput();

        Gdx.graphics.setTitle("Sudoku Master - Kâğıt Edisyonu");
    }

    /**
     * ShapeDrawer için 1x1 beyaz piksel dokusu üretir.
     */
    private void initShapeDrawer() {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        pixelTexture = new Texture(pixmap);
        pixmap.dispose();

        TextureRegion region = new TextureRegion(pixelTexture, 0, 0, 1, 1);
        drawer = new ShapeDrawer(batch, region);
    }

    /**
     * Kâğıt teması için FreeType fontlarını yükler.
     * Başlangıç ipuçları gazete/baskı stili (36pt), kullanıcının eklediği sayılar ise
     * dolgun ve net el yazısı stiliyle (40pt) oluşturulur.
     */
    private void initFonts() {
        disposeFonts();

        FileHandle segoeHandle = Gdx.files.internal("fonts/segoeui.ttf");
        FileHandle handHandle = Gdx.files.internal("fonts/handwriting.ttf");
        if (!handHandle.exists()) handHandle = segoeHandle;
        if (!segoeHandle.exists()) segoeHandle = handHandle;

        if (segoeHandle.exists() || handHandle.exists()) {
            FreeTypeFontParameter param = new FreeTypeFontParameter();
            param.minFilter = Texture.TextureFilter.Linear;
            param.magFilter = Texture.TextureFilter.Linear;
            param.characters = PaperSkinHelper.TURKISH_CHARS;

            FreeTypeFontGenerator segoeGen = new FreeTypeFontGenerator(segoeHandle);
            param.size = 36;
            titleFont = segoeGen.generateFont(param);

            param.size = 22;
            headerFont = segoeGen.generateFont(param);

            // Başlangıç ipucu sayıları (Kitap / Gazete baskı hissi - 36pt)
            param.size = 36;
            clueDigitFont = segoeGen.generateFont(param);

            // Kurşun kalem notları (Net ve rahat okunan 15pt)
            param.size = 15;
            noteFont = segoeGen.generateFont(param);

            param.size = 17;
            buttonFont = segoeGen.generateFont(param);

            param.size = 13;
            smallFont = segoeGen.generateFont(param);
            segoeGen.dispose();

            // Kullanıcının eklediği yeni sayılar (Otantik El Yazısı / Dolgun Kalem Mürekkebi - 40pt)
            FreeTypeFontGenerator handGen = new FreeTypeFontGenerator(handHandle);
            param.size = 40;
            playerDigitFont = handGen.generateFont(param);
            handGen.dispose();
        } else {
            titleFont = new BitmapFont();
            headerFont = new BitmapFont();
            clueDigitFont = new BitmapFont();
            playerDigitFont = new BitmapFont();
            noteFont = new BitmapFont();
            buttonFont = new BitmapFont();
            smallFont = new BitmapFont();
        }
    }

    private void disposeFonts() {
        if (titleFont != null) { titleFont.dispose(); titleFont = null; }
        if (headerFont != null) { headerFont.dispose(); headerFont = null; }
        if (clueDigitFont != null) { clueDigitFont.dispose(); clueDigitFont = null; }
        if (playerDigitFont != null) { playerDigitFont.dispose(); playerDigitFont = null; }
        if (noteFont != null) { noteFont.dispose(); noteFont = null; }
        if (buttonFont != null) { buttonFont.dispose(); buttonFont = null; }
        if (smallFont != null) { smallFont.dispose(); smallFont = null; }
    }

    /**
     * Zafer kutlaması için Box2D zemin gövdesi.
     */
    private void initBox2D() {
        world = new World(new Vector2(0, -22f), true);

        BodyDef bdef = new BodyDef();
        bdef.type = BodyDef.BodyType.StaticBody;
        bdef.position.set(SCREEN_WIDTH / 2f / PPM, 15f / PPM);
        groundBody = world.createBody(bdef);

        PolygonShape shape = new PolygonShape();
        shape.setAsBox((SCREEN_WIDTH / 2f) / PPM, 15f / PPM);
        groundBody.createFixture(shape, 0f);
        shape.dispose();
    }

    /**
     * Box2DLights: Sıcak Masa Lambası Efekti (Neon değil, yumuşak sarı/amber masa lambası).
     */
    private void initLighting() {
        RayHandler.useDiffuseLight(true);
        rayHandler = new RayHandler(world);
        rayHandler.setBlurNum(2);

        // Kâğıt yüzeyine düşen sıcak gün ışığı / oda aydınlatması
        rayHandler.setAmbientLight(0.96f, 0.94f, 0.90f, 0.92f);

        // Sıcak sarı / amber tonlu masa lambası ışığı (Soft light)
        Color lampColor = new Color(1.0f, 0.94f, 0.82f, 0.32f);
        cursorLight = new PointLight(rayHandler, 64, lampColor, 12.0f, SCREEN_WIDTH / 2f / PPM, SCREEN_HEIGHT / 2f / PPM);
        cursorLight.setSoft(true);
        cursorLight.setSoftnessLength(2.5f);
        cursorLight.setActive(saveManager.isLightEnabled());

        // Tahta üzerine yumuşak odak ışığı
        boardAmbientLight = new PointLight(rayHandler, 64, new Color(1.0f, 0.97f, 0.90f, 0.16f), 18f,
                (boardX + boardSize / 2f) / PPM, (boardY + boardSize / 2f) / PPM);
        boardAmbientLight.setSoft(true);
    }

    /**
     * Zafer Konfetisi: Kâğıt parçacıkları / küçük mat origami kareleri (pastel tonlar).
     */
    private void triggerVictoryCelebration() {
        for (Body b : confettiBodies) {
            world.destroyBody(b);
        }
        confettiBodies.clear();
        confettiColors.clear();
        confettiSizes.clear();

        // Pastel ve mat kâğıt renk paleti
        Color[] paperPalette = new Color[] {
                new Color(0.92f, 0.82f, 0.72f, 1.0f), // Saman / Kraft Kâğıdı
                new Color(0.72f, 0.82f, 0.90f, 1.0f), // Pastel Gök Mavisi
                new Color(0.92f, 0.75f, 0.78f, 1.0f), // Pastel Pudra Gül
                new Color(0.76f, 0.88f, 0.78f, 1.0f), // Pastel Adaçayı Yeşili
                new Color(0.96f, 0.90f, 0.70f, 1.0f), // Pastel Vanilya Sarısı
                new Color(0.85f, 0.80f, 0.88f, 1.0f)  // Pastel Lavanta
        };

        for (int i = 0; i < 70; i++) {
            BodyDef bdef = new BodyDef();
            bdef.type = BodyDef.BodyType.DynamicBody;
            float spawnX = MathUtils.random(boardX - 50f, boardX + boardSize + 50f);
            float spawnY = MathUtils.random(SCREEN_HEIGHT - 40f, SCREEN_HEIGHT + 140f);
            bdef.position.set(spawnX / PPM, spawnY / PPM);

            Body body = world.createBody(bdef);

            PolygonShape box = new PolygonShape();
            // Rastgele dikdörtgen/kare kâğıt kırpıntı boyutları
            float halfW = MathUtils.random(5f, 11f) / PPM;
            float halfH = MathUtils.random(5f, 11f) / PPM;
            box.setAsBox(halfW, halfH);

            FixtureDef fdef = new FixtureDef();
            fdef.shape = box;
            fdef.density = 0.8f;   // Kâğıt gibi hafif
            fdef.friction = 0.6f;
            fdef.restitution = 0.55f;
            body.createFixture(fdef);
            box.dispose();

            body.setLinearVelocity(MathUtils.random(-7f, 7f), MathUtils.random(-4f, 2f));
            body.setAngularVelocity(MathUtils.random(-10f, 10f));

            confettiBodies.add(body);
            confettiColors.add(paperPalette[i % paperPalette.length]);
            confettiSizes.add(new Vector2(halfW * 2f * PPM, halfH * 2f * PPM));
        }

        sfx.playVictory();
    }

    // ==========================================
    // SCENE2D MENÜ, AYARLAR VE İSTATİSTİKLER EKRANI
    // ==========================================

    private void buildScene2DUI() {
        stage.clear();

        // 1. ANA MENÜ TABLOSU
        menuTable = new Table();
        menuTable.setFillParent(true);
        menuTable.center();

        Table menuCard = new Table();
        menuCard.setBackground(skin.getDrawable("card-bg"));
        menuCard.pad(30f, 40f, 35f, 40f);

        Label titleLabel = new Label("SUDOKU MASTER", skin, "title");
        Label subtitleLabel = new Label("KÂĞIT & KALEM EDİSYONU", skin, "small");

        // Zorluk Butonları Satırı
        Table diffTable = new Table();
        for (SudokuEngine.Difficulty diff : SudokuEngine.Difficulty.values()) {
            final SudokuEngine.Difficulty d = diff;
            TextButton diffBtn = new TextButton(d.label, skin, "small");
            diffBtn.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    selectedDifficulty = d;
                    sfx.playClick();
                }
            });
            diffTable.add(diffBtn).width(75f).height(32f).pad(3f);
        }

        TextButton playButton = new TextButton("YENİ OYUN", skin, "play");
        playButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                sfx.playClick();
                startNewGame(selectedDifficulty);
            }
        });

        resumeMenuButton = new TextButton("DEVAM ET", skin, "default");
        resumeMenuButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (saveManager.hasSavedGame()) {
                    sfx.playClick();
                    resumeSavedGame();
                }
            }
        });

        TextButton statsButton = new TextButton("İSTATİSTİKLER", skin, "default");
        statsButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                sfx.playClick();
                updateStatsData();
                transitionToState(GameState.STATS);
            }
        });

        TextButton settingsButton = new TextButton("AYARLAR", skin, "default");
        settingsButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                sfx.playClick();
                stateBeforeSettings = GameState.MAIN_MENU;
                transitionToState(GameState.SETTINGS);
            }
        });

        TextButton quitButton = new TextButton("ÇIKIŞ", skin, "default");
        quitButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                sfx.playClick();
                Gdx.app.exit();
            }
        });

        menuCard.add(subtitleLabel).padBottom(4f).row();
        menuCard.add(titleLabel).padBottom(15f).row();
        menuCard.add(diffTable).padBottom(20f).row();
        menuCard.add(playButton).width(300f).height(44f).padBottom(10f).row();
        menuCard.add(resumeMenuButton).width(300f).height(44f).padBottom(10f).row();
        menuCard.add(statsButton).width(300f).height(44f).padBottom(10f).row();
        menuCard.add(settingsButton).width(300f).height(44f).padBottom(10f).row();
        menuCard.add(quitButton).width(300f).height(44f).row();

        menuTable.add(menuCard);
        stage.addActor(menuTable);

        // 2. AYARLAR TABLOSU
        settingsTable = new Table();
        settingsTable.setFillParent(true);
        settingsTable.center();
        settingsTable.setVisible(false);

        Table settingsCard = new Table();
        settingsCard.setBackground(skin.getDrawable("card-bg"));
        settingsCard.pad(35f, 45f, 35f, 45f);

        Label setHeader = new Label("AYARLAR", skin, "title");

        soundSettingButton = new TextButton(getSoundButtonText(), skin, "default");
        soundSettingButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                boolean newState = !saveManager.isSoundEnabled();
                saveManager.setSoundEnabled(newState);
                sfx.toggleSound();
                soundSettingButton.setText(getSoundButtonText());
                sfx.playClick();
            }
        });

        lightSettingButton = new TextButton(getLightButtonText(), skin, "default");
        lightSettingButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                boolean newState = !saveManager.isLightEnabled();
                saveManager.setLightEnabled(newState);
                if (cursorLight != null) cursorLight.setActive(newState);
                lightSettingButton.setText(getLightButtonText());
                sfx.playClick();
            }
        });

        TextButton settingsBackBtn = new TextButton("< GERİ DÖN", skin, "play");
        settingsBackBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                sfx.playClick();
                transitionToState(stateBeforeSettings);
            }
        });

        settingsCard.add(setHeader).padBottom(25f).row();
        settingsCard.add(soundSettingButton).width(320f).height(48f).padBottom(14f).row();
        settingsCard.add(lightSettingButton).width(320f).height(48f).padBottom(22f).row();
        settingsCard.add(settingsBackBtn).width(320f).height(46f).row();

        settingsTable.add(settingsCard);
        stage.addActor(settingsTable);

        // 3. İSTATİSTİKLER TABLOSU
        statsTable = new Table();
        statsTable.setFillParent(true);
        statsTable.center();
        statsTable.setVisible(false);

        Table statsCard = new Table();
        statsCard.setBackground(skin.getDrawable("card-bg"));
        statsCard.pad(30f, 40f, 35f, 40f);

        Label statsHeader = new Label("İSTATİSTİKLER", skin, "title");

        totalGamesLabel = new Label("Tamamlanan Oyun: 0", skin, "default");
        bestTimeEasyLabel = new Label("En İyi (Kolay): --:--", skin, "default");
        bestTimeMediumLabel = new Label("En İyi (Orta): --:--", skin, "default");
        bestTimeHardLabel = new Label("En İyi (Zor): --:--", skin, "default");
        bestTimeExpertLabel = new Label("En İyi (Uzman): --:--", skin, "default");
        totalPlayTimeLabel = new Label("Toplam Oynama: 0 dk", skin, "small");

        TextButton statsBackBtn = new TextButton("< GERİ DÖN", skin, "play");
        statsBackBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                sfx.playClick();
                transitionToState(GameState.MAIN_MENU);
            }
        });

        statsCard.add(statsHeader).padBottom(20f).row();
        statsCard.add(totalGamesLabel).padBottom(8f).row();
        statsCard.add(bestTimeEasyLabel).padBottom(6f).row();
        statsCard.add(bestTimeMediumLabel).padBottom(6f).row();
        statsCard.add(bestTimeHardLabel).padBottom(6f).row();
        statsCard.add(bestTimeExpertLabel).padBottom(14f).row();
        statsCard.add(totalPlayTimeLabel).padBottom(22f).row();
        statsCard.add(statsBackBtn).width(280f).height(44f).row();

        statsTable.add(statsCard);
        stage.addActor(statsTable);

        updateResumeButtonState();
    }

    private String getSoundButtonText() {
        return saveManager.isSoundEnabled() ? "SES EFEKTLERİ: AÇIK" : "SES EFEKTLERİ: KAPALI";
    }

    private String getLightButtonText() {
        return saveManager.isLightEnabled() ? "MASA LAMBASI IŞIĞI: AÇIK" : "MASA LAMBASI IŞIĞI: KAPALI";
    }

    private void updateResumeButtonState() {
        boolean canResume = saveManager.hasSavedGame();
        resumeMenuButton.setDisabled(!canResume);
    }

    private void updateStatsData() {
        totalGamesLabel.setText("Tamamlanan Oyun: " + saveManager.getGamesCompleted());
        bestTimeEasyLabel.setText("En İyi (Kolay): " + SaveManager.formatTime(saveManager.getBestTime(SudokuEngine.Difficulty.EASY)));
        bestTimeMediumLabel.setText("En İyi (Orta): " + SaveManager.formatTime(saveManager.getBestTime(SudokuEngine.Difficulty.MEDIUM)));
        bestTimeHardLabel.setText("En İyi (Zor): " + SaveManager.formatTime(saveManager.getBestTime(SudokuEngine.Difficulty.HARD)));
        bestTimeExpertLabel.setText("En İyi (Uzman): " + SaveManager.formatTime(saveManager.getBestTime(SudokuEngine.Difficulty.EXPERT)));
        totalPlayTimeLabel.setText("Toplam Süre: " + SaveManager.formatTotalTime(saveManager.getTotalPlayTime()));
    }

    /**
     * Ekranlar arası Tween ile yumuşak fade (kâğıt tonu açılıp kapanması) animasyonu.
     */
    private void transitionToState(final GameState targetState) {
        if (isTransitioning) return;
        isTransitioning = true;

        Timeline.createSequence()
                .push(Tween.to(screenFade, ScreenFadeAccessor.TYPE_ALPHA, 0.16f)
                        .target(1.0f)
                        .ease(TweenEquations.easeOutQuad)
                        .setCallback(new TweenCallback() {
                            @Override
                            public void onEvent(int type, BaseTween<?> source) {
                                state = targetState;
                                updateScene2DVisibility();
                                isTransitioning = false;
                            }
                        }))
                .push(Tween.to(screenFade, ScreenFadeAccessor.TYPE_ALPHA, 0.18f)
                        .target(0.0f)
                        .ease(TweenEquations.easeInQuad))
                .start(tweenManager);
    }

    private void updateScene2DVisibility() {
        menuTable.setVisible(state == GameState.MAIN_MENU);
        settingsTable.setVisible(state == GameState.SETTINGS);
        statsTable.setVisible(state == GameState.STATS);
        if (state == GameState.MAIN_MENU) {
            updateResumeButtonState();
        }
    }

    // ==========================================
    // OYUN İÇİ ARAYÜZ (HUD & NUMPAD)
    // ==========================================

    private void buildGameUI() {
        hudButtons.clear();
        actionButtons.clear();
        numPadButtons.clear();
        pauseButtons.clear();
        modalButtons.clear();
        victoryButtons.clear();
        gameOverButtons.clear();

        Color base = new Color(0.95f, 0.93f, 0.88f, 0.95f);
        Color hover = new Color(0.89f, 0.86f, 0.79f, 1.0f);
        Color border = new Color(0.68f, 0.64f, 0.58f, 0.85f);
        Color hoverBorder = new Color(0.35f, 0.32f, 0.28f, 1.0f);
        Color txt = new Color(0.20f, 0.20f, 0.24f, 1.0f);

        Color playBase = new Color(0.86f, 0.93f, 0.86f, 0.95f);
        Color playHov = new Color(0.78f, 0.90f, 0.78f, 1.0f);
        Color playBrd = new Color(0.40f, 0.65f, 0.44f, 0.90f);
        Color playHovBrd = new Color(0.24f, 0.50f, 0.27f, 1.0f);
        Color playTxt = new Color(0.12f, 0.35f, 0.16f, 1.0f);

        Color quitBase = new Color(0.95f, 0.87f, 0.87f, 0.95f);
        Color quitHov = new Color(0.90f, 0.78f, 0.78f, 1.0f);
        Color quitBrd = new Color(0.75f, 0.48f, 0.48f, 0.90f);
        Color quitHovBrd = new Color(0.60f, 0.25f, 0.25f, 1.0f);
        Color quitTxt = new Color(0.50f, 0.14f, 0.16f, 1.0f);

        // --- HUD Üst Bar Butonları ---
        hudButtons.add(new UIElement(
                25f, SCREEN_HEIGHT - 54f, 105f, 38f, "MENÜ",
                base, hover, border, hoverBorder, txt, txt,
                () -> {
                    saveManager.saveGame(sudoku);
                    state = GameState.PAUSED;
                }
        ));

        hudButtons.add(new UIElement(
                140f, SCREEN_HEIGHT - 54f, 115f, 38f, "AYARLAR",
                base, hover, border, hoverBorder, txt, txt,
                () -> {
                    saveManager.saveGame(sudoku);
                    stateBeforeSettings = GameState.PAUSED;
                    transitionToState(GameState.SETTINGS);
                }
        ));

        hudButtons.add(new UIElement(
                SCREEN_WIDTH - 275f, SCREEN_HEIGHT - 54f, 120f, 38f,
                saveManager.isSoundEnabled() ? "SES: AÇIK" : "SES: KAPALI",
                base, hover, border, hoverBorder, txt, txt,
                this::toggleAudio
        ));

        hudButtons.add(new UIElement(
                SCREEN_WIDTH - 145f, SCREEN_HEIGHT - 54f, 120f, 38f, "+ YENİ",
                playBase, playHov, playBrd, playHovBrd, playTxt, playTxt,
                () -> startNewGame(selectedDifficulty)
        ));

        float midX = SCREEN_WIDTH / 2f;
        float headerY = SCREEN_HEIGHT - 32f;
        diffPillButton = new UIElement(
                midX - 220f, headerY - 14f, 96f, 28f, selectedDifficulty.label,
                new Color(0.86f, 0.91f, 0.97f, 0.95f), new Color(0.76f, 0.85f, 0.95f, 1.0f),
                new Color(0.38f, 0.58f, 0.82f, 0.90f), new Color(0.22f, 0.42f, 0.72f, 1.0f),
                new Color(0.14f, 0.30f, 0.58f, 1.0f), new Color(0.14f, 0.30f, 0.58f, 1.0f),
                () -> {
                    int nextIdx = (selectedDifficulty.ordinal() + 1) % SudokuEngine.Difficulty.values().length;
                    selectedDifficulty = SudokuEngine.Difficulty.values()[nextIdx];
                    startNewGame(selectedDifficulty);
                }
        );
        hudButtons.add(diffPillButton);

        // --- Sağ Kontrol Paneli (Geri Al, Notlar, Sil, İpucu) ---
        float panelX = 735f;
        float actBtnW = 100f;
        float actBtnH = 50f;
        float actGap = 12f;
        float actY = 560f;

        actionButtons.add(new UIElement(
                panelX, actY, actBtnW, actBtnH, "GERİ AL",
                base, hover, border, hoverBorder, txt, txt,
                () -> {
                    if (sudoku.undo()) {
                        sfx.playPlace();
                        saveManager.saveGame(sudoku);
                    }
                }
        ));

        actionButtons.add(new UIElement(
                panelX + (actBtnW + actGap), actY, actBtnW, actBtnH, "NOTLAR",
                base, hover, border, hoverBorder, txt, txt,
                () -> {
                    sudoku.togglePencilMode();
                    sfx.playClick();
                }
        ));

        actionButtons.add(new UIElement(
                panelX + (actBtnW + actGap) * 2, actY, actBtnW, actBtnH, "SİL",
                quitBase, quitHov, quitBrd, quitHovBrd, quitTxt, quitTxt,
                () -> {
                    if (sudoku.erase(selectedRow, selectedCol)) {
                        sfx.playErase();
                        saveManager.saveGame(sudoku);
                    }
                }
        ));

        Color hintBase = new Color(0.96f, 0.92f, 0.82f, 0.95f);
        Color hintHov = new Color(0.92f, 0.86f, 0.68f, 1.0f);
        Color hintBrd = new Color(0.76f, 0.62f, 0.28f, 0.90f);
        Color hintHovBrd = new Color(0.86f, 0.70f, 0.18f, 1.0f);
        Color hintTxt = new Color(0.48f, 0.32f, 0.08f, 1.0f);
        actionButtons.add(new UIElement(
                panelX + (actBtnW + actGap) * 3, actY, actBtnW, actBtnH, "İPUCU",
                hintBase, hintHov, hintBrd, hintHovBrd, hintTxt, hintTxt,
                () -> {
                    if (sudoku.giveHint(selectedRow, selectedCol)) {
                        sfx.playVictory();
                        animateNumberPlacement(selectedRow, selectedCol, false);
                        saveManager.saveGame(sudoku);
                    }
                }
        ));

        // --- 3x3 NumPad (1-9 Rakam Tuşları) ---
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
                    base, hover, border, hoverBorder, txt, txt,
                    () -> handleNumberInput(digit)
            ));
        }

        // --- Duraklatma Modalı Butonları ---
        float pauseCardW = 380f;
        float pauseCardH = 370f;
        float pauseCx = (SCREEN_WIDTH - pauseCardW) / 2f;
        float pauseCy = (SCREEN_HEIGHT - pauseCardH) / 2f;
        float pauseBtnW = 300f;
        float pauseBtnH = 46f;
        float pauseBx = pauseCx + 40f;

        pauseButtons.add(new UIElement(
                pauseBx, pauseCy + 235f, pauseBtnW, pauseBtnH, "DEVAM ET",
                playBase, playHov, playBrd, playHovBrd, playTxt, playTxt,
                () -> state = GameState.PLAYING
        ));
        pauseButtons.add(new UIElement(
                pauseBx, pauseCy + 175f, pauseBtnW, pauseBtnH, "AYARLAR",
                base, hover, border, hoverBorder, txt, txt,
                () -> {
                    stateBeforeSettings = GameState.PAUSED;
                    transitionToState(GameState.SETTINGS);
                }
        ));
        pauseButtons.add(new UIElement(
                pauseBx, pauseCy + 115f, pauseBtnW, pauseBtnH, "+ YENİ OYUN",
                base, hover, border, hoverBorder, txt, txt,
                () -> startNewGame(selectedDifficulty)
        ));
        pauseButtons.add(new UIElement(
                pauseBx, pauseCy + 55f, pauseBtnW, pauseBtnH, "ANA MENÜ",
                quitBase, quitHov, quitBrd, quitHovBrd, quitTxt, quitTxt,
                () -> {
                    saveManager.saveGame(sudoku);
                    transitionToState(GameState.MAIN_MENU);
                }
        ));

        // --- Zafer Modalı Butonları ---
        victoryButtons.add(new UIElement(
                (SCREEN_WIDTH - 260f) / 2f, 110f, 260f, 48f, "YENİDEN OYNA",
                playBase, playHov, playBrd, playHovBrd, playTxt, playTxt,
                () -> startNewGame(selectedDifficulty)
        ));
        victoryButtons.add(new UIElement(
                (SCREEN_WIDTH - 260f) / 2f, 50f, 260f, 44f, "ANA MENÜ",
                base, hover, border, hoverBorder, txt, txt,
                () -> transitionToState(GameState.MAIN_MENU)
        ));

        // --- Oyun Bitti Modalı Butonları ---
        gameOverButtons.add(new UIElement(
                (SCREEN_WIDTH - 260f) / 2f, 150f, 260f, 48f, "TEKRAR DENE",
                quitBase, quitHov, quitBrd, quitHovBrd, quitTxt, quitTxt,
                () -> startNewGame(selectedDifficulty)
        ));
        gameOverButtons.add(new UIElement(
                (SCREEN_WIDTH - 260f) / 2f, 90f, 260f, 44f, "ANA MENÜ",
                base, hover, border, hoverBorder, txt, txt,
                () -> transitionToState(GameState.MAIN_MENU)
        ));
    }

    private void toggleAudio() {
        boolean newState = !saveManager.isSoundEnabled();
        saveManager.setSoundEnabled(newState);
        sfx.toggleSound();
        buildGameUI();
    }

    private void startNewGame(SudokuEngine.Difficulty diff) {
        this.selectedDifficulty = diff;
        sudoku.startNewGame(selectedDifficulty);
        saveManager.clearSavedGame();
        saveManager.saveGame(sudoku);

        if (diffPillButton != null) diffPillButton.text = selectedDifficulty.label;
        selectCell(4, 4);
        transitionToState(GameState.PLAYING);
    }

    private void resumeSavedGame() {
        if (saveManager.loadGame(sudoku)) {
            this.selectedDifficulty = sudoku.getDifficulty();
            if (diffPillButton != null) diffPillButton.text = selectedDifficulty.label;
            selectCell(4, 4);
            transitionToState(GameState.PLAYING);
        }
    }

    // ==========================================
    // TWEEN ANIMASYONLARI
    // ==========================================

    /**
     * Hücre seçildiğinde yumuşak kâğıt vurgusu ve hafif scale animasyonu.
     */
    private void selectCell(int r, int c) {
        if (r < 0 || r >= 9 || c < 0 || c >= 9) return;
        if (r == selectedRow && c == selectedCol) return;

        // Önceki seçili hücre animasyonu (geri çekilme)
        Cell prevCell = cells[selectedRow][selectedCol];
        Tween.to(prevCell, CellAccessor.TYPE_SELECT_HIGHLIGHT, 0.22f)
                .target(0.0f)
                .ease(TweenEquations.easeOutQuad)
                .start(tweenManager);
        Tween.to(prevCell, CellAccessor.TYPE_SCALE, 0.22f)
                .target(1.0f)
                .ease(TweenEquations.easeOutQuad)
                .start(tweenManager);

        selectedRow = r;
        selectedCol = c;

        // Yeni seçilen hücre animasyonu (hafif kâğıt kabarması & renk geçişi)
        Cell newCell = cells[selectedRow][selectedCol];
        Tween.to(newCell, CellAccessor.TYPE_SELECT_HIGHLIGHT, 0.20f)
                .target(1.0f)
                .ease(TweenEquations.easeOutQuad)
                .start(tweenManager);
        Tween.to(newCell, CellAccessor.TYPE_SCALE, 0.20f)
                .target(1.04f)
                .ease(TweenEquations.easeOutQuad)
                .start(tweenManager);

        sfx.playHover();
    }

    /**
     * Sayı yerleştirildiğinde "kalemle yazma" hissi veren yumuşak pop animasyonu (0.9 -> 1.05 -> 1.0).
     */
    private void animateNumberPlacement(int r, int c, boolean isConflict) {
        Cell cell = cells[r][c];
        if (isConflict) {
            // Hatalı sayıda: Kısa sallanma (shake) ve kırmızı mürekkep tonuna geçiş
            Timeline.createParallel()
                    .push(Timeline.createSequence()
                            .push(Tween.to(cell, CellAccessor.TYPE_OFFSET_X, 0.04f).target(-5f).ease(TweenEquations.easeInOutQuad))
                            .push(Tween.to(cell, CellAccessor.TYPE_OFFSET_X, 0.04f).target(5f).ease(TweenEquations.easeInOutQuad))
                            .push(Tween.to(cell, CellAccessor.TYPE_OFFSET_X, 0.04f).target(-3f).ease(TweenEquations.easeInOutQuad))
                            .push(Tween.to(cell, CellAccessor.TYPE_OFFSET_X, 0.04f).target(3f).ease(TweenEquations.easeInOutQuad))
                            .push(Tween.to(cell, CellAccessor.TYPE_OFFSET_X, 0.04f).target(0f).ease(TweenEquations.easeInOutQuad)))
                    .push(Timeline.createSequence()
                            .push(Tween.to(cell, CellAccessor.TYPE_ERROR_HIGHLIGHT, 0.06f).target(1.0f).ease(TweenEquations.easeOutQuad))
                            .push(Tween.to(cell, CellAccessor.TYPE_ERROR_HIGHLIGHT, 0.45f).target(0.0f).ease(TweenEquations.easeInOutQuad)))
                    .start(tweenManager);
        } else {
            // Başarılı yazımda: Kalemle yazma pop animasyonu
            Timeline.createSequence()
                    .push(Tween.set(cell, CellAccessor.TYPE_POP_SCALE).target(0.90f))
                    .push(Tween.to(cell, CellAccessor.TYPE_POP_SCALE, 0.10f).target(1.05f).ease(TweenEquations.easeOutQuad))
                    .push(Tween.to(cell, CellAccessor.TYPE_POP_SCALE, 0.12f).target(1.00f).ease(TweenEquations.easeInOutQuad))
                    .start(tweenManager);
        }
    }

    private void handleNumberInput(int digit) {
        if (state != GameState.PLAYING) return;
        boolean wasPencil = sudoku.isPencilMode();
        boolean placed = sudoku.setNumber(selectedRow, selectedCol, digit);

        if (placed) {
            boolean conflict = sudoku.isConflict(selectedRow, selectedCol);
            animateNumberPlacement(selectedRow, selectedCol, conflict);

            if (wasPencil) {
                sfx.playPencil();
            } else {
                if (conflict) {
                    sfx.playError();
                } else {
                    sfx.playPlace();
                }
            }

            saveManager.saveGame(sudoku);

            if (sudoku.isCompleted()) {
                saveManager.recordGameCompleted(sudoku.getDifficulty(), sudoku.getElapsedTime());
                state = GameState.VICTORY;
                triggerVictoryCelebration();
            } else if (sudoku.isGameOver()) {
                saveManager.clearSavedGame();
                state = GameState.GAME_OVER;
                sfx.playError();
            }
        }
    }

    // ==========================================
    // GİRDİ YÖNETİMİ (TOUCH & MOUSE UYUMLU)
    // ==========================================

    private void setupInput() {
        InputAdapter gameInput = new InputAdapter() {
            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                Vector3 worldCoords = viewport.unproject(new Vector3(screenX, screenY, 0));
                lastTouchPos.set(worldCoords);
                float mx = worldCoords.x;
                float my = worldCoords.y;

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

                    // Tahta Hücre Dokunma Kontrolü (Hızlı ve Kesin)
                    if (mx >= boardX && mx <= boardX + boardSize && my >= boardY && my <= boardY + boardSize) {
                        int col = (int) ((mx - boardX) / cellSize);
                        int row = (int) ((boardSize - (my - boardY)) / cellSize); // Y ters çevrilir
                        if (row >= 0 && row < 9 && col >= 0 && col < 9) {
                            selectCell(row, col);
                            return true;
                        }
                    }
                } else if (state == GameState.PAUSED) {
                    for (UIElement btn : pauseButtons) {
                        if (btn.handleClick(mx, my, sfx)) return true;
                    }
                } else if (state == GameState.VICTORY) {
                    for (UIElement btn : victoryButtons) {
                        if (btn.handleClick(mx, my, sfx)) return true;
                    }
                } else if (state == GameState.GAME_OVER) {
                    for (UIElement btn : gameOverButtons) {
                        if (btn.handleClick(mx, my, sfx)) return true;
                    }
                }
                return false;
            }

            @Override
            public boolean touchDragged(int screenX, int screenY, int pointer) {
                Vector3 worldCoords = viewport.unproject(new Vector3(screenX, screenY, 0));
                lastTouchPos.set(worldCoords);
                return false;
            }

            @Override
            public boolean mouseMoved(int screenX, int screenY) {
                Vector3 worldCoords = viewport.unproject(new Vector3(screenX, screenY, 0));
                lastTouchPos.set(worldCoords);
                updateHover(worldCoords.x, worldCoords.y, Gdx.graphics.getDeltaTime());
                return false;
            }

            @Override
            public boolean keyDown(int keycode) {
                if (keycode == Input.Keys.ESCAPE) {
                    if (state == GameState.PLAYING) {
                        saveManager.saveGame(sudoku);
                        state = GameState.PAUSED;
                    } else if (state == GameState.PAUSED) {
                        state = GameState.PLAYING;
                    } else if (state == GameState.SETTINGS) {
                        transitionToState(stateBeforeSettings);
                    } else if (state == GameState.STATS) {
                        transitionToState(GameState.MAIN_MENU);
                    }
                    return true;
                }

                if (keycode == Input.Keys.M) {
                    toggleAudio();
                    return true;
                }

                if (state == GameState.PLAYING) {
                    if (keycode == Input.Keys.UP && selectedRow > 0) { selectCell(selectedRow - 1, selectedCol); return true; }
                    if (keycode == Input.Keys.DOWN && selectedRow < 8) { selectCell(selectedRow + 1, selectedCol); return true; }
                    if (keycode == Input.Keys.LEFT && selectedCol > 0) { selectCell(selectedRow, selectedCol - 1); return true; }
                    if (keycode == Input.Keys.RIGHT && selectedCol < 8) { selectCell(selectedRow, selectedCol + 1); return true; }

                    if (keycode >= Input.Keys.NUM_1 && keycode <= Input.Keys.NUM_9) {
                        handleNumberInput(keycode - Input.Keys.NUM_1 + 1);
                        return true;
                    }
                    if (keycode >= Input.Keys.NUMPAD_1 && keycode <= Input.Keys.NUMPAD_9) {
                        handleNumberInput(keycode - Input.Keys.NUMPAD_1 + 1);
                        return true;
                    }

                    if (keycode == Input.Keys.BACKSPACE || keycode == Input.Keys.DEL) {
                        if (sudoku.erase(selectedRow, selectedCol)) {
                            sfx.playErase();
                            saveManager.saveGame(sudoku);
                        }
                        return true;
                    }

                    if (keycode == Input.Keys.SPACE || keycode == Input.Keys.N) {
                        sudoku.togglePencilMode();
                        sfx.playClick();
                        return true;
                    }

                    if (keycode == Input.Keys.Z || keycode == Input.Keys.U) {
                        if (sudoku.undo()) {
                            sfx.playPlace();
                            saveManager.saveGame(sudoku);
                        }
                        return true;
                    }
                }
                return false;
            }
        };

        InputMultiplexer multiplexer = new InputMultiplexer();
        multiplexer.addProcessor(stage);
        multiplexer.addProcessor(gameInput);
        Gdx.input.setInputProcessor(multiplexer);
    }

    private void updateHover(float mx, float my, float dt) {
        if (state == GameState.PLAYING) {
            for (UIElement btn : hudButtons) btn.update(mx, my, dt, sfx);
            for (UIElement btn : actionButtons) btn.update(mx, my, dt, sfx);
            for (UIElement btn : numPadButtons) btn.update(mx, my, dt, sfx);
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
        stage.getViewport().update(width, height, true);
        box2dCamera.setToOrtho(false, width / PPM, height / PPM);
    }

    // ==========================================
    // RENDER DÖNGÜSÜ
    // ==========================================

    @Override
    public void render() {
        float dt = Gdx.graphics.getDeltaTime();
        if (dt > 0.1f) dt = 0.1f;

        // 1. Universal Tween Engine Güncellemesi
        tweenManager.update(dt);

        // 2. Zaman & Fizik Güncellemesi
        if (state == GameState.PLAYING) {
            sudoku.updateTimer(dt);
            saveManager.addTotalPlayTime(dt);
        }

        if (state == GameState.VICTORY) {
            world.step(1 / 60f, 6, 2);
        }

        // 3. Dokunmatik / Fare Işık Takibi (Android ve Masaüstü Uyumlu)
        if (Gdx.input.isTouched()) {
            Vector3 touchCoords = viewport.unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));
            lastTouchPos.set(touchCoords);
        }
        if (cursorLight != null && cursorLight.isActive()) {
            cursorLight.setPosition(lastTouchPos.x / PPM, lastTouchPos.y / PPM);
        }

        // 4. Kâğıt Teması Arka Plan Temizliği (Krem / Parşömen)
        Gdx.gl.glClearColor(0.965f, 0.945f, 0.900f, 1.0f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        viewport.apply();
        batch.setProjectionMatrix(camera.combined);

        // 5. Arka Plan Kâğıt Çizgileri ve Oyun Tahtası
        batch.begin();
        renderNotebookLines();

        if (state == GameState.PLAYING || state == GameState.PAUSED ||
                state == GameState.VICTORY || state == GameState.GAME_OVER) {
            renderGameHeader();
            renderSudokuBoard();
            renderControlPanel();
        }

        if (state == GameState.VICTORY) {
            renderConfetti();
        }
        batch.end();

        // 6. Box2DLights (Sıcak Masa Lambası)
        if (saveManager.isLightEnabled()) {
            box2dCamera.position.set(camera.position.x / PPM, camera.position.y / PPM, 0);
            box2dCamera.update();
            rayHandler.setCombinedMatrix(box2dCamera);
            rayHandler.updateAndRender();
        }

        // 7. Modallar ve Scene2D Arayüzü
        batch.setProjectionMatrix(camera.combined);

        if (state == GameState.MAIN_MENU || state == GameState.SETTINGS || state == GameState.STATS) {
            stage.act(dt);
            stage.draw();
        } else {
            batch.begin();
            if (state == GameState.PAUSED) {
                renderPauseModal();
            } else if (state == GameState.VICTORY) {
                renderVictoryModal();
            } else if (state == GameState.GAME_OVER) {
                renderGameOverModal();
            }
            batch.end();
        }

        // 8. Ekranlar Arası Yumuşak Fade Geçiş Çizimi (Kâğıt Krem Tonu)
        if (screenFade.getAlpha() > 0.001f) {
            batch.begin();
            drawer.setColor(new Color(0.965f, 0.945f, 0.900f, screenFade.getAlpha()));
            drawer.filledRectangle(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);
            batch.end();
        }
    }

    /**
     * Otantik çizgili defter kâğıdı deseni (mavi kurşun kalem satırları ve kırmızı marjin çizgisi).
     */
    private void renderNotebookLines() {
        // Yumuşak mavi kâğıt çizgileri
        drawer.setColor(new Color(0.72f, 0.80f, 0.88f, 0.40f));
        for (float y = 25f; y < SCREEN_HEIGHT; y += 32f) {
            drawer.line(0, y, SCREEN_WIDTH, y, 1.2f);
        }
        // Sol kenar klasik defter marjin çizgisi (yumuşak kırmızı kalem)
        drawer.setColor(new Color(0.92f, 0.60f, 0.60f, 0.50f));
        drawer.line(80f, 0, 80f, SCREEN_HEIGHT, 2.0f);
    }

    private void renderGameHeader() {
        // Üst Kâğıt Başlık Şeridi
        drawer.setColor(new Color(0.93f, 0.90f, 0.84f, 0.96f));
        drawer.filledRectangle(0, SCREEN_HEIGHT - 65f, SCREEN_WIDTH, 65f);
        drawer.setColor(new Color(0.76f, 0.72f, 0.66f, 0.90f));
        drawer.line(0, SCREEN_HEIGHT - 65f, SCREEN_WIDTH, SCREEN_HEIGHT - 65f, 2f);

        // Üst Bar Butonları
        for (int i = 0; i < hudButtons.size(); i++) {
            renderButton(hudButtons.get(i), i == 4 ? smallFont : buttonFont);
        }

        // Merkez Sayaç: Süre
        float midX = SCREEN_WIDTH / 2f;
        float headerY = SCREEN_HEIGHT - 32f;
        headerFont.setColor(new Color(0.18f, 0.18f, 0.22f, 1.0f));
        String timeStr = "Süre: " + sudoku.getFormattedTime();
        layout.setText(headerFont, timeStr);
        headerFont.draw(batch, timeStr, midX - layout.width / 2f, headerY + layout.height / 2f);

        // Hata Kutusu
        String mistakeStr = "Hata: " + sudoku.getMistakes() + "/" + sudoku.getMaxMistakes();
        drawer.setColor(new Color(0.91f, 0.87f, 0.80f, 0.95f));
        drawer.filledRectangle(midX + 130f, headerY - 14f, 130f, 28f);
        drawer.setColor(sudoku.getMistakes() > 0 ? new Color(0.85f, 0.22f, 0.22f, 0.90f) : new Color(0.68f, 0.64f, 0.58f, 0.85f));
        drawer.rectangle(midX + 130f, headerY - 14f, 130f, 28f, 1.5f);

        smallFont.setColor(sudoku.getMistakes() > 0 ? new Color(0.85f, 0.18f, 0.18f, 1.0f) : new Color(0.22f, 0.22f, 0.25f, 1.0f));
        layout.setText(smallFont, mistakeStr);
        smallFont.draw(batch, mistakeStr, midX + 130f + (130f - layout.width) / 2f, headerY + layout.height / 2f);
    }

    /**
     * Kâğıt Temalı Sudoku Tahtası Çizimi (Tween animasyonları entegreli).
     */
    private void renderSudokuBoard() {
        // Kâğıt Kart Dış Gölgesi
        drawer.setColor(new Color(0.78f, 0.74f, 0.68f, 0.60f));
        drawer.filledRectangle(boardX + 4f, boardY - 4f, boardSize, boardSize);

        // Kırık Beyaz / Krem Tahta Zemin
        drawer.setColor(new Color(0.995f, 0.995f, 0.99f, 1.0f));
        drawer.filledRectangle(boardX, boardY, boardSize, boardSize);

        // İnce Kalem Çerçevesi
        drawer.setColor(new Color(0.25f, 0.24f, 0.22f, 0.95f));
        drawer.rectangle(boardX, boardY, boardSize, boardSize, 2.5f);

        int selectedVal = sudoku.getCell(selectedRow, selectedCol);

        // Hücrelerin Çizimi (Universal Tween Engine animasyonlarıyla)
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                Cell cellAnim = cells[r][c];
                float cx = boardX + c * cellSize + cellAnim.getOffsetX();
                float cy = boardY + (8 - r) * cellSize + cellAnim.getOffsetY();
                int val = sudoku.getCell(r, c);

                boolean isSelected = (r == selectedRow && c == selectedCol);
                boolean isSamePeer = (r == selectedRow || c == selectedCol ||
                        (r / 3 == selectedRow / 3 && c / 3 == selectedCol / 3));
                boolean isSameNumber = (selectedVal > 0 && val == selectedVal);
                boolean isConflict = sudoku.isConflict(r, c);

                // 1. Satır / Sütun / Kutu Akraba Hücre Vurgusu (Hafif Sarı Kâğıt Vurgusu)
                if (isSamePeer && !isSelected) {
                    drawer.setColor(new Color(0.96f, 0.94f, 0.84f, 0.55f));
                    drawer.filledRectangle(cx, cy, cellSize, cellSize);
                }

                // 2. Tahta Boyunca Aynı Sayıların Vurgusu (Hafif Mavi Mürekkep Vurgusu)
                if (isSameNumber && !isSelected) {
                    drawer.setColor(new Color(0.84f, 0.91f, 0.93f, 0.70f));
                    drawer.filledRectangle(cx, cy, cellSize, cellSize);
                }

                // 3. Hatalı Çakışma / Sallanma Durumu (Kırmızı Mürekkep Geçişi)
                if (isConflict || cellAnim.getErrorHighlight() > 0.01f) {
                    float alpha = Math.max(0.70f, cellAnim.getErrorHighlight());
                    drawer.setColor(new Color(0.96f, 0.78f, 0.78f, alpha));
                    drawer.filledRectangle(cx, cy, cellSize, cellSize);
                }

                // 4. Seçili Hücre Vurgusu (Tween Destekli Kâğıt Kabarması)
                if (isSelected || cellAnim.getSelectHighlight() > 0.01f) {
                    float hAlpha = cellAnim.getSelectHighlight();
                    drawer.setColor(new Color(0.92f, 0.88f, 0.75f, 0.85f * hAlpha));
                    drawer.filledRectangle(cx, cy, cellSize, cellSize);

                    drawer.setColor(new Color(0.25f, 0.25f, 0.28f, 0.95f * hAlpha));
                    drawer.rectangle(cx + 1f, cy + 1f, cellSize - 2f, cellSize - 2f, 2.5f);
                }

                // 5. Rakam veya Notların Çizimi (Kalem Pop Efekti)
                if (val > 0) {
                    boolean isClue = (sudoku.getInitialCell(r, c) > 0);
                    BitmapFont activeFont = isClue ? clueDigitFont : playerDigitFont;

                    if (isConflict) {
                        activeFont.setColor(0.85f, 0.18f, 0.18f, 1.0f); // Kırmızı düzeltme kalemi
                    } else if (isClue) {
                        activeFont.setColor(0.12f, 0.14f, 0.18f, 1.0f); // Kalın siyah basılı ipucu
                    } else {
                        activeFont.setColor(0.08f, 0.32f, 0.78f, 1.0f); // Mavi tükenmez kalem mürekkebi
                    }

                    // Kalem Pop Animasyonu Ölçeği
                    float effectiveScale = cellAnim.getPopScale();
                    float originalScaleX = activeFont.getScaleX();
                    float originalScaleY = activeFont.getScaleY();
                    activeFont.getData().setScale(originalScaleX * effectiveScale, originalScaleY * effectiveScale);

                    String digitStr = String.valueOf(val);
                    layout.setText(activeFont, digitStr);
                    activeFont.draw(batch, digitStr, cx + (cellSize - layout.width) / 2f, cy + (cellSize + layout.height) / 2f);

                    activeFont.getData().setScale(originalScaleX, originalScaleY);
                } else {
                    // Kurşun Kalem Notları (3x3 Mini Izgara)
                    float subCell = cellSize / 3f;
                    for (int n = 1; n <= 9; n++) {
                        if (sudoku.hasNote(r, c, n)) {
                            int nr = (n - 1) / 3;
                            int nc = (n - 1) % 3;
                            float nx = cx + nc * subCell;
                            float ny = cy + (2 - nr) * subCell;

                            noteFont.setColor(0.38f, 0.40f, 0.44f, 0.95f); // Grafit kurşun kalem grisi
                            String noteStr = String.valueOf(n);
                            layout.setText(noteFont, noteStr);
                            noteFont.draw(batch, noteStr, nx + (subCell - layout.width) / 2f, ny + (subCell + layout.height) / 2f);
                        }
                    }
                }
            }
        }

        // İnce Izgara Çizgileri (Kurşun Kalem)
        drawer.setColor(new Color(0.72f, 0.70f, 0.66f, 0.85f));
        for (int i = 1; i < 9; i++) {
            if (i % 3 != 0) {
                float pos = i * cellSize;
                drawer.line(boardX + pos, boardY, boardX + pos, boardY + boardSize, 1.2f);
                drawer.line(boardX, boardY + pos, boardX + boardSize, boardY + pos, 1.2f);
            }
        }

        // Kalın 3x3 Kutu Çizgileri (Mürekkep Çizgisi)
        drawer.setColor(new Color(0.22f, 0.22f, 0.25f, 0.95f));
        for (int i = 0; i <= 3; i++) {
            float pos = i * (cellSize * 3);
            drawer.line(boardX + pos, boardY, boardX + pos, boardY + boardSize, 3.2f);
            drawer.line(boardX, boardY + pos, boardX + boardSize, boardY + pos, 3.2f);
        }
    }

    private void renderControlPanel() {
        // Kontrol Butonları (Geri Al, Notlar, Sil, İpucu)
        for (int i = 0; i < actionButtons.size(); i++) {
            UIElement btn = actionButtons.get(i);
            if (i == 1 && sudoku.isPencilMode()) {
                btn.borderColor.set(0.80f, 0.55f, 0.15f, 1.0f);
                btn.baseColor.set(0.96f, 0.90f, 0.74f, 0.95f);
            } else if (i == 1) {
                btn.borderColor.set(0.68f, 0.64f, 0.58f, 0.85f);
                btn.baseColor.set(0.94f, 0.91f, 0.85f, 0.95f);
            }
            renderButton(btn, buttonFont);
        }

        // 3x3 NumPad
        for (int i = 0; i < numPadButtons.size(); i++) {
            UIElement btn = numPadButtons.get(i);
            int digit = i + 1;
            int remaining = sudoku.getRemainingCount(digit);

            renderButton(btn, clueDigitFont);

            String remText = remaining > 0 ? remaining + " kaldı" : "Bitti";
            smallFont.setColor(remaining > 0 ? new Color(0.40f, 0.42f, 0.48f, 0.95f) : new Color(0.15f, 0.55f, 0.25f, 0.95f));
            layout.setText(smallFont, remText);
            smallFont.draw(batch, remText, btn.x + (btn.width - layout.width) / 2f, btn.y + 22f);
        }
    }

    private void renderButton(UIElement btn, BitmapFont font) {
        float lift = btn.hoverAnim * 2f;
        float dy = btn.y + lift;

        Color bg = btn.baseColor.cpy().lerp(btn.hoverColor, btn.hoverAnim);
        Color border = btn.borderColor.cpy().lerp(btn.hoverBorderColor, btn.hoverAnim);

        // Kâğıt Gölgesi
        drawer.setColor(new Color(0.75f, 0.70f, 0.65f, 0.35f + btn.hoverAnim * 0.20f));
        drawer.filledRectangle(btn.x + 2f, dy - 2f, btn.width, btn.height);

        drawer.setColor(bg);
        drawer.filledRectangle(btn.x, dy, btn.width, btn.height);

        drawer.setColor(border);
        drawer.rectangle(btn.x, dy, btn.width, btn.height, 2f);

        Color tc = btn.textColor.cpy().lerp(btn.hoverTextColor, btn.hoverAnim);
        font.setColor(tc);
        layout.setText(font, btn.text);
        font.draw(batch, btn.text, btn.x + (btn.width - layout.width) / 2f, dy + (btn.height + layout.height) / 2f);
    }

    private void renderConfetti() {
        for (int i = 0; i < confettiBodies.size; i++) {
            Body b = confettiBodies.get(i);
            Vector2 pos = b.getPosition().scl(PPM);
            float angle = b.getAngle();
            Color c = confettiColors.get(i % confettiColors.size());
            Vector2 sz = confettiSizes.get(i % confettiSizes.size());
            drawer.setColor(c);
            drawer.filledRectangle(pos.x, pos.y, sz.x, sz.y, angle);
        }
    }

    // ==========================================
    // KÂĞIT MODAL PENCERELERİ
    // ==========================================

    private void renderPauseModal() {
        float cardW = 380f;
        float cardH = 370f;
        renderModalCard(cardW, cardH, "OYUN DURAKLATILDI");
        for (UIElement btn : pauseButtons) {
            renderButton(btn, buttonFont);
        }
    }

    private void renderVictoryModal() {
        float cardW = 440f;
        float cardH = 370f;
        float cx = (SCREEN_WIDTH - cardW) / 2f;
        float cy = (SCREEN_HEIGHT - cardH) / 2f;

        renderModalCard(cardW, cardH, "TEBRİKLER!");

        headerFont.setColor(new Color(0.12f, 0.50f, 0.25f, 1.0f));
        String sub = "SUDOKU ÇÖZÜLDÜ!";
        layout.setText(headerFont, sub);
        headerFont.draw(batch, sub, cx + (cardW - layout.width) / 2f, cy + cardH - 85f);

        String timeInfo = "Tamamlama Süresi: " + sudoku.getFormattedTime();
        String diffInfo = "Zorluk: " + sudoku.getDifficulty().label;
        String mistInfo = "Hata Sayısı: " + sudoku.getMistakes() + "/" + sudoku.getMaxMistakes();

        buttonFont.setColor(new Color(0.22f, 0.22f, 0.25f, 1.0f));
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
        float cardW = 420f;
        float cardH = 310f;
        float cx = (SCREEN_WIDTH - cardW) / 2f;
        float cy = (SCREEN_HEIGHT - cardH) / 2f;

        renderModalCard(cardW, cardH, "OYUN BİTTİ");

        headerFont.setColor(new Color(0.75f, 0.20f, 0.20f, 1.0f));
        String info = "3 Hata Sınırına Ulaşıldı!";
        layout.setText(headerFont, info);
        headerFont.draw(batch, info, cx + (cardW - layout.width) / 2f, cy + cardH - 95f);

        for (UIElement btn : gameOverButtons) {
            renderButton(btn, buttonFont);
        }
    }

    private void renderModalCard(float w, float h, String title) {
        float cx = (SCREEN_WIDTH - w) / 2f;
        float cy = (SCREEN_HEIGHT - h) / 2f;

        // Karartma Perdesi
        drawer.setColor(new Color(0.20f, 0.18f, 0.15f, 0.40f));
        drawer.filledRectangle(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);

        // Kâğıt Gölgesi
        drawer.setColor(new Color(0.75f, 0.70f, 0.65f, 0.45f));
        drawer.filledRectangle(cx + 6f, cy - 6f, w, h);

        // Kâğıt Kart
        drawer.setColor(new Color(0.98f, 0.96f, 0.92f, 0.98f));
        drawer.filledRectangle(cx, cy, w, h);
        drawer.setColor(new Color(0.35f, 0.32f, 0.30f, 0.95f));
        drawer.rectangle(cx, cy, w, h, 2.5f);

        titleFont.setColor(new Color(0.15f, 0.22f, 0.45f, 1.0f));
        layout.setText(titleFont, title);
        titleFont.draw(batch, title, cx + (w - layout.width) / 2f, cy + h - 35f);

        drawer.setColor(new Color(0.70f, 0.65f, 0.60f, 0.70f));
        drawer.line(cx + 35f, cy + h - 65f, cx + w - 35f, cy + h - 65f, 2f);
    }

    @Override
    public void pause() {
        if (state == GameState.PLAYING && sudoku != null && saveManager != null) {
            saveManager.saveGame(sudoku);
        }
    }

    @Override
    public void dispose() {
        if (paperSkinHelper != null) paperSkinHelper.dispose();
        if (stage != null) stage.dispose();
        if (pixelTexture != null) pixelTexture.dispose();
        if (batch != null) batch.dispose();
        disposeFonts();
        if (sfx != null) sfx.dispose();
        if (rayHandler != null) rayHandler.dispose();
        if (world != null) world.dispose();
    }
}
