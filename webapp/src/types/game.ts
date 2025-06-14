export enum Player {
  EMPTY = 0,
  BLACK = 1,
  WHITE = 2
}

export enum GameState {
  SETUP = 'SETUP',
  BLACK_TURN = 'BLACK_TURN',
  WHITE_TURN = 'WHITE_TURN',
  BLACK_WIN = 'BLACK_WIN',
  WHITE_WIN = 'WHITE_WIN',
  DRAW = 'DRAW'
}

export enum AIDifficulty {
  EASY = 'EASY',
  MEDIUM = 'MEDIUM',
  HARD = 'HARD'
}

export enum Theme {
  CLASSIC = 'CLASSIC',
  DARK = 'DARK',
  OCEAN = 'OCEAN',
  FOREST = 'FOREST'
}

export interface Hex {
  q: number;
  r: number;
}

// export interface Move {
//   marbles: Hex[];
//   direction: number;
// }

export interface GameConfig {
  theme: Theme;
  aiDifficulty?: AIDifficulty;
  isVsAI: boolean;
  isOnline: boolean;
}