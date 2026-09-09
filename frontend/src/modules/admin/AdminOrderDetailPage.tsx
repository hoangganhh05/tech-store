import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  CircularProgress,
  Divider,
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
  TableRow,
  TextField,
  Typography,
} from "@mui/material";
import { isAxiosError } from "axios";
import { useCallback, useEffect, useState } from "react";
import { Link as RouterLink, useParams } from "react-router-dom";
import { PageIntro } from "../../components/common/PageIntro";
import { ROUTES } from "../../constants/routes";
import {
  getAdminOrderDetail,
  updateAdminOrderStatus,
  type AdminOrderDetail,
} from "../../services/adminOrderService";

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

const nextStatusOptions: Record<
  string,
  Array<{ value: string; label: string }>
> = {
  PENDING: [
    { value: "CONFIRMED", label: "Xác nhận đơn hàng" },
    { value: "CANCELLED", label: "Huỷ đơn hàng" },
  ],
  CONFIRMED: [
    { value: "SHIPPING", label: "Chuyển sang đang giao" },
    { value: "CANCELLED", label: "Huỷ đơn hàng" },
  ],
  SHIPPING: [{ value: "COMPLETED", label: "Hoàn thành đơn hàng" }],
};

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

function statusColor(
  status: string,
): "default" | "info" | "warning" | "success" | "error" {
  if (status === "CONFIRMED") return "info";
  if (status === "SHIPPING") return "warning";
  if (status === "COMPLETED") return "success";
  if (status === "CANCELLED") return "error";
  return "default";
}

function StatusTimeline({ order }: { order: AdminOrderDetail }) {
  const historyByStatus = new Map(
    order.statusHistory.map((entry) => [entry.status, entry]),
  );
  const steps =
    order.status === "CANCELLED" ? ["PENDING", "CANCELLED"] : standardSteps;
  const currentIndex = steps.indexOf(order.status);

  return (
    <Stack spacing={0} aria-label="Lịch sử trạng thái đơn hàng">
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
                {statusLabels[status] || status}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                {history
                  ? `${formatDate(history.changedAt)} · ${history.changedBy ? `Cập nhật bởi ${history.changedBy.fullName}` : "Khởi tạo bởi hệ thống"}`
                  : "Chưa đạt trạng thái này"}
              </Typography>
            </Box>
          </Stack>
        );
      })}
    </Stack>
  );
}

