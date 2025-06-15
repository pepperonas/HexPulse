package io.celox.hexpulse.game;

import java.util.*;

import java.util.Stack;

/**
 * Core game logic for Abalone
 */
public class AbaloneGame {
    private Map<Hex, Player> board;
    private Player currentPlayer;
    private Map<Player, Integer> scores;
    private List<Hex> selectedMarbles;
    private Set<Hex> validMoves;
    private static final int WINNING_SCORE = 6;
    private static final int MAX_UNDO_HISTORY = 10; // Maximum number of moves to keep for undo
    
    // Multi-Undo system
    private Stack<GameSnapshot> undoHistory;
    private int currentMoveNumber;
    
    // Debug functionality
    private Map<Hex, Player> preMoveBoardState;
    private List<Hex> debugSelectedMarbles;
    private Hex debugTargetPosition;
    private Player debugCurrentPlayer;
    
    public AbaloneGame() {
        board = new HashMap<>();
        scores = new HashMap<>();
        selectedMarbles = new ArrayList<>();
        validMoves = new HashSet<>();
        undoHistory = new Stack<>();
        currentMoveNumber = 0;
        preMoveBoardState = null;
        debugSelectedMarbles = new ArrayList<>();
        debugTargetPosition = null;
        debugCurrentPlayer = null;
        initializeGame();
    }
    
    /**
     * Initialize a new game
     */
    private void initializeGame() {
        currentPlayer = Player.BLACK;
        scores.put(Player.BLACK, 0);
        scores.put(Player.WHITE, 0);
        selectedMarbles.clear();
        validMoves.clear();
        createBoard();
        setupInitialPosition();
    }
    
    /**
     * Create the hexagonal board
     */
    private void createBoard() {
        board.clear();
        for (int q = -4; q <= 4; q++) {
            for (int r = -4; r <= 4; r++) {
                int s = -q - r;
                if (s >= -4 && s <= 4) {
                    board.put(new Hex(q, r), Player.EMPTY);
                }
            }
        }
    }
    
    /**
     * Setup initial marble positions
     */
    private void setupInitialPosition() {
        // Black marbles (top)
        List<Hex> blackPositions = Arrays.asList(
            new Hex(-4, 0), new Hex(-3, -1), new Hex(-2, -2), new Hex(-1, -3), new Hex(0, -4),
            new Hex(-4, 1), new Hex(-3, 0), new Hex(-2, -1), new Hex(-1, -2), new Hex(0, -3), new Hex(1, -4),
            new Hex(-2, 0), new Hex(-1, -1), new Hex(0, -2)
        );
        
        // White marbles (bottom)
        List<Hex> whitePositions = Arrays.asList(
            new Hex(4, 0), new Hex(3, 1), new Hex(2, 2), new Hex(1, 3), new Hex(0, 4),
            new Hex(4, -1), new Hex(3, 0), new Hex(2, 1), new Hex(1, 2), new Hex(0, 3), new Hex(-1, 4),
            new Hex(2, 0), new Hex(1, 1), new Hex(0, 2)
        );
        
        for (Hex pos : blackPositions) {
            board.put(pos, Player.BLACK);
        }
        
        for (Hex pos : whitePositions) {
            board.put(pos, Player.WHITE);
        }
    }
    
    /**
     * Get player at position
     */
    public Player getPlayerAt(Hex position) {
        return board.getOrDefault(position, Player.EMPTY);
    }
    
    /**
     * Check if position is valid on board
     */
    public boolean isValidPosition(Hex position) {
        return board.containsKey(position);
    }
    
    /**
     * Get current player
     */
    public Player getCurrentPlayer() {
        return currentPlayer;
    }
    
    /**
     * Get scores
     */
    public Map<Player, Integer> getScores() {
        return new HashMap<>(scores);
    }
    
    /**
     * Get selected marbles
     */
    public List<Hex> getSelectedMarbles() {
        return new ArrayList<>(selectedMarbles);
    }
    
