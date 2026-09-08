import { useCallback, useEffect, useState } from "react";
import { Alert, Box, Button, CircularProgress, Divider, Stack, Typography } from "@mui/material";
import EditOutlinedIcon from "@mui/icons-material/EditOutlined";
import ShoppingBagOutlinedIcon from "@mui/icons-material/ShoppingBagOutlined";
import { getCheckoutReview, type CheckoutReview, type PaymentOption } from "../../services/checkoutService";

const formatPrice = (value: number) => new Intl.NumberFormat("vi-VN", {
  style: "currency", currency: "VND",
}).format(value);

type Props = {
  addressId: number;
  paymentOption: PaymentOption;
  onEditAddress: () => void;
  onEditPayment: () => void;
};

export function OrderReviewStep({ addressId, paymentOption, onEditAddress, onEditPayment }: Props) {
  const [review, setReview] = useState<CheckoutReview | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadReview = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setReview(await getCheckoutReview(addressId, paymentOption.paymentMethod));
    } catch {
      setReview(null);
      setError("Không thể tải thông tin xem lại đơn hàng. Vui lòng thử lại.");
    } finally {
      setLoading(false);
    }
  }, [addressId, paymentOption.paymentMethod]);

  useEffect(() => { void loadReview(); }, [loadReview]);

  if (loading) return <Stack alignItems="center" py={5} spacing={2}>
    <CircularProgress /><Typography color="text.secondary">Đang kiểm tra đơn hàng...</Typography>
  </Stack>;
  if (error || !review) return <Alert severity="error" action={<Button onClick={loadReview}>Thử lại</Button>}>
    {error}
  </Alert>;

  const { cart, shippingAddress, paymentMethod, readyToPlaceOrder } = review;
  return <Stack spacing={3} data-testid="order-review-content">
    {!readyToPlaceOrder && <Alert severity="warning">Giỏ hàng có sản phẩm không còn đủ tồn kho. Vui lòng chỉnh sửa giỏ hàng.</Alert>}

    <Box>
      <Stack direction="row" justifyContent="space-between" alignItems="center" mb={1}>
        <Typography variant="h6">Sản phẩm ({cart.totalItems})</Typography>
      </Stack>
      <Divider />
      {cart.items.map((item) => <Stack key={item.id} direction="row" spacing={2} py={2} data-testid={`review-item-${item.id}`}>
        {item.imageUrl ? <Box component="img" src={item.imageUrl} alt={item.productName}
          sx={{ width: 72, height: 72, objectFit: "cover", borderRadius: 1 }} /> :
          <ShoppingBagOutlinedIcon sx={{ width: 72, height: 72, color: "text.disabled" }} />}
        <Box flex={1}>
          <Typography fontWeight={600}>{item.productName}</Typography>
          <Typography variant="body2" color="text.secondary">
            {[item.color, item.storage].filter(Boolean).join(" · ") || `SKU: ${item.sku}`}
          </Typography>
          <Typography variant="body2">{formatPrice(item.price)} × {item.quantity}</Typography>
        </Box>
        <Typography fontWeight={600}>{formatPrice(item.subtotal)}</Typography>
      </Stack>)}
    </Box>

    <Box>
      <Stack direction="row" justifyContent="space-between" alignItems="center">
        <Typography variant="h6">Địa chỉ giao hàng</Typography>
        <Button size="small" startIcon={<EditOutlinedIcon />} onClick={onEditAddress}>Thay đổi</Button>
      </Stack>
      <Typography fontWeight={600}>{shippingAddress.recipientName} · {shippingAddress.phone}</Typography>
      <Typography color="text.secondary">{shippingAddress.streetAddress}, {shippingAddress.ward}, {shippingAddress.district}, {shippingAddress.province}</Typography>
    </Box>

    <Box>
      <Stack direction="row" justifyContent="space-between" alignItems="center">
        <Typography variant="h6">Phương thức thanh toán</Typography>
        <Button size="small" startIcon={<EditOutlinedIcon />} onClick={onEditPayment}>Thay đổi</Button>
      </Stack>
      <Typography fontWeight={600}>{paymentMethod.label}</Typography>
      <Typography color="text.secondary">{paymentMethod.instructions}</Typography>
    </Box>

    <Box data-testid="review-totals">
      <Divider sx={{ mb: 2 }} />
      <Stack spacing={1}>
        <Stack direction="row" justifyContent="space-between"><Typography>Tạm tính</Typography><Typography>{formatPrice(cart.subtotal)}</Typography></Stack>
        <Stack direction="row" justifyContent="space-between"><Typography>Phí vận chuyển</Typography><Typography>{cart.shippingFee === 0 ? "Miễn phí" : formatPrice(cart.shippingFee)}</Typography></Stack>
        <Stack direction="row" justifyContent="space-between"><Typography>Giảm giá</Typography><Typography color={cart.discountAmount > 0 ? "error.main" : "inherit"}>-{formatPrice(cart.discountAmount)}</Typography></Stack>
        <Divider />
        <Stack direction="row" justifyContent="space-between"><Typography variant="h6">Tổng cộng</Typography><Typography variant="h6" color="primary" fontWeight={700}>{formatPrice(cart.total)}</Typography></Stack>
      </Stack>
    </Box>

    <Button variant="contained" size="large" disabled={!readyToPlaceOrder}
      data-testid="place-order-btn">Đặt hàng</Button>
  </Stack>;
}
