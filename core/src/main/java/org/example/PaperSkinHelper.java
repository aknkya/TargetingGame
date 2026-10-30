package org.example;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;

/**
 * Scene2D için Kâğıt Teması (Paper Edition) ile uyumlu programatik Skin ve bileşen üreticisi.
 * Krem/kırık beyaz zemin, ince kalem çizgisi kenarlıklar, yumuşak kurşun kalem gölgeleri
 * ve mürekkep rengi tipografi sunar.
 */
public class PaperSkinHelper implements Disposable {

    private final Array<Texture> generatedTextures = new Array<>();
    private final Array<BitmapFont> generatedFonts = new Array<>();
    private Skin skin;

    // Kâğıt Teması Renk Paleti (Krem, Kurşun Kalem ve Mürekkep Tonları)
    public static final Color COLOR_PARCHMENT = new Color(0.965f, 0.945f, 0.900f, 1.0f);     // Krem / Parşömen Zemin
    public static final Color COLOR_CARD_BG = new Color(0.985f, 0.970f, 0.940f, 0.98f);       // Kâğıt Kart Yüzeyi
    public static final Color COLOR_PENCIL_BORDER = new Color(0.40f, 0.36f, 0.32f, 0.95f);    // İnce Kurşun Kalem Çizgisi
    public static final Color COLOR_PENCIL_LIGHT = new Color(0.70f, 0.66f, 0.60f, 0.85f);     // Açık Kalem Çizgisi
    public static final Color COLOR_SHADOW = new Color(0.68f, 0.62f, 0.55f, 0.40f);           // Yumuşak Kâğıt Gölgesi
    public static final Color COLOR_INK_BLUE = new Color(0.11f, 0.22f, 0.42f, 1.0f);          // Mavi Tükenmez Kalem Mürekkebi
    public static final Color COLOR_INK_CHARCOAL = new Color(0.18f, 0.17f, 0.16f, 1.0f);      // Koyu Mürekkep / Grafit
    public static final Color COLOR_INK_RED = new Color(0.75f, 0.18f, 0.18f, 1.0f);           // Kırmızı Mürekkep / Düzeltme
    public static final Color COLOR_INK_GREEN = new Color(0.16f, 0.48f, 0.24f, 1.0f);         // Yeşil Mürekkep / Başarı

    public static final String TURKISH_CHARS = FreeTypeFontGenerator.DEFAULT_CHARS
            + "abcçdefgğhıijklmnoöprsştuüvyzABCÇDEFGĞHIİJKLMNOÖPRSŞTUÜVYZ"
            + "0123456789.,:;!?()[]{}/+-*=%&'\"_#<>|~^@$";

    public Skin createPaperSkin() {
        skin = new Skin();

        // 1. FreeType Fontları Yükle (segoeui.ttf Türkçe karakter desteğiyle)
        FileHandle fontHandle = Gdx.files.internal("fonts/segoeui.ttf");
        if (!fontHandle.exists()) {
            fontHandle = Gdx.files.internal("fonts/handwriting.ttf");
        }

        BitmapFont titleFont;
        BitmapFont mediumFont;
        BitmapFont bodyFont;
        BitmapFont smallFont;

        if (fontHandle.exists()) {
            FreeTypeFontGenerator gen = new FreeTypeFontGenerator(fontHandle);
            FreeTypeFontParameter param = new FreeTypeFontParameter();
            param.characters = TURKISH_CHARS;
            param.minFilter = Texture.TextureFilter.Linear;
            param.magFilter = Texture.TextureFilter.Linear;

            param.size = 32;
            titleFont = gen.generateFont(param);

            param.size = 22;
            mediumFont = gen.generateFont(param);

            param.size = 17;
            bodyFont = gen.generateFont(param);

            param.size = 13;
            smallFont = gen.generateFont(param);

            gen.dispose();
        } else {
            titleFont = new BitmapFont();
            mediumFont = new BitmapFont();
            bodyFont = new BitmapFont();
            smallFont = new BitmapFont();
        }

        generatedFonts.add(titleFont);
        generatedFonts.add(mediumFont);
        generatedFonts.add(bodyFont);
        generatedFonts.add(smallFont);

        skin.add("title-font", titleFont);
        skin.add("medium-font", mediumFont);
        skin.add("default-font", bodyFont);
        skin.add("small-font", smallFont);

        // 2. Programatik Kâğıt Dokulu Drawable'lar Üret
        Drawable cardDrawable = createPaperCardDrawable(400, 300, 3, COLOR_CARD_BG, COLOR_PENCIL_BORDER, COLOR_SHADOW);
        Drawable buttonUp = createPaperButtonDrawable(COLOR_CARD_BG, COLOR_PENCIL_BORDER, COLOR_SHADOW, false);
        Drawable buttonOver = createPaperButtonDrawable(new Color(0.93f, 0.90f, 0.83f, 1.0f), COLOR_INK_BLUE, COLOR_SHADOW, false);
        Drawable buttonDown = createPaperButtonDrawable(new Color(0.88f, 0.84f, 0.76f, 1.0f), COLOR_INK_CHARCOAL, null, true);
        Drawable buttonDisabled = createPaperButtonDrawable(new Color(0.93f, 0.92f, 0.90f, 0.65f), COLOR_PENCIL_LIGHT, null, false);

        skin.add("card-bg", cardDrawable, Drawable.class);
        skin.add("button-up", buttonUp, Drawable.class);
        skin.add("button-over", buttonOver, Drawable.class);
        skin.add("button-down", buttonDown, Drawable.class);
        skin.add("button-disabled", buttonDisabled, Drawable.class);

        // 3. TextButtonStyle Tanımla
        TextButtonStyle btnStyle = new TextButtonStyle();
        btnStyle.font = bodyFont;
        btnStyle.fontColor = COLOR_INK_CHARCOAL;
        btnStyle.overFontColor = COLOR_INK_BLUE;
        btnStyle.downFontColor = COLOR_INK_CHARCOAL;
        btnStyle.disabledFontColor = new Color(0.60f, 0.58f, 0.54f, 0.70f);
        btnStyle.up = buttonUp;
        btnStyle.over = buttonOver;
        btnStyle.down = buttonDown;
        btnStyle.disabled = buttonDisabled;
        skin.add("default", btnStyle);

        // Vurgulu (Play / Başlat) Buton Stili
        TextButtonStyle playBtnStyle = new TextButtonStyle(btnStyle);
        playBtnStyle.up = createPaperButtonDrawable(new Color(0.88f, 0.94f, 0.88f, 1.0f), new Color(0.28f, 0.50f, 0.32f, 1.0f), COLOR_SHADOW, false);
        playBtnStyle.over = createPaperButtonDrawable(new Color(0.80f, 0.92f, 0.80f, 1.0f), new Color(0.18f, 0.42f, 0.22f, 1.0f), COLOR_SHADOW, false);
        playBtnStyle.fontColor = COLOR_INK_GREEN;
        playBtnStyle.overFontColor = new Color(0.10f, 0.34f, 0.15f, 1.0f);
        skin.add("play", playBtnStyle);

        // Küçük Buton Stili
        TextButtonStyle smallBtnStyle = new TextButtonStyle(btnStyle);
        smallBtnStyle.font = smallFont;
        skin.add("small", smallBtnStyle);

        // 4. LabelStyle Tanımları
        LabelStyle titleStyle = new LabelStyle(titleFont, COLOR_INK_BLUE);
        LabelStyle defaultStyle = new LabelStyle(bodyFont, COLOR_INK_CHARCOAL);
        LabelStyle mediumStyle = new LabelStyle(mediumFont, COLOR_INK_BLUE);
        LabelStyle smallStyle = new LabelStyle(smallFont, new Color(0.45f, 0.42f, 0.38f, 1.0f));
        LabelStyle greenStyle = new LabelStyle(bodyFont, COLOR_INK_GREEN);
        LabelStyle redStyle = new LabelStyle(bodyFont, COLOR_INK_RED);

        skin.add("title", titleStyle);
        skin.add("default", defaultStyle);
        skin.add("medium", mediumStyle);
        skin.add("small", smallStyle);
        skin.add("green", greenStyle);
        skin.add("red", redStyle);

        return skin;
    }

