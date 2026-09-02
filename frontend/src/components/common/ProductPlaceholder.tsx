import { Box } from '@mui/material'
import SmartphoneRoundedIcon from '@mui/icons-material/SmartphoneRounded'

export function ProductPlaceholder({ compact = false }: { compact?: boolean }) {
  return (
    <Box
      aria-label="Hình minh họa sản phẩm"
      display="grid"
      minHeight={compact ? 100 : 180}
      bgcolor="#eef1f4"
      color="#607d8b"
      sx={{ placeItems: 'center' }}
    >
      <SmartphoneRoundedIcon sx={{ fontSize: compact ? 48 : 76 }} />
    </Box>
  )
}
