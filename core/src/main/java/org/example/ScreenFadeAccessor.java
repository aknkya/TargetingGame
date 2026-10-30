package org.example;

import aurelienribon.tweenengine.TweenAccessor;

/**
 * Universal Tween Engine TweenAccessor for the ScreenFade class.
 * Interpolates alpha values for smooth fade transitions between screens.
 */
public class ScreenFadeAccessor implements TweenAccessor<ScreenFade> {

    public static final int TYPE_ALPHA = 1;

    @Override
    public int getValues(ScreenFade target, int tweenType, float[] returnValues) {
        if (tweenType == TYPE_ALPHA) {
            returnValues[0] = target.getAlpha();
            return 1;
        }
        return -1;
    }

    @Override
    public void setValues(ScreenFade target, int tweenType, float[] newValues) {
        if (tweenType == TYPE_ALPHA) {
            target.setAlpha(newValues[0]);
        }
    }
}
