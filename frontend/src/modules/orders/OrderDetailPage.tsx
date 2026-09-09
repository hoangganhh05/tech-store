import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  CircularProgress,
  Divider,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from "@mui/material";
import { isAxiosError } from "axios";
import { useCallback, useEffect, useState } from "react";
import { Link as RouterLink, useParams } from "react-router-dom";
import { PageIntro } from "../../components/common/PageIntro";
import { ProductReviewDialog } from "../../components/common/ProductReviewDialog";
import RateReviewOutlinedIcon from "@mui/icons-material/RateReviewOutlined";
import {
  cancelOrder,
  getOrderDetail,
  type OrderDetail,
} from "../../services/orderService";

const statusLabels: Record<string, string> = {
  PENDING: "Chờ xác nhận",
  CONFIRMED: "Đã xác nhận",
  SHIPPING: "Đang giao",
  COMPLETED: "Hoàn thành",
  CANCELLED: "Đã huỷ",
};

const paymentMethodLabels: Record<string, string> = {
  COD: "Thanh toán khi nhận hàng",
  BANK_TRANSFER: "Chuyển khoản ngân hàng",
  ONLINE: "Thanh toán online",
};

const paymentStatusLabels: Record<string, string> = {
  UNPAID: "Chưa thanh toán",
  PENDING: "Đang xử lý thanh toán",
  PAID: "Đã thanh toán",
  FAILED: "Thanh toán thất bại",
  REFUNDED: "Đã hoàn tiền",
};

const standardSteps = ["PENDING", "CONFIRMED", "SHIPPING", "COMPLETED"];

function formatDate(value: string) {
  return new Intl.DateTimeFormat("vi-VN", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}

function formatAmount(value: number) {
  return new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
    maximumFractionDigits: 0,
  }).format(value);
}

function StatusTimeline({ order }: { order: OrderDetail }) {
  const historyByStatus = new Map(
    order.statusHistory.map((entry) => [entry.status, entry]),
  );
  const steps =
    order.status === "CANCELLED" ? ["PENDING", "CANCELLED"] : standardSteps;
  const currentIndex = steps.indexOf(order.status);

  return (
    <Stack spacing={0} aria-label="Tiến trình xử lý đơn hàng">
      {steps.map((status, index) => {
        const history = historyByStatus.get(status);
        const completed =
          history !== undefined ||
          (currentIndex >= index && order.status !== "CANCELLED");
        return (
          <Stack key={status} direction="row" spacing={2} alignItems="stretch">
            <Stack alignItems="center" sx={{ width: 24 }}>
              <Box
                sx={{
                  mt: 0.25,
                  width: 16,
                  height: 16,
                  borderRadius: "50%",
                  bgcolor: completed ? "primary.main" : "grey.300",
                  border: "3px solid",
                  borderColor: completed ? "primary.light" : "grey.200",
                }}
              />
              {index < steps.length - 1 && (
                <Box
                  sx={{
                    width: 2,
                    minHeight: 34,
                    bgcolor: completed ? "primary.light" : "grey.300",
                  }}
                />
              )}
            </Stack>
            <Box pb={index < steps.length - 1 ? 1.5 : 0}>
              <Typography fontWeight={completed ? 700 : 400}>
                {statusLabels[status]}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                {history
                  ? formatDate(history.changedAt)
                  : "Chưa đạt trạng thái này"}
              </Typography>
            </Box>
          </Stack>
        );
      })}
    </Stack>
  );
}

