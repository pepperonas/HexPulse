import React, { useState, useCallback, useEffect } from 'react';
import { HexagonalBoardView } from './HexagonalBoardView';
import { AbaloneGame } from '../game/AbaloneGame';
import { AbaloneAI } from '../game/AbaloneAI';
import { Theme, GameState } from '../types/game';
import { getThemeColors } from '../utils/theme';
import '../styles/GameScreen.css';

interface GameScreenProps {
  game: AbaloneGame;
  theme: Theme;
  onBack: () => void;
  onThemeChange: (theme: Theme) => void;
}

export const GameScreen: React.FC<GameScreenProps> = ({ game, theme, onBack, onThemeChange }) => {
  const [, forceUpdate] = useState({});
  const [isAIThinking, setIsAIThinking] = useState(false);
  const [aiInstance] = useState(() => new AbaloneAI());
  const colors = getThemeColors(theme);

  const makeAIMove = useCallback(async () => {
    if (isAIThinking) return;
    
    setIsAIThinking(true);
    
    try {
      // Add small delay for better UX
      await new Promise(resolve => setTimeout(resolve, 500));
      
      const aiMove = await aiInstance.getBestMove(
        game, 
        game.getAIPlayer(), 
        game.getAIDifficulty()
      );
      
      if (aiMove) {
        // Clear any existing selection
        while (game.getSelectedMarbles().length > 0) {
          const selected = game.getSelectedMarbles()[0];
          game.selectMarble(selected);
        }
        
        // Select AI marbles
        for (const marble of aiMove.selectedMarbles) {
          game.selectMarble(marble);
        }
        
        // Make the AI move
        game.makeMove(aiMove.target);
        forceUpdate({});
      }
    } catch (error) {
      console.error('AI move failed:', error);
    } finally {
      setIsAIThinking(false);
    }
  }, [game, aiInstance, isAIThinking]);

  const isGameOver = useCallback(() => {
    const state = game.getGameState();
    return state === GameState.BLACK_WIN || state === GameState.WHITE_WIN;
  }, [game]);

  const handleMove = useCallback(() => {
    forceUpdate({});
    
    // Check if it's AI's turn after the move
    if (game.isAITurn() && !isGameOver()) {
      makeAIMove().catch(console.error);
    }
  }, [game, makeAIMove, isGameOver]);

  // Effect to trigger AI move when it becomes AI's turn
  useEffect(() => {
    if (game.isAITurn() && !isGameOver() && !isAIThinking) {
      const timer = setTimeout(() => {
        makeAIMove().catch(console.error);
      }, 500); // Short delay before AI moves
      
      return () => clearTimeout(timer);
    }
  }, [game, makeAIMove, isAIThinking, isGameOver]);

  const handleNewGame = () => {
    game.resetGame();
    forceUpdate({});
  };

  const getStatusText = () => {
    if (isAIThinking) {
      return "AI is thinking...";
    }
    
    const state = game.getGameState();
    switch (state) {
      case GameState.BLACK_TURN:
        return game.isAITurn() ? "AI's Turn" : "Black's Turn";
      case GameState.WHITE_TURN:
        return game.isAITurn() ? "AI's Turn" : "White's Turn";
      case GameState.BLACK_WIN:
        return "Black Wins!";
      case GameState.WHITE_WIN:
        return "White Wins!";
      default:
        return "";
    }
  };

  return (
    <div className="game-screen" style={{ backgroundColor: colors.backgroundColor }}>
      <div className="game-header">
        <button 
          className="back-button"
          onClick={onBack}
          style={{
            background: `linear-gradient(135deg, ${colors.buttonStartColor}, ${colors.buttonEndColor})`,
            color: colors.textPrimary
          }}
        >
          ← Back
        </button>
        
        <div className="game-title" style={{ color: colors.textPrimary }}>
          HexPulse
        </div>
        
        <div className="theme-selector">
          <select 
            value={theme} 
            onChange={(e) => onThemeChange(e.target.value as Theme)}
            style={{
              backgroundColor: colors.boardStartColor,
              color: colors.textPrimary,
              border: `1px solid ${colors.boardBorderColor}`
            }}
          >
            <option value={Theme.CLASSIC}>Classic</option>
            <option value={Theme.DARK}>Dark</option>
            <option value={Theme.OCEAN}>Ocean</option>
            <option value={Theme.FOREST}>Forest</option>
          </select>
        </div>
      </div>

      <div className="game-info" style={{ color: colors.textPrimary }}>
        <div className="score-display">
          <div className="score-item">
            <div 
              className="marble-indicator"
              style={{ 
                background: `radial-gradient(circle at 30% 30%, #4A4A55, ${colors.marbleBlack}, #0A0A0F)`
              }} 
            />
            <span>Black: {game.getBlackScore()}</span>
          </div>
          
          <div className="status-text" style={{ color: colors.textSecondary }}>
            {getStatusText()}
          </div>
          
          <div className="score-item">
            <div 
              className="marble-indicator"
              style={{ 
                background: `radial-gradient(circle at 30% 30%, #FFFFFF, ${colors.marbleWhite}, #D0D0D5)`
              }} 
            />
            <span>White: {game.getWhiteScore()}</span>
          </div>
        </div>
      </div>

      <div className="board-container">
        <HexagonalBoardView 
          game={game} 
          theme={theme}
          onMove={handleMove}
        />
      </div>

      <div className="game-controls">
        <button 
          className="control-button"
          onClick={handleNewGame}
          style={{
            background: `linear-gradient(135deg, ${colors.buttonStartColor}, ${colors.buttonEndColor})`,
            color: colors.textPrimary
          }}
        >
          New Game
        </button>
        
        {isGameOver() && (
          <div className="game-over-message" style={{ color: colors.highlightColor }}>
            Game Over! {getStatusText()}
          </div>
        )}
      </div>
    </div>
  );
};