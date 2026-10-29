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
import java.util.Iterator;
import java.util.List;

public class TargetingGame extends ApplicationAdapter {

    public static final float SCREEN_WIDTH = 1280f;
    public static final float SCREEN_HEIGHT = 720f;
    public static final float PPM = 32f; // Pixels per meter

    // Rendering & Viewport
    private OrthographicCamera camera;
    private OrthographicCamera box2dCamera;
    private Viewport viewport;
    private SpriteBatch spriteBatch;
    private ShapeDrawer drawer;
    private Texture pixelTexture;

    // Box2DLights
    private RayHandler rayHandler;
    private PointLight ballLight;
    private PointLight towerLight;

    // Fonts
    private BitmapFont titleFont;
    private BitmapFont regularFont;

    // Procedural Audio System
    private SoundEffects soundEffects;

    // Box2D World
    private World world;
    private Body groundBody;
    private Body ballBody;

    // Ball configs
    private final float ballRadiusPx = 20f;
    private final Vector2 spawnPositionPx = new Vector2(200f, 160f);
    private boolean isAirborne = false;
    private float stationaryTimer = 0f;
    private static final float RESET_STOP_DELAY = 1.0f;

    // Slingshot / Aiming
    private boolean isDragging = false;
    private Vector2 dragStartPx = new Vector2();
    private Vector2 currentDragPx = new Vector2();
    private final float FORCE_MULTIPLIER = 0.28f;
    private final float MAX_DRAG_PX = 180f;

    // Screen Shake effect
    private float screenShake = 0f;

    // Motion trail
    private final List<Vector2> trail = new ArrayList<>();
    private float trailTimer = 0f;

    // Particle effect system for impacts
    public static class Particle {
        Vector2 pos = new Vector2();
        Vector2 vel = new Vector2();
        float life;
        float maxLife;
        float size;
        Color color;

        public Particle(float x, float y, float vx, float vy, float life, float size, Color color) {
            this.pos.set(x, y);
            this.vel.set(vx, vy);
            this.life = life;
            this.maxLife = life;
            this.size = size;
            this.color = new Color(color);
        }
    }

    private final List<Particle> particles = new ArrayList<>();

    // Tower blocks representation
    public static class BlockData {
        public float widthPx;
        public float heightPx;
        public Color fillColor;
        public Color borderColor;

        public BlockData(float widthPx, float heightPx, Color fillColor, Color borderColor) {
            this.widthPx = widthPx;
            this.heightPx = heightPx;
            this.fillColor = fillColor;
            this.borderColor = borderColor;
        }
    }

    private final Array<Body> towerBodies = new Array<>();

    // Tip Box HUD
    private float tipTimer = 0f;
    private float tipAlpha = 1.0f;
    private boolean userInteracted = false;
    private static final float TIP_INITIAL_DELAY = 4.0f;
    private static final float TIP_FADE_SPEED = 1.5f;

    @Override
    public void create() {
        camera = new OrthographicCamera();
        viewport = new FitViewport(SCREEN_WIDTH, SCREEN_HEIGHT, camera);
        viewport.apply();
        camera.position.set(SCREEN_WIDTH / 2f, SCREEN_HEIGHT / 2f, 0);
        camera.update();

        // Box2D camera for lighting (meter scale)
        box2dCamera = new OrthographicCamera();
        box2dCamera.setToOrtho(false, SCREEN_WIDTH / PPM, SCREEN_HEIGHT / PPM);

        spriteBatch = new SpriteBatch();

        // Initialize ShapeDrawer with 1x1 white pixel texture
        initShapeDrawer();

        // Initialize fonts with FreeType
        initFonts();

        // Initialize Procedural Sound Effects
        soundEffects = new SoundEffects();
        soundEffects.init();

        // Create Box2D World
        world = new World(new Vector2(0f, -24f), true);

        // Setup collision listener for particle impacts & screen shake
        setupCollisionListener();

        // Build static world bounds
        buildEnvironment();

        // Create player ball
        createBall();

        // Build towers
        buildTowers();

        // Initialize Box2DLights
        initLighting();

        // Setup input processor
        setupInputProcessor();
    }

    private void initShapeDrawer() {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        pixelTexture = new Texture(pixmap);
        pixmap.dispose();

        TextureRegion region = new TextureRegion(pixelTexture, 0, 0, 1, 1);
        drawer = new ShapeDrawer(spriteBatch, region);
    }

