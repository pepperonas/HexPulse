import { Theme } from '../types/game';

export interface ThemeColors {
  backgroundColor: string;
  boardStartColor: string;
  boardEndColor: string;
  boardBorderColor: string;
  highlightColor: string;
  buttonStartColor: string;
  buttonEndColor: string;
  marbleBlack: string;
  marbleWhite: string;
  selectionHighlight: string;
  validMoveHighlight: string;
  textPrimary: string;
  textSecondary: string;
}

export const getThemeColors = (theme: Theme): ThemeColors => {
  switch (theme) {
    case Theme.CLASSIC:
      return {
        backgroundColor: '#0F1423',
        boardStartColor: '#2D374B',
        boardEndColor: '#415773',
        boardBorderColor: '#19233B',
        highlightColor: '#66BB6A',
        buttonStartColor: '#3F51B5',
        buttonEndColor: '#303F9F',
        marbleBlack: '#1E1E25',
        marbleWhite: '#F0F0F5',
        selectionHighlight: '#FFC107',
        validMoveHighlight: '#66BB6A',
        textPrimary: '#FFFFFF',
        textSecondary: '#E2E8F0'
      };
    
    case Theme.DARK:
      return {
        backgroundColor: '#0A0A0F',
        boardStartColor: '#1E1E28',
        boardEndColor: '#32323C',
        boardBorderColor: '#14141E',
        highlightColor: '#9696A0',
        buttonStartColor: '#282832',
        buttonEndColor: '#1E1E28',
        marbleBlack: '#141419',
        marbleWhite: '#E6E6EB',
        selectionHighlight: '#FFC107',
        validMoveHighlight: '#9696A0',
        textPrimary: '#FFFFFF',
        textSecondary: '#E2E8F0'
      };
    
    case Theme.OCEAN:
      return {
        backgroundColor: '#0A1928',
        boardStartColor: '#143C64',
        boardEndColor: '#285078',
        boardBorderColor: '#0F2D4B',
        highlightColor: '#64C8FF',
        buttonStartColor: '#1E90FF',
        buttonEndColor: '#0064C8',
        marbleBlack: '#0F1E2D',
        marbleWhite: '#E6F4FF',
        selectionHighlight: '#FFC107',
        validMoveHighlight: '#64C8FF',
        textPrimary: '#FFFFFF',
        textSecondary: '#E2E8F0'
      };
    
    case Theme.FOREST:
      return {
        backgroundColor: '#141E14',
        boardStartColor: '#284628',
        boardEndColor: '#3C5A3C',
        boardBorderColor: '#192D19',
        highlightColor: '#90EE90',
        buttonStartColor: '#228B22',
        buttonEndColor: '#006400',
        marbleBlack: '#0F1E0F',
        marbleWhite: '#E6FFE6',
        selectionHighlight: '#FFC107',
        validMoveHighlight: '#90EE90',
        textPrimary: '#FFFFFF',
        textSecondary: '#E2E8F0'
      };
    
    default:
      return getThemeColors(Theme.CLASSIC);
  }
};