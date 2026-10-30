package org.example;

/**
 * Sudoku ızgarasındaki her bir hücrenin görsel ve animasyon durumunu tutan sınıf.
 * Universal Tween Engine (CellAccessor) tarafından animasyon değerleri güncellenir.
 */
public class Cell {
    public final int row;
    public final int col;

    // Animasyon durum değişkenleri
    private float scale = 1.0f;
    private float offsetX = 0.0f;
    private float offsetY = 0.0f;
    private float selectHighlight = 0.0f; // 0.0f (seçili değil) -> 1.0f (seçili kâğıt vurgusu)
    private float errorHighlight = 0.0f;  // 0.0f -> 1.0f (kırmızı mürekkep geçişi)
    private float popScale = 1.0f;        // Sayı yazıldığında kalem vuruşu yumuşak pop efekti

    public Cell(int row, int col) {
        this.row = row;
        this.col = col;
    }

    public float getScale() {
        return scale;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

    public float getOffsetX() {
        return offsetX;
    }

    public void setOffsetX(float offsetX) {
        this.offsetX = offsetX;
    }

    public float getOffsetY() {
        return offsetY;
    }

    public void setOffsetY(float offsetY) {
        this.offsetY = offsetY;
    }

    public float getSelectHighlight() {
        return selectHighlight;
    }

    public void setSelectHighlight(float selectHighlight) {
        this.selectHighlight = selectHighlight;
    }

    public float getErrorHighlight() {
        return errorHighlight;
    }

    public void setErrorHighlight(float errorHighlight) {
        this.errorHighlight = errorHighlight;
    }

    public float getPopScale() {
        return popScale;
    }

    public void setPopScale(float popScale) {
        this.popScale = popScale;
    }
}
