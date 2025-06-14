import { Player, AIDifficulty } from '../types/game';
import { Hex } from './Hex';
import { AbaloneGame } from './AbaloneGame';

export interface AIMove {
  selectedMarbles: Hex[];
  target: Hex;
  score?: number;
}

export class AbaloneAI {
  private moveCache: Map<string, AIMove> = new Map();
  private static readonly MAX_CACHE_SIZE = 50;

  private getDifficultySettings(difficulty: AIDifficulty) {
    switch (difficulty) {
      case AIDifficulty.EASY:
        return { depth: 1, maxMoves: 5, useRandom: true };
      case AIDifficulty.MEDIUM:
        return { depth: 2, maxMoves: 10, useRandom: false };
      case AIDifficulty.HARD:
        return { depth: 3, maxMoves: 15, useRandom: false };
      default:
        return { depth: 1, maxMoves: 5, useRandom: true };
    }
  }

  private createCacheKey(game: AbaloneGame, player: Player): string {
    const board = game.getBoard();
    const positions: string[] = [];
    
    board.forEach((p, key) => {
      if (p !== Player.EMPTY) {
        positions.push(`${key}:${p}`);
      }
    });
    
    return `${player}-${positions.sort().join('-')}`;
  }

  private evaluatePosition(game: AbaloneGame, player: Player, difficulty: AIDifficulty): number {
    const opponent = player === Player.BLACK ? Player.WHITE : Player.BLACK;
    let score = 0;

    // Score difference (most important) - weight: 1000
    const scoreDiff = (player === Player.BLACK ? game.getBlackScore() : game.getWhiteScore()) -
                     (opponent === Player.BLACK ? game.getBlackScore() : game.getWhiteScore());
    score += scoreDiff * 1000;

    // Center control - weight: 50
    const centerPositions = [
      new Hex(0, 0), new Hex(1, 0), new Hex(-1, 0),
      new Hex(0, 1), new Hex(0, -1), new Hex(1, -1), new Hex(-1, 1)
    ];
    
    let centerControl = 0;
    centerPositions.forEach(pos => {
      const marble = game.getMarbleAt(pos);
      if (marble === player) centerControl++;
      else if (marble === opponent) centerControl--;
    });
    score += centerControl * 50;

    // Marble count difference - weight: 20
    const board = game.getBoard();
    let playerMarbles = 0, opponentMarbles = 0;
    board.forEach(marble => {
      if (marble === player) playerMarbles++;
      else if (marble === opponent) opponentMarbles++;
    });
    score += (playerMarbles - opponentMarbles) * 20;

    // Cohesion (only for HARD difficulty) - weight: 5
    if (difficulty === AIDifficulty.HARD) {
      const cohesion = this.calculateCohesion(game, player) - this.calculateCohesion(game, opponent);
      score += cohesion * 5;
    }

    return score;
  }

  private calculateCohesion(game: AbaloneGame, player: Player): number {
    const board = game.getBoard();
    let cohesion = 0;

    board.forEach((marble, key) => {
      if (marble === player) {
        const [q, r] = key.split(',').map(Number);
        const hex = new Hex(q, r);
        
        // Count adjacent same-color marbles
        for (let dir = 0; dir < 6; dir++) {
          const neighbor = hex.neighbor(dir);
          if (game.getMarbleAt(neighbor) === player) {
            cohesion++;
          }
        }
      }
    });

    return cohesion;
  }

