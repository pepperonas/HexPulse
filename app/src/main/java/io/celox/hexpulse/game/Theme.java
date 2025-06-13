package io.celox.hexpulse.game;

import android.graphics.Color;

/**
 * Represents different visual themes for the game
 */
public enum Theme {
    CLASSIC,
    DARK,
    OCEAN,
    FOREST;
    
    /**
     * Get background color for this theme
     */
    public int getBackgroundColor() {
        switch (this) {
            case CLASSIC:
                return Color.rgb(15, 20, 35);
            case DARK:
                return Color.rgb(10, 10, 15);
            case OCEAN:
                return Color.rgb(10, 25, 40);
            case FOREST:
                return Color.rgb(20, 30, 20);
            default:
                return Color.rgb(15, 20, 35);
        }
    }
    
    /**
     * Get board start gradient color
     */
    public int getBoardStartColor() {
        switch (this) {
            case CLASSIC:
                return Color.rgb(45, 55, 75);
            case DARK:
                return Color.rgb(30, 30, 40);
            case OCEAN:
                return Color.rgb(20, 60, 100);
            case FOREST:
                return Color.rgb(40, 70, 40);
            default:
                return Color.rgb(45, 55, 75);
        }
    }
    
    /**
     * Get board end gradient color
     */
    public int getBoardEndColor() {
        switch (this) {
            case CLASSIC:
                return Color.rgb(65, 75, 95);
            case DARK:
                return Color.rgb(50, 50, 60);
            case OCEAN:
                return Color.rgb(40, 80, 120);
            case FOREST:
                return Color.rgb(60, 90, 60);
            default:
                return Color.rgb(65, 75, 95);
        }
    }
    
    /**
     * Get board border color
     */
    public int getBoardBorderColor() {
        switch (this) {
            case CLASSIC:
                return Color.rgb(25, 35, 55);
            case DARK:
                return Color.rgb(20, 20, 30);
            case OCEAN:
                return Color.rgb(15, 45, 75);
            case FOREST:
                return Color.rgb(25, 45, 25);
            default:
                return Color.rgb(25, 35, 55);
        }
    }
    
    /**
     * Get highlight color
     */
    public int getHighlightColor() {
        switch (this) {
            case CLASSIC:
                return Color.rgb(102, 187, 106);
            case DARK:
                return Color.rgb(150, 150, 160);
            case OCEAN:
                return Color.rgb(100, 200, 255);
            case FOREST:
                return Color.rgb(144, 238, 144);
            default:
                return Color.rgb(102, 187, 106);
        }
    }
    
    /**
     * Get button start gradient color
     */
    public int getButtonStartColor() {
        switch (this) {
            case CLASSIC:
                return Color.rgb(63, 81, 181);
            case DARK:
                return Color.rgb(40, 40, 50);
            case OCEAN:
                return Color.rgb(30, 144, 255);
            case FOREST:
                return Color.rgb(34, 139, 34);
            default:
                return Color.rgb(63, 81, 181);
        }
    }
    
    /**
     * Get button end gradient color
     */
    public int getButtonEndColor() {
        switch (this) {
            case CLASSIC:
                return Color.rgb(48, 63, 159);
            case DARK:
                return Color.rgb(30, 30, 40);
            case OCEAN:
                return Color.rgb(0, 100, 200);
            case FOREST:
                return Color.rgb(0, 100, 0);
            default:
                return Color.rgb(48, 63, 159);
        }
    }
}