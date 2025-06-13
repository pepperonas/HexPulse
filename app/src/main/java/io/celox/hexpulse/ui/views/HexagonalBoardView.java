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
    
    // Drawing constants - Increased for better visibility
    private static final float HEX_SIZE = 55f;  // Increased from 40f
    private static final float MARBLE_RADIUS = 22f;  // Increased from 16f
    private static final float BOARD_PADDING = 120f;
    
    // Paints for drawing
    private Paint hexPaint;
    private Paint hexBorderPaint;
    private Paint hexSelectedPaint;
    private Paint hexValidMovePaint;
    private Paint hexValidMoveFillPaint;
    private Paint blackMarblePaint;
    private Paint whiteMarblePaint;
    private Paint marbleHighlightPaint;
    private Paint textPaint;
    private Paint arrowPaint;
    private Paint previewMarblePaint;
    
    // Game state
    private AbaloneGame game;
    private Theme currentTheme = Theme.CLASSIC;
    private Hex hoveredHex = null;
    
    // Animation state
    private boolean isAnimating = false;
    private long animationStartTime = 0;
    private static final long ANIMATION_DURATION = 1000; // 1 second
    private List<AnimatedMarble> animatedMarbles = new ArrayList<>();
    
    // Board center coordinates
    private float centerX;
    private float centerY;
    
    // Touch handling
    public interface BoardTouchListener {
        void onHexTouched(Hex position);
        void onAnimationComplete();
    }
    
    /**
     * Represents a marble being animated from one position to another
     */
    private static class AnimatedMarble {
        public final Hex fromPosition;
        public final Hex toPosition;
        public final Player player;
        public float currentX;
        public float currentY;
        
        public AnimatedMarble(Hex from, Hex to, Player player) {
            this.fromPosition = from;
            this.toPosition = to;
            this.player = player;
        }
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
        hexValidMovePaint.setStrokeWidth(4f);
        
        hexValidMoveFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        hexValidMoveFillPaint.setStyle(Paint.Style.FILL);
        
        arrowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        arrowPaint.setStyle(Paint.Style.STROKE);
        arrowPaint.setStrokeWidth(6f);
        arrowPaint.setStrokeCap(Paint.Cap.ROUND);
        
        previewMarblePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        previewMarblePaint.setStyle(Paint.Style.FILL);
        
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
    
    /**
     * Check if currently animating
     */
    public boolean isAnimating() {
        return isAnimating;
    }
    
    /**
     * Start animating a move
     */
    public void animateMove(List<Hex> selectedMarbles, Hex targetPosition) {
        if (isAnimating || game == null) {
            return;
        }
        
        // Determine move direction
        Integer direction = findMoveDirection(selectedMarbles, targetPosition);
        if (direction == null) {
            return;
        }
        
        // Setup animation data
        animatedMarbles.clear();
        for (Hex marble : selectedMarbles) {
            Hex newPos = marble.neighbor(direction);
            if (game.isValidPosition(newPos)) {
                Player player = game.getPlayerAt(marble);
                AnimatedMarble animMarble = new AnimatedMarble(marble, newPos, player);
                
                // Set initial position
                float[] fromPixel = marble.toPixel(centerX, centerY, HEX_SIZE);
                animMarble.currentX = fromPixel[0];
                animMarble.currentY = fromPixel[1];
                
                animatedMarbles.add(animMarble);
            }
        }
        
        // Start animation
        isAnimating = true;
        animationStartTime = System.currentTimeMillis();
        invalidate();
    }
    
    private void updateThemeColors() {
        hexPaint.setColor(currentTheme.getBoardStartColor());
        hexBorderPaint.setColor(currentTheme.getBoardBorderColor());
        hexSelectedPaint.setColor(Color.YELLOW);
        hexValidMovePaint.setColor(currentTheme.getHighlightColor());
        
        // Enhanced valid move visualization
        hexValidMoveFillPaint.setColor(Color.argb(60, 102, 187, 106)); // Semi-transparent green
        arrowPaint.setColor(Color.argb(200, 255, 193, 7)); // Golden arrows
        previewMarblePaint.setColor(Color.argb(120, 255, 255, 255)); // Semi-transparent white
        
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
        
        // Update animation if running
        if (isAnimating) {
            updateAnimation();
        }
        
        // Draw background
        canvas.drawColor(currentTheme.getBackgroundColor());
        
        // Draw hexagons
        for (Hex position : game.getAllPositions()) {
            drawHexagon(canvas, position);
        }
        
        // Draw movement preview arrows (only if not animating)
        if (!isAnimating) {
            drawMovementPreview(canvas);
        }
        
        // Draw marbles (skip animated ones during animation)
        for (Hex position : game.getAllPositions()) {
            Player player = game.getPlayerAt(position);
            if (player.isPlayer() && !isMarbleBeingAnimated(position)) {
                drawMarble(canvas, position, player);
            }
        }
        
        // Draw animated marbles
        if (isAnimating) {
            drawAnimatedMarbles(canvas);
        }
        
        // Draw preview marbles for valid moves
        drawPreviewMarbles(canvas);
        
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
        
        // Draw enhanced valid move highlight
        if (isValidMove) {
            // Fill the hexagon with semi-transparent color
            canvas.drawPath(hexPath, hexValidMoveFillPaint);
            
            // Draw stronger border
            Paint validPaint = new Paint(hexValidMovePaint);
            validPaint.setColor(Color.argb(200, 102, 187, 106)); // Stronger green
            validPaint.setStrokeWidth(5f);
            canvas.drawPath(hexPath, validPaint);
            
            // Draw target indicator (circle in center)
            Paint targetPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            targetPaint.setColor(Color.argb(180, 255, 255, 255));
            targetPaint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(x, y, HEX_SIZE * 0.2f, targetPaint);
            
            // Draw target border
            targetPaint.setStyle(Paint.Style.STROKE);
            targetPaint.setStrokeWidth(3f);
            targetPaint.setColor(currentTheme.getHighlightColor());
            canvas.drawCircle(x, y, HEX_SIZE * 0.2f, targetPaint);
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
        String scoreText = String.format(java.util.Locale.getDefault(), "Black: %d/6  White: %d/6", 
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
    
    /**
     * Update animation state and positions
     */
    private void updateAnimation() {
        long currentTime = System.currentTimeMillis();
        long elapsed = currentTime - animationStartTime;
        float progress = Math.min(1.0f, (float) elapsed / ANIMATION_DURATION);
        
        // Use easing function for smoother animation
        progress = easeInOutCubic(progress);
        
        // Update positions of all animated marbles
        for (AnimatedMarble marble : animatedMarbles) {
            float[] fromPixel = marble.fromPosition.toPixel(centerX, centerY, HEX_SIZE);
            float[] toPixel = marble.toPosition.toPixel(centerX, centerY, HEX_SIZE);
            
            marble.currentX = fromPixel[0] + (toPixel[0] - fromPixel[0]) * progress;
            marble.currentY = fromPixel[1] + (toPixel[1] - fromPixel[1]) * progress;
        }
        
        // Check if animation is complete
        if (progress >= 1.0f) {
            finishAnimation();
        } else {
            // Continue animation
            invalidate();
        }
    }
    
    /**
     * Easing function for smooth animation
     */
    private float easeInOutCubic(float t) {
        return t < 0.5f ? 4 * t * t * t : 1 - (float) Math.pow(-2 * t + 2, 3) / 2;
    }
    
    /**
     * Finish the animation and notify the game
     */
    private void finishAnimation() {
        isAnimating = false;
        animatedMarbles.clear();
        
        // Notify that animation is complete
        if (touchListener != null) {
            touchListener.onAnimationComplete();
        }
        
        invalidate();
    }
    
    /**
     * Check if a marble at given position is currently being animated
     */
    private boolean isMarbleBeingAnimated(Hex position) {
        if (!isAnimating) {
            return false;
        }
        
        for (AnimatedMarble marble : animatedMarbles) {
            if (marble.fromPosition.equals(position)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Draw all currently animated marbles
     */
    private void drawAnimatedMarbles(Canvas canvas) {
        for (AnimatedMarble marble : animatedMarbles) {
            drawMarbleAtPosition(canvas, marble.currentX, marble.currentY, marble.player, false);
        }
    }
    
    /**
     * Draw a marble at specific pixel coordinates
     */
    private void drawMarbleAtPosition(Canvas canvas, float x, float y, Player player, boolean selected) {
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
        
        // Draw selection glow if selected
        if (selected) {
            Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            glowPaint.setColor(Color.argb(150, 255, 193, 7));
            for (int i = 0; i < 5; i++) {
                canvas.drawCircle(x, y, MARBLE_RADIUS + i * 2, glowPaint);
                glowPaint.setAlpha(glowPaint.getAlpha() - 30);
            }
        }
    }
    
    /**
     * Draw movement preview arrows from selected marbles to valid targets
     */
    private void drawMovementPreview(Canvas canvas) {
        if (game == null || game.getSelectedMarbles().isEmpty() || game.getValidMoves().isEmpty()) {
            return;
        }
        
        List<Hex> selectedMarbles = game.getSelectedMarbles();
        Set<Hex> validMoves = game.getValidMoves();
        
        // For each valid move, determine which marbles would move and draw arrows
        for (Hex target : validMoves) {
            Integer direction = findMoveDirection(selectedMarbles, target);
            if (direction != null) {
                drawMovementArrows(canvas, selectedMarbles, direction);
            }
        }
    }
    
    /**
     * Find the movement direction for a target
     */
    private Integer findMoveDirection(List<Hex> selectedMarbles, Hex target) {
        if (selectedMarbles.size() == 1) {
            Hex marble = selectedMarbles.get(0);
            for (int dir = 0; dir < 6; dir++) {
                if (marble.neighbor(dir).equals(target)) {
                    return dir;
                }
            }
        } else {
            // For multiple marbles, check all directions
            for (int dir = 0; dir < 6; dir++) {
                // Check if this could be the target for this direction
                if (isValidDirectionForTarget(selectedMarbles, target, dir)) {
                    return dir;
                }
            }
        }
        return null;
    }
    
    /**
     * Check if a direction could lead to the target
     */
    private boolean isValidDirectionForTarget(List<Hex> selectedMarbles, Hex target, int direction) {
        // Simple check: see if any marble moved in this direction reaches the target
        for (Hex marble : selectedMarbles) {
            if (marble.neighbor(direction).equals(target)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Draw arrows showing marble movement
     */
    private void drawMovementArrows(Canvas canvas, List<Hex> selectedMarbles, int direction) {
        for (Hex marble : selectedMarbles) {
            float[] fromPos = marble.toPixel(centerX, centerY, HEX_SIZE);
            Hex newPos = marble.neighbor(direction);
            float[] toPos = newPos.toPixel(centerX, centerY, HEX_SIZE);
            
            // Draw dashed line
            drawDashedArrow(canvas, fromPos[0], fromPos[1], toPos[0], toPos[1]);
        }
    }
    
    /**
     * Draw a dashed arrow between two points
     */
    private void drawDashedArrow(Canvas canvas, float startX, float startY, float endX, float endY) {
        // Calculate arrow properties
        float dx = endX - startX;
        float dy = endY - startY;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        
        if (length < 10) return; // Too short to draw
        
        // Normalize direction
        dx /= length;
        dy /= length;
        
        // Shorten the arrow to not overlap with marbles
        float shortenBy = MARBLE_RADIUS + 5;
        startX += dx * shortenBy;
        startY += dy * shortenBy;
        endX -= dx * shortenBy;
        endY -= dy * shortenBy;
        
        // Draw dashed line
        Paint dashPaint = new Paint(arrowPaint);
        dashPaint.setPathEffect(new DashPathEffect(new float[]{10, 5}, 0));
        canvas.drawLine(startX, startY, endX, endY, dashPaint);
        
        // Draw arrowhead
        float arrowLength = 15f;
        float arrowAngle = (float) Math.PI / 6; // 30 degrees
        
        float arrowX1 = endX - arrowLength * (float) Math.cos(Math.atan2(dy, dx) - arrowAngle);
        float arrowY1 = endY - arrowLength * (float) Math.sin(Math.atan2(dy, dx) - arrowAngle);
        float arrowX2 = endX - arrowLength * (float) Math.cos(Math.atan2(dy, dx) + arrowAngle);
        float arrowY2 = endY - arrowLength * (float) Math.sin(Math.atan2(dy, dx) + arrowAngle);
        
        canvas.drawLine(endX, endY, arrowX1, arrowY1, arrowPaint);
        canvas.drawLine(endX, endY, arrowX2, arrowY2, arrowPaint);
    }
    
    /**
     * Draw preview marbles at target positions
     */
    private void drawPreviewMarbles(Canvas canvas) {
        if (game == null || game.getSelectedMarbles().isEmpty() || hoveredHex == null) {
            return;
        }
        
        // Only draw preview if hovering over a valid move
        if (!game.getValidMoves().contains(hoveredHex)) {
            return;
        }
        
        List<Hex> selectedMarbles = game.getSelectedMarbles();
        Integer direction = findMoveDirection(selectedMarbles, hoveredHex);
        
        if (direction != null) {
            // Draw semi-transparent marbles at new positions
            for (Hex marble : selectedMarbles) {
                Hex newPos = marble.neighbor(direction);
                if (game.isValidPosition(newPos)) {
                    drawPreviewMarble(canvas, newPos, game.getCurrentPlayer());
                }
            }
        }
    }
    
    /**
     * Draw a semi-transparent preview marble
     */
    private void drawPreviewMarble(Canvas canvas, Hex position, Player player) {
        float[] center = position.toPixel(centerX, centerY, HEX_SIZE);
        float x = center[0];
        float y = center[1];
        
        // Draw preview marble with transparency
        Paint previewPaint = new Paint(previewMarblePaint);
        if (player == Player.BLACK) {
            previewPaint.setColor(Color.argb(100, 30, 30, 35));
        } else {
            previewPaint.setColor(Color.argb(100, 240, 240, 245));
        }
        
        canvas.drawCircle(x, y, MARBLE_RADIUS * 0.8f, previewPaint);
        
        // Draw preview border
        Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(2f);
        borderPaint.setColor(Color.argb(150, 255, 193, 7));
        canvas.drawCircle(x, y, MARBLE_RADIUS * 0.8f, borderPaint);
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Block touch events during animation
        if (isAnimating) {
            return true;
        }
        
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
                
                performClick();
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
    
    @Override
    public boolean performClick() {
        return super.performClick();
    }
    
    /**
     * Get optimal view size - increased for larger board
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int size = (int) (HEX_SIZE * 24 + BOARD_PADDING * 2); // Increased from 20 to 24
        setMeasuredDimension(size, size);
    }
}