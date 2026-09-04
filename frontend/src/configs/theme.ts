import { createTheme } from '@mui/material/styles'

export const appTheme = createTheme({
  palette: {
    mode: 'light',
    primary: { main: '#c62828', dark: '#8e0000', contrastText: '#ffffff' },
    secondary: { main: '#263238' },
    background: { default: '#f6f7f9', paper: '#ffffff' },
    text: { primary: '#182026', secondary: '#5f6b73' },
  },
  shape: { borderRadius: 10 },
  typography: {
    fontFamily: 'Inter, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif',
    h1: { fontSize: '2.25rem', fontWeight: 750, lineHeight: 1.2 },
    h2: { fontSize: '1.75rem', fontWeight: 700 },
    h3: { fontSize: '1.25rem', fontWeight: 700 },
    button: { fontWeight: 650, textTransform: 'none' },
  },
  components: {
    MuiButton: { defaultProps: { disableElevation: true } },
    MuiCard: { styleOverrides: { root: { border: '1px solid #e4e7eb', boxShadow: 'none' } } },
    MuiAppBar: { styleOverrides: { root: { boxShadow: 'none' } } },
  },
})
