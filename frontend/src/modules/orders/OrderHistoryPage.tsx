import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  CircularProgress,
  FormControl,
  InputLabel,
  MenuItem,
  Select,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TablePagination,
  TableRow,
  Typography,
} from '@mui/material'
import { isAxiosError } from 'axios'
import { useCallback, useEffect, useState } from 'react'
import { PageIntro } from '../../components/common/PageIntro'
import { getMyOrders, type OrderHistoryItem } from '../../services/orderService'

const PAGE_SIZE = 10

const STATUS_OPTIONS = [
  { value: '', label: 'Tất cả trạng thái' },
  { value: 'PENDING', label: 'Chờ xác nhận' },
  { value: 'CONFIRMED', label: 'Đã xác nhận' },
  { value: 'SHIPPING', label: 'Đang giao' },
  { value: 'COMPLETED', label: 'Hoàn thành' },
  { value: 'CANCELLED', label: 'Đã huỷ' },
]

const statusLabels = new Map(STATUS_OPTIONS.map((option) => [option.value, option.label]))

function formatDate(value: string) {
  return new Intl.DateTimeFormat('vi-VN', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value))
}

function formatAmount(value: number) {
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    maximumFractionDigits: 0,
  }).format(value)
}

function getStatusColor(status: string): 'default' | 'info' | 'warning' | 'success' | 'error' {
  switch (status) {
    case 'CONFIRMED':
      return 'info'
    case 'SHIPPING':
      return 'warning'
    case 'COMPLETED':
      return 'success'
    case 'CANCELLED':
      return 'error'
    default:
      return 'default'
  }
}

export function OrderHistoryPage() {
  const [orders, setOrders] = useState<OrderHistoryItem[]>([])
  const [status, setStatus] = useState('')
  const [page, setPage] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const loadOrders = useCallback(async (isActive: () => boolean = () => true) => {
    setLoading(true)
    setError('')
    try {
      const response = await getMyOrders({ page, size: PAGE_SIZE, status: status || undefined })
      if (!isActive()) return
      setOrders(response.items)
      setTotalElements(response.totalElements)
    } catch (requestError: unknown) {
      if (!isActive()) return
      const message = isAxiosError<{ message?: string }>(requestError)
        ? requestError.response?.data?.message
        : undefined
      setError(message || 'Không thể tải lịch sử đơn hàng. Vui lòng thử lại.')
    } finally {
      if (isActive()) setLoading(false)
    }
  }, [page, status])

  useEffect(() => {
    let active = true
    void loadOrders(() => active)
    return () => {
      active = false
    }
  }, [loadOrders])

  const handleStatusChange = (nextStatus: string) => {
    setStatus(nextStatus)
    setPage(0)
  }

  return (
    <Stack spacing={3}>
      <PageIntro
        eyebrow="Tài khoản"
        title="Đơn hàng của tôi"
        description="Theo dõi lịch sử các đơn hàng bạn đã đặt."
      />

      <Card>
        <CardContent>
          <Stack direction={{ xs: 'column', sm: 'row' }} justifyContent="space-between" alignItems={{ xs: 'stretch', sm: 'center' }} spacing={2} mb={2}>
            <Typography variant="h6" component="h2">Lịch sử đơn hàng</Typography>
            <FormControl size="small" sx={{ minWidth: 210 }}>
              <InputLabel id="order-status-label">Lọc trạng thái</InputLabel>
              <Select
                labelId="order-status-label"
                label="Lọc trạng thái"
                value={status}
                onChange={(event) => handleStatusChange(event.target.value)}
                inputProps={{ 'aria-label': 'Lọc trạng thái đơn hàng' }}
              >
                {STATUS_OPTIONS.map((option) => (
                  <MenuItem key={option.value || 'all'} value={option.value}>{option.label}</MenuItem>
                ))}
              </Select>
            </FormControl>
          </Stack>

          {loading && (
            <Stack alignItems="center" spacing={1} py={8}>
              <CircularProgress size={32} />
              <Typography color="text.secondary">Đang tải lịch sử đơn hàng...</Typography>
            </Stack>
          )}

          {!loading && error && (
            <Stack alignItems="center" spacing={2} py={6}>
              <Alert severity="error">{error}</Alert>
              <Button variant="outlined" onClick={() => void loadOrders()}>Thử lại</Button>
            </Stack>
          )}

          {!loading && !error && orders.length === 0 && (
            <Box py={6} textAlign="center">
              <Typography color="text.secondary">Chưa có đơn hàng phù hợp.</Typography>
            </Box>
          )}

          {!loading && !error && orders.length > 0 && (
            <>
              <TableContainer>
                <Table aria-label="Lịch sử đơn hàng">
                  <TableHead>
                    <TableRow>
                      <TableCell>Mã đơn</TableCell>
                      <TableCell>Ngày đặt</TableCell>
                      <TableCell align="right">Tổng tiền</TableCell>
                      <TableCell>Trạng thái</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {orders.map((order) => (
                      <TableRow key={order.id} hover>
                        <TableCell sx={{ fontWeight: 700 }}>{order.orderNumber}</TableCell>
                        <TableCell>{formatDate(order.placedAt)}</TableCell>
                        <TableCell align="right">{formatAmount(order.totalAmount)}</TableCell>
                        <TableCell>
                          <Chip
                            size="small"
                            color={getStatusColor(order.status)}
                            label={statusLabels.get(order.status) || order.status}
                          />
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
              <TablePagination
                component="div"
                count={totalElements}
                page={page}
                onPageChange={(_event, nextPage) => setPage(nextPage)}
                rowsPerPage={PAGE_SIZE}
                rowsPerPageOptions={[PAGE_SIZE]}
                labelDisplayedRows={({ from, to, count }) => `${from}-${to} trên ${count}`}
                labelRowsPerPage="Số dòng mỗi trang"
              />
            </>
          )}
        </CardContent>
      </Card>
    </Stack>
  )
}
