import { createTheme } from '@mui/material/styles'

export const appTheme = createTheme({
  palette: {
    mode: 'light',
    // The storefront uses the same blue/yellow identity as the physical shop.
    // Keep red reserved for destructive/error states so status meaning stays clear.
    primary: { main: '#0756a8', light: '#e4f0ff', dark: '#063d79', contrastText: '#ffffff' },
    secondary: { main: '#062e63', light: '#1d4f91', dark: '#041f43', contrastText: '#ffffff' },
    warning: { main: '#f2b705', light: '#fff3c4', dark: '#9a6800', contrastText: '#16213a' },
    background: { default: '#fbfcfe', paper: '#ffffff' },
    text: { primary: '#102a43', secondary: '#526579' },
    divider: '#dbe6f2',
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
          border: '1px solid #dbe6f2',
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
      styleOverrides: { root: { backgroundColor: '#fbfcfe' } },
    },
    MuiTableCell: {
      styleOverrides: { head: { color: '#475569', fontWeight: 700 } },
    },
  },
})
