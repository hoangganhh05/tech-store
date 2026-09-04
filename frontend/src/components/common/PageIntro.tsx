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
    <Box display="flex" justifyContent="space-between" alignItems="flex-start" gap={2} flexWrap="wrap" mb={3}>
      <Box maxWidth={700}>
        {eyebrow && <Typography color="primary" fontWeight={700} variant="overline">{eyebrow}</Typography>}
        <Typography component="h1" variant="h1" mb={1}>{title}</Typography>
        <Typography color="text.secondary">{description}</Typography>
      </Box>
      {action}
    </Box>
  )
}