    private void initLighting() {
        RayHandler.useDiffuseLight(true);
        rayHandler = new RayHandler(world);
        rayHandler.setAmbientLight(0.20f, 0.22f, 0.28f, 0.65f);
        rayHandler.setBlurNum(2);

        // Dynamic light attached to the ball (neon amber glow)
        ballLight = new PointLight(rayHandler, 48, new Color(1f, 0.65f, 0.2f, 0.85f), 5.5f, 0, 0);
        ballLight.attachToBody(ballBody);
        ballLight.setSoft(true);

        // Atmospheric moonlight illuminating the fortress from above
        towerLight = new PointLight(rayHandler, 96, new Color(0.35f, 0.70f, 0.95f, 0.75f), 18f, 920f / PPM, 600f / PPM);
        towerLight.setSoft(true);
    }

    private void setupCollisionListener() {
        world.setContactListener(new ContactListener() {
            @Override
            public void beginContact(Contact contact) {
                WorldManifold manifold = contact.getWorldManifold();
                if (manifold.getNumberOfContactPoints() > 0) {
                    Vector2 pointPx = new Vector2(manifold.getPoints()[0]).scl(PPM);

                    // Check if ball is involved in this impact
                    Fixture fA = contact.getFixtureA();
                    Fixture fB = contact.getFixtureB();
                    boolean ballInvolved = (fA.getBody() == ballBody || fB.getBody() == ballBody);

                    if (ballInvolved) {
                        float speed = ballBody.getLinearVelocity().len();
                        if (speed > 3f) {
                            // Trigger Camera Shake proportional to impact speed
                            screenShake = Math.min(speed * 0.5f, 10f);

                            // Spawn collision particles (sparks & dust)
                            spawnImpactParticles(pointPx.x, pointPx.y, (int) Math.min(speed * 3f, 25));

                            // Play dynamic impact sound with stereo panning
                            float volume = Math.min(speed / 18f, 1.0f);
                            float pitch = 1.25f - Math.min(speed / 35f, 0.45f);
                            float pan = (pointPx.x / SCREEN_WIDTH) * 2f - 1f; // -1 (left) to +1 (right)
                            soundEffects.playImpact(volume, pitch, pan);
                        }
                    }
                }
            }

            @Override
            public void endContact(Contact contact) {}

            @Override
            public void preSolve(Contact contact, Manifold oldManifold) {}

            @Override
            public void postSolve(Contact contact, ContactImpulse impulse) {}
        });
    }

    private void spawnImpactParticles(float x, float y, int count) {
        for (int i = 0; i < count; i++) {
            float angle = MathUtils.random(0f, MathUtils.PI2);
            float speed = MathUtils.random(60f, 320f);
            float vx = MathUtils.cos(angle) * speed;
            float vy = MathUtils.sin(angle) * speed;
            float life = MathUtils.random(0.3f, 0.7f);
            float size = MathUtils.random(3f, 7f);

            Color color = MathUtils.randomBoolean(0.6f)
                    ? new Color(1f, 0.75f, 0.25f, 1f)
                    : new Color(0.85f, 0.88f, 0.92f, 1f);

            particles.add(new Particle(x, y, vx, vy, life, size, color));
        }
    }

    private void initFonts() {
        FileHandle fontFile = Gdx.files.internal("fonts/segoeui.ttf");
        if (fontFile.exists()) {
            FreeTypeFontGenerator generator = new FreeTypeFontGenerator(fontFile);
            FreeTypeFontParameter param = new FreeTypeFontParameter();
            param.minFilter = Texture.TextureFilter.Linear;
            param.magFilter = Texture.TextureFilter.Linear;

            param.size = 20;
            titleFont = generator.generateFont(param);

            param.size = 16;
            regularFont = generator.generateFont(param);

            generator.dispose();
        } else {
            titleFont = new BitmapFont();
            regularFont = new BitmapFont();
        }
    }

    private void buildEnvironment() {
        BodyDef groundDef = new BodyDef();
        groundDef.type = BodyDef.BodyType.StaticBody;
        groundDef.position.set(SCREEN_WIDTH / 2f / PPM, 35f / PPM);

        groundBody = world.createBody(groundDef);

        PolygonShape groundShape = new PolygonShape();
        groundShape.setAsBox((SCREEN_WIDTH / 2f) / PPM, 35f / PPM);

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = groundShape;
        fixtureDef.friction = 0.85f;
        fixtureDef.restitution = 0.25f;

        groundBody.createFixture(fixtureDef);
        groundShape.dispose();

        // Left and Right wall boundaries
        createStaticWall(0f, SCREEN_HEIGHT / 2f, 10f, SCREEN_HEIGHT);
        createStaticWall(SCREEN_WIDTH, SCREEN_HEIGHT / 2f, 10f, SCREEN_HEIGHT);
    }

