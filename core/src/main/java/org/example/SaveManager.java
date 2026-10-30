package org.example;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

/**
 * Oyun ilerlemesi, kullanıcı ayarları ve istatistiklerin LibGDX Preferences
 * ile kalıcı olarak saklanmasını sağlayan merkezi yönetici sınıfı.
 */
public class SaveManager {

    private static final String PREFS_NAME = "SudokuMasterPaperPreferences";

    // İlerleme Anahtarları
    private static final String KEY_HAS_SAVED = "game_has_saved";
    private static final String KEY_BOARD = "game_board";
    private static final String KEY_INITIAL = "game_initial";
    private static final String KEY_SOLUTION = "game_solution";
    private static final String KEY_NOTES = "game_notes";
    private static final String KEY_ELAPSED_TIME = "game_elapsed_time";
    private static final String KEY_DIFFICULTY = "game_difficulty";
    private static final String KEY_MISTAKES = "game_mistakes";

    // İstatistik Anahtarları
    private static final String KEY_STATS_COMPLETED = "stats_games_completed";
    private static final String KEY_STATS_TOTAL_TIME = "stats_total_play_time";
    private static final String KEY_STATS_BEST_PREFIX = "stats_best_time_";

    // Ayar Anahtarları
    private static final String KEY_SETTING_SOUND = "setting_sound_enabled";
    private static final String KEY_SETTING_LIGHT = "setting_light_enabled";

    private final Preferences prefs;

    public SaveManager() {
        this.prefs = Gdx.app.getPreferences(PREFS_NAME);
    }

    // ==========================================
    // 1. OYUN İLERLEMESİ KAYIT & DEVAM ET
    // ==========================================

