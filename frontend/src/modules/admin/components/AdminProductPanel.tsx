import { Box, Dialog, type DialogProps } from "@mui/material";

/** Reuses the API-backed product editors in a workspace or a standalone dialog. */
export function AdminProductPanel({ embedded = false, open, children, ...props }: DialogProps & { embedded?: boolean }) {
  if (embedded) {
    if (!open) return null;
    return <Box sx={{ minWidth: 0, "& > .MuiDialogTitle-root": { px: { xs: 2, sm: 3 }, fontSize: 18 }, "& > .MuiDialogContent-root": { px: { xs: 2, sm: 3 } }, "& .MuiTableHead-root": { bgcolor: "#F7F9FC" } }}>{children}</Box>;
  }
  return <Dialog open={open} {...props}>{children}</Dialog>;
}