    /**
     * Kâğıt kartlar için gölgeli ve ince kalem kenarlıklı 9-patch doku oluşturur.
     */
    private NinePatchDrawable createPaperCardDrawable(int w, int h, int border, Color fill, Color stroke, Color shadow) {
        int pad = 12;
        int size = 32;
        Pixmap pix = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        pix.setColor(0, 0, 0, 0);
        pix.fill();

        // Yumuşak gölge
        if (shadow != null) {
            pix.setColor(shadow);
            pix.fillRectangle(4, 4, size - 4, size - 4);
        }

        // Ana kâğıt yüzey
        pix.setColor(fill);
        pix.fillRectangle(2, 2, size - 6, size - 6);

        // İnce kurşun kalem kenarlık
        pix.setColor(stroke);
        for (int b = 0; b < border; b++) {
            pix.drawRectangle(2 + b, 2 + b, size - 6 - b * 2, size - 6 - b * 2);
        }

        Texture tex = new Texture(pix);
        generatedTextures.add(tex);
        pix.dispose();

        NinePatch patch = new NinePatch(tex, pad, pad, pad, pad);
        return new NinePatchDrawable(patch);
    }

    /**
     * Kâğıt dokulu butonlar için 9-patch drawable üretir.
     */
    private NinePatchDrawable createPaperButtonDrawable(Color fill, Color stroke, Color shadow, boolean pressed) {
        int size = 28;
        int pad = 8;
        Pixmap pix = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        pix.setColor(0, 0, 0, 0);
        pix.fill();

        int ox = pressed ? 2 : 1;
        int oy = pressed ? 2 : 1;

        // Gölge (basılmamışsa çiz)
        if (shadow != null && !pressed) {
            pix.setColor(shadow);
            pix.fillRectangle(3, 3, size - 3, size - 3);
        }

        // Dolgu
        pix.setColor(fill);
        pix.fillRectangle(ox, oy, size - 4, size - 4);

        // İnce kalem kenarlık
        pix.setColor(stroke);
        pix.drawRectangle(ox, oy, size - 4, size - 4);

        Texture tex = new Texture(pix);
        generatedTextures.add(tex);
        pix.dispose();

        NinePatch patch = new NinePatch(tex, pad, pad, pad, pad);
        return new NinePatchDrawable(patch);
    }

    /**
     * 1x1 düz renkli doku oluşturucu (arka plan veya çizgi gereksinimleri için).
     */
    public TextureRegionDrawable createSolidDrawable(Color color) {
        Pixmap pix = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pix.setColor(color);
        pix.fill();
        Texture tex = new Texture(pix);
        generatedTextures.add(tex);
        pix.dispose();
        return new TextureRegionDrawable(new TextureRegion(tex));
    }

    public Skin getSkin() {
        return skin;
    }

    @Override
    public void dispose() {
        if (skin != null) {
            skin.dispose();
            skin = null;
        }
        for (Texture tex : generatedTextures) {
            if (tex != null) tex.dispose();
        }
        generatedTextures.clear();
        for (BitmapFont font : generatedFonts) {
            if (font != null) font.dispose();
        }
        generatedFonts.clear();
    }
}