export function AdminOrderDetailPage() {
  const { id } = useParams();
  const orderId = Number(id);
  const [order, setOrder] = useState<AdminOrderDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [selectedStatus, setSelectedStatus] = useState("");
  const [cancelReason, setCancelReason] = useState("");
  const [updatingStatus, setUpdatingStatus] = useState(false);
  const [statusActionError, setStatusActionError] = useState("");
  const [statusSuccessMessage, setStatusSuccessMessage] = useState("");

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
        const response = await getAdminOrderDetail(orderId);
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

  useEffect(() => {
    let active = true;
    void loadOrder(() => active);
    return () => {
      active = false;
    };
  }, [loadOrder]);

  const updateStatus = async () => {
    if (!order || !selectedStatus) return;
    setUpdatingStatus(true);
    setStatusActionError("");
    setStatusSuccessMessage("");
    try {
      const trimmedReason = cancelReason.trim();
      const response =
        selectedStatus === "CANCELLED" && trimmedReason
          ? await updateAdminOrderStatus(
              order.id,
              selectedStatus,
              trimmedReason,
            )
          : await updateAdminOrderStatus(order.id, selectedStatus);
      setOrder(response);
      setSelectedStatus("");
      setCancelReason("");
      setStatusSuccessMessage("Đã cập nhật trạng thái đơn hàng.");
    } catch (requestError: unknown) {
      const message = isAxiosError<{ message?: string }>(requestError)
        ? requestError.response?.data?.message
        : undefined;
      setStatusActionError(
        message || "Không thể cập nhật trạng thái đơn hàng. Vui lòng thử lại.",
      );
    } finally {
      setUpdatingStatus(false);
    }
  };

  return (
    <Stack spacing={3}>
      <Stack
        direction={{ xs: "column", sm: "row" }}
        justifyContent="space-between"
        alignItems={{ xs: "flex-start", sm: "center" }}
        spacing={1}
      >
        <PageIntro
          eyebrow="Quản trị"
          title="Chi tiết đơn hàng"
          description="Kiểm tra đầy đủ thông tin đơn hàng trước khi xử lý."
        />
        <Button
          component={RouterLink}
          to={ROUTES.adminOrders}
          variant="outlined"
        >
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
                <Chip
                  label={statusLabels[order.status] || order.status}
                  color={statusColor(order.status)}
                />
              </Stack>
              {order.cancellationReason && (
                <Typography color="text.secondary" mt={1}>
                  Lý do huỷ: {order.cancellationReason}
                </Typography>
              )}
            </CardContent>
          </Card>

          {statusSuccessMessage && (
            <Alert severity="success">{statusSuccessMessage}</Alert>
          )}
          {statusActionError && (
            <Alert severity="error">{statusActionError}</Alert>
          )}

          <Card>
            <CardContent>
              <Typography variant="h6" component="h2" mb={0.5}>
                Cập nhật trạng thái
              </Typography>
              <Typography color="text.secondary" variant="body2" mb={2}>
                Chỉ các bước chuyển tiếp hợp lệ của đơn hàng hiện tại mới được
                hiển thị.
              </Typography>
              {(nextStatusOptions[order.status] || []).length > 0 ? (
                <Stack spacing={2}>
                  <Stack
                    direction={{ xs: "column", sm: "row" }}
                    spacing={2}
                    alignItems={{ sm: "center" }}
                  >
                    <FormControl fullWidth>
                      <InputLabel id="admin-order-next-status-label">
                        Trạng thái mới
                      </InputLabel>
                      <Select
                        labelId="admin-order-next-status-label"
                        label="Trạng thái mới"
                        value={selectedStatus}
                        onChange={(event) => {
                          setSelectedStatus(event.target.value);
                          setCancelReason("");
                        }}
                        disabled={updatingStatus}
                      >
                        {(nextStatusOptions[order.status] || []).map(
                          (option) => (
                            <MenuItem key={option.value} value={option.value}>
                              {option.label}
                            </MenuItem>
                          ),
                        )}
                      </Select>
                    </FormControl>
                    <Button
                      variant="contained"
                      color={
                        selectedStatus === "CANCELLED" ? "error" : "primary"
                      }
                      onClick={() => void updateStatus()}
                      disabled={!selectedStatus || updatingStatus}
                      sx={{ minWidth: { sm: 180 } }}
                    >
                      {updatingStatus
                        ? "Đang cập nhật..."
                        : "Cập nhật trạng thái"}
                    </Button>
                  </Stack>
                  {selectedStatus === "CANCELLED" && (
                    <TextField
                      label="Lý do huỷ đơn (tuỳ chọn)"
                      placeholder="Nhập lý do quản trị viên huỷ đơn hàng..."
                      multiline
                      minRows={2}
                      maxRows={5}
                      fullWidth
                      value={cancelReason}
                      onChange={(event) => setCancelReason(event.target.value)}
                      disabled={updatingStatus}
                      inputProps={{ maxLength: 500 }}
                      helperText={`${cancelReason.length}/500 ký tự`}
                      color="error"
                    />
                  )}
                </Stack>
              ) : (
                <Typography color="text.secondary">
                  Đơn hàng ở trạng thái cuối, không thể cập nhật thêm.
                </Typography>
              )}
            </CardContent>
          </Card>

          <Stack direction={{ xs: "column", md: "row" }} spacing={3}>
            <Card sx={{ flex: 1 }}>
              <CardContent>
                <Typography variant="h6" component="h2" mb={1.5}>
                  Khách hàng
                </Typography>
                <Typography fontWeight={600}>
                  {order.customer.fullName}
                </Typography>
                <Typography>
                  {order.customer.phone || "Chưa cập nhật số điện thoại"}
                </Typography>
                <Typography color="text.secondary">
                  {order.customer.email}
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
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {order.items.map((item) => (
                      <TableRow key={`${item.sku}-${item.variantLabel || ""}`}>
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
                {order.shippingAddress ? (
                  <>
                    <Typography fontWeight={600}>
                      {order.shippingAddress.recipientName}
                    </Typography>
                    <Typography>
                      {order.shippingAddress.recipientPhone}
                    </Typography>
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
                  </>
                ) : (
                  <Typography color="text.secondary">
                    Không có thông tin địa chỉ giao hàng.
                  </Typography>
                )}
              </CardContent>
            </Card>
            <Card sx={{ flex: 1 }}>
              <CardContent>
                <Typography variant="h6" component="h2" mb={1.5}>
                  Ghi chú nội bộ
                </Typography>
                <Typography
                  color={order.internalNote ? "text.primary" : "text.secondary"}
                >
                  {order.internalNote || "Không có ghi chú nội bộ."}
                </Typography>
              </CardContent>
            </Card>
          </Stack>

          <Card>
            <CardContent>
              <Typography variant="h6" component="h2" mb={2}>
                Lịch sử trạng thái
              </Typography>
              <Divider sx={{ mb: 2 }} />
              <StatusTimeline order={order} />
            </CardContent>
          </Card>
        </>
      )}
    </Stack>
  );
}
