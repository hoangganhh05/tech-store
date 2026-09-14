import { Box } from '@mui/material'
import SmartphoneRoundedIcon from '@mui/icons-material/SmartphoneRounded'

export function ProductPlaceholder({ compact = false }: { compact?: boolean }) {
  return (
    <Box
      aria-label="Hình minh họa sản phẩm"
      display="grid"
      minHeight={compact ? 100 : 180}
      bgcolor="#F7F9FC"
      color="#A0B4C8"
      sx={{ placeItems: 'center', width: '100%', borderRadius: '12px' }}
    >
      <SmartphoneRoundedIcon sx={{ fontSize: compact ? 48 : 76 }} />
    </Box>
  )
}
