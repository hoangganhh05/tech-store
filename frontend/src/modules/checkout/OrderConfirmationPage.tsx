import { Card, CardContent, Stack, Typography, Button } from '@mui/material'
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutline'
import { Link, useLocation, useParams } from 'react-router-dom'
import { ROUTES } from '../../constants/routes'
import type { PlacedOrder } from '../../services/orderService'

const formatPrice = (value: number) => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(value)

export function OrderConfirmationPage() {
  const { orderNumber } = useParams()
  const location = useLocation()
  const order = (location.state as { order?: PlacedOrder } | null)?.order
  return <Card sx={{ maxWidth: 680, mx: 'auto', textAlign: 'center' }} data-testid="order-confirmation">
    <CardContent sx={{ p: { xs: 3, sm: 6 } }}><Stack spacing={2} alignItems="center">
      <CheckCircleOutlineIcon color="success" sx={{ fontSize: 72 }} />
      <Typography variant="h4" fontWeight={700}>Đặt hàng thành công</Typography>
      <Typography color="text.secondary">Mã đơn hàng của bạn</Typography>
      <Typography variant="h5" color="primary" fontWeight={700} data-testid="order-number">{orderNumber}</Typography>
      <Typography color="text.secondary">Đơn hàng đã được ghi nhận và đang chờ xử lý.</Typography>
      {order && <Stack spacing={1} sx={{ width: '100%', textAlign: 'left', mt: 1 }} data-testid="order-summary">
        <Typography variant="subtitle1" fontWeight={700}>Tóm tắt đơn hàng</Typography>
        {order.items.map((item, index) => <Stack key={`${item.productName}-${index}`} direction="row" justifyContent="space-between" spacing={2}>
          <Typography variant="body2">{item.productName}{item.variantLabel ? ` (${item.variantLabel})` : ''} × {item.quantity}</Typography>
          <Typography variant="body2" fontWeight={600}>{formatPrice(item.subtotal)}</Typography>
        </Stack>)}
        <Stack direction="row" justifyContent="space-between" sx={{ pt: 1, borderTop: 1, borderColor: 'divider' }}>
          <Typography fontWeight={700}>Tổng cộng</Typography><Typography fontWeight={700}>{formatPrice(order.totalAmount)}</Typography>
        </Stack>
        <Typography variant="body2" color="text.secondary">Dự kiến xử lý: {order.estimatedProcessingTime}</Typography>
      </Stack>}
      <Button component={Link} to={ROUTES.home} variant="contained">Tiếp tục mua sắm</Button>
    </Stack></CardContent>
  </Card>
}