    private void createStaticWall(float xPx, float yPx, float widthPx, float heightPx) {
        BodyDef wallDef = new BodyDef();
        wallDef.type = BodyDef.BodyType.StaticBody;
        wallDef.position.set(xPx / PPM, yPx / PPM);

        Body wall = world.createBody(wallDef);
        PolygonShape shape = new PolygonShape();
        shape.setAsBox((widthPx / 2f) / PPM, (heightPx / 2f) / PPM);

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = shape;
        fixtureDef.friction = 0.4f;
        fixtureDef.restitution = 0.3f;

        wall.createFixture(fixtureDef);
        shape.dispose();
    }

    private void createBall() {
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.KinematicBody;
        bodyDef.position.set(spawnPositionPx.x / PPM, spawnPositionPx.y / PPM);

        ballBody = world.createBody(bodyDef);

        CircleShape circle = new CircleShape();
        circle.setRadius(ballRadiusPx / PPM);

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = circle;
        fixtureDef.density = 2.4f;
        fixtureDef.friction = 0.5f;
        fixtureDef.restitution = 0.45f;

        ballBody.createFixture(fixtureDef);
        circle.dispose();
    }

    private void launchBall(Vector2 impulse) {
        ballBody.setType(BodyDef.BodyType.DynamicBody);
        ballBody.setLinearVelocity(impulse);
        isAirborne = true;
        stationaryTimer = 0f;
        trail.clear();

        if (soundEffects != null) {
            soundEffects.playLaunch();
        }
    }

    private void resetBallToSpawn() {
        ballBody.setType(BodyDef.BodyType.KinematicBody);
        ballBody.setLinearVelocity(0f, 0f);
        ballBody.setAngularVelocity(0f);
        ballBody.setTransform(spawnPositionPx.x / PPM, spawnPositionPx.y / PPM, 0f);
        isAirborne = false;
        stationaryTimer = 0f;
        trail.clear();

        if (soundEffects != null) {
            soundEffects.playReset();
        }
    }

    private void buildTowers() {
        for (Body body : towerBodies) {
            world.destroyBody(body);
        }
        towerBodies.clear();

        Color woodFill = new Color(0.80f, 0.54f, 0.33f, 1f);
        Color woodBorder = new Color(0.50f, 0.30f, 0.15f, 1f);

        Color stoneFill = new Color(0.54f, 0.60f, 0.68f, 1f);
        Color stoneBorder = new Color(0.36f, 0.40f, 0.46f, 1f);

        Color targetGold = new Color(0.98f, 0.78f, 0.22f, 1f);
        Color targetBorder = new Color(0.82f, 0.55f, 0.10f, 1f);

        // Tower 1 (Main Fortress) around X = 920px
        float t1X = 920f;
        float baseGroundY = 70f;

        // Ground floor pillars
        createBoxBlock(t1X - 70f, baseGroundY + 45f, 24f, 90f, stoneFill, stoneBorder, 3.0f);
        createBoxBlock(t1X,       baseGroundY + 45f, 24f, 90f, stoneFill, stoneBorder, 3.0f);
        createBoxBlock(t1X + 70f, baseGroundY + 45f, 24f, 90f, stoneFill, stoneBorder, 3.0f);

        // 1st Floor Slab
        float slab1Y = baseGroundY + 90f + 9f;
        createBoxBlock(t1X, slab1Y, 190f, 18f, woodFill, woodBorder, 1.8f);

        // 2nd floor pillars
        float p2Y = slab1Y + 9f + 40f;
        createBoxBlock(t1X - 50f, p2Y, 20f, 80f, woodFill, woodBorder, 1.5f);
        createBoxBlock(t1X + 50f, p2Y, 20f, 80f, woodFill, woodBorder, 1.5f);

        // 2nd Floor Slab
        float slab2Y = p2Y + 40f + 9f;
        createBoxBlock(t1X, slab2Y, 140f, 18f, woodFill, woodBorder, 1.5f);

        // Top Roof pillars
        float roofPillarY = slab2Y + 9f + 25f;
        createBoxBlock(t1X - 25f, roofPillarY, 18f, 50f, woodFill, woodBorder, 1.2f);
        createBoxBlock(t1X + 25f, roofPillarY, 18f, 50f, woodFill, woodBorder, 1.2f);

        // Golden Target Block
        float targetY = roofPillarY + 25f + 18f;
        createBoxBlock(t1X, targetY, 36f, 36f, targetGold, targetBorder, 1.0f);

        // Tower 2 (Forward Outpost) around X = 680px
        float t2X = 680f;
        createBoxBlock(t2X - 28f, baseGroundY + 35f, 20f, 70f, woodFill, woodBorder, 1.8f);
        createBoxBlock(t2X + 28f, baseGroundY + 35f, 20f, 70f, woodFill, woodBorder, 1.8f);
        createBoxBlock(t2X,       baseGroundY + 70f + 8f, 95f, 16f, woodFill, woodBorder, 1.5f);
        createBoxBlock(t2X,       baseGroundY + 70f + 16f + 15f, 30f, 30f, stoneFill, stoneBorder, 2.0f);
    }