    /**
     * Aktif oyunun tüm tahta verilerini, notlarını ve süresini kaydeder.
     */
    public void saveGame(SudokuEngine engine) {
        if (engine == null || engine.isCompleted() || engine.isGameOver()) {
            clearSavedGame();
            return;
        }

        StringBuilder boardSb = new StringBuilder();
        StringBuilder initialSb = new StringBuilder();
        StringBuilder solutionSb = new StringBuilder();
        StringBuilder notesSb = new StringBuilder();

        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                boardSb.append(engine.getCell(r, c)).append(",");
                initialSb.append(engine.getInitialCell(r, c)).append(",");
                solutionSb.append(engine.getSolutionCell(r, c)).append(",");

                // Her hücredeki 9 olası notu bitmask olarak kompakt sakla
                int mask = 0;
                for (int d = 1; d <= 9; d++) {
                    if (engine.hasNote(r, c, d)) {
                        mask |= (1 << (d - 1));
                    }
                }
                notesSb.append(mask).append(",");
            }
        }

        prefs.putBoolean(KEY_HAS_SAVED, true);
        prefs.putString(KEY_BOARD, boardSb.toString());
        prefs.putString(KEY_INITIAL, initialSb.toString());
        prefs.putString(KEY_SOLUTION, solutionSb.toString());
        prefs.putString(KEY_NOTES, notesSb.toString());
        prefs.putFloat(KEY_ELAPSED_TIME, engine.getElapsedTime());
        prefs.putString(KEY_DIFFICULTY, engine.getDifficulty().name());
        prefs.putInteger(KEY_MISTAKES, engine.getMistakes());
        prefs.flush();
    }

    /**
     * Kayıtlı oyun varsa motor içine aktararak oyunu geri yükler.
     * @return Yükleme başarılıysa true, kayıt yoksa false döner.
     */
    public boolean loadGame(SudokuEngine engine) {
        if (!hasSavedGame() || engine == null) return false;

        try {
            String boardStr = prefs.getString(KEY_BOARD, "");
            String initStr = prefs.getString(KEY_INITIAL, "");
            String solStr = prefs.getString(KEY_SOLUTION, "");
            String notesStr = prefs.getString(KEY_NOTES, "");

            if (boardStr.isEmpty() || initStr.isEmpty() || solStr.isEmpty()) {
                clearSavedGame();
                return false;
            }

            String[] bArr = boardStr.split(",");
            String[] iArr = initStr.split(",");
            String[] sArr = solStr.split(",");
            String[] nArr = notesStr.split(",");

            int[][] board = new int[9][9];
            int[][] initial = new int[9][9];
            int[][] solution = new int[9][9];
            boolean[][][] notes = new boolean[9][9][9];

            int idx = 0;
            for (int r = 0; r < 9; r++) {
                for (int c = 0; c < 9; c++) {
                    board[r][c] = Integer.parseInt(bArr[idx]);
                    initial[r][c] = Integer.parseInt(iArr[idx]);
                    solution[r][c] = Integer.parseInt(sArr[idx]);

                    int mask = Integer.parseInt(nArr[idx]);
                    for (int d = 1; d <= 9; d++) {
                        notes[r][c][d - 1] = (mask & (1 << (d - 1))) != 0;
                    }
                    idx++;
                }
            }

            float time = prefs.getFloat(KEY_ELAPSED_TIME, 0f);
            String diffName = prefs.getString(KEY_DIFFICULTY, SudokuEngine.Difficulty.MEDIUM.name());
            SudokuEngine.Difficulty diff;
            try {
                diff = SudokuEngine.Difficulty.valueOf(diffName);
            } catch (Exception ex) {
                diff = SudokuEngine.Difficulty.MEDIUM;
            }
            int mistakes = prefs.getInteger(KEY_MISTAKES, 0);

            engine.restoreGame(diff, board, initial, solution, notes, time, mistakes);
            return true;
        } catch (Exception e) {
            Gdx.app.error("SaveManager", "Kayıtlı oyun yüklenirken hata oluştu: " + e.getMessage());
            clearSavedGame();
            return false;
        }
    }

    /**
     * Devam edilebilir bir oyun kaydının var olup olmadığını kontrol eder.
     */
    public boolean hasSavedGame() {
        return prefs.getBoolean(KEY_HAS_SAVED, false);
    }

    /**
     * Mevcut oyun kaydını temizler.
     */
    public void clearSavedGame() {
        prefs.putBoolean(KEY_HAS_SAVED, false);
        prefs.remove(KEY_BOARD);
        prefs.remove(KEY_INITIAL);
        prefs.remove(KEY_SOLUTION);
        prefs.remove(KEY_NOTES);
        prefs.remove(KEY_ELAPSED_TIME);
        prefs.remove(KEY_DIFFICULTY);
        prefs.remove(KEY_MISTAKES);
        prefs.flush();
    }

    // ==========================================
    // 2. İSTATİSTİK YÖNETİMİ
    // ==========================================

    /**
     * Tamamlanan oyun sayısını döndürür.
     */
    public int getGamesCompleted() {
        return prefs.getInteger(KEY_STATS_COMPLETED, 0);
    }

    /**
     * Zorluk seviyesine göre en iyi süreyi saniye cinsinden döndürür (0 ise henüz rekor yok).
     */
    public float getBestTime(SudokuEngine.Difficulty diff) {
        return prefs.getFloat(KEY_STATS_BEST_PREFIX + diff.name(), 0f);
    }

    /**
     * Toplam oyun oynama süresini saniye cinsinden döndürür.
     */
    public float getTotalPlayTime() {
        return prefs.getFloat(KEY_STATS_TOTAL_TIME, 0f);
    }

    /**
     * Bir oyun başarıyla tamamlandığında istatistikleri günceller.
     */
    public void recordGameCompleted(SudokuEngine.Difficulty diff, float time) {
        int count = getGamesCompleted() + 1;
        prefs.putInteger(KEY_STATS_COMPLETED, count);

        float currentBest = getBestTime(diff);
        if (currentBest <= 0f || time < currentBest) {
            prefs.putFloat(KEY_STATS_BEST_PREFIX + diff.name(), time);
        }

        addTotalPlayTime(time);
        clearSavedGame();
        prefs.flush();
    }

    /**
     * Toplam oyun süresine ekleme yapar.
     */
    public void addTotalPlayTime(float seconds) {
        float current = getTotalPlayTime() + seconds;
        prefs.putFloat(KEY_STATS_TOTAL_TIME, current);
        prefs.flush();
    }

    // ==========================================
    // 3. KULLANICI AYARLARI
    // ==========================================

    /**
     * Ses efektlerinin açık olup olmadığını döndürür (varsayılan: açık).
     */
    public boolean isSoundEnabled() {
        return prefs.getBoolean(KEY_SETTING_SOUND, true);
    }

    /**
     * Ses efektleri ayarını kaydeder.
     */
    public void setSoundEnabled(boolean enabled) {
        prefs.putBoolean(KEY_SETTING_SOUND, enabled);
        prefs.flush();
    }

    /**
     * Masa lambası ışık efektinin açık olup olmadığını döndürür (varsayılan: açık).
     */
    public boolean isLightEnabled() {
        return prefs.getBoolean(KEY_SETTING_LIGHT, true);
    }

    /**
     * Masa lambası ışık efekti ayarını kaydeder.
     */
    public void setLightEnabled(boolean enabled) {
        prefs.putBoolean(KEY_SETTING_LIGHT, enabled);
        prefs.flush();
    }

    // ==========================================
    // 4. YARDIMCI BİÇİMLENDİRME METOTLARI
    // ==========================================

    /**
     * Saniyeyi MM:SS formatına dönüştürür.
     */
    public static String formatTime(float seconds) {
        if (seconds <= 0f) return "--:--";
        int totalSec = (int) seconds;
        int m = totalSec / 60;
        int s = totalSec % 60;
        return String.format("%02d:%02d", m, s);
    }

    /**
     * Saniyeyi saat ve dakika formatına (ör. 1s 24dk veya 45dk 12sn) dönüştürür.
     */
    public static String formatTotalTime(float seconds) {
        if (seconds <= 0f) return "0 dk";
        int totalSec = (int) seconds;
        int h = totalSec / 3600;
        int m = (totalSec % 3600) / 60;
        int s = totalSec % 60;
        if (h > 0) {
            return String.format("%ds %02ddk", h, m);
        } else {
            return String.format("%02ddk %02dsn", m, s);
        }
    }
}
