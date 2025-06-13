package io.celox.hexpulse.ui.views;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import androidx.annotation.Nullable;

import io.celox.hexpulse.game.*;
import java.util.*;

/**
 * Custom view for drawing the hexagonal Abalone board
 */
public class HexagonalBoardView extends View {
    
    // Drawing constants
    private static final float HEX_SIZE = 40f;
    private static final float MARBLE_RADIUS = 16f;
    private static final float BOARD_PADDING = 100f;
    
    // Paints for drawing
    private Paint hexPaint;
    private Paint hexBorderPaint;
    private Paint hexSelectedPaint;
    private Paint hexValidMovePaint;
    private Paint blackMarblePaint;
    private Paint whiteMarblePaint;
    private Paint marbleHighlightPaint;
    private Paint textPaint;
    
    // Game state
    private AbaloneGame game;
    private Theme currentTheme = Theme.CLASSIC;
    private Hex hoveredHex = null;
    
    // Board center coordinates
    private float centerX;
    private float centerY;
    
    // Touch handling
    public interface BoardTouchListener {
        void onHexTouched(Hex position);
    }
    
    private BoardTouchListener touchListener;
    
    public HexagonalBoardView(Context context) {
        super(context);
        init();
    }
    
    public HexagonalBoardView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    public HexagonalBoardView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    
    private void init() {
        // Initialize paints
        hexPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        hexPaint.setStyle(Paint.Style.FILL);
        
        hexBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        hexBorderPaint.setStyle(Paint.Style.STROKE);
        hexBorderPaint.setStrokeWidth(3f);
        
        hexSelectedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        hexSelectedPaint.setStyle(Paint.Style.STROKE);
        hexSelectedPaint.setStrokeWidth(4f);
        
        hexValidMovePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        hexValidMovePaint.setStyle(Paint.Style.STROKE);
        hexValidMovePaint.setStrokeWidth(3f);
        
        blackMarblePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        blackMarblePaint.setStyle(Paint.Style.FILL);
        
        whiteMarblePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        whiteMarblePaint.setStyle(Paint.Style.FILL);
        
        marbleHighlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        marbleHighlightPaint.setStyle(Paint.Style.STROKE);
        marbleHighlightPaint.setStrokeWidth(3f);
        
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(24f);
        
        updateThemeColors();
        
        // Initialize game
        game = new AbaloneGame();
    }
    
    public void setGame(AbaloneGame game) {
        this.game = game;
        invalidate();
    }
    
    public void setTheme(Theme theme) {
        this.currentTheme = theme;
        updateThemeColors();
        invalidate();
    }
    
    public void setBoardTouchListener(BoardTouchListener listener) {
        this.touchListener = listener;
    }
    
    private void updateThemeColors() {
        hexPaint.setColor(currentTheme.getBoardStartColor());
        hexBorderPaint.setColor(currentTheme.getBoardBorderColor());
        hexSelectedPaint.setColor(Color.YELLOW);
        hexValidMovePaint.setColor(currentTheme.getHighlightColor());
        
        // Marble colors with gradients
        blackMarblePaint.setColor(Color.rgb(30, 30, 35));
        whiteMarblePaint.setColor(Color.rgb(240, 240, 245));
        marbleHighlightPaint.setColor(Color.YELLOW);
        
        textPaint.setColor(Color.WHITE);
    }
    
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        centerX = w / 2f;
        centerY = h / 2f;
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        if (game == null) {
            return;
        }
        
        // Draw background
        canvas.drawColor(currentTheme.getBackgroundColor());
        
        // Draw hexagons
        for (Hex position : game.getAllPositions()) {
            drawHexagon(canvas, position);
        }
        
        // Draw marbles
        for (Hex position : game.getAllPositions()) {
            Player player = game.getPlayerAt(position);
            if (player.isPlayer()) {
                drawMarble(canvas, position, player);
            }
        }
        
