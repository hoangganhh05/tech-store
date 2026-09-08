import { Card, CardContent, Stack, Typography, Button } from '@mui/material'
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutline'
import { Link, useParams } from 'react-router-dom'
import { ROUTES } from '../../constants/routes'

export function OrderConfirmationPage() {
  const { orderNumber } = useParams()
  return <Card sx={{ maxWidth: 680, mx: 'auto', textAlign: 'center' }} data-testid="order-confirmation">
    <CardContent sx={{ p: { xs: 3, sm: 6 } }}><Stack spacing={2} alignItems="center">
      <CheckCircleOutlineIcon color="success" sx={{ fontSize: 72 }} />
      <Typography variant="h4" fontWeight={700}>Đặt hàng thành công</Typography>
      <Typography color="text.secondary">Mã đơn hàng của bạn</Typography>
      <Typography variant="h5" color="primary" fontWeight={700} data-testid="order-number">{orderNumber}</Typography>
      <Typography color="text.secondary">Đơn hàng đã được ghi nhận và đang chờ xử lý.</Typography>
      <Button component={Link} to={ROUTES.home} variant="contained">Tiếp tục mua sắm</Button>
    </Stack></CardContent>
  </Card>
}
