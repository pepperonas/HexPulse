import React, { useRef, useEffect, useState, useCallback } from 'react';
import { Hex } from '../game/Hex';
import { AbaloneGame } from '../game/AbaloneGame';
import { Player, Theme } from '../types/game';
import { getThemeColors } from '../utils/theme';

interface HexagonalBoardViewProps {
  game: AbaloneGame;
  theme: Theme;
  onMove: () => void;
}

export const HexagonalBoardView: React.FC<HexagonalBoardViewProps> = ({ game, theme, onMove }) => {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const [dimensions, setDimensions] = useState({ width: 0, height: 0 });
  const [hexSize, setHexSize] = useState(40);
  const [boardCenter, setBoardCenter] = useState({ x: 0, y: 0 });
  const animationRef = useRef<number>(0);
  const [animatingMarbles] = useState<Map<string, { from: Hex, to: Hex, progress: number }>>(new Map());

  const colors = getThemeColors(theme);

  useEffect(() => {
    const updateDimensions = () => {
      if (canvasRef.current) {
        const parent = canvasRef.current.parentElement;
        if (parent) {
          const width = parent.clientWidth;
          const height = parent.clientHeight;
          setDimensions({ width, height });
          
          // Calculate hex size with more padding to prevent clipping
          // The board extends 4 hexes in each direction, so we need space for ~9 hexes
          const availableWidth = width - 80; // More padding
          const availableHeight = height - 80; // More padding
          
          // Calculate size based on hex grid geometry
          const sizeByWidth = availableWidth / (9 * 1.5); // 9 hexes * 1.5 spacing
          const sizeByHeight = availableHeight / (9 * Math.sqrt(3)); // Height consideration
          
          const size = Math.min(sizeByWidth, sizeByHeight, 45); // Max size limit
          setHexSize(size);
          setBoardCenter({ x: width / 2, y: height / 2 });
          
          // Set canvas size
          canvasRef.current.width = width;
          canvasRef.current.height = height;
        }
      }
    };

    updateDimensions();
    window.addEventListener('resize', updateDimensions);
    return () => window.removeEventListener('resize', updateDimensions);
  }, []);

  const drawHexagon = (ctx: CanvasRenderingContext2D, centerX: number, centerY: number, size: number) => {
    ctx.beginPath();
    for (let i = 0; i < 6; i++) {
      const angle = (Math.PI / 3) * i;
      const x = centerX + size * Math.cos(angle);
      const y = centerY + size * Math.sin(angle);
      if (i === 0) {
        ctx.moveTo(x, y);
      } else {
        ctx.lineTo(x, y);
      }
    }
    ctx.closePath();
  };

  const drawDashedArrow = useCallback((ctx: CanvasRenderingContext2D, startX: number, startY: number, endX: number, endY: number) => {
    // Arrow styling - wie im Android Screenshot
    ctx.strokeStyle = 'rgba(255, 193, 7, 0.95)'; // Golden like in Android
    ctx.lineWidth = 2.5; // Sehr dünn und filigran
    ctx.lineCap = 'round';
    ctx.setLineDash([6, 3]); // Feines Strichmuster

    // Calculate direction - VIEL längere Pfeile wie im Screenshot
    const dx = endX - startX;
    const dy = endY - startY;
    const length = Math.sqrt(dx * dx + dy * dy);
    
    if (length < 10) return;
    
    const unitX = dx / length;
    const unitY = dy / length;

    // Minimaler Margin - Pfeile gehen fast bis zu den Murmeln (wie im Screenshot)
    const marbleRadius = hexSize * 0.7;
    const margin = marbleRadius * 0.6; // Viel kleiner für längere Pfeile

    // Extend arrows beyond hex boundaries for maximum length
    const adjustedStartX = startX + unitX * margin;
    const adjustedStartY = startY + unitY * margin;
    const adjustedEndX = endX - unitX * margin;
    const adjustedEndY = endY - unitY * margin;

    // Draw main arrow line
    ctx.beginPath();
    ctx.moveTo(adjustedStartX, adjustedStartY);
    ctx.lineTo(adjustedEndX, adjustedEndY);
    ctx.stroke();

    // Draw sehr kleine, feine Pfeilspitze
    const arrowHeadLength = 8; // Sehr klein wie im Screenshot
    const arrowHeadAngle = Math.PI / 8; // Sehr spitz (~22.5°)

    // Calculate arrowhead points
    const angle = Math.atan2(dy, dx);
    const arrowTip1X = adjustedEndX - arrowHeadLength * Math.cos(angle - arrowHeadAngle);
    const arrowTip1Y = adjustedEndY - arrowHeadLength * Math.sin(angle - arrowHeadAngle);
    const arrowTip2X = adjustedEndX - arrowHeadLength * Math.cos(angle + arrowHeadAngle);
    const arrowTip2Y = adjustedEndY - arrowHeadLength * Math.sin(angle + arrowHeadAngle);

    // Draw arrowhead lines - sehr fein
    ctx.setLineDash([]); // Solid lines for arrowhead
    ctx.lineWidth = 2; // Noch dünner für Pfeilspitzen
    
    ctx.beginPath();
    ctx.moveTo(adjustedEndX, adjustedEndY);
    ctx.lineTo(arrowTip1X, arrowTip1Y);
    ctx.stroke();

    ctx.beginPath();
    ctx.moveTo(adjustedEndX, adjustedEndY);
    ctx.lineTo(arrowTip2X, arrowTip2Y);
    ctx.stroke();

    // Reset line dash and width
    ctx.setLineDash([]);
  }, [hexSize]);



  const isValidDirectionForTarget = useCallback((selectedMarbles: Hex[], target: Hex, direction: number): boolean => {
    // Simple check: see if any marble moved in this direction reaches the target
    for (const marble of selectedMarbles) {
      if (marble.neighbor(direction).equals(target)) {
        return true;
      }
    }
    return false;
  }, []);

  const findMoveDirection = useCallback((selectedMarbles: Hex[], target: Hex): number | null => {
    if (selectedMarbles.length === 1) {
      const marble = selectedMarbles[0];
      for (let dir = 0; dir < 6; dir++) {
        if (marble.neighbor(dir).equals(target)) {
          return dir;
        }
      }
    } else {
      // For multiple marbles, check all directions
      for (let dir = 0; dir < 6; dir++) {
        // Check if this could be the target for this direction
        if (isValidDirectionForTarget(selectedMarbles, target, dir)) {
          return dir;
        }
      }
    }
    return null;
  }, [isValidDirectionForTarget]);

  const drawMovementArrows = useCallback((ctx: CanvasRenderingContext2D, selectedMarbles: Hex[], direction: number) => {
    for (const marble of selectedMarbles) {
      const [fromX, fromY] = marble.toPixel(boardCenter.x, boardCenter.y, hexSize);
      const newPos = marble.neighbor(direction);
      const [toX, toY] = newPos.toPixel(boardCenter.x, boardCenter.y, hexSize);
      
      drawDashedArrow(ctx, fromX, fromY, toX, toY);
    }
  }, [boardCenter, hexSize, drawDashedArrow]);

  const drawMovementPreview = useCallback((ctx: CanvasRenderingContext2D) => {
    const selectedMarbles = game.getSelectedMarbles();
    const validMoves = game.getValidMoves();

    if (selectedMarbles.length === 0 || validMoves.size === 0) return;

    // For each valid move, determine which marbles would move and draw arrows
    validMoves.forEach((_, targetKey) => {
      const [q, r] = targetKey.split(',').map(Number);
      const target = new Hex(q, r);
      const direction = findMoveDirection(selectedMarbles, target);
      
      if (direction !== null) {
        drawMovementArrows(ctx, selectedMarbles, direction);
      }
    });
  }, [game, findMoveDirection, drawMovementArrows]);

  const drawMarble = useCallback((ctx: CanvasRenderingContext2D, centerX: number, centerY: number, player: Player, isSelected: boolean, isPreview: boolean = false) => {
    const marbleSize = hexSize * 0.7;
    
    // Shadow
    if (!isPreview) {
      ctx.shadowColor = 'rgba(0, 0, 0, 0.3)';
      ctx.shadowBlur = 5;
      ctx.shadowOffsetX = 2;
      ctx.shadowOffsetY = 2;
    }
    
    // Create gradient for 3D effect
    const gradient = ctx.createRadialGradient(
      centerX - marbleSize * 0.3,
      centerY - marbleSize * 0.3,
      0,
      centerX,
      centerY,
      marbleSize
    );
    
    if (player === Player.BLACK) {
      gradient.addColorStop(0, '#4A4A55');
      gradient.addColorStop(0.7, colors.marbleBlack);
      gradient.addColorStop(1, '#0A0A0F');
    } else {
      gradient.addColorStop(0, '#FFFFFF');
      gradient.addColorStop(0.7, colors.marbleWhite);
      gradient.addColorStop(1, '#D0D0D5');
    }
    
    // Draw marble
    ctx.fillStyle = gradient;
    ctx.beginPath();
    ctx.arc(centerX, centerY, marbleSize, 0, Math.PI * 2);
    ctx.fill();
    
    // Reset shadow
    ctx.shadowColor = 'transparent';
    ctx.shadowBlur = 0;
    ctx.shadowOffsetX = 0;
    ctx.shadowOffsetY = 0;
    
    // Selection highlight
    if (isSelected) {
      ctx.strokeStyle = colors.selectionHighlight;
      ctx.lineWidth = 3;
      ctx.beginPath();
      ctx.arc(centerX, centerY, marbleSize + 4, 0, Math.PI * 2);
      ctx.stroke();
      
      // Glow effect
      ctx.strokeStyle = colors.selectionHighlight + '40';
      ctx.lineWidth = 6;
      ctx.beginPath();
      ctx.arc(centerX, centerY, marbleSize + 8, 0, Math.PI * 2);
      ctx.stroke();
    }
    
    // Preview opacity
    if (isPreview) {
      ctx.globalAlpha = 0.5;
    }
  }, [hexSize, colors]);

  const draw = useCallback(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    
    const ctx = canvas.getContext('2d');
    if (!ctx) return;
    
    // Clear canvas
    ctx.fillStyle = colors.backgroundColor;
    ctx.fillRect(0, 0, dimensions.width, dimensions.height);
    
    // Draw board
    const board = game.getBoard();
    const selectedMarbles = game.getSelectedMarbles();
    const validMoves = game.getValidMoves();
    
    // First pass: Draw hexagons
    board.forEach((player, key) => {
      const [q, r] = key.split(',').map(Number);
      const hex = new Hex(q, r);
      const [x, y] = hex.toPixel(boardCenter.x, boardCenter.y, hexSize);
      
      // Board gradient
      const gradient = ctx.createRadialGradient(
        boardCenter.x, boardCenter.y, 0,
        boardCenter.x, boardCenter.y, hexSize * 6
      );
      gradient.addColorStop(0, colors.boardStartColor);
      gradient.addColorStop(1, colors.boardEndColor);
      
      // Fill hexagon
      drawHexagon(ctx, x, y, hexSize * 0.95);
      ctx.fillStyle = gradient;
      ctx.fill();
      
      // Border
      ctx.strokeStyle = colors.boardBorderColor;
      ctx.lineWidth = 1;
      ctx.stroke();
      
      // Valid move highlight - wie im Android Screenshot
      if (validMoves.has(key)) {
        // Äußerer grüner Ring wie im Screenshot
        drawHexagon(ctx, x, y, hexSize * 0.95);
        ctx.strokeStyle = colors.validMoveHighlight;
        ctx.lineWidth = 3;
        ctx.stroke();
        
        // Inneres grünes Highlight wie im Screenshot
        drawHexagon(ctx, x, y, hexSize * 0.8);
        ctx.fillStyle = colors.validMoveHighlight + '20'; // Sehr transparent
        ctx.fill();
        
        // Zusätzlicher innerer Ring für den "Glow"-Effekt
        drawHexagon(ctx, x, y, hexSize * 0.7);
        ctx.strokeStyle = colors.validMoveHighlight + '60';
        ctx.lineWidth = 1.5;
        ctx.stroke();
      }
    });
    
    // Second pass: Draw marbles
    board.forEach((player, key) => {
      if (player === Player.EMPTY) return;
      
      const [q, r] = key.split(',').map(Number);
      const hex = new Hex(q, r);
      const [x, y] = hex.toPixel(boardCenter.x, boardCenter.y, hexSize);
      
      const isSelected = selectedMarbles.some(m => m.equals(hex));
      
      // Check if marble is animating
      const animKey = `${q},${r}`;
      const animData = animatingMarbles.get(animKey);
      
      if (animData) {
        const [fromX, fromY] = animData.from.toPixel(boardCenter.x, boardCenter.y, hexSize);
        const [toX, toY] = animData.to.toPixel(boardCenter.x, boardCenter.y, hexSize);
        const currentX = fromX + (toX - fromX) * animData.progress;
        const currentY = fromY + (toY - fromY) * animData.progress;
        drawMarble(ctx, currentX, currentY, player, isSelected);
      } else {
        drawMarble(ctx, x, y, player, isSelected);
      }
    });
    
    // Draw movement preview arrows for selected marbles
    drawMovementPreview(ctx);
    
    // Draw preview marbles at valid moves
    if (selectedMarbles.length > 0) {
      validMoves.forEach((_direction, key) => {
        const [q, r] = key.split(',').map(Number);
        const targetHex = new Hex(q, r);
        const [x, y] = targetHex.toPixel(boardCenter.x, boardCenter.y, hexSize);
        
        // Draw preview marble
        ctx.globalAlpha = 0.3;
        drawMarble(ctx, x, y, game.getCurrentPlayer(), false, true);
        ctx.globalAlpha = 1;
      });
    }
  }, [game, dimensions, hexSize, boardCenter, colors, animatingMarbles, drawMarble, drawMovementPreview]);

  useEffect(() => {
    const animate = () => {
      draw();
      animationRef.current = requestAnimationFrame(animate);
    };
    animate();
    
    return () => {
      if (animationRef.current) {
        cancelAnimationFrame(animationRef.current);
      }
    };
  }, [draw]);

  const handleClick = (event: React.MouseEvent<HTMLCanvasElement>) => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    
    const rect = canvas.getBoundingClientRect();
    const x = event.clientX - rect.left;
    const y = event.clientY - rect.top;
    
    const hex = Hex.fromPixel(x, y, boardCenter.x, boardCenter.y, hexSize);
    
    // Check if it's a valid board position
    if (!game.isValidPosition(hex)) return;
    
    // Check if clicking on a valid move
    const hexKey = `${hex.q},${hex.r}`;
    if (game.getValidMoves().has(hexKey)) {
      // Make the move
      game.makeMove(hex);
      onMove();
    } else {
      // Try to select marble
      game.selectMarble(hex);
    }
  };

  const handleTouchStart = (event: React.TouchEvent<HTMLCanvasElement>) => {
    event.preventDefault();
    const canvas = canvasRef.current;
    if (!canvas || event.touches.length === 0) return;
    
    const rect = canvas.getBoundingClientRect();
    const touch = event.touches[0];
    const x = touch.clientX - rect.left;
    const y = touch.clientY - rect.top;
    
    const hex = Hex.fromPixel(x, y, boardCenter.x, boardCenter.y, hexSize);
    
    // Check if it's a valid board position
    if (!game.isValidPosition(hex)) return;
    
    // Check if touching a valid move
    const hexKey = `${hex.q},${hex.r}`;
    if (game.getValidMoves().has(hexKey)) {
      // Make the move
      game.makeMove(hex);
      onMove();
    } else {
      // Try to select marble
      game.selectMarble(hex);
    }
  };

  return (
    <canvas
      ref={canvasRef}
      onClick={handleClick}
      onTouchStart={handleTouchStart}
      style={{
        width: '100%',
        height: '100%',
        cursor: 'pointer',
        touchAction: 'none'
      }}
    />
  );
};