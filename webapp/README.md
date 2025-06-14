# HexPulse - React Progressive Web App

A strategic hexagonal marble game based on Abalone, ported from the original Android application to a React Progressive Web App.

## Game Rules

HexPulse is based on the classic board game Abalone:

- **Objective**: Push 6 opponent marbles off the board to win
- **Setup**: Each player starts with 14 marbles arranged in a triangular formation
- **Movement**: Select 1-3 marbles and move them in a straight line
- **Pushing (Sumito)**: You can push opponent marbles if you have numerical superiority (3v2, 3v1, 2v1)
- **Move Types**:
  - **Single marble**: Can move to any adjacent empty hex
  - **Inline**: Move along the line direction, can push opponents
  - **Broadside**: Move perpendicular to the line, no pushing allowed

## Features

### Complete Game Implementation
- ✅ Exact port of Android game mechanics
- ✅ Hexagonal board with 61 positions
- ✅ Full move validation (inline, broadside, pushing)
- ✅ Win condition detection
- ✅ Multiple visual themes (Classic, Dark, Ocean, Forest)

### User Interface
- ✅ Touch-optimized controls for mobile devices
- ✅ Responsive design for all screen sizes
- ✅ Marble selection with visual feedback
- ✅ Valid move highlighting
- ✅ Score tracking display
- ✅ Theme switcher

### Progressive Web App
- ✅ PWA manifest for app installation
- ✅ Optimized for mobile and desktop
- ✅ Offline-ready architecture
- ✅ App-like experience

## Development

### Prerequisites
- Node.js 16+ and npm

### Setup
```bash
cd /Users/martin/WebstormProjects/games/hexpulse/HexPulse/webapp
npm install
```

### Development Commands
```bash
npm start      # Start development server
npm run build  # Build for production
npm test       # Run tests
```

### Project Structure
```
src/
├── components/          # React components
│   ├── GameScreen.tsx      # Main game interface
│   ├── HomeScreen.tsx      # Menu screen
│   └── HexagonalBoardView.tsx  # Game board canvas
├── game/               # Game logic
│   ├── AbaloneGame.ts     # Core game engine
│   └── Hex.ts             # Hexagonal coordinates
├── types/              # TypeScript types
├── utils/              # Utility functions
│   └── theme.ts           # Theme configurations
└── styles/             # CSS styles
```

## Game Architecture

### Coordinate System
- Uses axial coordinates (q, r) for hexagonal grid
- Constraint: q + r + s = 0 (where s = -q-r)
- Board bounds: -4 ≤ q, r, s ≤ 4

### Game Logic
- **AbaloneGame**: Main game class with complete rule implementation
- **Move validation**: Supports all official Abalone rules
- **State management**: Tracks game state, selections, and valid moves

### Rendering
- **Canvas-based**: High-performance hexagonal board rendering
- **Touch handling**: Optimized for mobile interaction
- **Visual effects**: 3D marble effects, selection highlights, shadows

## Themes

Four visual themes available:
- **Classic**: Traditional blue/gray color scheme
- **Dark**: Dark mode with muted colors
- **Ocean**: Blue ocean-inspired palette
- **Forest**: Green nature-inspired colors

## Future Enhancements

- AI opponent implementation
- Online multiplayer functionality
- Move history and undo
- Game statistics tracking
- Advanced animations

## Technical Notes

This React PWA is a complete port of the Android HexPulse application, maintaining:
- Identical game mechanics and rules
- Same visual design and user experience
- Equivalent touch/click interactions
- Matching theme system
- Identical win conditions and scoring

The codebase uses TypeScript for type safety and follows React best practices for component architecture and state management.