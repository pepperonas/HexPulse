import { Player, GameState, AIDifficulty } from '../types/game';
import { Hex } from './Hex';

export class AbaloneGame {
  private board: Map<string, Player>;
  private selectedMarbles: Hex[];
  private currentPlayer: Player;
  private gameState: GameState;
  private blackScore: number;
  private whiteScore: number;
  private validMoves: Map<string, number>;
  private isVsAI: boolean;
  private aiDifficulty: AIDifficulty;
  private aiPlayer: Player;
  private static readonly WINNING_SCORE = 6;

  constructor(isVsAI: boolean = false, aiDifficulty: AIDifficulty = AIDifficulty.MEDIUM, aiPlayer: Player = Player.WHITE) {
    this.board = new Map();
    this.selectedMarbles = [];
    this.currentPlayer = Player.BLACK;
    this.gameState = GameState.BLACK_TURN;
    this.blackScore = 0;
    this.whiteScore = 0;
    this.validMoves = new Map();
    this.isVsAI = isVsAI;
    this.aiDifficulty = aiDifficulty;
    this.aiPlayer = aiPlayer;
    this.initializeBoard();
  }

  private initializeBoard(): void {
    // Create all valid board positions
    for (let q = -4; q <= 4; q++) {
      for (let r = -4; r <= 4; r++) {
        const s = -q - r;
        if (Math.abs(s) <= 4) {
          this.board.set(this.hexKey(new Hex(q, r)), Player.EMPTY);
        }
      }
    }

    // Place black marbles (top)
    const blackPositions = [
      // Row 1 (5 marbles)
      new Hex(-4, 0), new Hex(-3, -1), new Hex(-2, -2), new Hex(-1, -3), new Hex(0, -4),
      // Row 2 (6 marbles)
      new Hex(-4, 1), new Hex(-3, 0), new Hex(-2, -1), new Hex(-1, -2), new Hex(0, -3), new Hex(1, -4),
      // Row 3 (3 marbles)
      new Hex(-2, 0), new Hex(-1, -1), new Hex(0, -2)
    ];

    // Place white marbles (bottom)
    const whitePositions = [
      // Row 1 (5 marbles)
      new Hex(4, 0), new Hex(3, 1), new Hex(2, 2), new Hex(1, 3), new Hex(0, 4),
      // Row 2 (6 marbles)
      new Hex(4, -1), new Hex(3, 0), new Hex(2, 1), new Hex(1, 2), new Hex(0, 3), new Hex(-1, 4),
      // Row 3 (3 marbles)
      new Hex(2, 0), new Hex(1, 1), new Hex(0, 2)
    ];

    blackPositions.forEach(pos => this.board.set(this.hexKey(pos), Player.BLACK));
    whitePositions.forEach(pos => this.board.set(this.hexKey(pos), Player.WHITE));
  }

  private hexKey(hex: Hex): string {
    return `${hex.q},${hex.r}`;
  }

  public getBoard(): Map<string, Player> {
    return new Map(this.board);
  }

  public getSelectedMarbles(): Hex[] {
    return [...this.selectedMarbles];
  }

  public getCurrentPlayer(): Player {
    return this.currentPlayer;
  }

  public getGameState(): GameState {
    return this.gameState;
  }

  public getBlackScore(): number {
    return this.blackScore;
  }

  public getWhiteScore(): number {
    return this.whiteScore;
  }

  public getValidMoves(): Map<string, number> {
    return new Map(this.validMoves);
  }

  public isValidPosition(hex: Hex): boolean {
    const s = -hex.q - hex.r;
    return Math.abs(hex.q) <= 4 && Math.abs(hex.r) <= 4 && Math.abs(s) <= 4;
  }

  public getMarbleAt(hex: Hex): Player {
    return this.board.get(this.hexKey(hex)) ?? Player.EMPTY;
  }

  public selectMarble(hex: Hex): boolean {
    const marble = this.getMarbleAt(hex);
    
    // Can only select current player's marbles
    if (marble !== this.currentPlayer) return false;

    // Check if already selected
    const index = this.selectedMarbles.findIndex(m => m.equals(hex));
    if (index >= 0) {
      // Deselect
      this.selectedMarbles.splice(index, 1);
      this.calculateValidMoves();
      return true;
    }

    // Can't select more than 3 marbles
    if (this.selectedMarbles.length >= 3) return false;

    // Add to selection
    this.selectedMarbles.push(hex);

    // Validate selection (must be in line)
    if (this.selectedMarbles.length > 1 && !this.areMarblesInLine()) {
      this.selectedMarbles.pop();
      return false;
    }

    this.calculateValidMoves();
    return true;
  }

