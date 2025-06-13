package io.celox.hexpulse.game;

import android.os.AsyncTask;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * AI opponent for Abalone game with different difficulty levels
 */
public class AbaloneAI {
    private AIDifficulty difficulty;
    private Map<String, Move> moveCache;
    private ExecutorService executor;
    
    /**
     * Represents a move (selected marbles + target position)
     */
    public static class Move {
        public final List<Hex> selectedMarbles;
        public final Hex target;
        
        public Move(List<Hex> selectedMarbles, Hex target) {
            this.selectedMarbles = new ArrayList<>(selectedMarbles);
            this.target = target;
        }
    }
    
    /**
     * Interface for AI move completion callback
     */
    public interface MoveCallback {
        void onMoveCalculated(Move move);
        void onMoveError(String error);
    }
    
    public AbaloneAI(AIDifficulty difficulty) {
        this.difficulty = difficulty;
        this.moveCache = new HashMap<>();
        this.executor = Executors.newSingleThreadExecutor();
    }
    
    /**
     * Get best move asynchronously
     */
    public void getBestMoveAsync(AbaloneGame game, Player player, MoveCallback callback) {
        executor.submit(() -> {
            try {
                Move move = getBestMove(game, player);
                callback.onMoveCalculated(move);
            } catch (Exception e) {
                callback.onMoveError("AI calculation error: " + e.getMessage());
            }
        });
    }
    
    /**
     * Get best move synchronously (for testing/debugging)
     */
    public Move getBestMove(AbaloneGame game, Player player) {
        // Create cache key
        String cacheKey = createCacheKey(game, player);
        if (moveCache.containsKey(cacheKey)) {
            return moveCache.get(cacheKey);
        }
        
        List<Move> allMoves = generateAllMoves(game, player);
        
        if (allMoves.isEmpty()) {
            return null;
        }
        
        Move bestMove;
        
        if (difficulty == AIDifficulty.EASY) {
            // Easy: 50% random, 50% quick evaluation
            if (Math.random() < 0.5) {
                bestMove = allMoves.get(new Random().nextInt(allMoves.size()));
            } else {
                bestMove = quickEvaluateMoves(game, allMoves, player);
            }
        } else {
            // Medium/Hard: Use minimax
            bestMove = minimaxEvaluate(game, allMoves, player);
        }
        
        // Cache result
        if (bestMove != null && moveCache.size() < 50) { // Limit cache size
            moveCache.put(cacheKey, bestMove);
        }
        
        return bestMove;
    }
    
    /**
     * Generate all possible moves for a player
     */
    private List<Move> generateAllMoves(AbaloneGame game, Player player) {
        List<Move> moves = new ArrayList<>();
        List<Hex> playerMarbles = new ArrayList<>();
        
        // Find all player marbles
        for (Hex pos : game.getAllPositions()) {
            if (game.getPlayerAt(pos) == player) {
                playerMarbles.add(pos);
            }
        }
        
        // Single marble moves
        for (Hex marble : playerMarbles) {
            List<Hex> selected = Arrays.asList(marble);
            AbaloneGame gameCopy = new AbaloneGame(game);
            gameCopy.clearSelection();
            gameCopy.selectMarble(marble);
            
            for (Hex target : gameCopy.getValidMoves()) {
                moves.add(new Move(selected, target));
            }
        }
        
        // Two marble combinations (limited for performance)
        int maxCombos = difficulty.getMaxMovesToEvaluate();
        for (int i = 0; i < Math.min(playerMarbles.size(), maxCombos); i++) {
            for (int j = i + 1; j < Math.min(playerMarbles.size(), maxCombos); j++) {
                List<Hex> combo = Arrays.asList(playerMarbles.get(i), playerMarbles.get(j));
                
                AbaloneGame gameCopy = new AbaloneGame(game);
                gameCopy.clearSelection();
                for (Hex marble : combo) {
                    gameCopy.selectMarble(marble);
                }
                
                if (!gameCopy.getValidMoves().isEmpty()) {
                    for (Hex target : gameCopy.getValidMoves()) {
                        moves.add(new Move(combo, target));
                    }
                }
            }
        }
        
        // Three marble combinations (only for HARD difficulty)
        if (difficulty == AIDifficulty.HARD) {
            int limit = Math.min(playerMarbles.size(), 6);
            for (int i = 0; i < limit; i++) {
                for (int j = i + 1; j < limit; j++) {
                    for (int k = j + 1; k < limit; k++) {
                        List<Hex> combo = Arrays.asList(
                            playerMarbles.get(i), 
                            playerMarbles.get(j), 
                            playerMarbles.get(k)
                        );
                        
                        AbaloneGame gameCopy = new AbaloneGame(game);
                        gameCopy.clearSelection();
                        for (Hex marble : combo) {
                            gameCopy.selectMarble(marble);
                        }
                        
                        if (!gameCopy.getValidMoves().isEmpty()) {
                            for (Hex target : gameCopy.getValidMoves()) {
                                moves.add(new Move(combo, target));
                            }
                        }
                    }
                }
            }
        }
        
        return moves;
    }
    