    private void createBoxBlock(float xPx, float yPx, float widthPx, float heightPx,
                                Color fillColor, Color borderColor, float density) {
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.position.set(xPx / PPM, yPx / PPM);

        Body body = world.createBody(bodyDef);

        PolygonShape boxShape = new PolygonShape();
        boxShape.setAsBox((widthPx / 2f) / PPM, (heightPx / 2f) / PPM);

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = boxShape;
        fixtureDef.density = density;
        fixtureDef.friction = 0.7f;
        fixtureDef.restitution = 0.15f;

        body.createFixture(fixtureDef);
        boxShape.dispose();

        body.setUserData(new BlockData(widthPx, heightPx, fillColor, borderColor));
        towerBodies.add(body);
    }

    private void setupInputProcessor() {
        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                Vector3 worldCoords = viewport.unproject(new Vector3(screenX, screenY, 0));
                Vector2 ballPosPx = new Vector2(ballBody.getPosition()).scl(PPM);

                float dist = worldCoords.dst(ballPosPx.x, ballPosPx.y, 0);
                if (dist <= ballRadiusPx * 2.8f || !isAirborne) {
                    isDragging = true;
                    userInteracted = true;
                    dragStartPx.set(ballPosPx);
                    currentDragPx.set(worldCoords.x, worldCoords.y);
                    return true;
                }
                return false;
            }

            @Override
            public boolean touchDragged(int screenX, int screenY, int pointer) {
                if (isDragging) {
                    Vector3 worldCoords = viewport.unproject(new Vector3(screenX, screenY, 0));
                    currentDragPx.set(worldCoords.x, worldCoords.y);

                    Vector2 pull = new Vector2(currentDragPx).sub(dragStartPx);
                    if (pull.len() > MAX_DRAG_PX) {
                        pull.setLength(MAX_DRAG_PX);
                        currentDragPx.set(dragStartPx).add(pull);
                    }
                    return true;
                }
                return false;
            }

            @Override
            public boolean touchUp(int screenX, int screenY, int pointer, int button) {
                if (isDragging) {
                    isDragging = false;
                    Vector2 impulse = new Vector2(dragStartPx).sub(currentDragPx).scl(FORCE_MULTIPLIER);
                    if (impulse.len() > 1.5f) {
                        launchBall(impulse);
                    }
                    return true;
                }
                return false;
            }