export function OrderDetailPage() {
  const { id } = useParams();
  const orderId = Number(id);
  const [order, setOrder] = useState<OrderDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [actionError, setActionError] = useState("");
  const [successMessage, setSuccessMessage] = useState("");
  const [cancelDialogOpen, setCancelDialogOpen] = useState(false);
  const [cancellationReason, setCancellationReason] = useState("");
  const [cancelling, setCancelling] = useState(false);
  const [reviewItem, setReviewItem] = useState<{
    productId: number;
    productName: string;
    variantLabel?: string;
  } | null>(null);

  const loadOrder = useCallback(
    async (isActive: () => boolean = () => true) => {
      if (!Number.isInteger(orderId) || orderId < 1) {
        setError("Mã đơn hàng không hợp lệ.");
        setLoading(false);
        return;
      }
      setLoading(true);
      setError("");
      try {
        const response = await getOrderDetail(orderId);
        if (isActive()) setOrder(response);
      } catch (requestError: unknown) {
        if (!isActive()) return;
        const message = isAxiosError<{ message?: string }>(requestError)
          ? requestError.response?.data?.message
          : undefined;
        setError(
          message || "Không thể tải chi tiết đơn hàng. Vui lòng thử lại.",
        );
      } finally {
        if (isActive()) setLoading(false);
      }
    },
    [orderId],
  );

  const handleCancel = async () => {
    if (!order) return;
    setCancelling(true);
    setActionError("");
    setSuccessMessage("");
    try {
      const cancellation = await cancelOrder(order.id, cancellationReason);
      setOrder((current) =>
        current
          ? {
              ...current,
              status: cancellation.status,
              cancellationReason: cancellation.cancellationReason,
              statusHistory: [
                ...current.statusHistory,
                {
                  status: cancellation.status,
                  changedAt: new Date().toISOString(),
                },
              ],
            }
          : current,
      );
      setCancelDialogOpen(false);
      setCancellationReason("");
      setSuccessMessage("Đơn hàng đã được huỷ và tồn kho đã được hoàn lại.");
    } catch (requestError: unknown) {
      const message = isAxiosError<{ message?: string }>(requestError)
        ? requestError.response?.data?.message
        : undefined;
      setActionError(message || "Không thể huỷ đơn hàng. Vui lòng thử lại.");
    } finally {
      setCancelling(false);
    }
  };

  useEffect(() => {
    let active = true;
    void loadOrder(() => active);
    return () => {
      active = false;
    };
  }, [loadOrder]);

  return (
    <Stack spacing={3}>
      <Stack
        direction={{ xs: "column", sm: "row" }}
        justifyContent="space-between"
        alignItems={{ xs: "flex-start", sm: "center" }}
        spacing={1}
      >
        <PageIntro
          eyebrow="Tài khoản"
          title="Chi tiết đơn hàng"
          description="Theo dõi sản phẩm, thanh toán và tiến trình xử lý đơn hàng."
        />
        <Button component={RouterLink} to="/account/orders" variant="outlined">
          Quay lại đơn hàng
        </Button>
      </Stack>

      {loading && (
        <Stack alignItems="center" spacing={1} py={8}>
          <CircularProgress size={32} />
          <Typography color="text.secondary">
            Đang tải chi tiết đơn hàng...
          </Typography>
        </Stack>
      )}

      {!loading && error && (
        <Stack alignItems="center" spacing={2} py={6}>
          <Alert severity="error">{error}</Alert>
          <Button variant="outlined" onClick={() => void loadOrder()}>
            Thử lại
          </Button>
        </Stack>
      )}

      {!loading && !error && order && (
        <>
          <Card>
            <CardContent>
              <Stack
                direction={{ xs: "column", sm: "row" }}
                justifyContent="space-between"
                spacing={2}
              >
                <Box>
                  <Typography variant="h6">{order.orderNumber}</Typography>
                  <Typography color="text.secondary">
                    Đặt lúc {formatDate(order.placedAt)}
                  </Typography>
                </Box>
                <Stack
                  direction="row"
                  spacing={1}
                  alignItems="center"
                  flexWrap="wrap"
                  useFlexGap
                >
                  <Chip
                    label={statusLabels[order.status] || order.status}
                    color={order.status === "CANCELLED" ? "error" : "primary"}
                  />
                  {["PENDING", "CONFIRMED"].includes(order.status) && (
                    <Button
                      color="error"
                      variant="outlined"
                      onClick={() => {
                        setActionError("");
                        setCancelDialogOpen(true);
                      }}
                    >
                      Huỷ đơn hàng
                    </Button>
                  )}
                </Stack>
              </Stack>
              {order.cancellationReason && (
                <Typography color="text.secondary" mt={1}>
                  Lý do huỷ: {order.cancellationReason}
                </Typography>
              )}
            </CardContent>
          </Card>

          {successMessage && <Alert severity="success">{successMessage}</Alert>}
          {actionError && <Alert severity="error">{actionError}</Alert>}

          <Card>
            <CardContent>
              <Typography variant="h6" component="h2" mb={2}>
                Sản phẩm đã đặt
              </Typography>
              <TableContainer>
                <Table aria-label="Sản phẩm trong đơn hàng">
                  <TableHead>
                    <TableRow>
                      <TableCell>Sản phẩm</TableCell>
                      <TableCell>Phân loại</TableCell>
                      <TableCell align="right">Đơn giá</TableCell>
                      <TableCell align="right">Số lượng</TableCell>
                      <TableCell align="right">Thành tiền</TableCell>
                      {order.status === "COMPLETED" && (
                        <TableCell align="center">Đánh giá</TableCell>
                      )}
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {order.items.map((item) => (
                      <TableRow key={`${item.sku}-${item.variantLabel}`}>
                        <TableCell>
                          <Typography fontWeight={600}>
                            {item.productName}
                          </Typography>
                          <Typography variant="body2" color="text.secondary">
                            SKU: {item.sku}
                          </Typography>
                        </TableCell>
                        <TableCell>{item.variantLabel || "—"}</TableCell>
                        <TableCell align="right">
                          {formatAmount(item.unitPrice)}
                        </TableCell>
                        <TableCell align="right">{item.quantity}</TableCell>
                        <TableCell align="right">
                          {formatAmount(item.subtotal)}
                        </TableCell>
                        {order.status === "COMPLETED" && (
                          <TableCell align="center">
                            <Button
                              size="small"
                              variant="outlined"
                              startIcon={
                                <RateReviewOutlinedIcon fontSize="small" />
                              }
                              onClick={() =>
                                setReviewItem({
                                  productId: item.productId || 0,
                                  productName: item.productName,
                                  variantLabel: item.variantLabel,
                                })
                              }
                            >
                              Đánh giá
                            </Button>
                          </TableCell>
                        )}
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
              <Stack alignItems="flex-end" spacing={0.5} mt={2}>
                <Typography>
                  Tiền hàng: {formatAmount(order.subtotal)}
                </Typography>
                <Typography>
                  Phí vận chuyển: {formatAmount(order.shippingFee)}
                </Typography>
                {order.discountAmount > 0 && (
                  <Typography>
                    Giảm giá: -{formatAmount(order.discountAmount)}
                  </Typography>
                )}
                <Typography variant="h6">
                  Tổng thanh toán: {formatAmount(order.totalAmount)}
                </Typography>
              </Stack>
            </CardContent>
          </Card>

          <Stack direction={{ xs: "column", md: "row" }} spacing={3}>
            <Card sx={{ flex: 1 }}>
              <CardContent>
                <Typography variant="h6" component="h2" mb={1.5}>
                  Địa chỉ giao hàng
                </Typography>
                <Typography fontWeight={600}>
                  {order.shippingAddress.recipientName}
                </Typography>
                <Typography>{order.shippingAddress.recipientPhone}</Typography>
                <Typography color="text.secondary">
                  {[
                    order.shippingAddress.line1,
                    order.shippingAddress.ward,
                    order.shippingAddress.district,
                    order.shippingAddress.province,
                  ]
                    .filter(Boolean)
                    .join(", ")}
                </Typography>
              </CardContent>
            </Card>
            <Card sx={{ flex: 1 }}>
              <CardContent>
                <Typography variant="h6" component="h2" mb={1.5}>
                  Thanh toán
                </Typography>
                <Typography>
                  {paymentMethodLabels[order.paymentMethod] ||
                    order.paymentMethod}
                </Typography>
                <Typography color="text.secondary">
                  {paymentStatusLabels[order.paymentStatus] ||
                    order.paymentStatus}
                </Typography>
              </CardContent>
            </Card>
          </Stack>

          <Card>
            <CardContent>
              <Typography variant="h6" component="h2" mb={2}>
                Tiến trình xử lý đơn hàng
              </Typography>
              <Divider sx={{ mb: 2 }} />
              <StatusTimeline order={order} />
            </CardContent>
          </Card>
        </>
      )}

      <Dialog
        open={cancelDialogOpen}
        onClose={() => !cancelling && setCancelDialogOpen(false)}
        fullWidth
        maxWidth="sm"
      >
        <DialogTitle>Huỷ đơn hàng</DialogTitle>
        <DialogContent>
          <Typography color="text.secondary" mb={2}>
            Bạn có chắc muốn huỷ đơn hàng này? Tồn kho sẽ được hoàn lại sau khi
            huỷ thành công.
          </Typography>
          <TextField
            autoFocus
            fullWidth
            multiline
            minRows={3}
            label="Lý do huỷ (tuỳ chọn)"
            value={cancellationReason}
            onChange={(event) => setCancellationReason(event.target.value)}
            inputProps={{ maxLength: 500 }}
            helperText={`${cancellationReason.length}/500`}
          />
        </DialogContent>
        <DialogActions>
          <Button
            onClick={() => setCancelDialogOpen(false)}
            disabled={cancelling}
          >
            Đóng
          </Button>
          <Button
            color="error"
            variant="contained"
            onClick={() => void handleCancel()}
            disabled={cancelling}
          >
            {cancelling ? "Đang huỷ..." : "Xác nhận huỷ"}
          </Button>
        </DialogActions>
      </Dialog>

      {reviewItem && (
        <ProductReviewDialog
          open={Boolean(reviewItem)}
          onClose={() => setReviewItem(null)}
          productId={reviewItem.productId}
          productName={reviewItem.productName}
          variantLabel={reviewItem.variantLabel}
          onSuccess={() => {
            setSuccessMessage("Đánh giá sản phẩm đã được gửi thành công!");
          }}
        />
      )}
    </Stack>
  );
}
