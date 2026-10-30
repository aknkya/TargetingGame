package org.example;

/**
 * Model representing the visual and animation state of an individual cell on the Sudoku board.
 * Animation properties are interpolated by Universal Tween Engine via CellAccessor.
 */
public class Cell {
    public final int row;
    public final int col;

    // Animation state properties
    private float scale = 1.0f;
    private float offsetX = 0.0f;
    private float offsetY = 0.0f;
    private float selectHighlight = 0.0f; // 0.0f (unselected) -> 1.0f (selected paper wash)
    private float errorHighlight = 0.0f;  // 0.0f -> 1.0f (red ink transition)
    private float popScale = 1.0f;        // Soft pop scale effect when placing a digit

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
