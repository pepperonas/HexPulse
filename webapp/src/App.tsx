import React, { useState, useEffect } from 'react';
import { HomeScreen } from './components/HomeScreen';
import { GameScreen } from './components/GameScreen';
import { AbaloneGame } from './game/AbaloneGame';
import { Theme, AIDifficulty, Player } from './types/game';
import './App.css';

type Screen = 'home' | 'game';

function App() {
  const [currentScreen, setCurrentScreen] = useState<Screen>('home');
  const [theme, setTheme] = useState<Theme>(Theme.CLASSIC);
  const [game, setGame] = useState<AbaloneGame>(() => new AbaloneGame());

  // Load theme from localStorage
  useEffect(() => {
    const savedTheme = localStorage.getItem('hexpulse-theme');
    if (savedTheme && Object.values(Theme).includes(savedTheme as Theme)) {
      setTheme(savedTheme as Theme);
    }
  }, []);

  // Save theme to localStorage
  const handleThemeChange = (newTheme: Theme) => {
    setTheme(newTheme);
    localStorage.setItem('hexpulse-theme', newTheme);
  };

  const startPlayerVsPlayer = () => {
    setGame(new AbaloneGame(false));
    setCurrentScreen('game');
  };

  const startPlayerVsAI = (difficulty: AIDifficulty) => {
    // Human is always BLACK (goes first), AI is WHITE
    setGame(new AbaloneGame(true, difficulty, Player.WHITE));
    setCurrentScreen('game');
  };

  const startOnlineGame = () => {
    // TODO: Implement online mode
    alert('Online mode coming soon!');
  };

  const goToHome = () => {
    setCurrentScreen('home');
  };

  return (
    <div className="app">
      {currentScreen === 'home' && (
        <HomeScreen
          theme={theme}
          onPlayVsPlayer={startPlayerVsPlayer}
          onPlayVsAI={startPlayerVsAI}
          onPlayOnline={startOnlineGame}
        />
      )}
      
      {currentScreen === 'game' && (
        <GameScreen
          game={game}
          theme={theme}
          onBack={goToHome}
          onThemeChange={handleThemeChange}
        />
      )}
    </div>
  );
}

export default App;
