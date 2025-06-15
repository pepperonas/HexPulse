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
    
    // Drawing constants - Optimized for larger board that fills screen
    private static final float HEX_SIZE = 75f;  // Larger for better visibility
    private static final float MARBLE_RADIUS = 30f;  // Proportional to hex size
    private static final float BOARD_PADDING = 60f;  // Minimal padding for edge-to-edge
    private static final float SELECTION_RING_WIDTH = 5f;
    private static final float VALID_MOVE_RING_WIDTH = 4f;
    private static final float TOUCH_TOLERANCE = MARBLE_RADIUS * 1.5f;
    
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
        hexSelectedPaint.setStrokeWidth(SELECTION_RING_WIDTH);
        
        hexValidMovePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        hexValidMovePaint.setStyle(Paint.Style.STROKE);
        hexValidMovePaint.setStrokeWidth(VALID_MOVE_RING_WIDTH);
        
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
            
            // Enhanced target indicator with better visibility
            Paint targetPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            targetPaint.setColor(Color.argb(200, 255, 255, 255));
            targetPaint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(x, y, MARBLE_RADIUS * 0.6f, targetPaint);
            
            // Draw target border with improved styling
            targetPaint.setStyle(Paint.Style.STROKE);
            targetPaint.setStrokeWidth(VALID_MOVE_RING_WIDTH);
            targetPaint.setColor(Color.argb(220, 102, 187, 106));
            canvas.drawCircle(x, y, MARBLE_RADIUS * 0.6f, targetPaint);
            
            // Add a small inner dot for precise targeting
            Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            dotPaint.setColor(Color.argb(255, 76, 175, 80));
            dotPaint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(x, y, MARBLE_RADIUS * 0.2f, dotPaint);
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
        boolean isValidMoveTarget = game.getValidMoves().contains(position);
        
        // Use the enhanced marble drawing method
        drawMarbleAtPosition(canvas, x, y, player, isSelected);
        
        // Draw valid move highlight OVER the marble for push moves
        if (isValidMoveTarget) {
            // SUPER OBVIOUS GREEN HIGHLIGHT for debugging
            Paint validMovePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            validMovePaint.setColor(Color.argb(255, 0, 255, 0)); // BRIGHT GREEN, fully opaque
            validMovePaint.setStyle(Paint.Style.STROKE);
            validMovePaint.setStrokeWidth(12f); // Very thick
            canvas.drawCircle(x, y, MARBLE_RADIUS + 8, validMovePaint);
            
            // Additional outer ring 
            Paint outerRingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            outerRingPaint.setColor(Color.argb(255, 255, 255, 0)); // BRIGHT YELLOW
            outerRingPaint.setStyle(Paint.Style.STROKE);
            outerRingPaint.setStrokeWidth(8f);
            canvas.drawCircle(x, y, MARBLE_RADIUS + 16, outerRingPaint);
            
            // Add text label for debugging
            Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            textPaint.setColor(Color.RED);
            textPaint.setTextSize(24f);
            textPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("VALID", x, y - MARBLE_RADIUS - 20, textPaint);
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
     * Draw a marble at specific pixel coordinates with enhanced visual quality
     */
    private void drawMarbleAtPosition(Canvas canvas, float x, float y, Player player, boolean selected) {
        // Draw marble shadow with better precision
        Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        shadowPaint.setColor(Color.argb(120, 0, 0, 0));
        canvas.drawCircle(x + 3, y + 3, MARBLE_RADIUS + 1, shadowPaint);
        
        // Enhanced marble colors for better contrast
        Paint marblePaint;
        int lightColor, darkColor, borderColor;
        
        if (player == Player.BLACK) {
            lightColor = Color.rgb(80, 80, 90);
            darkColor = Color.rgb(15, 15, 20);
            borderColor = Color.rgb(60, 60, 70);
            marblePaint = blackMarblePaint;
        } else {
            lightColor = Color.rgb(255, 255, 255);
            darkColor = Color.rgb(180, 180, 190);
            borderColor = Color.rgb(150, 150, 160);
            marblePaint = whiteMarblePaint;
        }
        
        // Create enhanced radial gradient for better 3D effect
        RadialGradient marbleGradient = new RadialGradient(
            x - MARBLE_RADIUS * 0.3f, y - MARBLE_RADIUS * 0.3f, MARBLE_RADIUS * 1.2f,
            lightColor, darkColor, Shader.TileMode.CLAMP
        );
        
        Paint gradientMarblePaint = new Paint(marblePaint);
        gradientMarblePaint.setShader(marbleGradient);
        
        // Draw marble border for better definition
        Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setColor(borderColor);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(2f);
        canvas.drawCircle(x, y, MARBLE_RADIUS, borderPaint);
        
        // Draw marble
        canvas.drawCircle(x, y, MARBLE_RADIUS - 1, gradientMarblePaint);
        
        // Enhanced highlight for better 3D effect
        Paint highlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        highlightPaint.setColor(Color.argb(200, 255, 255, 255));
        canvas.drawCircle(x - MARBLE_RADIUS * 0.35f, y - MARBLE_RADIUS * 0.35f, 
                         MARBLE_RADIUS * 0.25f, highlightPaint);
        
        // Improved selection visualization
        if (selected) {
            // Primary selection ring
            Paint selectionPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            selectionPaint.setColor(Color.argb(200, 255, 193, 7));
            selectionPaint.setStyle(Paint.Style.STROKE);
            selectionPaint.setStrokeWidth(SELECTION_RING_WIDTH);
            canvas.drawCircle(x, y, MARBLE_RADIUS + 6, selectionPaint);
            
            // Secondary glow effect
            Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            glowPaint.setColor(Color.argb(100, 255, 193, 7));
            glowPaint.setStyle(Paint.Style.STROKE);
            glowPaint.setStrokeWidth(2f);
            canvas.drawCircle(x, y, MARBLE_RADIUS + 10, glowPaint);
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
        if (isAnimating || game == null) {
            return true;
        }
        
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                return handleTouchDown(event.getX(), event.getY());
                
            case MotionEvent.ACTION_MOVE:
                return handleTouchMove(event.getX(), event.getY());
                
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                return handleTouchUp();
                
            default:
                return super.onTouchEvent(event);
        }
    }
    
    /**
     * Handle touch down - start marble selection process
     */
    private boolean handleTouchDown(float touchX, float touchY) {
        Hex touchedHex = findHexAtPosition(touchX, touchY);
        
        if (touchedHex != null && game.isValidPosition(touchedHex)) {
            hoveredHex = touchedHex;
            
            // Process the marble selection like in native Android apps
            processMarbleSelection(touchedHex);
            
            performClick();
            invalidate();
            return true;
        }
        
        return false;
    }
    
    /**
     * Handle touch move - update hover state
     */
    private boolean handleTouchMove(float touchX, float touchY) {
        Hex newHoveredHex = findHexAtPosition(touchX, touchY);
        
        if (newHoveredHex != null && !newHoveredHex.equals(hoveredHex)) {
            hoveredHex = newHoveredHex;
            invalidate();
            return true;
        }
        
        return false;
    }
    
    /**
     * Handle touch up - clear hover state
     */
    private boolean handleTouchUp() {
        hoveredHex = null;
        invalidate();
        return true;
    }
    
    /**
     * Process marble selection with native Android app behavior
     */
    private void processMarbleSelection(Hex touchedHex) {
        Player currentPlayer = game.getCurrentPlayer();
        Player marbleAtPosition = game.getPlayerAt(touchedHex);
        
        // Case 1: Clicked on own marble - toggle selection
        if (marbleAtPosition == currentPlayer) {
            handleOwnMarbleSelection(touchedHex);
        }
        // Case 2: Clicked on empty space or valid move target
        else if (marbleAtPosition == Player.EMPTY) {
            handleEmptySpaceSelection(touchedHex);
        }
        // Case 3: Clicked on opponent marble - clear selection
        else {
            handleOpponentMarbleSelection(touchedHex);
        }
    }
    
    /**
     * Handle selection of own marble - native Android behavior
     */
    private void handleOwnMarbleSelection(Hex marblePosition) {
        List<Hex> currentSelection = game.getSelectedMarbles();
        
        if (currentSelection.contains(marblePosition)) {
            // If marble is already selected, deselect it
            game.clearSelection();
            game.selectMarble(marblePosition);
            
            // If this was the only selected marble, clear all
            if (currentSelection.size() == 1) {
                game.clearSelection();
            }
        } else {
            // Check if this marble can be added to current selection
            if (canAddToSelection(marblePosition, currentSelection)) {
                game.selectMarble(marblePosition);
            } else {
                // Start new selection with this marble
                game.clearSelection();
                game.selectMarble(marblePosition);
            }
        }
    }
    
    /**
     * Handle selection of empty space - try to make a move
     */
    private void handleEmptySpaceSelection(Hex emptyPosition) {
        List<Hex> selectedMarbles = game.getSelectedMarbles();
        
        if (!selectedMarbles.isEmpty() && game.getValidMoves().contains(emptyPosition)) {
            // Execute move - notify the touch listener
            if (touchListener != null) {
                touchListener.onHexTouched(emptyPosition);
            }
        } else {
            // Clear selection if clicking on invalid move target
            game.clearSelection();
        }
    }
    
    /**
     * Handle selection of opponent marble - clear selection
     */
    private void handleOpponentMarbleSelection(Hex opponentPosition) {
        // CRITICAL FIX: Check if this opponent marble is a valid move target BEFORE clearing selection!
        android.util.Log.d("HexagonalBoardView", "=== OPPONENT MARBLE CLICKED ===");
        android.util.Log.d("HexagonalBoardView", "Opponent position: " + opponentPosition);
        android.util.Log.d("HexagonalBoardView", "Current selection: " + game.getSelectedMarbles());
        android.util.Log.d("HexagonalBoardView", "Valid moves: " + game.getValidMoves());
        android.util.Log.d("HexagonalBoardView", "Is valid move: " + game.getValidMoves().contains(opponentPosition));
        
        // Check if clicking on this opponent marble is a valid move (Sumito!)
        if (game.getValidMoves().contains(opponentPosition)) {
            android.util.Log.d("HexagonalBoardView", "SUMITO MOVE DETECTED! Executing...");
            
            // Execute the move via TouchListener (same as empty space logic)
            if (touchListener != null) {
                touchListener.onHexTouched(opponentPosition);
            }
        } else {
            android.util.Log.d("HexagonalBoardView", "Not a valid move, clearing selection");
            game.clearSelection();
        }
        android.util.Log.d("HexagonalBoardView", "=== END OPPONENT MARBLE CLICKED ===");
    }
    
    /**
     * Check if a marble can be added to the current selection (adjacency rules)
     */
    private boolean canAddToSelection(Hex newMarble, List<Hex> currentSelection) {
        if (currentSelection.isEmpty()) {
            return true;
        }
        
        // Check if the new marble is adjacent to any selected marble
        for (Hex selectedMarble : currentSelection) {
            if (areAdjacent(newMarble, selectedMarble)) {
                return true;
            }
        }
        
        // Check if all marbles (including new one) form a line
        List<Hex> testSelection = new ArrayList<>(currentSelection);
        testSelection.add(newMarble);
        
        return formsStraightLine(testSelection);
    }
    
    /**
     * Check if two hexes are adjacent
     */
    private boolean areAdjacent(Hex hex1, Hex hex2) {
        for (int direction = 0; direction < 6; direction++) {
            if (hex1.neighbor(direction).equals(hex2)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Check if selected marbles form a straight line (max 3 marbles)
     */
    private boolean formsStraightLine(List<Hex> marbles) {
        if (marbles.size() <= 1) {
            return true;
        }
        
        if (marbles.size() > 3) {
            return false; // Abalone rule: max 3 marbles
        }
        
        if (marbles.size() == 2) {
            return areAdjacent(marbles.get(0), marbles.get(1));
        }
        
        // For 3 marbles, check if they form a straight line
        Hex first = marbles.get(0);
        Hex second = marbles.get(1);
        Hex third = marbles.get(2);
        
        // Find direction from first to second
        Integer direction = null;
        for (int dir = 0; dir < 6; dir++) {
            if (first.neighbor(dir).equals(second)) {
                direction = dir;
                break;
            }
        }
        
        if (direction == null) {
            return false; // First two are not adjacent
        }
        
        // Check if third marble is in the same direction
        return second.neighbor(direction).equals(third) || first.neighbor((direction + 3) % 6).equals(third);
    }
    
    /**
     * Find hex at touch position with precise detection
     */
    private Hex findHexAtPosition(float touchX, float touchY) {
        // Convert touch coordinates to hex using improved algorithm
        Hex candidateHex = Hex.fromPixel(touchX, touchY, centerX, centerY, HEX_SIZE);
        
        if (game.isValidPosition(candidateHex)) {
            float[] hexCenter = candidateHex.toPixel(centerX, centerY, HEX_SIZE);
            float distance = (float) Math.sqrt(
                Math.pow(touchX - hexCenter[0], 2) + Math.pow(touchY - hexCenter[1], 2)
            );
            
            // Accept if within touch tolerance
            if (distance <= TOUCH_TOLERANCE) {
                return candidateHex;
            }
        }
        
        // Search nearby valid hexes if direct conversion failed
        return findNearestValidHex(touchX, touchY);
    }
    
    /**
     * Find the nearest valid hex within touch tolerance
     */
    private Hex findNearestValidHex(float touchX, float touchY) {
        float minDistance = Float.MAX_VALUE;
        Hex nearestHex = null;
        
        // Check all valid board positions
        for (int q = -4; q <= 4; q++) {
            for (int r = Math.max(-4, -q - 4); r <= Math.min(4, -q + 4); r++) {
                Hex hex = new Hex(q, r);
                
                if (game.isValidPosition(hex)) {
                    float[] hexCenter = hex.toPixel(centerX, centerY, HEX_SIZE);
                    float distance = (float) Math.sqrt(
                        Math.pow(touchX - hexCenter[0], 2) + Math.pow(touchY - hexCenter[1], 2)
                    );
                    
                    if (distance <= TOUCH_TOLERANCE && distance < minDistance) {
                        minDistance = distance;
                        nearestHex = hex;
                    }
                }
            }
        }
        
        return nearestHex;
    }
    
    @Override
    public boolean performClick() {
        return super.performClick();
    }
    
    /**
     * Get optimal view size - fills available space with minimal padding
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Get the available space
        int availableWidth = MeasureSpec.getSize(widthMeasureSpec);
        int availableHeight = MeasureSpec.getSize(heightMeasureSpec);
        
        // Calculate optimal board size to fill most of the available space
        int optimalSize = Math.min(availableWidth, availableHeight);
        
        // Ensure minimum size for playability
        int minSize = (int) (HEX_SIZE * 24 + BOARD_PADDING * 2);
        int finalSize = Math.max(optimalSize, minSize);
        
        setMeasuredDimension(finalSize, finalSize);
    }
}