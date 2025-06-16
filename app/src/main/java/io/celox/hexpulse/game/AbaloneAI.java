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
        
        // Apply randomness based on difficulty
        if (Math.random() < difficulty.getRandomnessFactor()) {
            // Occasionally make a random move (more for EASY, less for MEDIUM, never for HARD)
            bestMove = allMoves.get(new Random().nextInt(allMoves.size()));
        } else {
            // Use enhanced evaluation based on difficulty
            if (difficulty == AIDifficulty.EASY) {
                bestMove = enhancedQuickEvaluate(game, allMoves, player);
            } else {
                bestMove = minimaxEvaluateWithTimeLimit(game, allMoves, player);
            }
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
        
        // Three marble combinations (for MEDIUM and HARD difficulty)
        if (difficulty == AIDifficulty.MEDIUM || difficulty == AIDifficulty.HARD) {
            int limit = difficulty == AIDifficulty.HARD ? 
                       Math.min(playerMarbles.size(), 8) : 
                       Math.min(playerMarbles.size(), 6);
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
     * Advanced move scoring with strategic considerations
     */
    private int advancedMoveScore(AbaloneGame game, Move move, Player player) {
        int score = quickMoveScore(game, move, player);
        Player opponent = player.getOpponent();
        
        // Simulate the move to evaluate its effects
        AbaloneGame tempGame = new AbaloneGame(game);
        if (executeMove(tempGame, move, player)) {
            // Check if move creates pushing opportunities
            List<Move> followUpMoves = generateAllMoves(tempGame, player);
            for (Move followUp : followUpMoves.subList(0, Math.min(5, followUpMoves.size()))) {
                AbaloneGame testGame = new AbaloneGame(tempGame);
                if (executeMove(testGame, followUp, player)) {
                    Map<Player, Integer> newScores = testGame.getScores();
                    Map<Player, Integer> oldScores = game.getScores();
                    if (newScores.get(player) > oldScores.get(player)) {
                        score += 100; // Bonus for creating push opportunities
                    }
                }
            }
            
            // Penalty for exposing marbles to counterattack
            List<Move> opponentMoves = generateAllMoves(tempGame, opponent);
            for (Move opponentMove : opponentMoves.subList(0, Math.min(8, opponentMoves.size()))) {
                AbaloneGame testGame = new AbaloneGame(tempGame);
                if (executeMove(testGame, opponentMove, opponent)) {
                    Map<Player, Integer> newScores = testGame.getScores();
                    Map<Player, Integer> oldScores = tempGame.getScores();
                    if (newScores.get(opponent) > oldScores.get(opponent)) {
                        score -= 80; // Penalty for vulnerable positions
                    }
                }
            }
        }
        
        return score;
    }
    
    /**
     * Enhanced quick evaluation for Easy difficulty
     */
    private Move enhancedQuickEvaluate(AbaloneGame game, List<Move> moves, Player player) {
        Move bestMove = moves.get(0);
        int bestScore = Integer.MIN_VALUE;
        
        // Evaluate more moves with better scoring
        int movesToEvaluate = Math.min(moves.size(), difficulty.getMaxMovesToEvaluate());
        for (Move move : moves.subList(0, movesToEvaluate)) {
            int score = advancedMoveScore(game, move, player);
            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
            }
        }
        
        return bestMove;
    }
    
    /**
     * Minimax evaluation with time limit and iterative deepening
     */
    private Move minimaxEvaluateWithTimeLimit(AbaloneGame game, List<Move> moves, Player player) {
        long startTime = System.currentTimeMillis();
        long timeLimit = difficulty.getTimeLimit();
        
        Move bestMove = null;
        int bestScore = Integer.MIN_VALUE;
        
        // Enhanced move sorting with multiple criteria
        moves.sort((a, b) -> {
            int scoreA = advancedMoveScore(game, a, player);
            int scoreB = advancedMoveScore(game, b, player);
            return Integer.compare(scoreB, scoreA);
        });
        
        // Limit evaluated moves for performance
        int maxMoves = difficulty.getMaxMovesToEvaluate();
        List<Move> limitedMoves = moves.subList(0, Math.min(moves.size(), maxMoves));
        
        // Iterative deepening for better time management
        int maxDepth = difficulty.getSearchDepth();
        for (int depth = 1; depth <= maxDepth; depth++) {
            if (System.currentTimeMillis() - startTime > timeLimit * 0.8) {
                break; // Reserve 20% time for safety
            }
            
            Move currentBestMove = null;
            int currentBestScore = Integer.MIN_VALUE;
            
            for (Move move : limitedMoves) {
                if (System.currentTimeMillis() - startTime > timeLimit * 0.9) {
                    break; // Time almost up
                }
                
                AbaloneGame gameCopy = new AbaloneGame(game);
                if (executeMove(gameCopy, move, player)) {
                    int score = minimax(gameCopy, depth - 1, 
                                     Integer.MIN_VALUE, Integer.MAX_VALUE, false, player, startTime, timeLimit);
                    
                    if (score > currentBestScore) {
                        currentBestScore = score;
                        currentBestMove = move;
                    }
                }
            }
            
            if (currentBestMove != null) {
                bestMove = currentBestMove;
                bestScore = currentBestScore;
            }
        }
        
        return bestMove != null ? bestMove : moves.get(0);
    }
    
    /**
     * Minimax algorithm with alpha-beta pruning and time management
     */
    private int minimax(AbaloneGame game, int depth, int alpha, int beta, 
                       boolean maximizingPlayer, Player aiPlayer, long startTime, long timeLimit) {
        // Time check to avoid going over limit
        if (System.currentTimeMillis() - startTime > timeLimit * 0.95) {
            return evaluatePosition(game, aiPlayer);
        }
        
        // Terminal conditions
        Player winner = game.checkWinner();
        if (winner == aiPlayer) {
            return 10000 + depth; // Prefer faster wins with higher reward
        } else if (winner != null) {
            return -10000 - depth; // Avoid fast losses with severe penalty
        } else if (depth == 0) {
            return evaluatePosition(game, aiPlayer);
        }
        
        Player currentPlayer = maximizingPlayer ? aiPlayer : aiPlayer.getOpponent();
        List<Move> moves = generateAllMoves(game, currentPlayer);
        
        if (moves.isEmpty()) {
            return evaluatePosition(game, aiPlayer);
        }
        
        // Sort moves for better pruning efficiency
        moves.sort((a, b) -> {
            int scoreA = quickMoveScore(game, a, currentPlayer);
            int scoreB = quickMoveScore(game, b, currentPlayer);
            return maximizingPlayer ? Integer.compare(scoreB, scoreA) : Integer.compare(scoreA, scoreB);
        });
        
        // Limit moves at deeper levels for performance
        int moveLimit = Math.max(8, difficulty.getMaxMovesToEvaluate() - depth * 2);
        List<Move> limitedMoves = moves.subList(0, Math.min(moves.size(), moveLimit));
        
        if (maximizingPlayer) {
            int maxEval = Integer.MIN_VALUE;
            for (Move move : limitedMoves) {
                AbaloneGame gameCopy = new AbaloneGame(game);
                if (executeMove(gameCopy, move, currentPlayer)) {
                    int eval = minimax(gameCopy, depth - 1, alpha, beta, false, aiPlayer, startTime, timeLimit);
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
            for (Move move : limitedMoves) {
                AbaloneGame gameCopy = new AbaloneGame(game);
                if (executeMove(gameCopy, move, currentPlayer)) {
                    int eval = minimax(gameCopy, depth - 1, alpha, beta, true, aiPlayer, startTime, timeLimit);
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
     * Enhanced board position evaluation
     */
    private int evaluatePosition(AbaloneGame game, Player aiPlayer) {
        Player opponent = aiPlayer.getOpponent();
        int score = 0;
        
        // Score difference (most important factor)
        Map<Player, Integer> scores = game.getScores();
        int scoreDiff = scores.get(aiPlayer) - scores.get(opponent);
        score += scoreDiff * 2000; // Doubled importance
        
        // Extended center control - larger center area
        List<Hex> centerPositions = Arrays.asList(
            new Hex(0, 0), new Hex(1, 0), new Hex(-1, 0),
            new Hex(0, 1), new Hex(0, -1), new Hex(1, -1), new Hex(-1, 1),
            new Hex(2, 0), new Hex(-2, 0), new Hex(0, 2), new Hex(0, -2),
            new Hex(1, 1), new Hex(-1, -1), new Hex(2, -1), new Hex(-2, 1)
        );
        
        int aiCenter = 0, oppCenter = 0;
        for (Hex pos : centerPositions) {
            Player player = game.getPlayerAt(pos);
            if (player == aiPlayer) aiCenter++;
            else if (player == opponent) oppCenter++;
        }
        score += (aiCenter - oppCenter) * 60;
        
        // Marble count with exponential scaling
        int aiCount = 0, oppCount = 0;
        for (Hex pos : game.getAllPositions()) {
            Player player = game.getPlayerAt(pos);
            if (player == aiPlayer) aiCount++;
            else if (player == opponent) oppCount++;
        }
        score += (aiCount - oppCount) * 30;
        
        // Advanced features for Medium and Hard
        if (difficulty.useAdvancedEvaluation()) {
            // Formation strength
            score += (calculateFormationStrength(game, aiPlayer) - calculateFormationStrength(game, opponent)) * 15;
            
            // Mobility (number of available moves)
            int aiMobility = generateAllMoves(game, aiPlayer).size();
            int oppMobility = generateAllMoves(game, opponent).size();
            score += (aiMobility - oppMobility) * 8;
            
            // Edge safety (penalty for marbles near the edge)
            score -= calculateEdgeVulnerability(game, aiPlayer) * 25;
            score += calculateEdgeVulnerability(game, opponent) * 25;
            
            // Cohesion (only for HARD difficulty)
            if (difficulty == AIDifficulty.HARD) {
                score += (calculateCohesion(game, aiPlayer) - calculateCohesion(game, opponent)) * 12;
                
                // Advanced tactical patterns
                score += evaluateTacticalPatterns(game, aiPlayer) * 20;
            }
        }
        
        return score;
    }
    
    /**
     * Calculate formation strength - how well marbles support each other
     */
    private int calculateFormationStrength(AbaloneGame game, Player player) {
        int strength = 0;
        List<Hex> playerMarbles = new ArrayList<>();
        
        for (Hex pos : game.getAllPositions()) {
            if (game.getPlayerAt(pos) == player) {
                playerMarbles.add(pos);
            }
        }
        
        for (Hex marble : playerMarbles) {
            int neighbors = 0;
            for (int dir = 0; dir < 6; dir++) {
                Hex neighbor = marble.neighbor(dir);
                if (game.getPlayerAt(neighbor) == player) {
                    neighbors++;
                }
            }
            // Reward marbles with 2-3 neighbors (good formation)
            if (neighbors >= 2) {
                strength += neighbors * 2;
            }
        }
        
        return strength;
    }
    
    /**
     * Calculate vulnerability to being pushed off the edge
     */
    private int calculateEdgeVulnerability(AbaloneGame game, Player player) {
        int vulnerability = 0;
        
        for (Hex pos : game.getAllPositions()) {
            if (game.getPlayerAt(pos) == player) {
                // Calculate distance to nearest edge
                int edgeDistance = Math.min(Math.min(
                    4 + pos.q + pos.r,    // Distance to one edge
                    4 - pos.q),           // Distance to opposite edge
                    4 - pos.r);           // Distance to third edge
                
                if (edgeDistance <= 1) {
                    vulnerability += 3; // High penalty for edge marbles
                } else if (edgeDistance <= 2) {
                    vulnerability += 1; // Lower penalty for near-edge marbles
                }
            }
        }
        
        return vulnerability;
    }
    
    /**
     * Evaluate tactical patterns and threats
     */
    private int evaluateTacticalPatterns(AbaloneGame game, Player player) {
        int score = 0;
        Player opponent = player.getOpponent();
        
        // Look for potential sumito situations
        List<Move> moves = generateAllMoves(game, player);
        for (Move move : moves.subList(0, Math.min(10, moves.size()))) {
            AbaloneGame tempGame = new AbaloneGame(game);
            if (executeMove(tempGame, move, player)) {
                Map<Player, Integer> newScores = tempGame.getScores();
                Map<Player, Integer> oldScores = game.getScores();
                if (newScores.get(player) > oldScores.get(player)) {
                    score += 5; // Reward moves that create immediate threats
                }
            }
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