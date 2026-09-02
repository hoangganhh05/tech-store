import { Button, Card, CardContent, Divider, Stack, Typography } from '@mui/material'
import { Link } from 'react-router-dom'
import { PageIntro } from '../../components/common/PageIntro'
import { ROUTES } from '../../constants/routes'

export function CartPage() {
  return (
    <>
      <PageIntro title="Giỏ hàng" description="Kiểm tra sản phẩm và số lượng trước khi thanh toán." />
      <Card><CardContent><Typography color="text.secondary">Giỏ hàng của bạn đang trống.</Typography><Divider sx={{ my: 2 }} /><Stack direction={{ xs: 'column', sm: 'row' }} spacing={1}><Button component={Link} to={ROUTES.products} variant="outlined">Tiếp tục mua sắm</Button><Button component={Link} to={ROUTES.checkout} variant="contained" disabled>Thanh toán</Button></Stack></CardContent></Card>
    </>
  )
}
