import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  CircularProgress,
  Divider,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material'
import { isAxiosError } from 'axios'
import { useCallback, useEffect, useState } from 'react'
import { Link as RouterLink, useParams } from 'react-router-dom'
import { PageIntro } from '../../components/common/PageIntro'
import { getOrderDetail, type OrderDetail } from '../../services/orderService'

const statusLabels: Record<string, string> = {
  PENDING: 'Chờ xác nhận',
  CONFIRMED: 'Đã xác nhận',
  SHIPPING: 'Đang giao',
  COMPLETED: 'Hoàn thành',
  CANCELLED: 'Đã huỷ',
}

const paymentMethodLabels: Record<string, string> = {
  COD: 'Thanh toán khi nhận hàng',
  BANK_TRANSFER: 'Chuyển khoản ngân hàng',
  ONLINE: 'Thanh toán online',
}

const paymentStatusLabels: Record<string, string> = {
  UNPAID: 'Chưa thanh toán',
  PENDING: 'Đang xử lý thanh toán',
  PAID: 'Đã thanh toán',
  FAILED: 'Thanh toán thất bại',
  REFUNDED: 'Đã hoàn tiền',
}

const standardSteps = ['PENDING', 'CONFIRMED', 'SHIPPING', 'COMPLETED']

function formatDate(value: string) {
  return new Intl.DateTimeFormat('vi-VN', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))
}

function formatAmount(value: number) {
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency', currency: 'VND', maximumFractionDigits: 0,
  }).format(value)
}

function StatusTimeline({ order }: { order: OrderDetail }) {
  const historyByStatus = new Map(order.statusHistory.map((entry) => [entry.status, entry]))
  const steps = order.status === 'CANCELLED' ? ['PENDING', 'CANCELLED'] : standardSteps
  const currentIndex = steps.indexOf(order.status)

  return (
    <Stack spacing={0} aria-label="Tiến trình xử lý đơn hàng">
      {steps.map((status, index) => {
        const history = historyByStatus.get(status)
        const completed = history !== undefined || (currentIndex >= index && order.status !== 'CANCELLED')
        return (
          <Stack key={status} direction="row" spacing={2} alignItems="stretch">
            <Stack alignItems="center" sx={{ width: 24 }}>
              <Box
                sx={{
                  mt: 0.25, width: 16, height: 16, borderRadius: '50%',
                  bgcolor: completed ? 'primary.main' : 'grey.300',
                  border: '3px solid', borderColor: completed ? 'primary.light' : 'grey.200',
                }}
              />
              {index < steps.length - 1 && <Box sx={{ width: 2, minHeight: 34, bgcolor: completed ? 'primary.light' : 'grey.300' }} />}
            </Stack>
            <Box pb={index < steps.length - 1 ? 1.5 : 0}>
              <Typography fontWeight={completed ? 700 : 400}>{statusLabels[status]}</Typography>
              <Typography variant="body2" color="text.secondary">
                {history ? formatDate(history.changedAt) : 'Chưa đạt trạng thái này'}
              </Typography>
            </Box>
          </Stack>
        )
      })}
    </Stack>
  )
}

