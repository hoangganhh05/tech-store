import { Box, Typography } from '@mui/material'
import type { ReactNode } from 'react'

type PageIntroProps = {
  eyebrow?: string
  title: string
  description: string
  action?: ReactNode
}

export function PageIntro({ eyebrow, title, description, action }: PageIntroProps) {
  return (
    <Box display="flex" justifyContent="space-between" alignItems="flex-start" gap={2} flexWrap="wrap" mb={{ xs: 2.5, md: 3.5 }}>
      <Box maxWidth={700}>
        {eyebrow && <Typography color="primary" fontWeight={600} variant="caption" display="block" mb={0.75}>{eyebrow}</Typography>}
        <Typography component="h1" variant="h1" mb={0.75}>{title}</Typography>
        <Typography variant="body2" color="text.secondary">{description}</Typography>
      </Box>
      {action}
    </Box>
  )
}
