import { Box, Typography } from "@mui/material";
import type { ReactNode } from "react";

export function AdminPageIntro({ eyebrow, title, description, action }: {
  eyebrow?: string;
  title: string;
  description: string;
  action?: ReactNode;
}) {
  return (
    <Box
      display="flex"
      justifyContent="space-between"
      alignItems={{ xs: "flex-start", md: "flex-end" }}
      gap={2}
      flexWrap="wrap"
    >
      <Box minWidth={0} maxWidth={760}>
        {eyebrow && (
          <Typography
            variant="overline"
            color="primary.main"
            fontWeight={800}
            letterSpacing="0.08em"
            display="block"
            sx={{ mb: 0.25 }}
          >
            {eyebrow}
          </Typography>
        )}
        <Typography
          component="h1"
          sx={{ fontSize: { xs: 24, md: 30 }, fontWeight: 800, lineHeight: 1.2, mb: 0.75 }}
        >
          {title}
        </Typography>
        <Typography variant="body2" color="text.secondary" sx={{ maxWidth: 680 }}>
          {description}
        </Typography>
      </Box>
      {action && (
        <Box sx={{ "& > .MuiStack-root": { flexWrap: "wrap", rowGap: 1 } }}>
          {action}
        </Box>
      )}
    </Box>
  );
}