    /**
     * Get valid moves for current selection
     */
    public Set<Hex> getValidMoves() {
        return new HashSet<>(validMoves);
    }
    
    /**
     * Check if the current selection is valid (forms a straight line and has valid moves)
     */
    public boolean isCurrentSelectionValid() {
        if (selectedMarbles.isEmpty()) {
            return false;
        }
        if (selectedMarbles.size() == 1) {
            return true; // Single marble is always valid
        }
        return areMarblesStraightLine(selectedMarbles);
    }
    
    /**
     * Select or deselect a marble
     * UPDATED: Allow selection of any marbles (even invalid combinations), but only show valid moves for legal selections
     */
    public boolean selectMarble(Hex position) {
        android.util.Log.d("AbaloneGame", "=== selectMarble DEBUG START ===");
        android.util.Log.d("AbaloneGame", "selectMarble - position: " + position);
        android.util.Log.d("AbaloneGame", "selectMarble - currentPlayer: " + currentPlayer);
        android.util.Log.d("AbaloneGame", "selectMarble - selectedMarbles BEFORE: " + selectedMarbles);
        android.util.Log.d("AbaloneGame", "selectMarble - selectedMarbles.size() BEFORE: " + selectedMarbles.size());
        
        if (!isValidPosition(position)) {
            android.util.Log.d("AbaloneGame", "selectMarble - REJECTED: Invalid position");
            return false;
        }
        
        Player marble = board.get(position);
        android.util.Log.d("AbaloneGame", "selectMarble - marble at position: " + marble);
        
        // Can only select current player's marbles
        if (marble == currentPlayer) {
            if (selectedMarbles.contains(position)) {
                // Deselect marble
                android.util.Log.d("AbaloneGame", "selectMarble - DESELECTING marble at: " + position);
                selectedMarbles.remove(position);
                android.util.Log.d("AbaloneGame", "selectMarble - selectedMarbles AFTER deselection: " + selectedMarbles);
            } else {
                // Try to select marble - now allow any combination up to 3 marbles
                android.util.Log.d("AbaloneGame", "selectMarble - ATTEMPTING to select marble at: " + position);
                
                // Check maximum of 3 marbles
                if (selectedMarbles.size() >= 3) {
                    android.util.Log.d("AbaloneGame", "selectMarble - REJECTED: Too many marbles (already have 3)");
                    return false; // Cannot select more than 3 marbles
                }
                
                android.util.Log.d("AbaloneGame", "selectMarble - ADDING marble to selection: " + position);
                selectedMarbles.add(position);
                android.util.Log.d("AbaloneGame", "selectMarble - selectedMarbles AFTER addition: " + selectedMarbles);
            }
            updateValidMoves();
            android.util.Log.d("AbaloneGame", "selectMarble - validMoves updated, count: " + validMoves.size());
            android.util.Log.d("AbaloneGame", "selectMarble - FINAL selectedMarbles: " + selectedMarbles);
            android.util.Log.d("AbaloneGame", "=== selectMarble DEBUG END - SUCCESS ===");
            return true;
        }
        
        android.util.Log.d("AbaloneGame", "selectMarble - REJECTED: Not current player's marble");
        android.util.Log.d("AbaloneGame", "=== selectMarble DEBUG END - FAILURE ===");
        return false;
    }
    
