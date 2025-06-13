package io.celox.hexpulse.game;

import java.util.Objects;

/**
 * Represents a position on the hexagonal board using axial coordinates
 */
public class Hex {
    public final int q;
    public final int r;
    
    // Direction vectors for hexagonal movement
    public static final int[][] DIRECTIONS = {
        {1, 0}, {1, -1}, {0, -1},
        {-1, 0}, {-1, 1}, {0, 1}
    };
    
    public Hex(int q, int r) {
        this.q = q;
        this.r = r;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Hex hex = (Hex) obj;
        return q == hex.q && r == hex.r;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(q, r);
    }
    
    @Override
    public String toString() {
        return "Hex(" + q + ", " + r + ")";
    }
    
    /**
     * Parse a Hex from its string representation
     */
    public static Hex fromString(String str) {
        if (str == null || str.trim().isEmpty()) return null;
        
        try {
            // Parse format "Hex(q, r)"
            String clean = str.trim();
            if (clean.startsWith("Hex(") && clean.endsWith(")")) {
                String coords = clean.substring(4, clean.length() - 1);
                String[] parts = coords.split(",");
                if (parts.length == 2) {
                    int q = Integer.parseInt(parts[0].trim());
                    int r = Integer.parseInt(parts[1].trim());
                    return new Hex(q, r);
                }
            }
        } catch (NumberFormatException e) {
            // Return null on parse error
        }
        return null;
    }
    
    /**
     * Calculate distance between two hexagons
     */
    public int distance(Hex other) {
        return (Math.abs(q - other.q) + Math.abs(q + r - other.q - other.r) + Math.abs(r - other.r)) / 2;
    }
    
    /**
     * Get neighbor in specified direction (0-5)
     */
    public Hex neighbor(int direction) {
        int[] dir = DIRECTIONS[direction];
        return new Hex(q + dir[0], r + dir[1]);
    }
    
    /**
     * Add two hex coordinates
     */
    public Hex add(Hex other) {
        return new Hex(q + other.q, r + other.r);
    }
    
    /**
     * Subtract hex coordinates
     */
    public Hex subtract(Hex other) {
        return new Hex(q - other.q, r - other.r);
    }
    
    /**
     * Convert hex coordinates to pixel coordinates
     */
    public float[] toPixel(float centerX, float centerY, float hexSize) {
        float x = hexSize * (3f / 2f * q);
        float y = hexSize * ((float) Math.sqrt(3) / 2f * q + (float) Math.sqrt(3) * r);
        return new float[]{centerX + x, centerY + y};
    }
    
    /**
     * Convert pixel coordinates to hex coordinates
     */
    public static Hex fromPixel(float x, float y, float centerX, float centerY, float hexSize) {
        x -= centerX;
        y -= centerY;
        
        float q = (2f / 3f * x) / hexSize;
        float r = (-1f / 3f * x + (float) Math.sqrt(3) / 3f * y) / hexSize;
        
        return roundHex(q, r);
    }
    
    /**
     * Round fractional hex coordinates to nearest hex
     */
    private static Hex roundHex(float q, float r) {
        float s = -q - r;
        int rq = Math.round(q);
        int rr = Math.round(r);
        int rs = Math.round(s);
        
        float qDiff = Math.abs(rq - q);
        float rDiff = Math.abs(rr - r);
        float sDiff = Math.abs(rs - s);
        
        if (qDiff > rDiff && qDiff > sDiff) {
            rq = -rr - rs;
        } else if (rDiff > sDiff) {
            rr = -rq - rs;
        }
        
        return new Hex(rq, rr);
    }
}