package org.example;

/**
 * Ekranlar arası yumuşak fade (kararma / açılma) animasyonunu tutan model sınıfı.
 */
public class ScreenFade {
    private float alpha = 0.0f;

    public float getAlpha() {
        return alpha;
    }

    public void setAlpha(float alpha) {
        this.alpha = alpha;
    }
}
