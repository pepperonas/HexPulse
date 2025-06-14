import React, { useState } from 'react';
import { Theme, AIDifficulty } from '../types/game';
import { getThemeColors } from '../utils/theme';
import '../styles/HomeScreen.css';

interface HomeScreenProps {
  theme: Theme;
  onPlayVsPlayer: () => void;
  onPlayVsAI: (difficulty: AIDifficulty) => void;
  onPlayOnline: () => void;
}

export const HomeScreen: React.FC<HomeScreenProps> = ({ 
  theme, 
  onPlayVsPlayer, 
  onPlayVsAI, 
  onPlayOnline 
}) => {
  const colors = getThemeColors(theme);
  const [showAIDifficulty, setShowAIDifficulty] = useState(false);

  return (
    <div className="home-screen" style={{ backgroundColor: colors.backgroundColor }}>
      <div className="home-content">
        <h1 className="game-logo" style={{ color: colors.textPrimary }}>
          HexPulse
        </h1>
        
        <p className="game-subtitle" style={{ color: colors.textSecondary }}>
          A strategic marble game
        </p>
        
        <div className="menu-buttons">
          <button 
            className="menu-button"
            onClick={onPlayVsPlayer}
            style={{
              background: `linear-gradient(135deg, ${colors.buttonStartColor}, ${colors.buttonEndColor})`,
              color: colors.textPrimary
            }}
          >
            <span className="button-icon">👥</span>
            <span className="button-text">Player vs Player</span>
          </button>
          
          {!showAIDifficulty ? (
            <button 
              className="menu-button"
              onClick={() => setShowAIDifficulty(true)}
              style={{
                background: `linear-gradient(135deg, ${colors.buttonStartColor}, ${colors.buttonEndColor})`,
                color: colors.textPrimary
              }}
            >
              <span className="button-icon">🤖</span>
              <span className="button-text">Player vs AI</span>
            </button>
          ) : (
            <div className="ai-difficulty-selection">
              <div className="difficulty-header" style={{ color: colors.textPrimary }}>
                Choose AI Difficulty
              </div>
              <div className="difficulty-buttons">
                <button 
                  className="difficulty-button"
                  onClick={() => onPlayVsAI(AIDifficulty.EASY)}
                  style={{
                    background: `linear-gradient(135deg, #4CAF50, #45a049)`,
                    color: 'white'
                  }}
                >
                  <span className="difficulty-icon">😊</span>
                  <span className="difficulty-text">Easy</span>
                </button>
                <button 
                  className="difficulty-button"
                  onClick={() => onPlayVsAI(AIDifficulty.MEDIUM)}
                  style={{
                    background: `linear-gradient(135deg, #FF9800, #F57C00)`,
                    color: 'white'
                  }}
                >
                  <span className="difficulty-icon">🤔</span>
                  <span className="difficulty-text">Medium</span>
                </button>
                <button 
                  className="difficulty-button"
                  onClick={() => onPlayVsAI(AIDifficulty.HARD)}
                  style={{
                    background: `linear-gradient(135deg, #F44336, #D32F2F)`,
                    color: 'white'
                  }}
                >
                  <span className="difficulty-icon">😤</span>
                  <span className="difficulty-text">Hard</span>
                </button>
              </div>
              <button 
                className="back-button"
                onClick={() => setShowAIDifficulty(false)}
                style={{
                  background: `linear-gradient(135deg, ${colors.buttonStartColor}, ${colors.buttonEndColor})`,
                  color: colors.textPrimary
                }}
              >
                ← Back
              </button>
            </div>
          )}
          
          <button 
            className="menu-button"
            onClick={onPlayOnline}
            style={{
              background: `linear-gradient(135deg, ${colors.buttonStartColor}, ${colors.buttonEndColor})`,
              color: colors.textPrimary
            }}
          >
            <span className="button-icon">🌐</span>
            <span className="button-text">Play Online</span>
          </button>
        </div>
        
        <div className="home-footer" style={{ color: colors.textSecondary }}>
          <p>Push 6 marbles off the board to win!</p>
        </div>
      </div>
    </div>
  );
};