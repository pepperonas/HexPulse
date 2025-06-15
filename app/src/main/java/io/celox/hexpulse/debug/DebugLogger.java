package io.celox.hexpulse.debug;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.celox.hexpulse.game.Hex;
import io.celox.hexpulse.game.Player;

/**
 * Debug logger for critical game moves and board states
 */
public class DebugLogger {
    private static final String TAG = "DebugLogger";
    private static final String LOG_DIR = "hexpulse";
    private static final String LOG_PREFIX = "debug_moves_";
    private static final String LOG_EXTENSION = ".log";
    
    private Context context;
    private File logDir;
    
    public DebugLogger(Context context) {
        this.context = context;
        initializeLogDirectory();
    }
    
    private void initializeLogDirectory() {
        try {
            // Use Downloads directory for easier access
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            logDir = new File(downloadsDir, LOG_DIR);
            
            if (!logDir.exists()) {
                boolean created = logDir.mkdirs();
                Log.d(TAG, "Log directory created: " + created + " at " + logDir.getAbsolutePath());
            }
            
            Log.d(TAG, "Log directory: " + logDir.getAbsolutePath());
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize log directory", e);
            // Fallback to internal storage if Downloads directory fails
            try {
                File internalDir = context.getFilesDir();
                logDir = new File(internalDir, LOG_DIR);
                if (!logDir.exists()) {
                    logDir.mkdirs();
                }
                Log.d(TAG, "Using fallback internal directory: " + logDir.getAbsolutePath());
            } catch (Exception fallbackException) {
                Log.e(TAG, "Failed to initialize fallback log directory", fallbackException);
            }
        }
    }
    
    /**
     * Log a critical move with before and after board states
     */
    public void logCriticalMove(Map<Hex, Player> beforeState, Map<Hex, Player> afterState, 
                               List<Hex> selectedMarbles, Hex targetPosition, 
                               Player currentPlayer, String description) {
        try {
            String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(new Date());
            String filename = LOG_PREFIX + timestamp + LOG_EXTENSION;
            File logFile = new File(logDir, filename);
            
            StringBuilder logContent = new StringBuilder();
            
            // Header
            logContent.append("=== HEXPULSE CRITICAL MOVE DEBUG LOG ===\n");
            logContent.append("Timestamp: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date())).append("\n");
            logContent.append("Description: ").append(description).append("\n");
            logContent.append("Current Player: ").append(currentPlayer).append("\n");
            logContent.append("Selected Marbles: ").append(selectedMarbles).append("\n");
            logContent.append("Target Position: ").append(targetPosition).append("\n");
            logContent.append("Log File: ").append(logFile.getAbsolutePath()).append("\n");
            logContent.append("\n");
            
            // Before state
            logContent.append("=== BOARD STATE BEFORE MOVE ===\n");
            logContent.append(formatBoardState(beforeState));
            logContent.append("\n");
            
            // After state
            logContent.append("=== BOARD STATE AFTER MOVE ===\n");
            logContent.append(formatBoardState(afterState));
            logContent.append("\n");
            
            // Differences
            logContent.append("=== POSITION CHANGES ===\n");
            logContent.append(formatPositionChanges(beforeState, afterState));
            logContent.append("\n");
            
            // Coordinate mapping for debugging
            logContent.append("=== COORDINATE REFERENCE ===\n");
            logContent.append(formatCoordinateReference());
            
            // Write to file
            try (FileWriter writer = new FileWriter(logFile)) {
                writer.write(logContent.toString());
                writer.flush();
            }
            
            Log.d(TAG, "Critical move logged to: " + logFile.getAbsolutePath());
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to log critical move", e);
        }
    }
    
    /**
     * Format board state as readable text
     */
    private String formatBoardState(Map<Hex, Player> boardState) {
        StringBuilder sb = new StringBuilder();
        
        // Create a visual representation of the hexagonal board
        // Board layout: radius 4, so q and r range from -4 to 4
        for (int r = -4; r <= 4; r++) {
            // Add indentation for hexagonal shape
            for (int i = 0; i < Math.abs(r); i++) {
                sb.append(" ");
            }
            
            for (int q = -4; q <= 4; q++) {
                int s = -q - r;
                if (s >= -4 && s <= 4) {
                    Hex hex = new Hex(q, r);
                    Player player = boardState.get(hex);
                    
                    if (player == null) {
                        sb.append("? "); // Unknown state
                    } else {
                        switch (player) {
                            case BLACK:
                                sb.append("B ");
                                break;
                            case WHITE:
                                sb.append("W ");
                                break;
                            case EMPTY:
                                sb.append(". ");
                                break;
                        }
                    }
                }
            }
            sb.append("\n");
        }
        
        // Also add detailed coordinate list
        sb.append("\nDetailed position list:\n");
        for (Map.Entry<Hex, Player> entry : boardState.entrySet()) {
            if (entry.getValue() != Player.EMPTY) {
                sb.append(String.format("(%d,%d): %s\n", 
                    entry.getKey().q, entry.getKey().r, entry.getValue()));
            }
        }
        
        return sb.toString();
    }
    
    /**
     * Format position changes between two states
     */
    private String formatPositionChanges(Map<Hex, Player> before, Map<Hex, Player> after) {
        StringBuilder sb = new StringBuilder();
        
        // Find all positions that changed
        for (Hex hex : before.keySet()) {
            Player beforePlayer = before.get(hex);
            Player afterPlayer = after.get(hex);
            
            if (beforePlayer != afterPlayer) {
                sb.append(String.format("Position (%d,%d): %s -> %s\n", 
                    hex.q, hex.r, beforePlayer, afterPlayer));
            }
        }
        
        // Check for new positions in after state
        for (Hex hex : after.keySet()) {
            if (!before.containsKey(hex)) {
                sb.append(String.format("NEW Position (%d,%d): -> %s\n", 
                    hex.q, hex.r, after.get(hex)));
            }
        }
        
        return sb.toString();
    }
    
    /**
     * Generate coordinate reference for debugging
     */
    private String formatCoordinateReference() {
        StringBuilder sb = new StringBuilder();
        sb.append("Hexagonal coordinate system (q, r):\n");
        
        for (int r = -4; r <= 4; r++) {
            // Add indentation for hexagonal shape
            for (int i = 0; i < Math.abs(r); i++) {
                sb.append("      ");
            }
            
            for (int q = -4; q <= 4; q++) {
                int s = -q - r;
                if (s >= -4 && s <= 4) {
                    sb.append(String.format("(%2d,%2d) ", q, r));
                }
            }
            sb.append("\n");
        }
        
        return sb.toString();
    }
    
    /**
     * Get the log directory path for user information
     */
    public String getLogDirectoryPath() {
        return logDir != null ? logDir.getAbsolutePath() : "Log directory not available";
    }
    
    /**
     * Check if logging is available
     */
    public boolean isLoggingAvailable() {
        return logDir != null && logDir.exists() && logDir.canWrite();
    }
}