  private getAllPossibleMoves(game: AbaloneGame, player: Player, maxMoves: number): AIMove[] {
    const moves: AIMove[] = [];
    const board = game.getBoard();
    const gameClone = this.cloneGame(game);
    
    // Get all player marbles
    const playerMarbles: Hex[] = [];
    board.forEach((marble, key) => {
      if (marble === player) {
        const [q, r] = key.split(',').map(Number);
        playerMarbles.push(new Hex(q, r));
      }
    });

    // Single marble moves
    for (const marble of playerMarbles) {
      gameClone.selectMarble(marble);
      const validMoves = gameClone.getValidMoves();
      
      validMoves.forEach((_, targetKey) => {
        const [q, r] = targetKey.split(',').map(Number);
        moves.push({
          selectedMarbles: [marble],
          target: new Hex(q, r)
        });
      });
      
      // Clear selection for next iteration
      while (gameClone.getSelectedMarbles().length > 0) {
        gameClone.selectMarble(marble);
      }
      
      if (moves.length >= maxMoves) break;
    }

    // Two marble combinations (if we haven't reached max moves)
    if (moves.length < maxMoves) {
      for (let i = 0; i < Math.min(playerMarbles.length, 6) && moves.length < maxMoves; i++) {
        for (let j = i + 1; j < Math.min(playerMarbles.length, 6) && moves.length < maxMoves; j++) {
          const marble1 = playerMarbles[i];
          const marble2 = playerMarbles[j];
          
          // Check if marbles are adjacent
          if (marble1.distance(marble2) === 1) {
            gameClone.selectMarble(marble1);
            gameClone.selectMarble(marble2);
            const validMoves = gameClone.getValidMoves();
            
            validMoves.forEach((_, targetKey) => {
              const [q, r] = targetKey.split(',').map(Number);
              moves.push({
                selectedMarbles: [marble1, marble2],
                target: new Hex(q, r)
              });
            });
            
            // Clear selection
            while (gameClone.getSelectedMarbles().length > 0) {
              gameClone.selectMarble(marble1);
            }
          }
        }
      }
    }

    // Three marble combinations (only for HARD difficulty)
    if (moves.length < maxMoves) {
      for (let i = 0; i < Math.min(playerMarbles.length, 6) && moves.length < maxMoves; i++) {
        for (let j = i + 1; j < Math.min(playerMarbles.length, 6) && moves.length < maxMoves; j++) {
          for (let k = j + 1; k < Math.min(playerMarbles.length, 6) && moves.length < maxMoves; k++) {
            const marble1 = playerMarbles[i];
            const marble2 = playerMarbles[j];
            const marble3 = playerMarbles[k];
            
            // Check if marbles form a line
            if (this.areMarblesInLine([marble1, marble2, marble3])) {
              gameClone.selectMarble(marble1);
              gameClone.selectMarble(marble2);
              gameClone.selectMarble(marble3);
              const validMoves = gameClone.getValidMoves();
              
              validMoves.forEach((_, targetKey) => {
                const [q, r] = targetKey.split(',').map(Number);
                moves.push({
                  selectedMarbles: [marble1, marble2, marble3],
                  target: new Hex(q, r)
                });
              });
              
              // Clear selection
              while (gameClone.getSelectedMarbles().length > 0) {
                gameClone.selectMarble(marble1);
              }
            }
          }
        }
      }
    }

    return moves.slice(0, maxMoves);
  }

  private areMarblesInLine(marbles: Hex[]): boolean {
    if (marbles.length < 3) return true;
    
    const m1 = marbles[0];
    const m2 = marbles[1];
    const m3 = marbles[2];

    // Check all 6 possible line directions
    for (let dir = 0; dir < 6; dir++) {
      if ((m1.neighbor(dir).equals(m2) && m2.neighbor(dir).equals(m3)) ||
          (m1.neighbor(dir).equals(m3) && m3.neighbor(dir).equals(m2)) ||
          (m2.neighbor(dir).equals(m1) && m1.neighbor(dir).equals(m3)) ||
          (m2.neighbor(dir).equals(m3) && m3.neighbor(dir).equals(m1)) ||
          (m3.neighbor(dir).equals(m1) && m1.neighbor(dir).equals(m2)) ||
          (m3.neighbor(dir).equals(m2) && m2.neighbor(dir).equals(m1))) {
        return true;
      }
    }
    return false;
  }

  private cloneGame(game: AbaloneGame): AbaloneGame {
    // Create a proper game state clone
    const clone = new AbaloneGame(false); // PvP clone
    clone.resetGame();
    
    // Copy board state
    // We need to set the board state - for now simulate by making moves
    // This is a limitation we'll work around
    
    // For now, return the original game for move simulation
    // In production, we'd need proper state serialization/deserialization
    return game;
  }

  private quickEvaluate(game: AbaloneGame, move: AIMove, player: Player): number {
    let score = 0;
    const opponent = player === Player.BLACK ? Player.WHITE : Player.BLACK;

    // Center preference - penalize distance from center
    const centerDistance = move.target.distance(new Hex(0, 0));
    score -= centerDistance * 2;

    // Attack bonus - check if move captures opponent marble
    const targetMarble = game.getMarbleAt(move.target);
    if (targetMarble === opponent) {
      score += 50;
    }

    // Proximity to opponent marbles
    const board = game.getBoard();
    let minOpponentDistance = 10;
    board.forEach((marble, key) => {
      if (marble === opponent) {
        const [q, r] = key.split(',').map(Number);
        const opponentHex = new Hex(q, r);
        const distance = move.target.distance(opponentHex);
        minOpponentDistance = Math.min(minOpponentDistance, distance);
      }
    });
    score += Math.max(0, 5 - minOpponentDistance) * 3;

    return score;
  }

