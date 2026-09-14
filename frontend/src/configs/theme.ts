import { createTheme } from '@mui/material/styles'

export const appTheme = createTheme({
  palette: {
    mode: 'light',
    // The storefront uses the same blue/yellow identity as the physical shop.
    // Keep red reserved for destructive/error states so status meaning stays clear.
    primary: { main: '#0756a8', light: '#e4f0ff', dark: '#063d79', contrastText: '#ffffff' },
    secondary: { main: '#062e63', light: '#1d4f91', dark: '#041f43', contrastText: '#ffffff' },
    warning: { main: '#f2b705', light: '#fff3c4', dark: '#9a6800', contrastText: '#16213a' },
    background: { default: '#f7f9fc', paper: '#ffffff' },
    text: { primary: '#102a43', secondary: '#526579' },
    divider: '#dbe6f2',
  },
  shape: { borderRadius: 12 },
  typography: {
    fontFamily: '"Be Vietnam Pro", Inter, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif',
    h1: { fontSize: 'clamp(1.625rem, 2.8vw, 2.25rem)', fontWeight: 800, lineHeight: 1.3, letterSpacing: '-0.025em' },
    h2: { fontSize: 'clamp(1.25rem, 2vw, 1.5rem)', fontWeight: 700, lineHeight: 1.4, letterSpacing: '-0.015em' },
    h3: { fontSize: '1.125rem', fontWeight: 700, lineHeight: 1.4, letterSpacing: '-0.01em' },
    h4: { fontSize: '1.5rem', fontWeight: 700, lineHeight: 1.4 },
    h5: { fontSize: '1.25rem', fontWeight: 700, lineHeight: 1.4 },
    h6: { fontSize: '1.05rem', fontWeight: 700, lineHeight: 1.5 },
    body1: { fontSize: '0.9375rem', lineHeight: 1.7 },
    body2: { fontSize: '0.875rem', lineHeight: 1.6 },
    caption: { fontSize: '0.75rem', lineHeight: 1.5 },
    button: { fontWeight: 700, textTransform: 'none' },
  },
  components: {
    MuiButton: {
      defaultProps: { disableElevation: true },
      styleOverrides: {
        root: { borderRadius: 10, minHeight: 44, paddingInline: 18, whiteSpace: 'normal' },
        sizeSmall: { minHeight: 36, paddingInline: 12 },
        sizeLarge: { minHeight: 48 },
        outlined: { borderColor: '#dbe6f2' },
      },
    },
    MuiCard: {
      styleOverrides: {
        root: {
          border: '1px solid #dbe6f2',
          borderRadius: 16,
          boxShadow: '0 1px 4px rgba(7, 86, 168, 0.05)',
          backgroundImage: 'none',
        },
      },
    },
    MuiAppBar: { styleOverrides: { root: { boxShadow: 'none' } } },
    MuiContainer: { styleOverrides: { maxWidthXl: { '@media (min-width:1536px)': { maxWidth: 1280 }, '@media (min-width:1200px)': { maxWidth: 1280 } } } },
    MuiPaper: { styleOverrides: { root: { backgroundImage: 'none' }, rounded: { borderRadius: 16 } } },
    MuiDialog: { styleOverrides: { paper: { borderRadius: 20, boxShadow: '0 20px 60px rgba(6,46,99,.18)', '@media (max-width:599px)': { margin: 12, maxWidth: 'calc(100% - 24px)' } } } },
    MuiDialogTitle: { styleOverrides: { root: { fontSize: '1.125rem', fontWeight: 700 } } },
    MuiDialogActions: { styleOverrides: { root: { padding: '16px 24px', gap: 8, flexWrap: 'wrap' } } },
    MuiChip: { styleOverrides: { root: { fontWeight: 600, borderRadius: 8 } } },
    MuiAlert: { styleOverrides: { root: { borderRadius: 12 }, message: { overflowWrap: 'anywhere' } } },
    MuiTabs: { styleOverrides: { indicator: { height: 3, borderRadius: 3 } } },
    MuiTab: { styleOverrides: { root: { textTransform: 'none', minHeight: 48, fontWeight: 600 } } },
    MuiTableContainer: { styleOverrides: { root: { borderRadius: 12, border: '1px solid #dbe6f2' } } },
    MuiTableRow: { styleOverrides: { root: { '&:last-child td': { borderBottom: 0 }, '&.MuiTableRow-hover:hover': { backgroundColor: '#f7f9fc' } } } },
    MuiTooltip: { defaultProps: { arrow: true } },
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
      styleOverrides: { root: { backgroundColor: '#f7f9fc' } },
    },
    MuiTableCell: {
      styleOverrides: { root: { borderColor: '#eaf0f6', padding: '14px 16px' }, head: { color: '#526579', fontWeight: 600, fontSize: '0.75rem', whiteSpace: 'nowrap' } },
    },
  },
})