        // Draw UI information
        drawGameInfo(canvas);
    }
    
    private void drawHexagon(Canvas canvas, Hex position) {
        float[] center = position.toPixel(centerX, centerY, HEX_SIZE);
        float x = center[0];
        float y = center[1];
        
        // Create hexagon path
        Path hexPath = createHexagonPath(x, y, HEX_SIZE * 0.9f);
        
        // Determine hexagon state
        boolean isSelected = game.getSelectedMarbles().contains(position);
        boolean isValidMove = game.getValidMoves().contains(position);
        boolean isHovered = position.equals(hoveredHex);
        
        // Draw hexagon fill with gradient effect
        drawHexagonWithGradient(canvas, hexPath, x, y);
        
        // Draw border
        canvas.drawPath(hexPath, hexBorderPaint);
        
        // Draw selection highlight
        if (isSelected) {
            Paint selectedPaint = new Paint(hexSelectedPaint);
            selectedPaint.setColor(Color.argb(200, 255, 193, 7)); // Golden glow
            canvas.drawPath(hexPath, selectedPaint);
        }
        
        // Draw valid move highlight
        if (isValidMove) {
            Paint validPaint = new Paint(hexValidMovePaint);
            validPaint.setColor(Color.argb(150, 102, 187, 106)); // Green glow
            canvas.drawPath(hexPath, validPaint);
        }
        
        // Draw hover effect
        if (isHovered && !isSelected) {
            Paint hoverPaint = new Paint(hexBorderPaint);
            hoverPaint.setColor(Color.argb(100, 100, 181, 246)); // Blue glow
            hoverPaint.setStrokeWidth(5f);
            canvas.drawPath(hexPath, hoverPaint);
        }
    }
    
    private void drawHexagonWithGradient(Canvas canvas, Path hexPath, float centerX, float centerY) {
        // Create radial gradient for 3D effect
        RadialGradient gradient = new RadialGradient(
            centerX - HEX_SIZE * 0.3f, centerY - HEX_SIZE * 0.3f, HEX_SIZE,
            currentTheme.getBoardEndColor(),
            currentTheme.getBoardStartColor(),
            Shader.TileMode.CLAMP
        );
        
        Paint gradientPaint = new Paint(hexPaint);
        gradientPaint.setShader(gradient);
        canvas.drawPath(hexPath, gradientPaint);
    }
    
    private Path createHexagonPath(float centerX, float centerY, float radius) {
        Path path = new Path();
        boolean first = true;
        
        for (int i = 0; i < 6; i++) {
            double angle = Math.PI / 3 * i + Math.PI / 6;
            float x = centerX + radius * (float) Math.cos(angle);
            float y = centerY + radius * (float) Math.sin(angle);
            
            if (first) {
                path.moveTo(x, y);
                first = false;
            } else {
                path.lineTo(x, y);
            }
        }
        path.close();
        return path;
    }
    
    private void drawMarble(Canvas canvas, Hex position, Player player) {
        float[] center = position.toPixel(centerX, centerY, HEX_SIZE);
        float x = center[0];
        float y = center[1];
        
        boolean isSelected = game.getSelectedMarbles().contains(position);
        
        // Draw marble shadow
        Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        shadowPaint.setColor(Color.argb(100, 0, 0, 0));
        canvas.drawCircle(x + 2, y + 2, MARBLE_RADIUS, shadowPaint);
        
        // Choose marble color and create gradient
        Paint marblePaint;
        int lightColor, darkColor;
        
        if (player == Player.BLACK) {
            lightColor = Color.rgb(60, 60, 70);
            darkColor = Color.rgb(20, 20, 25);
            marblePaint = blackMarblePaint;
        } else {
            lightColor = Color.rgb(255, 255, 255);
            darkColor = Color.rgb(200, 200, 210);
            marblePaint = whiteMarblePaint;
        }
        
        // Create radial gradient for 3D marble effect
        RadialGradient marbleGradient = new RadialGradient(
            x - MARBLE_RADIUS * 0.4f, y - MARBLE_RADIUS * 0.4f, MARBLE_RADIUS,
            lightColor, darkColor, Shader.TileMode.CLAMP
        );
        
        Paint gradientMarblePaint = new Paint(marblePaint);
        gradientMarblePaint.setShader(marbleGradient);
        
        // Draw marble
        canvas.drawCircle(x, y, MARBLE_RADIUS, gradientMarblePaint);
        
        // Draw highlight on marble
        Paint highlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        highlightPaint.setColor(Color.argb(180, 255, 255, 255));
        canvas.drawCircle(x - MARBLE_RADIUS * 0.4f, y - MARBLE_RADIUS * 0.4f, 
                         MARBLE_RADIUS * 0.3f, highlightPaint);
        
        // Draw selection glow
        if (isSelected) {
            Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            glowPaint.setColor(Color.argb(150, 255, 193, 7));
            for (int i = 0; i < 5; i++) {
                canvas.drawCircle(x, y, MARBLE_RADIUS + i * 2, glowPaint);
                glowPaint.setAlpha(glowPaint.getAlpha() - 30);
            }
        }
    }
    
    private void drawGameInfo(Canvas canvas) {
        if (game == null) return;
        
        float infoY = 50f;
        float leftX = 50f;
        float rightX = getWidth() - 200f;
        
        // Current player
        String currentPlayerText = "Current: " + 
            (game.getCurrentPlayer() == Player.BLACK ? "Black" : "White");
        canvas.drawText(currentPlayerText, leftX, infoY, textPaint);
        
        // Scores
        Map<Player, Integer> scores = game.getScores();
        String scoreText = String.format("Black: %d/6  White: %d/6", 
            scores.get(Player.BLACK), scores.get(Player.WHITE));
        canvas.drawText(scoreText, rightX, infoY, textPaint);
        
        // Winner announcement
        Player winner = game.checkWinner();
        if (winner != null) {
            Paint winPaint = new Paint(textPaint);
            winPaint.setTextSize(48f);
            winPaint.setColor(Color.YELLOW);
            String winText = (winner == Player.BLACK ? "Black" : "White") + " Wins!";
            canvas.drawText(winText, centerX, centerY - 200f, winPaint);
        }
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            float touchX = event.getX();
            float touchY = event.getY();
            
            Hex touchedHex = Hex.fromPixel(touchX, touchY, centerX, centerY, HEX_SIZE);
            
            // Check if the touched position is valid
            if (game != null && game.isValidPosition(touchedHex)) {
                hoveredHex = touchedHex;
                
                if (touchListener != null) {
                    touchListener.onHexTouched(touchedHex);
                }
                
                invalidate();
                return true;
            }
        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            float touchX = event.getX();
            float touchY = event.getY();
            
            Hex newHoveredHex = Hex.fromPixel(touchX, touchY, centerX, centerY, HEX_SIZE);
            
            if (game != null && game.isValidPosition(newHoveredHex) && 
                !newHoveredHex.equals(hoveredHex)) {
                hoveredHex = newHoveredHex;
                invalidate();
            }
        } else if (event.getAction() == MotionEvent.ACTION_UP || 
                   event.getAction() == MotionEvent.ACTION_CANCEL) {
            hoveredHex = null;
            invalidate();
        }
        
        return super.onTouchEvent(event);
    }
    
    /**
     * Get optimal view size
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int size = (int) (HEX_SIZE * 20 + BOARD_PADDING * 2);
        setMeasuredDimension(size, size);
    }
}