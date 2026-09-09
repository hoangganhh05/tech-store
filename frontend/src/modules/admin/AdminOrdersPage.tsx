import SearchIcon from '@mui/icons-material/Search'
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
  Paper,
  Select,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TablePagination,
  TableRow,
  TextField,
  Typography,
} from '@mui/material'
import { isAxiosError } from 'axios'
import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { Link as RouterLink } from 'react-router-dom'
import { PageIntro } from '../../components/common/PageIntro'
import { getAdminOrders, type AdminOrderSummary } from '../../services/adminOrderService'

const STATUS_OPTIONS = [
  { value: '', label: 'Tất cả trạng thái' },
  { value: 'PENDING', label: 'Chờ xác nhận' },
  { value: 'CONFIRMED', label: 'Đã xác nhận' },
  { value: 'SHIPPING', label: 'Đang giao' },
  { value: 'COMPLETED', label: 'Hoàn thành' },
  { value: 'CANCELLED', label: 'Đã huỷ' },
]

const statusLabels = new Map(STATUS_OPTIONS.map((option) => [option.value, option.label]))

type Filters = {
  search: string
  status: string
  fromDate: string
  toDate: string
}

const emptyFilters: Filters = { search: '', status: '', fromDate: '', toDate: '' }

function formatDate(value: string) {
  return new Intl.DateTimeFormat('vi-VN', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))
}

function formatAmount(value: number) {
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency', currency: 'VND', maximumFractionDigits: 0,
  }).format(value)
}

function statusColor(status: string): 'default' | 'info' | 'warning' | 'success' | 'error' {
  if (status === 'CONFIRMED') return 'info'
  if (status === 'SHIPPING') return 'warning'
  if (status === 'COMPLETED') return 'success'
  if (status === 'CANCELLED') return 'error'
  return 'default'
}