            @Override
            public boolean keyDown(int keycode) {
                if (keycode == Input.Keys.R) {
                    resetGame();
                    return true;
                }
                return false;
            }
        });
    }

    private void resetGame() {
        resetBallToSpawn();
        buildTowers();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        box2dCamera.setToOrtho(false, width / PPM, height / PPM);
    }

    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();
        if (delta > 0.1f) delta = 0.1f;

        // Step Box2D Physics
        world.step(1 / 60f, 8, 3);

        checkBallStatus(delta);
        updateTipBox(delta);
        updateParticles(delta);

        // Apply screen shake decay
        applyCameraShake(delta);

        // Clear screen
        Gdx.gl.glClearColor(0.08f, 0.10f, 0.14f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        viewport.apply();
        spriteBatch.setProjectionMatrix(camera.combined);

        // 1. Draw World Graphics with ShapeDrawer in single high-performance Batch
        spriteBatch.begin();

        drawEnvironment();
        drawTowerBlocks();
        drawTrail(delta);

        if (isDragging) {
            drawAimAssist();
        }

        drawBall();
        drawParticles();

        if (tipAlpha > 0.01f) {
            drawTipBox();
        }

        spriteBatch.end();

        // 2. Render Box2DLights (Dynamic 2D shadows and glowing ball)
        box2dCamera.position.set(camera.position.x / PPM, camera.position.y / PPM, 0);
        box2dCamera.update();
        rayHandler.setCombinedMatrix(box2dCamera);
        rayHandler.updateAndRender();
    }

    private void applyCameraShake(float dt) {
        if (screenShake > 0.05f) {
            float offsetX = MathUtils.random(-screenShake, screenShake);
            float offsetY = MathUtils.random(-screenShake, screenShake);
            camera.position.set(SCREEN_WIDTH / 2f + offsetX, SCREEN_HEIGHT / 2f + offsetY, 0);
            screenShake = Math.max(0f, screenShake - 18f * dt);
        } else {
            camera.position.set(SCREEN_WIDTH / 2f, SCREEN_HEIGHT / 2f, 0);
        }
        camera.update();
    }

    private void updateParticles(float dt) {
        Iterator<Particle> it = particles.iterator();
        while (it.hasNext()) {
            Particle p = it.next();
            p.life -= dt;
            if (p.life <= 0) {
                it.remove();
                continue;
            }
            p.vel.y += -980f * dt;
            p.pos.add(p.vel.x * dt, p.vel.y * dt);

            if (p.pos.y <= 70f + p.size / 2f) {
                p.pos.y = 70f + p.size / 2f;
                p.vel.y = -p.vel.y * 0.4f;
                p.vel.x *= 0.8f;
            }
        }
    }

    private void checkBallStatus(float dt) {
        if (!isAirborne) return;

        Vector2 vel = ballBody.getLinearVelocity();
        float speed = vel.len();

        if (speed < 0.45f) {
            stationaryTimer += dt;
            if (stationaryTimer >= RESET_STOP_DELAY) {
                resetBallToSpawn();
            }
        } else {
            stationaryTimer = 0f;
        }

        Vector2 pos = ballBody.getPosition();
        if (pos.y < 0 || pos.x > (SCREEN_WIDTH + 100f) / PPM || pos.x < -50f / PPM) {
            resetBallToSpawn();
        }
    }

    private void updateTipBox(float dt) {
        tipTimer += dt;
        if (userInteracted || tipTimer > TIP_INITIAL_DELAY) {
            tipAlpha = Math.max(0f, tipAlpha - TIP_FADE_SPEED * dt);
        }
    }

    private void drawEnvironment() {
        // Ground base
        drawer.setColor(new Color(0.14f, 0.17f, 0.22f, 1f));
        drawer.filledRectangle(0, 0, SCREEN_WIDTH, 70f);

        // Ground accent line
        drawer.setColor(new Color(0.28f, 0.78f, 0.65f, 1f));
        drawer.filledRectangle(0, 68f, SCREEN_WIDTH, 4f);

        // Spawn pedestal
        drawer.setColor(new Color(0.20f, 0.24f, 0.32f, 1f));
        drawer.filledRectangle(spawnPositionPx.x - 26f, 70f, 52f, spawnPositionPx.y - 70f - ballRadiusPx);

        // Pedestal cap
        drawer.setColor(new Color(0.35f, 0.42f, 0.52f, 1f));
        drawer.filledRectangle(spawnPositionPx.x - 30f, spawnPositionPx.y - ballRadiusPx - 4f, 60f, 4f);
    }

    private void drawTowerBlocks() {
        for (Body body : towerBodies) {
            if (!(body.getUserData() instanceof BlockData)) continue;
            BlockData data = (BlockData) body.getUserData();

            Vector2 pos = body.getPosition().scl(PPM);
            float angleRad = body.getAngle();

            // Draw rotated filled block using ShapeDrawer
            drawer.setColor(data.fillColor);
            drawer.filledRectangle(pos.x - data.widthPx / 2f, pos.y - data.heightPx / 2f,
                    data.widthPx, data.heightPx, angleRad);

            // Draw block border with thickness
            drawer.setColor(data.borderColor);
            drawer.rectangle(pos.x - data.widthPx / 2f, pos.y - data.heightPx / 2f,
                    data.widthPx, data.heightPx, 2f, angleRad);
        }
    }

    private void drawTrail(float dt) {
        if (isAirborne) {
            trailTimer += dt;
            if (trailTimer > 0.02f) {
                trailTimer = 0f;
                Vector2 pos = new Vector2(ballBody.getPosition()).scl(PPM);
                trail.add(pos);
                if (trail.size() > 28) {
                    trail.remove(0);
                }
            }
        }

        if (trail.size() < 2) return;
        for (int i = 0; i < trail.size(); i++) {
            Vector2 p = trail.get(i);
            float ratio = (float) i / trail.size();
            drawer.setColor(new Color(1f, 0.55f, 0.15f, ratio * 0.45f));
            drawer.filledCircle(p.x, p.y, ballRadiusPx * 0.35f * ratio);
        }
    }

    private void drawAimAssist() {
        Vector2 ballPosPx = new Vector2(ballBody.getPosition()).scl(PPM);

        // Slingshot pull line
        drawer.setColor(new Color(1f, 0.35f, 0.35f, 0.85f));
        drawer.line(ballPosPx.x, ballPosPx.y, currentDragPx.x, currentDragPx.y, 3f);

        // Trajectory preview
        Vector2 simPosPx = new Vector2(ballPosPx);
        Vector2 simVelPx = new Vector2(dragStartPx).sub(currentDragPx).scl(FORCE_MULTIPLIER * PPM);
        Vector2 gravityPx = new Vector2(world.getGravity()).scl(PPM);
        float stepTime = 0.04f;

        for (int i = 0; i < 22; i++) {
            simVelPx.add(gravityPx.x * stepTime, gravityPx.y * stepTime);
            simPosPx.add(simVelPx.x * stepTime, simVelPx.y * stepTime);

            if (simPosPx.y <= 70f + ballRadiusPx) break;

            float alpha = 1f - (i / 22f);
            drawer.setColor(new Color(0.3f, 0.95f, 0.6f, alpha * 0.9f));
            drawer.filledCircle(simPosPx.x, simPosPx.y, 4.5f);
        }
    }

    private void drawBall() {
        Vector2 ballPosPx = new Vector2(ballBody.getPosition()).scl(PPM);

        // Outer ball circle
        drawer.setColor(new Color(0.92f, 0.38f, 0.18f, 1f));
        drawer.filledCircle(ballPosPx.x, ballPosPx.y, ballRadiusPx);

        // Inner specular highlight
        drawer.setColor(new Color(1f, 0.82f, 0.42f, 1f));
        drawer.filledCircle(ballPosPx.x - ballRadiusPx * 0.25f, ballPosPx.y + ballRadiusPx * 0.25f,
                ballRadiusPx * 0.52f);
    }

    private void drawParticles() {
        for (Particle p : particles) {
            float alpha = p.life / p.maxLife;
            drawer.setColor(new Color(p.color.r, p.color.g, p.color.b, alpha));
            drawer.filledCircle(p.pos.x, p.pos.y, p.size * alpha);
        }
    }

    private void drawTipBox() {
        float boxWidth = 460f;
        float boxHeight = 110f;
        float boxX = (SCREEN_WIDTH - boxWidth) / 2f;
        float boxY = SCREEN_HEIGHT - boxHeight - 35f;

        // Background card
        drawer.setColor(new Color(0.08f, 0.10f, 0.14f, 0.85f * tipAlpha));
        drawer.filledRectangle(boxX, boxY, boxWidth, boxHeight);

        // Card border
        drawer.setColor(new Color(0.28f, 0.78f, 0.65f, 0.80f * tipAlpha));
        drawer.rectangle(boxX, boxY, boxWidth, boxHeight, 2f);

        // Text content
        titleFont.setColor(0.28f, 0.78f, 0.65f, tipAlpha);
        titleFont.draw(spriteBatch, "HOW TO PLAY", boxX + 22f, boxY + boxHeight - 20f);

        regularFont.setColor(0.92f, 0.94f, 0.96f, tipAlpha);
        regularFont.draw(spriteBatch, "• Drag & release the ball to demolish the towers!", boxX + 22f, boxY + boxHeight - 52f);
        regularFont.draw(spriteBatch, "• Press [R] to rebuild the towers & reset ball", boxX + 22f, boxY + boxHeight - 78f);
    }

    @Override
    public void dispose() {
        if (pixelTexture != null) pixelTexture.dispose();
        spriteBatch.dispose();
        if (titleFont != null) titleFont.dispose();
        if (regularFont != null) regularFont.dispose();
        if (soundEffects != null) soundEffects.dispose();
        if (rayHandler != null) rayHandler.dispose();
        if (world != null) world.dispose();
    }
}