    /**
     * Check if marbles form a straight line (helper method for selection validation)
     */
    private boolean areMarblesStraightLine(List<Hex> marbles) {
        android.util.Log.d("AbaloneGame", "=== areMarblesStraightLine DEBUG START ===");
        android.util.Log.d("AbaloneGame", "areMarblesStraightLine - marbles: " + marbles);
        android.util.Log.d("AbaloneGame", "areMarblesStraightLine - marbles.size(): " + marbles.size());
        
        if (marbles.size() <= 1) {
            android.util.Log.d("AbaloneGame", "areMarblesStraightLine - RESULT: true (size <= 1)");
            return true;
        }
        
        // Use new MoveValidator to check if marbles form a valid column
        MoveValidator validator = new MoveValidator(board, currentPlayer);
        List<MoveValidator.ValidatedMove> moves = validator.getValidMoves(marbles);
        android.util.Log.d("AbaloneGame", "areMarblesStraightLine - validator found " + moves.size() + " valid moves");
        
        boolean result = !moves.isEmpty(); // If there are valid moves, marbles form a line
        android.util.Log.d("AbaloneGame", "areMarblesStraightLine - RESULT: " + result);
        android.util.Log.d("AbaloneGame", "=== areMarblesStraightLine DEBUG END ===");
        return result;
    }
    
    
    /**
     * Clear selection
     */
    public void clearSelection() {
        selectedMarbles.clear();
        validMoves.clear();
    }
    
    /**
     * Attempt to make a move
     * UPDATED: Only allow moves if the current selection is valid
     */
    public boolean makeMove(Hex targetPosition) {
        // First check if current selection is valid
        if (!isCurrentSelectionValid()) {
            android.util.Log.d("AbaloneGame", "makeMove - REJECTED: Current selection is invalid");
            return false;
        }
        
        if (!validMoves.contains(targetPosition)) {
            android.util.Log.d("AbaloneGame", "makeMove - REJECTED: Target not in valid moves");
            return false;
        }
        
        if (selectedMarbles.isEmpty()) {
            android.util.Log.d("AbaloneGame", "makeMove - REJECTED: No marbles selected");
            return false;
        }
        
        // Save current state before making the move
        saveGameState();
        
        // Save debug information before move
        saveDebugPreMoveState(targetPosition);
        
        // Find and execute the validated move using new logic
        MoveValidator validator = new MoveValidator(board, currentPlayer);
        List<MoveValidator.ValidatedMove> validatedMoves = validator.getValidMoves(selectedMarbles);
        
        // Find the move that matches the target position
        MoveValidator.ValidatedMove selectedMove = null;
        for (MoveValidator.ValidatedMove move : validatedMoves) {
            if (move.targetPosition.equals(targetPosition)) {
                selectedMove = move;
                break;
            }
        }
        
        if (selectedMove == null) {
            android.util.Log.d("AbaloneGame", "makeMove - REJECTED: No matching validated move found");
            return false;
        }
        
        // Execute the validated move
        MoveExecutor executor = new MoveExecutor(board, scores);
        executor.executeMove(selectedMove, currentPlayer);
        
        // Increment move number and switch players
        currentMoveNumber++;
        currentPlayer = currentPlayer.getOpponent();
        clearSelection();
        
        android.util.Log.d("AbaloneGame", "makeMove - SUCCESS: Move executed");
        return true;
    }
    
    /**
     * Update valid moves for current selection using new rule-compliant logic
     * UPDATED: Only show valid moves if the selection itself is valid (forms a straight line)
     */
    private void updateValidMoves() {
        validMoves.clear();
        
        if (selectedMarbles.isEmpty()) {
            return;
        }
        
        // Check if the current selection is valid (forms a straight line)
        if (selectedMarbles.size() > 1 && !areMarblesStraightLine(selectedMarbles)) {
            android.util.Log.d("AbaloneGame", "updateValidMoves - Selection is invalid (not straight line), no valid moves");
            return; // Don't show any valid moves for invalid selections
        }
        
        // Use new MoveValidator for rule-compliant validation
        MoveValidator validator = new MoveValidator(board, currentPlayer);
        List<MoveValidator.ValidatedMove> validatedMoves = validator.getValidMoves(selectedMarbles);
        
        // Convert ValidatedMoves to target positions for UI
        for (MoveValidator.ValidatedMove move : validatedMoves) {
            validMoves.add(move.targetPosition);
        }
        
        android.util.Log.d("AbaloneGame", "updateValidMoves - Selection is valid, found " + validMoves.size() + " valid moves");
    }
    
    
    