  private areMarblesInLine(): boolean {
    if (this.selectedMarbles.length < 2) return true;
    if (this.selectedMarbles.length === 2) return true;

    // For 3 marbles, check if they're in line
    const m1 = this.selectedMarbles[0];
    const m2 = this.selectedMarbles[1];
    const m3 = this.selectedMarbles[2];

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

  private calculateValidMoves(): void {
    this.validMoves.clear();

    if (this.selectedMarbles.length === 0) return;

    if (this.selectedMarbles.length === 1) {
      // Single marble can move to any adjacent empty hex
      const marble = this.selectedMarbles[0];
      for (let dir = 0; dir < 6; dir++) {
        const target = marble.neighbor(dir);
        if (this.isValidPosition(target) && this.getMarbleAt(target) === Player.EMPTY) {
          this.validMoves.set(this.hexKey(target), dir);
        }
      }
    } else if (this.selectedMarbles.length <= 3 && this.areMarblesInLine()) {
      // Multiple marbles in line - check all 6 directions
      for (let dir = 0; dir < 6; dir++) {
        if (this.canMoveInDirection(dir)) {
          const target = this.getTargetForDirection(dir);
          if (target) {
            this.validMoves.set(this.hexKey(target), dir);
          }
        }
      }
    }
  }

  private canMoveInDirection(direction: number): boolean {
    if (this.selectedMarbles.length === 1) {
      const target = this.selectedMarbles[0].neighbor(direction);
      return this.isValidPosition(target) && this.getMarbleAt(target) === Player.EMPTY;
    }

    // For multiple marbles, check inline and broadside moves
    return this.canMoveInline(direction) || this.canMoveBroadside(direction);
  }

  private canMoveInline(direction: number): boolean {
    const lineDir = this.getLineDirection();
    if (lineDir === -1) return false;

    // Inline move if direction is parallel to line
    if (lineDir === direction || lineDir === (direction + 3) % 6) {
      const leadMarble = this.getLeadMarble(direction);
      const target = leadMarble.neighbor(direction);

      if (!this.isValidPosition(target)) return false;

      const targetPlayer = this.getMarbleAt(target);

      // Empty target - simple move
      if (targetPlayer === Player.EMPTY) return true;

      // Own marble - not allowed
      if (targetPlayer === this.currentPlayer) return false;

      // Opponent marble - check if can push (Sumito)
      return this.canPush(direction);
    }

    return false;
  }

  private canMoveBroadside(direction: number): boolean {
    if (this.selectedMarbles.length < 2) return false;

    const lineDir = this.getLineDirection();
    if (lineDir === -1) return false;

    // Broadside move if direction is perpendicular to line
    if (lineDir !== direction && lineDir !== (direction + 3) % 6) {
      // Check if all target positions are empty
      for (const marble of this.selectedMarbles) {
        const target = marble.neighbor(direction);
        if (!this.isValidPosition(target) || this.getMarbleAt(target) !== Player.EMPTY) {
          return false;
        }
      }
      return true;
    }

    return false;
  }

  private getTargetForDirection(direction: number): Hex | null {
    if (this.canMoveInline(direction)) {
      const leadMarble = this.getLeadMarble(direction);
      return leadMarble.neighbor(direction);
    } else if (this.canMoveBroadside(direction)) {
      // For broadside moves, return target of first marble (like Android)
      return this.selectedMarbles[0].neighbor(direction);
    }
    return null;
  }

  // Keep the old canPush method as it's still needed
  private canPush(direction: number): boolean {
    const attackers = this.selectedMarbles.length;
    let defenders = 0;
    let currentHex = this.getLeadMarble(direction).neighbor(direction);
    const opponent = this.currentPlayer === Player.BLACK ? Player.WHITE : Player.BLACK;

    // Count opponent marbles in line
    while (this.isValidPosition(currentHex) && this.getMarbleAt(currentHex) === opponent) {
      defenders++;
      currentHex = currentHex.neighbor(direction);
    }

    // Need numerical superiority and space to push
    if (attackers <= defenders) return false;

    // Check if there's space to push (empty or off-board)
    return !this.isValidPosition(currentHex) || this.getMarbleAt(currentHex) === Player.EMPTY;
  }

  private getLineDirection(): number {
    if (this.selectedMarbles.length < 2) return -1;

    const m1 = this.selectedMarbles[0];
    const m2 = this.selectedMarbles[1];

    for (let dir = 0; dir < 6; dir++) {
      if (m1.neighbor(dir).equals(m2) || m2.neighbor(dir).equals(m1)) {
        return dir;
      }
    }
    return -1;
  }

  private getPerpendicularDirections(lineDir: number): number[] {
    // Perpendicular directions in hexagonal grid
    return [(lineDir + 2) % 6, (lineDir + 4) % 6];
  }

  private getLeadMarble(direction: number): Hex {
    let lead = this.selectedMarbles[0];
    for (const marble of this.selectedMarbles) {
      const diff = marble.subtract(lead);
      const dirVector = new Hex(Hex.DIRECTIONS[direction][0], Hex.DIRECTIONS[direction][1]);
      
      // If marble is further in the direction, it's the new lead
      const dot = diff.q * dirVector.q + diff.r * dirVector.r;
      if (dot > 0) {
        lead = marble;
      }
    }
    return lead;
  }

  public makeMove(targetHex: Hex): boolean {
    const key = this.hexKey(targetHex);
    if (!this.validMoves.has(key)) return false;

    const direction = this.validMoves.get(key)!;
    
    if (this.selectedMarbles.length === 1) {
      // Single marble move
      const marble = this.selectedMarbles[0];
      this.board.set(this.hexKey(marble), Player.EMPTY);
      this.board.set(this.hexKey(targetHex), this.currentPlayer);
    } else {
      // Multiple marble move
      const lineDir = this.getLineDirection();
      const isInline = direction === lineDir || direction === (lineDir + 3) % 6;
      
      if (isInline) {
        this.executeInlineMove(direction);
      } else {
        this.executeBroadsideMove(direction);
      }
    }

    // Clear selection
    this.selectedMarbles = [];
    this.validMoves.clear();

    // Check win condition
    if (this.blackScore >= AbaloneGame.WINNING_SCORE) {
      this.gameState = GameState.BLACK_WIN;
    } else if (this.whiteScore >= AbaloneGame.WINNING_SCORE) {
      this.gameState = GameState.WHITE_WIN;
    } else {
      // Switch turns
      this.currentPlayer = this.currentPlayer === Player.BLACK ? Player.WHITE : Player.BLACK;
      this.gameState = this.currentPlayer === Player.BLACK ? GameState.BLACK_TURN : GameState.WHITE_TURN;
    }

    return true;
  }

  private executeInlineMove(direction: number): void {
    // Sort marbles by position in movement direction
    const sorted = [...this.selectedMarbles].sort((a, b) => {
      const dirVector = new Hex(Hex.DIRECTIONS[direction][0], Hex.DIRECTIONS[direction][1]);
      const diffA = a.q * dirVector.q + a.r * dirVector.r;
      const diffB = b.q * dirVector.q + b.r * dirVector.r;
      return diffB - diffA; // Front to back
    });

    // Check if we're pushing
    const leadMarble = sorted[0];
    const targetHex = leadMarble.neighbor(direction);
    const targetMarble = this.getMarbleAt(targetHex);

    if (targetMarble !== Player.EMPTY && targetMarble !== this.currentPlayer) {
      // Push opponent marbles
      this.pushMarbles(direction);
    }

    // Move own marbles from front to back
    for (const marble of sorted) {
      this.board.set(this.hexKey(marble), Player.EMPTY);
      const newPos = marble.neighbor(direction);
      this.board.set(this.hexKey(newPos), this.currentPlayer);
    }
  }

  private pushMarbles(direction: number): void {
    const opponent = this.currentPlayer === Player.BLACK ? Player.WHITE : Player.BLACK;
    const toMove: Hex[] = [];
    let currentHex = this.getLeadMarble(direction).neighbor(direction);

    // Collect opponent marbles to push
    while (this.isValidPosition(currentHex) && this.getMarbleAt(currentHex) === opponent) {
      toMove.push(currentHex);
      currentHex = currentHex.neighbor(direction);
    }

    // Push from back to front
    for (let i = toMove.length - 1; i >= 0; i--) {
      const marble = toMove[i];
      const newPos = marble.neighbor(direction);
      
      this.board.set(this.hexKey(marble), Player.EMPTY);
      
      if (this.isValidPosition(newPos)) {
        this.board.set(this.hexKey(newPos), opponent);
      } else {
        // Marble pushed off board
        if (this.currentPlayer === Player.BLACK) {
          this.blackScore++;
        } else {
          this.whiteScore++;
        }
      }
    }
  }

  private executeBroadsideMove(direction: number): void {
    // Move all marbles simultaneously
    const newPositions: Hex[] = [];
    
    // Clear old positions
    for (const marble of this.selectedMarbles) {
      this.board.set(this.hexKey(marble), Player.EMPTY);
      newPositions.push(marble.neighbor(direction));
    }
    
    // Set new positions
    for (const pos of newPositions) {
      this.board.set(this.hexKey(pos), this.currentPlayer);
    }
  }

  public isAITurn(): boolean {
    return this.isVsAI && this.currentPlayer === this.aiPlayer;
  }

  public getAIDifficulty(): AIDifficulty {
    return this.aiDifficulty;
  }

  public getAIPlayer(): Player {
    return this.aiPlayer;
  }

  public setAIConfiguration(isVsAI: boolean, difficulty: AIDifficulty, aiPlayer: Player): void {
    this.isVsAI = isVsAI;
    this.aiDifficulty = difficulty;
    this.aiPlayer = aiPlayer;
  }

  public resetGame(): void {
    this.board.clear();
    this.selectedMarbles = [];
    this.currentPlayer = Player.BLACK;
    this.gameState = GameState.BLACK_TURN;
    this.blackScore = 0;
    this.whiteScore = 0;
    this.validMoves.clear();
    this.initializeBoard();
  }
}