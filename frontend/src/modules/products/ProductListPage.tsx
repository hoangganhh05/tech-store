import { Button, Card, CardActions, CardContent, Grid, MenuItem, Stack, TextField, Typography } from '@mui/material'
import { Link } from 'react-router-dom'
import { PageIntro } from '../../components/common/PageIntro'
import { ProductPlaceholder } from '../../components/common/ProductPlaceholder'

const products = ['TechPhone Standard', 'TechPhone Plus', 'Tai nghe không dây', 'Sạc nhanh 30W']

export function ProductListPage() {
  return (
    <>
      <PageIntro title="Sản phẩm" description="Tìm điện thoại và phụ kiện phù hợp với nhu cầu của bạn." />
      <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} mb={3}>
        <TextField label="Tìm kiếm" placeholder="Nhập tên sản phẩm" fullWidth />
        <TextField select label="Danh mục" defaultValue="all" sx={{ minWidth: 190 }}>
          <MenuItem value="all">Tất cả</MenuItem><MenuItem value="phone">Điện thoại</MenuItem><MenuItem value="accessory">Phụ kiện</MenuItem>
        </TextField>
      </Stack>
      <Grid container spacing={2}>
        {products.map((product, index) => (
          <Grid key={product} size={{ xs: 12, sm: 6, md: 3 }}>
            <Card><ProductPlaceholder /><CardContent><Typography fontWeight={700}>{product}</Typography><Typography color="primary" mt={1}>{(8_990_000 + index * 2_000_000).toLocaleString('vi-VN')} ₫</Typography></CardContent><CardActions><Button component={Link} to={`/products/product-${index + 1}`}>Xem chi tiết</Button></CardActions></Card>
          </Grid>
        ))}
      </Grid>
    </>
  )
}