  private minimax(game: AbaloneGame, depth: number, alpha: number, beta: number, 
                  maximizingPlayer: boolean, aiPlayer: Player, difficulty: AIDifficulty): number {
    
    // Terminal conditions
    if (depth === 0 || game.getGameState().includes('WIN')) {
      return this.evaluatePosition(game, aiPlayer, difficulty);
    }

    const currentPlayer = maximizingPlayer ? aiPlayer : (aiPlayer === Player.BLACK ? Player.WHITE : Player.BLACK);
    const settings = this.getDifficultySettings(difficulty);
    const moves = this.getAllPossibleMoves(game, currentPlayer, settings.maxMoves);

    if (maximizingPlayer) {
      let maxEval = -Infinity;
      for (const move of moves) {
        const gameClone = this.cloneGame(game);
        this.executeMove(gameClone, move);
        const eval_ = this.minimax(gameClone, depth - 1, alpha, beta, false, aiPlayer, difficulty);
        maxEval = Math.max(maxEval, eval_);
        alpha = Math.max(alpha, eval_);
        if (beta <= alpha) break; // Alpha-beta pruning
      }
      return maxEval;
    } else {
      let minEval = Infinity;
      for (const move of moves) {
        const gameClone = this.cloneGame(game);
        this.executeMove(gameClone, move);
        const eval_ = this.minimax(gameClone, depth - 1, alpha, beta, true, aiPlayer, difficulty);
        minEval = Math.min(minEval, eval_);
        beta = Math.min(beta, eval_);
        if (beta <= alpha) break; // Alpha-beta pruning
      }
      return minEval;
    }
  }

  private executeMove(game: AbaloneGame, move: AIMove): void {
    // Clear any existing selection
    while (game.getSelectedMarbles().length > 0) {
      const selected = game.getSelectedMarbles()[0];
      game.selectMarble(selected);
    }

    // Select the marbles for this move
    for (const marble of move.selectedMarbles) {
      game.selectMarble(marble);
    }

    // Make the move
    game.makeMove(move.target);
  }

  public async getBestMove(game: AbaloneGame, player: Player, difficulty: AIDifficulty): Promise<AIMove | null> {
    const settings = this.getDifficultySettings(difficulty);
    const moves = this.getAllPossibleMoves(game, player, settings.maxMoves);

    if (moves.length === 0) {
      return null;
    }

    let bestMove: AIMove;

    if (settings.useRandom && Math.random() < 0.5) {
      // EASY difficulty: 50% random moves
      bestMove = moves[Math.floor(Math.random() * moves.length)];
    } else if (difficulty === AIDifficulty.EASY) {
      // EASY: Quick evaluation with randomness
      let bestScore = -Infinity;
      bestMove = moves[0];

      for (const move of moves) {
        let score = this.quickEvaluate(game, move, player);
        score += (Math.random() - 0.5) * 30; // More randomness for EASY
        
        if (score > bestScore) {
          bestScore = score;
          bestMove = move;
        }
      }
      bestMove.score = bestScore;
    } else {
      // MEDIUM/HARD: Enhanced evaluation with tactical considerations
      let bestScore = -Infinity;
      bestMove = moves[0];

      for (const move of moves) {
        let score = this.enhancedEvaluate(game, move, player, difficulty);
        
        // Add slight randomness for variety
        if (difficulty === AIDifficulty.MEDIUM) {
          score += (Math.random() - 0.5) * 5;
        }
        
        if (score > bestScore) {
          bestScore = score;
          bestMove = move;
        }
      }
      bestMove.score = bestScore;
    }

    return bestMove;
  }

  private enhancedEvaluate(game: AbaloneGame, move: AIMove, player: Player, difficulty: AIDifficulty): number {
    let score = 0;

    // 1. Quick evaluation base score
    score += this.quickEvaluate(game, move, player);

    // 2. Tactical considerations for MEDIUM/HARD
    
    // Formation bonus - prefer keeping marbles together
    score += this.evaluateFormation(game, move, player) * 3;
    
    // Defensive bonus - protect marbles from being pushed off
    score += this.evaluateDefense(game, move, player) * 5;
    
    // Attack bonus - create pushing opportunities
    score += this.evaluateAttack(game, move, player) * 7;
    
    // Center control bonus
    score += this.evaluateCenterControl(move) * 4;
    
    // Edge avoidance - don't get too close to edges
    score += this.evaluateEdgeAvoidance(move) * 2;

    // HARD difficulty: Additional strategic considerations
    if (difficulty === AIDifficulty.HARD) {
      score += this.evaluateAdvancedStrategy(game, move, player) * 6;
    }

    return score;
  }

