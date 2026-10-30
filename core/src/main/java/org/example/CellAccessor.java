package org.example;

import aurelienribon.tweenengine.TweenAccessor;

/**
 * Universal Tween Engine TweenAccessor for the Cell class.
 * Handles cell scaling, shake animations, selection paper tinting, and error ink transitions.
 */
public class CellAccessor implements TweenAccessor<Cell> {

    public static final int TYPE_SCALE = 1;
    public static final int TYPE_OFFSET_X = 2;
    public static final int TYPE_SELECT_HIGHLIGHT = 3;
    public static final int TYPE_ERROR_HIGHLIGHT = 4;
    public static final int TYPE_POP_SCALE = 5;

    @Override
    public int getValues(Cell target, int tweenType, float[] returnValues) {
        switch (tweenType) {
            case TYPE_SCALE:
                returnValues[0] = target.getScale();
                return 1;
            case TYPE_OFFSET_X:
                returnValues[0] = target.getOffsetX();
                return 1;
            case TYPE_SELECT_HIGHLIGHT:
                returnValues[0] = target.getSelectHighlight();
                return 1;
            case TYPE_ERROR_HIGHLIGHT:
                returnValues[0] = target.getErrorHighlight();
                return 1;
            case TYPE_POP_SCALE:
                returnValues[0] = target.getPopScale();
                return 1;
            default:
                return -1;
        }
    }

    @Override
    public void setValues(Cell target, int tweenType, float[] newValues) {
        switch (tweenType) {
            case TYPE_SCALE:
                target.setScale(newValues[0]);
                break;
            case TYPE_OFFSET_X:
                target.setOffsetX(newValues[0]);
                break;
            case TYPE_SELECT_HIGHLIGHT:
                target.setSelectHighlight(newValues[0]);
                break;
            case TYPE_ERROR_HIGHLIGHT:
                target.setErrorHighlight(newValues[0]);
                break;
            case TYPE_POP_SCALE:
                target.setPopScale(newValues[0]);
                break;
            default:
                break;
        }
    }
}