export function AdminOrdersPage() {
  const [orders, setOrders] = useState<AdminOrderSummary[]>([])
  const [page, setPage] = useState(0)
  const [rowsPerPage, setRowsPerPage] = useState(10)
  const [totalElements, setTotalElements] = useState(0)
  const [draftFilters, setDraftFilters] = useState<Filters>(emptyFilters)
  const [activeFilters, setActiveFilters] = useState<Filters>(emptyFilters)
  const [refreshIndex, setRefreshIndex] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const loadOrders = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const response = await getAdminOrders({
        ...activeFilters,
        page,
        size: rowsPerPage,
      })
      setOrders(response.items)
      setTotalElements(response.totalElements)
    } catch (requestError: unknown) {
      const message = isAxiosError<{ message?: string }>(requestError) ? requestError.response?.data?.message : undefined
      setError(message || 'Không thể tải danh sách đơn hàng. Vui lòng thử lại.')
    } finally {
      setLoading(false)
    }
  }, [activeFilters, page, rowsPerPage])

  useEffect(() => {
    void loadOrders()
  }, [loadOrders, refreshIndex])

  const applyFilters = (event: FormEvent) => {
    event.preventDefault()
    const nextFilters = { ...draftFilters, search: draftFilters.search.trim() }
    setPage(0)
    setActiveFilters(nextFilters)
    setRefreshIndex((value) => value + 1)
  }

  const resetFilters = () => {
    setDraftFilters(emptyFilters)
    setActiveFilters(emptyFilters)
    setPage(0)
    setRefreshIndex((value) => value + 1)
  }

  return (
    <Box>
      <PageIntro
        eyebrow="Quản trị"
        title="Quản lý đơn hàng"
        description="Tìm kiếm và theo dõi toàn bộ đơn hàng theo trạng thái hoặc thời gian đặt hàng."
      />

      <Card sx={{ mb: 3 }}>
        <CardContent component="form" onSubmit={applyFilters}>
          <Stack spacing={2}>
            <Stack direction={{ xs: 'column', md: 'row' }} spacing={2}>
              <TextField
                label="Tìm kiếm"
                placeholder="Mã đơn, tên khách hàng, số điện thoại"
                value={draftFilters.search}
                onChange={(event) => setDraftFilters((current) => ({ ...current, search: event.target.value }))}
                fullWidth
              />
              <FormControl fullWidth sx={{ minWidth: { md: 210 } }}>
                <InputLabel id="admin-order-status-label">Trạng thái</InputLabel>
                <Select
                  labelId="admin-order-status-label"
                  label="Trạng thái"
                  value={draftFilters.status}
                  onChange={(event) => setDraftFilters((current) => ({ ...current, status: event.target.value }))}
                >
                  {STATUS_OPTIONS.map((option) => <MenuItem key={option.value || 'all'} value={option.value}>{option.label}</MenuItem>)}
                </Select>
              </FormControl>
            </Stack>
            <Stack direction={{ xs: 'column', md: 'row' }} spacing={2} alignItems={{ md: 'center' }}>
              <TextField
                label="Từ ngày"
                type="date"
                value={draftFilters.fromDate}
                onChange={(event) => setDraftFilters((current) => ({ ...current, fromDate: event.target.value }))}
                slotProps={{ inputLabel: { shrink: true } }}
                fullWidth
              />
              <TextField
                label="Đến ngày"
                type="date"
                value={draftFilters.toDate}
                onChange={(event) => setDraftFilters((current) => ({ ...current, toDate: event.target.value }))}
                slotProps={{ inputLabel: { shrink: true } }}
                fullWidth
              />
              <Stack direction="row" spacing={1} sx={{ minWidth: 'max-content' }}>
                <Button type="submit" variant="contained" startIcon={<SearchIcon />}>Lọc đơn hàng</Button>
                <Button variant="outlined" onClick={resetFilters}>Đặt lại</Button>
              </Stack>
            </Stack>
          </Stack>
        </CardContent>
      </Card>

      {error && <Alert severity="error" sx={{ mb: 2 }} action={<Button color="inherit" size="small" onClick={() => void loadOrders()}>Thử lại</Button>}>{error}</Alert>}

      <Paper sx={{ overflow: 'hidden' }}>
        <TableContainer>
          <Table stickyHeader aria-label="Danh sách đơn hàng">
            <TableHead>
              <TableRow>
                <TableCell>Mã đơn</TableCell>
                <TableCell>Khách hàng</TableCell>
                <TableCell>Số điện thoại</TableCell>
                <TableCell align="right">Tổng tiền</TableCell>
                <TableCell>Trạng thái</TableCell>
                <TableCell>Ngày đặt</TableCell>
                <TableCell align="right">Thao tác</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {loading ? (
                <TableRow><TableCell colSpan={7} align="center" sx={{ py: 6 }}><CircularProgress size={32} /><Typography color="text.secondary" mt={1}>Đang tải danh sách đơn hàng...</Typography></TableCell></TableRow>
              ) : orders.length === 0 ? (
                <TableRow><TableCell colSpan={7} align="center" sx={{ py: 6 }}><Typography color="text.secondary">Không tìm thấy đơn hàng phù hợp.</Typography></TableCell></TableRow>
              ) : orders.map((order) => (
                <TableRow key={order.id} hover>
                  <TableCell sx={{ fontWeight: 700 }}>{order.orderNumber}</TableCell>
                  <TableCell>{order.customerName}</TableCell>
                  <TableCell>{order.customerPhone || '—'}</TableCell>
                  <TableCell align="right">{formatAmount(order.totalAmount)}</TableCell>
                  <TableCell><Chip size="small" color={statusColor(order.status)} label={statusLabels.get(order.status) || order.status} /></TableCell>
                  <TableCell>{formatDate(order.placedAt)}</TableCell>
                  <TableCell align="right"><Button component={RouterLink} to={`/admin/orders/${order.id}`} size="small">Xem chi tiết</Button></TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
        <TablePagination
          component="div"
          count={totalElements}
          page={page}
          rowsPerPage={rowsPerPage}
          rowsPerPageOptions={[10, 25, 50]}
          onPageChange={(_event, nextPage) => setPage(nextPage)}
          onRowsPerPageChange={(event) => { setRowsPerPage(Number(event.target.value)); setPage(0) }}
          labelRowsPerPage="Số dòng mỗi trang"
          labelDisplayedRows={({ from, to, count }) => `${from}-${to} trên ${count}`}
        />
      </Paper>
    </Box>
  )
}
