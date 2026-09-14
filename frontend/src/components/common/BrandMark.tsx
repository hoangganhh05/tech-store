import { Box, Stack, Typography } from '@mui/material'
import { env } from '../../configs/env'

/** Shared brand signature for navigation, authentication and the footer. */
export function BrandMark({ inverse = false, compact = false }: { inverse?: boolean; compact?: boolean }) {
  return (
    <Stack direction="row" alignItems="center" spacing={1.2} minWidth={0}>
      <Box aria-hidden="true" sx={{ width: compact ? 34 : 40, height: compact ? 34 : 40, flexShrink: 0, display: 'grid', placeItems: 'center', borderRadius: '11px', bgcolor: inverse ? 'warning.main' : 'primary.main', color: inverse ? 'secondary.main' : 'warning.main', fontWeight: 800, fontSize: compact ? 13 : 16 }}>ĐT</Box>
      <Box minWidth={0}>
        <Typography component="span" display="block" sx={{ color: inverse ? 'white' : 'primary.main', fontSize: compact ? 14 : { xs: 14, sm: 16 }, fontWeight: 800, letterSpacing: '-.035em', lineHeight: 1.3, whiteSpace: 'nowrap' }}>{env.brand.name}</Typography>
        {!compact && <Typography component="span" display="block" sx={{ color: inverse ? '#c4d4e9' : 'text.secondary', fontSize: 10, lineHeight: 1.6, letterSpacing: '.045em' }}>ĐIỆN THOẠI & PHỤ KIỆN</Typography>}
      </Box>
    </Stack>
  )
}