export function OrderDetailPage() {
  const { id } = useParams()
  const orderId = Number(id)
  const [order, setOrder] = useState<OrderDetail | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const loadOrder = useCallback(async (isActive: () => boolean = () => true) => {
    if (!Number.isInteger(orderId) || orderId < 1) {
      setError('Mã đơn hàng không hợp lệ.')
      setLoading(false)
      return
    }
    setLoading(true)
    setError('')
    try {
      const response = await getOrderDetail(orderId)
      if (isActive()) setOrder(response)
    } catch (requestError: unknown) {
      if (!isActive()) return
      const message = isAxiosError<{ message?: string }>(requestError) ? requestError.response?.data?.message : undefined
      setError(message || 'Không thể tải chi tiết đơn hàng. Vui lòng thử lại.')
    } finally {
      if (isActive()) setLoading(false)
    }
  }, [orderId])

  useEffect(() => {
    let active = true
    void loadOrder(() => active)
    return () => { active = false }
  }, [loadOrder])

  return (
    <Stack spacing={3}>
      <Stack direction={{ xs: 'column', sm: 'row' }} justifyContent="space-between" alignItems={{ xs: 'flex-start', sm: 'center' }} spacing={1}>
        <PageIntro eyebrow="Tài khoản" title="Chi tiết đơn hàng" description="Theo dõi sản phẩm, thanh toán và tiến trình xử lý đơn hàng." />
        <Button component={RouterLink} to="/account/orders" variant="outlined">Quay lại đơn hàng</Button>
      </Stack>

      {loading && (
        <Stack alignItems="center" spacing={1} py={8}>
          <CircularProgress size={32} />
          <Typography color="text.secondary">Đang tải chi tiết đơn hàng...</Typography>
        </Stack>
      )}

      {!loading && error && (
        <Stack alignItems="center" spacing={2} py={6}>
          <Alert severity="error">{error}</Alert>
          <Button variant="outlined" onClick={() => void loadOrder()}>Thử lại</Button>
        </Stack>
      )}

      {!loading && !error && order && (
        <>
          <Card>
            <CardContent>
              <Stack direction={{ xs: 'column', sm: 'row' }} justifyContent="space-between" spacing={2}>
                <Box>
                  <Typography variant="h6">{order.orderNumber}</Typography>
                  <Typography color="text.secondary">Đặt lúc {formatDate(order.placedAt)}</Typography>
                </Box>
                <Chip label={statusLabels[order.status] || order.status} color={order.status === 'CANCELLED' ? 'error' : 'primary'} />
              </Stack>
            </CardContent>
          </Card>

          <Card>
            <CardContent>
              <Typography variant="h6" component="h2" mb={2}>Sản phẩm đã đặt</Typography>
              <TableContainer>
                <Table aria-label="Sản phẩm trong đơn hàng">
                  <TableHead><TableRow><TableCell>Sản phẩm</TableCell><TableCell>Phân loại</TableCell><TableCell align="right">Đơn giá</TableCell><TableCell align="right">Số lượng</TableCell><TableCell align="right">Thành tiền</TableCell></TableRow></TableHead>
                  <TableBody>
                    {order.items.map((item) => (
                      <TableRow key={`${item.sku}-${item.variantLabel}`}>
                        <TableCell><Typography fontWeight={600}>{item.productName}</Typography><Typography variant="body2" color="text.secondary">SKU: {item.sku}</Typography></TableCell>
                        <TableCell>{item.variantLabel || '—'}</TableCell>
                        <TableCell align="right">{formatAmount(item.unitPrice)}</TableCell>
                        <TableCell align="right">{item.quantity}</TableCell>
                        <TableCell align="right">{formatAmount(item.subtotal)}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
              <Stack alignItems="flex-end" spacing={0.5} mt={2}>
                <Typography>Tiền hàng: {formatAmount(order.subtotal)}</Typography>
                <Typography>Phí vận chuyển: {formatAmount(order.shippingFee)}</Typography>
                {order.discountAmount > 0 && <Typography>Giảm giá: -{formatAmount(order.discountAmount)}</Typography>}
                <Typography variant="h6">Tổng thanh toán: {formatAmount(order.totalAmount)}</Typography>
              </Stack>
            </CardContent>
          </Card>

          <Stack direction={{ xs: 'column', md: 'row' }} spacing={3}>
            <Card sx={{ flex: 1 }}><CardContent><Typography variant="h6" component="h2" mb={1.5}>Địa chỉ giao hàng</Typography><Typography fontWeight={600}>{order.shippingAddress.recipientName}</Typography><Typography>{order.shippingAddress.recipientPhone}</Typography><Typography color="text.secondary">{[order.shippingAddress.line1, order.shippingAddress.ward, order.shippingAddress.district, order.shippingAddress.province].filter(Boolean).join(', ')}</Typography></CardContent></Card>
            <Card sx={{ flex: 1 }}><CardContent><Typography variant="h6" component="h2" mb={1.5}>Thanh toán</Typography><Typography>{paymentMethodLabels[order.paymentMethod] || order.paymentMethod}</Typography><Typography color="text.secondary">{paymentStatusLabels[order.paymentStatus] || order.paymentStatus}</Typography></CardContent></Card>
          </Stack>

          <Card><CardContent><Typography variant="h6" component="h2" mb={2}>Tiến trình xử lý đơn hàng</Typography><Divider sx={{ mb: 2 }} /><StatusTimeline order={order} /></CardContent></Card>
        </>
      )}
    </Stack>
  )
}
