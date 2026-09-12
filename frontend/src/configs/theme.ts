import { createTheme } from '@mui/material/styles'

export const appTheme = createTheme({
  palette: {
    mode: 'light',
    primary: { main: '#dc2626', light: '#fee2e2', dark: '#991b1b', contrastText: '#ffffff' },
    secondary: { main: '#0f172a', light: '#334155', contrastText: '#ffffff' },
    background: { default: '#f8fafc', paper: '#ffffff' },
    text: { primary: '#0f172a', secondary: '#64748b' },
    divider: '#e2e8f0',
  },
  shape: { borderRadius: 12 },
  typography: {
    fontFamily: 'Inter, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif',
    h1: { fontSize: 'clamp(2rem, 4vw, 3.75rem)', fontWeight: 800, lineHeight: 1.08, letterSpacing: '-0.035em' },
    h2: { fontSize: 'clamp(1.5rem, 2.5vw, 2.125rem)', fontWeight: 800, letterSpacing: '-0.02em' },
    h3: { fontSize: '1.25rem', fontWeight: 750, letterSpacing: '-0.01em' },
    button: { fontWeight: 700, textTransform: 'none' },
  },
  components: {
    MuiButton: {
      defaultProps: { disableElevation: true },
      styleOverrides: { root: { borderRadius: 10, minHeight: 40 } },
    },
    MuiCard: {
      styleOverrides: {
        root: {
          border: '1px solid #e2e8f0',
          boxShadow: '0 1px 2px rgba(15, 23, 42, 0.04)',
          backgroundImage: 'none',
        },
      },
    },
    MuiAppBar: { styleOverrides: { root: { boxShadow: 'none' } } },
    MuiTextField: { defaultProps: { variant: 'outlined' } },
    MuiOutlinedInput: {
      styleOverrides: {
        root: {
          borderRadius: 10,
          backgroundColor: '#ffffff',
        },
      },
    },
    MuiTableHead: {
      styleOverrides: { root: { backgroundColor: '#f8fafc' } },
    },
    MuiTableCell: {
      styleOverrides: { head: { color: '#475569', fontWeight: 700 } },
    },
  },
})