    /**
     * Check for winner
     */
    public Player checkWinner() {
        if (scores.getOrDefault(Player.BLACK, 0) >= WINNING_SCORE) {
            return Player.BLACK;
        }
        if (scores.getOrDefault(Player.WHITE, 0) >= WINNING_SCORE) {
            return Player.WHITE;
        }
        return null;
    }
    
    /**
     * Save current game state to undo history
     */
    private void saveGameState() {
        GameSnapshot snapshot = new GameSnapshot(board, currentPlayer, scores, currentMoveNumber);
        undoHistory.push(snapshot);
        
        // Limit undo history size
        while (undoHistory.size() > MAX_UNDO_HISTORY) {
            undoHistory.removeElementAt(0); // Remove oldest snapshot
        }
    }
    
    /**
     * Undo the last move
     */
    public boolean undoLastMove() {
        if (undoHistory.isEmpty()) {
            return false; // No move to undo
        }
        
        // Restore previous state
        GameSnapshot snapshot = undoHistory.pop();
        board = snapshot.getBoard();
        currentPlayer = snapshot.getCurrentPlayer();
        scores = snapshot.getScores();
        currentMoveNumber = snapshot.getMoveNumber();
        
        // Clear selection and valid moves
        clearSelection();
        
        return true;
    }
    
    /**
     * Undo multiple moves
     */
    public boolean undoMoves(int count) {
        if (count <= 0 || count > undoHistory.size()) {
            return false;
        }
        
        for (int i = 0; i < count; i++) {
            if (!undoLastMove()) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Check if undo is available
     */
    public boolean canUndo() {
        return !undoHistory.isEmpty();
    }
    
    /**
     * Get number of moves that can be undone
     */
    public int getUndoCount() {
        return undoHistory.size();
    }
    
    /**
     * Get current move number
     */
    public int getCurrentMoveNumber() {
        return currentMoveNumber;
    }
    
    /**
     * Save debug information before making a move
     */
    private void saveDebugPreMoveState(Hex targetPosition) {
        preMoveBoardState = new HashMap<>(board);
        debugSelectedMarbles = new ArrayList<>(selectedMarbles);
        debugTargetPosition = targetPosition;
        debugCurrentPlayer = currentPlayer;
    }
    
    /**
     * Get debug information about the last move
     */
    public DebugMoveInfo getLastMoveDebugInfo() {
        if (preMoveBoardState == null) {
            return null;
        }
        
        return new DebugMoveInfo(
            new HashMap<>(preMoveBoardState),
            new HashMap<>(board),
            new ArrayList<>(debugSelectedMarbles),
            debugTargetPosition,
            debugCurrentPlayer
        );
    }
    
    /**
     * Clear debug information
     */
    public void clearDebugInfo() {
        preMoveBoardState = null;
        debugSelectedMarbles.clear();
        debugTargetPosition = null;
        debugCurrentPlayer = null;
    }
    
    /**
     * Check if debug information is available
     */
    public boolean hasDebugInfo() {
        return preMoveBoardState != null;
    }
    
    /**
     * Reset the game
     */
    public void resetGame() {
        undoHistory.clear();
        currentMoveNumber = 0;
        clearDebugInfo();
        initializeGame();
    }
    
    /**
     * Get all board positions
     */
    public Set<Hex> getAllPositions() {
        return new HashSet<>(board.keySet());
    }
    
    /**
     * Copy constructor for AI
     */
    public AbaloneGame(AbaloneGame other) {
        this.board = new HashMap<>(other.board);
        this.currentPlayer = other.currentPlayer;
        this.scores = new HashMap<>(other.scores);
        this.selectedMarbles = new ArrayList<>(other.selectedMarbles);
        this.validMoves = new HashSet<>(other.validMoves);
        this.undoHistory = new Stack<>(); // Don't copy undo state for AI copies
        this.currentMoveNumber = other.currentMoveNumber;
        this.preMoveBoardState = null; // Don't copy debug state for AI copies
        this.debugSelectedMarbles = new ArrayList<>();
        this.debugTargetPosition = null;
        this.debugCurrentPlayer = null;
    }
}