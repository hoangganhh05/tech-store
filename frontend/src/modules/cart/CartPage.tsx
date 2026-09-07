import { useState } from 'react'
import {
  Box,
  Button,
  Card,
  CardContent,
  Divider,
  Grid,
  IconButton,
  Stack,
  Typography,
  Chip,
  Alert,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Avatar,
} from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import RemoveIcon from '@mui/icons-material/Remove'
import ShoppingCartOutlinedIcon from '@mui/icons-material/ShoppingCartOutlined'
import ArrowBackIcon from '@mui/icons-material/ArrowBack'
import { Link } from 'react-router-dom'
import { PageIntro } from '../../components/common/PageIntro'
import { ROUTES } from '../../constants/routes'
import { useCart } from '../../hooks/useCart'

function formatPrice(val: number): string {
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val)
}

export function CartPage() {
  const { cart, updateQuantity } = useCart()
  const [updatingItemId, setUpdatingItemId] = useState<number | null>(null)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  const handleUpdateQuantity = async (itemId: number, newQty: number, maxStock: number) => {
    if (newQty < 1) return
    if (newQty > maxStock) {
      setErrorMessage(`Số lượng vượt quá tồn kho khả dụng (${maxStock})`)
      return
    }

    setUpdatingItemId(itemId)
    setErrorMessage(null)
    try {
      await updateQuantity(itemId, newQty)
    } catch (err: unknown) {
      const axiosErr = err as { response?: { data?: { message?: string } } }
      setErrorMessage(axiosErr?.response?.data?.message || 'Không thể cập nhật số lượng. Vui lòng thử lại.')
    } finally {
      setUpdatingItemId(null)
    }
  }

  const isEmpty = !cart || !cart.items || cart.items.length === 0

  return (
    <Box sx={{ py: 3 }} data-testid="cart-page">
      <PageIntro title="Giỏ hàng" description="Kiểm tra sản phẩm và số lượng trước khi đặt hàng." />

      {errorMessage && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setErrorMessage(null)} data-testid="cart-error-alert">
          {errorMessage}
        </Alert>
      )}

      {isEmpty ? (
        <Card data-testid="empty-cart-card">
          <CardContent sx={{ textAlign: 'center', py: 6 }}>
            <ShoppingCartOutlinedIcon sx={{ fontSize: 64, color: 'text.secondary', mb: 2 }} />
            <Typography variant="h6" color="text.secondary" gutterBottom data-testid="empty-cart-message">
              Giỏ hàng của bạn đang trống.
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
              Hãy chọn những sản phẩm công nghệ tuyệt vời để thêm vào giỏ nhé!
            </Typography>
            <Button
              component={Link}
              to={ROUTES.products}
              variant="contained"
              startIcon={<ArrowBackIcon />}
              data-testid="continue-shopping-btn"
            >
              Tiếp tục mua sắm
            </Button>
          </CardContent>
        </Card>
      ) : (
        <Grid container spacing={3}>
          <Grid size={{ xs: 12, md: 8 }}>
            <Card>
              <TableContainer>
                <Table data-testid="cart-table">
                  <TableHead>
                    <TableRow>
                      <TableCell>Sản phẩm</TableCell>
                      <TableCell align="center">Đơn giá</TableCell>
                      <TableCell align="center">Số lượng</TableCell>
                      <TableCell align="right">Thành tiền</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {cart.items.map((item) => {
                      const isMaxStock = item.quantity >= item.availableStock
                      const isMinQuantity = item.quantity <= 1
                      const isUpdating = updatingItemId === item.id

                      return (
                        <TableRow key={item.id} data-testid={`cart-item-${item.id}`}>
                          <TableCell>
                            <Stack direction="row" spacing={2} alignItems="center">
                              <Avatar
                                variant="rounded"
                                src={item.imageUrl || ''}
                                alt={item.productName}
                                sx={{ width: 64, height: 64, bgcolor: 'grey.100' }}
                              />
                              <Box>
                                <Typography
                                  component={Link}
                                  to={`/products/${item.productId}`}
                                  variant="subtitle2"
                                  data-testid={`item-name-${item.id}`}
                                  sx={{
                                    textDecoration: 'none',
                                    color: 'inherit',
                                    fontWeight: 600,
                                    '&:hover': { color: 'primary.main' },
                                  }}
                                >
                                  {item.productName}
                                </Typography>
                                <Stack direction="row" spacing={1} sx={{ mt: 0.5 }}>
                                  {item.color && (
                                    <Chip size="small" label={item.color} variant="outlined" sx={{ height: 22 }} />
                                  )}
                                  {item.storage && (
                                    <Chip size="small" label={item.storage} variant="outlined" sx={{ height: 22 }} />
                                  )}
                                </Stack>
                                {isMaxStock && (
                                  <Typography variant="caption" color="warning.main" display="block" sx={{ mt: 0.5 }}>
                                    Đã đạt giới hạn tồn kho ({item.availableStock})
                                  </Typography>
                                )}
                              </Box>
                            </Stack>
                          </TableCell>
                          <TableCell align="center">
                            <Typography variant="body2" fontWeight={600} data-testid={`item-price-${item.id}`}>
                              {formatPrice(item.price)}
                            </Typography>
                          </TableCell>
                          <TableCell align="center">
                            <Stack
                              direction="row"
                              alignItems="center"
                              justifyContent="center"
                              spacing={0.5}
                              sx={{
                                border: '1px solid',
                                borderColor: 'divider',
                                borderRadius: 1,
                                px: 0.5,
                                py: 0.25,
                                width: 'fit-content',
                                mx: 'auto',
                              }}
                            >
                              <IconButton
                                size="small"
                                disabled={isMinQuantity || isUpdating}
                                onClick={() => handleUpdateQuantity(item.id, item.quantity - 1, item.availableStock)}
                                data-testid={`decrease-qty-btn-${item.id}`}
                                aria-label="Giảm số lượng"
                              >
                                <RemoveIcon fontSize="small" />
                              </IconButton>
                              <Typography
                                data-testid={`item-qty-${item.id}`}
                                sx={{ minWidth: 32, textAlign: 'center', fontWeight: 600 }}
                              >
                                {item.quantity}
                              </Typography>
                              <IconButton
                                size="small"
                                disabled={isMaxStock || isUpdating}
                                onClick={() => handleUpdateQuantity(item.id, item.quantity + 1, item.availableStock)}
                                data-testid={`increase-qty-btn-${item.id}`}
                                aria-label="Tăng số lượng"
                              >
                                <AddIcon fontSize="small" />
                              </IconButton>
                            </Stack>
                          </TableCell>
                          <TableCell align="right">
                            <Typography
                              variant="body1"
                              fontWeight={700}
                              color="primary.main"
                              data-testid={`item-subtotal-${item.id}`}
                            >
                              {formatPrice(item.subtotal)}
                            </Typography>
                          </TableCell>
                        </TableRow>
                      )
                    })}
                  </TableBody>
                </Table>
              </TableContainer>
            </Card>
          </Grid>

          <Grid size={{ xs: 12, md: 4 }}>
            <Card data-testid="cart-summary">
              <CardContent>
                <Typography variant="h6" fontWeight={700} gutterBottom>
                  Tóm tắt đơn hàng
                </Typography>
                <Divider sx={{ my: 1.5 }} />
                <Stack spacing={1.5}>
                  <Stack direction="row" justifyContent="space-between">
                    <Typography color="text.secondary">Tổng số lượng:</Typography>
                    <Typography fontWeight={600} data-testid="cart-total-items">
                      {cart.totalItems} sản phẩm
                    </Typography>
                  </Stack>
                  <Stack direction="row" justifyContent="space-between">
                    <Typography color="text.secondary">Tạm tính:</Typography>
                    <Typography fontWeight={600} data-testid="cart-subtotal">
                      {formatPrice(cart.subtotal)}
                    </Typography>
                  </Stack>
                  <Stack direction="row" justifyContent="space-between">
                    <Typography color="text.secondary">Phí vận chuyển:</Typography>
                    <Typography variant="body2" color="text.secondary">
                      Tính khi thanh toán
                    </Typography>
                  </Stack>
                  <Divider sx={{ my: 1 }} />
                  <Stack direction="row" justifyContent="space-between" alignItems="baseline">
                    <Typography variant="subtitle1" fontWeight={700}>
                      Tổng cộng:
                    </Typography>
                    <Typography variant="h6" fontWeight={700} color="primary.main" data-testid="cart-total">
                      {formatPrice(cart.subtotal)}
                    </Typography>
                  </Stack>
                </Stack>
                <Button
                  component={Link}
                  to={ROUTES.checkout}
                  variant="contained"
                  fullWidth
                  size="large"
                  sx={{ mt: 3 }}
                  data-testid="checkout-btn"
                >
                  Tiến hành thanh toán
                </Button>
                <Button
                  component={Link}
                  to={ROUTES.products}
                  variant="outlined"
                  fullWidth
                  sx={{ mt: 1.5 }}
                  data-testid="continue-shopping-btn"
                >
                  Tiếp tục mua sắm
                </Button>
              </CardContent>
            </Card>
          </Grid>
        </Grid>
      )}
    </Box>
  )
}
