import { Box, Button, Chip, Grid, Stack, Typography } from '@mui/material'
import { useParams } from 'react-router-dom'
import { ProductPlaceholder } from '../../components/common/ProductPlaceholder'

export function ProductDetailPage() {
  const { slug } = useParams()
  return (
    <Grid container spacing={4}>
      <Grid size={{ xs: 12, md: 6 }}><ProductPlaceholder /></Grid>
      <Grid size={{ xs: 12, md: 6 }}>
        <Typography color="text.secondary" variant="body2">Mã sản phẩm: {slug}</Typography>
        <Typography component="h1" variant="h1" mt={1}>TechPhone Standard</Typography>
        <Typography color="primary" variant="h2" mt={2}>12.990.000 ₫</Typography>
        <Typography mt={2} color="text.secondary">Điện thoại cân bằng giữa hiệu năng, thời lượng pin và trải nghiệm sử dụng hàng ngày.</Typography>
        <Box mt={3}><Typography fontWeight={700} mb={1}>Dung lượng</Typography><Stack direction="row" spacing={1}><Chip label="128 GB" color="primary" /><Chip label="256 GB" variant="outlined" /></Stack></Box>
        <Button variant="contained" size="large" fullWidth sx={{ mt: 4 }}>Thêm vào giỏ hàng</Button>
      </Grid>
    </Grid>
  )
}