  private evaluateFormation(game: AbaloneGame, move: AIMove, player: Player): number {
    // Count connections after move
    let connections = 0;
    const target = move.target;
    
    // Check connections at target position
    for (let dir = 0; dir < 6; dir++) {
      const neighbor = target.neighbor(dir);
      if (game.getMarbleAt(neighbor) === player) {
        connections++;
      }
    }
    
    return connections;
  }

  private evaluateDefense(game: AbaloneGame, move: AIMove, player: Player): number {
    let defenseBonus = 0;
    
    // Check if move gets marble away from edge danger
    for (const marble of move.selectedMarbles) {
      const edgeDistance = Math.min(
        4 + marble.q, 4 - marble.q,
        4 + marble.r, 4 - marble.r,
        4 + (-marble.q - marble.r), 4 - (-marble.q - marble.r)
      );
      
      if (edgeDistance <= 1) {
        defenseBonus += 10; // Move away from edge
      }
    }
    
    return defenseBonus;
  }

  private evaluateAttack(game: AbaloneGame, move: AIMove, player: Player): number {
    let attackBonus = 0;
    const opponent = player === Player.BLACK ? Player.WHITE : Player.BLACK;
    
    // Check if move creates pushing opportunities
    const target = move.target;
    for (let dir = 0; dir < 6; dir++) {
      const nextPos = target.neighbor(dir);
      if (game.getMarbleAt(nextPos) === opponent) {
        // Check if we can create numerical superiority
        let ourCount = 1; // The moved marble
        let theirCount = 1; // The opponent marble
        
        // Count in line
        let checkPos = target.neighbor((dir + 3) % 6); // Behind us
        while (game.isValidPosition(checkPos) && game.getMarbleAt(checkPos) === player) {
          ourCount++;
          checkPos = checkPos.neighbor((dir + 3) % 6);
        }
        
        checkPos = nextPos.neighbor(dir); // In front of opponent
        while (game.isValidPosition(checkPos) && game.getMarbleAt(checkPos) === opponent) {
          theirCount++;
          checkPos = checkPos.neighbor(dir);
        }
        
        if (ourCount > theirCount) {
          attackBonus += (ourCount - theirCount) * 15; // Pushing opportunity
        }
      }
    }
    
    return attackBonus;
  }

  private evaluateCenterControl(move: AIMove): number {
    const target = move.target;
    const centerDistance = target.distance(new Hex(0, 0));
    
    // Prefer central positions
    return Math.max(0, 3 - centerDistance) * 2;
  }

  private evaluateEdgeAvoidance(move: AIMove): number {
    const target = move.target;
    const edgeDistance = Math.min(
      4 + target.q, 4 - target.q,
      4 + target.r, 4 - target.r,
      4 + (-target.q - target.r), 4 - (-target.q - target.r)
    );
    
    // Penalty for being too close to edges
    if (edgeDistance === 0) return -20; // On edge
    if (edgeDistance === 1) return -10; // Near edge
    return 0;
  }

  private evaluateAdvancedStrategy(game: AbaloneGame, move: AIMove, player: Player): number {
    let strategyBonus = 0;
    
    // 1. Prefer moves that advance towards opponent
    const opponent = player === Player.BLACK ? Player.WHITE : Player.BLACK;
    const board = game.getBoard();
    
    // Find opponent center of mass
    let opponentQ = 0, opponentR = 0, opponentCount = 0;
    board.forEach((marble, key) => {
      if (marble === opponent) {
        const [q, r] = key.split(',').map(Number);
        opponentQ += q;
        opponentR += r;
        opponentCount++;
      }
    });
    
    if (opponentCount > 0) {
      const opponentCenter = new Hex(
        Math.round(opponentQ / opponentCount),
        Math.round(opponentR / opponentCount)
      );
      
      // Prefer moves that get closer to opponent center
      const oldDistance = move.selectedMarbles[0].distance(opponentCenter);
      const newDistance = move.target.distance(opponentCenter);
      
      if (newDistance < oldDistance) {
        strategyBonus += 5; // Moving towards opponent
      }
    }
    
    // 2. Group coordination bonus
    if (move.selectedMarbles.length > 1) {
      strategyBonus += move.selectedMarbles.length * 2; // Prefer group moves
    }
    
    return strategyBonus;
  }

  public clearCache(): void {
    this.moveCache.clear();
  }
}