    /**
     * Quick evaluation for easy difficulty
     */
    private Move quickEvaluateMoves(AbaloneGame game, List<Move> moves, Player player) {
        Move bestMove = moves.get(0);
        int bestScore = Integer.MIN_VALUE;
        
        for (Move move : moves.subList(0, Math.min(moves.size(), 10))) {
            int score = quickMoveScore(game, move, player);
            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
            }
        }
        
        return bestMove;
    }
    
    /**
     * Quick scoring of a move
     */
    private int quickMoveScore(AbaloneGame game, Move move, Player player) {
        int score = 0;
        
        // Prefer center positions
        int centerDistance = Math.abs(move.target.q) + Math.abs(move.target.r) + Math.abs(-move.target.q - move.target.r);
        score -= centerDistance * 2;
        
        // Bonus for attacking moves
        if (game.getPlayerAt(move.target) != Player.EMPTY) {
            score += 50;
        }
        
        // Bonus for moves closer to opponent marbles
        Player opponent = player.getOpponent();
        int minDistanceToOpponent = Integer.MAX_VALUE;
        for (Hex pos : game.getAllPositions()) {
            if (game.getPlayerAt(pos) == opponent) {
                int distance = move.target.distance(pos);
                minDistanceToOpponent = Math.min(minDistanceToOpponent, distance);
            }
        }
        score -= minDistanceToOpponent;
        
        return score;
    }
    
    /**
     * Minimax evaluation for medium/hard difficulty
     */
    private Move minimaxEvaluate(AbaloneGame game, List<Move> moves, Player player) {
        Move bestMove = null;
        int bestScore = Integer.MIN_VALUE;
        
        // Sort moves for better pruning
        moves.sort((a, b) -> Integer.compare(
            quickMoveScore(game, b, player),
            quickMoveScore(game, a, player)
        ));
        
        // Limit evaluated moves for performance
        int maxMoves = difficulty.getMaxMovesToEvaluate();
        List<Move> limitedMoves = moves.subList(0, Math.min(moves.size(), maxMoves));
        
        for (Move move : limitedMoves) {
            AbaloneGame gameCopy = new AbaloneGame(game);
            if (executeMove(gameCopy, move, player)) {
                int score = minimax(gameCopy, difficulty.getSearchDepth() - 1, 
                                 Integer.MIN_VALUE, Integer.MAX_VALUE, false, player);
                
                if (score > bestScore) {
                    bestScore = score;
                    bestMove = move;
                }
            }
        }
        
        return bestMove != null ? bestMove : moves.get(0);
    }
    
    /**
     * Minimax algorithm with alpha-beta pruning
     */
    private int minimax(AbaloneGame game, int depth, int alpha, int beta, 
                       boolean maximizingPlayer, Player aiPlayer) {
        // Terminal conditions
        Player winner = game.checkWinner();
        if (winner == aiPlayer) {
            return 1000 + depth; // Prefer faster wins
        } else if (winner != null) {
            return -1000 - depth; // Avoid fast losses
        } else if (depth == 0) {
            return evaluatePosition(game, aiPlayer);
        }
        
        Player currentPlayer = maximizingPlayer ? aiPlayer : aiPlayer.getOpponent();
        List<Move> moves = generateAllMoves(game, currentPlayer);
        
        if (moves.isEmpty()) {
            return evaluatePosition(game, aiPlayer);
        }
        
        if (maximizingPlayer) {
            int maxEval = Integer.MIN_VALUE;
            for (Move move : moves) {
                AbaloneGame gameCopy = new AbaloneGame(game);
                if (executeMove(gameCopy, move, currentPlayer)) {
                    int eval = minimax(gameCopy, depth - 1, alpha, beta, false, aiPlayer);
                    maxEval = Math.max(maxEval, eval);
                    alpha = Math.max(alpha, eval);
                    if (beta <= alpha) {
                        break; // Alpha-beta pruning
                    }
                }
            }
            return maxEval;
        } else {
            int minEval = Integer.MAX_VALUE;
            for (Move move : moves) {
                AbaloneGame gameCopy = new AbaloneGame(game);
                if (executeMove(gameCopy, move, currentPlayer)) {
                    int eval = minimax(gameCopy, depth - 1, alpha, beta, true, aiPlayer);
                    minEval = Math.min(minEval, eval);
                    beta = Math.min(beta, eval);
                    if (beta <= alpha) {
                        break; // Alpha-beta pruning
                    }
                }
            }
            return minEval;
        }
    }
    
    /**
     * Evaluate board position
     */
    private int evaluatePosition(AbaloneGame game, Player aiPlayer) {
        Player opponent = aiPlayer.getOpponent();
        int score = 0;
        
        // Score difference (most important)
        Map<Player, Integer> scores = game.getScores();
        score += (scores.get(aiPlayer) - scores.get(opponent)) * 1000;
        
        // Center control
        List<Hex> centerPositions = Arrays.asList(
            new Hex(0, 0), new Hex(1, 0), new Hex(-1, 0),
            new Hex(0, 1), new Hex(0, -1), new Hex(1, -1), new Hex(-1, 1)
        );
        
        int aiCenter = 0, oppCenter = 0;
        for (Hex pos : centerPositions) {
            Player player = game.getPlayerAt(pos);
            if (player == aiPlayer) aiCenter++;
            else if (player == opponent) oppCenter++;
        }
        score += (aiCenter - oppCenter) * 50;
        
        // Marble count
        int aiCount = 0, oppCount = 0;
        for (Hex pos : game.getAllPositions()) {
            Player player = game.getPlayerAt(pos);
            if (player == aiPlayer) aiCount++;
            else if (player == opponent) oppCount++;
        }
        score += (aiCount - oppCount) * 20;
        
        // Cohesion (only for HARD difficulty)
        if (difficulty == AIDifficulty.HARD) {
            score += (calculateCohesion(game, aiPlayer) - calculateCohesion(game, opponent)) * 5;
        }
        
        return score;
    }
    
    /**
     * Calculate marble cohesion (how close marbles are to each other)
     */
    private int calculateCohesion(AbaloneGame game, Player player) {
        int cohesion = 0;
        List<Hex> playerMarbles = new ArrayList<>();
        
        for (Hex pos : game.getAllPositions()) {
            if (game.getPlayerAt(pos) == player) {
                playerMarbles.add(pos);
            }
        }
        
        for (Hex marble : playerMarbles.subList(0, Math.min(playerMarbles.size(), 8))) {
            for (int dir = 0; dir < 6; dir++) {
                Hex neighbor = marble.neighbor(dir);
                if (game.getPlayerAt(neighbor) == player) {
                    cohesion++;
                }
            }
        }
        
        return cohesion;
    }
    
    /**
     * Execute a move on game copy
     */
    private boolean executeMove(AbaloneGame game, Move move, Player player) {
        game.clearSelection();
        
        // Select marbles
        for (Hex marble : move.selectedMarbles) {
            if (!game.selectMarble(marble)) {
                return false;
            }
        }
        
        // Make move
        return game.makeMove(move.target);
    }
    
    /**
     * Create cache key for game state
     */
    private String createCacheKey(AbaloneGame game, Player player) {
        StringBuilder sb = new StringBuilder();
        sb.append(player.getSymbol());
        sb.append(difficulty.getLevel());
        
        // Simple board hash (positions only, not full state)
        List<Hex> sortedPositions = new ArrayList<>(game.getAllPositions());
        sortedPositions.sort((a, b) -> {
            if (a.q != b.q) return Integer.compare(a.q, b.q);
            return Integer.compare(a.r, b.r);
        });
        
        for (Hex pos : sortedPositions) {
            sb.append(game.getPlayerAt(pos).getSymbol());
        }
        
        return sb.toString();
    }
    
    /**
     * Clean up resources
     */
    public void shutdown() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
            }
        }
    }
}