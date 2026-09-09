import { useState } from "react";
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
  Snackbar,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Avatar,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogContentText,
  DialogActions,
  Tooltip,
} from "@mui/material";
import AddIcon from "@mui/icons-material/Add";
import RemoveIcon from "@mui/icons-material/Remove";
import DeleteOutlineIcon from "@mui/icons-material/DeleteOutline";
import ShoppingCartOutlinedIcon from "@mui/icons-material/ShoppingCartOutlined";
import ArrowBackIcon from "@mui/icons-material/ArrowBack";
import { Link } from "react-router-dom";
import { PageIntro } from "../../components/common/PageIntro";
import { ROUTES } from "../../constants/routes";
import { useCart } from "../../hooks/useCart";
import type { CartItem } from "../../services/cartService";

function formatPrice(val: number): string {
  return new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
  }).format(val);
}

export function CartPage() {
  const { cart, updateQuantity, removeCartItem } = useCart();
  const [updatingItemId, setUpdatingItemId] = useState<number | null>(null);
  const [deleting, setDeleting] = useState<boolean>(false);
  const [itemToDelete, setItemToDelete] = useState<CartItem | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  const handleUpdateQuantity = async (
    itemId: number,
    newQty: number,
    maxStock: number,
  ) => {
    if (newQty < 1) return;
    if (newQty > maxStock) {
      setErrorMessage(`Số lượng vượt quá tồn kho khả dụng (${maxStock})`);
      return;
    }

    setUpdatingItemId(itemId);
    setErrorMessage(null);
    try {
      await updateQuantity(itemId, newQty);
    } catch (err: unknown) {
      const axiosErr = err as { response?: { data?: { message?: string } } };
      setErrorMessage(
        axiosErr?.response?.data?.message ||
          "Không thể cập nhật số lượng. Vui lòng thử lại.",
      );
    } finally {
      setUpdatingItemId(null);
    }
  };

  const handleOpenDeleteDialog = (item: CartItem) => {
    setItemToDelete(item);
  };

  const handleCloseDeleteDialog = () => {
    if (deleting) return;
    setItemToDelete(null);
  };

  const handleConfirmDelete = async () => {
    if (!itemToDelete) return;
    setDeleting(true);
    setErrorMessage(null);
    try {
      await removeCartItem(itemToDelete.id);
      setToastMessage("Đã xoá sản phẩm khỏi giỏ hàng thành công!");
      setItemToDelete(null);
    } catch (err: unknown) {
      const axiosErr = err as { response?: { data?: { message?: string } } };
      setErrorMessage(
        axiosErr?.response?.data?.message ||
          "Không thể xoá sản phẩm. Vui lòng thử lại.",
      );
    } finally {
      setDeleting(false);
    }
  };

  const isEmpty = !cart || !cart.items || cart.items.length === 0;
  const isCheckoutDisabled = Boolean(
    !cart ||
      cart.totalItems === 0 ||
      cart.hasStockIssue ||
      cart.canCheckout === false,
  );

  return (
    <Box sx={{ py: 3 }} data-testid="cart-page">
      <PageIntro
        title="Giỏ hàng"
        description="Kiểm tra sản phẩm và số lượng trước khi đặt hàng."
      />

      {errorMessage && (
        <Alert
          severity="error"
          sx={{ mb: 2 }}
          onClose={() => setErrorMessage(null)}
          data-testid="cart-error-alert"
        >
          {errorMessage}
        </Alert>
      )}

      <Snackbar
        open={!!toastMessage}
        autoHideDuration={4000}
        onClose={() => setToastMessage(null)}
        anchorOrigin={{ vertical: "top", horizontal: "center" }}
      >
        <Alert
          onClose={() => setToastMessage(null)}
          severity="success"
          sx={{ width: "100%" }}
          data-testid="cart-toast"
        >
          {toastMessage}
        </Alert>
      </Snackbar>

      {cart && cart.hasStockIssue && (
        <Alert severity="error" sx={{ mb: 3 }} data-testid="cart-stock-alert">
          Một số sản phẩm trong giỏ hàng đã thay đổi tồn kho hoặc hết hàng. Vui
          lòng kiểm tra và cập nhật số lượng trước khi tiến hành thanh toán.
        </Alert>
      )}

      {isEmpty ? (
        <Card data-testid="empty-cart-card">
          <CardContent sx={{ textAlign: "center", py: 6 }}>
            <ShoppingCartOutlinedIcon
              sx={{ fontSize: 64, color: "text.secondary", mb: 2 }}
            />
            <Typography
              variant="h6"
              color="text.secondary"
              gutterBottom
              data-testid="empty-cart-message"
            >
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
                      <TableCell align="center" sx={{ width: 60 }}></TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {cart.items.map((item) => {
                      const isOutOfStock = item.availableStock <= 0;
                      const isExceedStock = item.quantity > item.availableStock;
                      const hasStockIssue = Boolean(
                        item.hasStockIssue || isOutOfStock || isExceedStock,
                      );
                      const isMaxStock = item.quantity >= item.availableStock;
                      const isMinQuantity = item.quantity <= 1;
                      const isUpdating = updatingItemId === item.id;

                      return (
                        <TableRow
                          key={item.id}
                          data-testid={`cart-item-${item.id}`}
                          sx={
                            hasStockIssue
                              ? { bgcolor: "action.hover" }
                              : undefined
                          }
                        >
                          <TableCell>
                            <Stack
                              direction="row"
                              spacing={2}
                              alignItems="center"
                            >
                              <Avatar
                                variant="rounded"
                                src={item.imageUrl || ""}
                                alt={item.productName}
                                sx={{
                                  width: 64,
                                  height: 64,
                                  bgcolor: "grey.100",
                                }}
                              />
                              <Box>
                                <Typography
                                  component={Link}
                                  to={`/products/${item.productId}`}
                                  variant="subtitle2"
                                  data-testid={`item-name-${item.id}`}
                                  sx={{
                                    textDecoration: "none",
                                    color: "inherit",
                                    fontWeight: 600,
                                    "&:hover": { color: "primary.main" },
                                  }}
                                >
                                  {item.productName}
                                </Typography>
                                <Stack
                                  direction="row"
                                  spacing={1}
                                  sx={{ mt: 0.5 }}
                                >
                                  {item.color && (
                                    <Chip
                                      size="small"
                                      label={item.color}
                                      variant="outlined"
                                      sx={{ height: 22 }}
                                    />
                                  )}
                                  {item.storage && (
                                    <Chip
                                      size="small"
                                      label={item.storage}
                                      variant="outlined"
                                      sx={{ height: 22 }}
                                    />
                                  )}
                                  {isOutOfStock && (
                                    <Chip
                                      size="small"
                                      label="Hết hàng"
                                      color="error"
                                      data-testid={`item-out-of-stock-chip-${item.id}`}
                                      sx={{ height: 22, fontWeight: 700 }}
                                    />
                                  )}
                                  {!isOutOfStock && isExceedStock && (
                                    <Chip
                                      size="small"
                                      label="Vượt tồn kho"
                                      color="warning"
                                      data-testid={`item-exceed-stock-chip-${item.id}`}
                                      sx={{ height: 22, fontWeight: 700 }}
                                    />
                                  )}
                                </Stack>
                                {isOutOfStock && (
                                  <Typography
                                    variant="caption"
                                    color="error.main"
                                    display="block"
                                    data-testid={`stock-error-msg-${item.id}`}
                                    sx={{ mt: 0.5, fontWeight: 600 }}
                                  >
                                    {item.stockStatusMessage ||
                                      "Sản phẩm hiện đã hết hàng. Vui lòng xoá khỏi giỏ hàng."}
                                  </Typography>
                                )}
                                {!isOutOfStock && isExceedStock && (
                                  <Typography
                                    variant="caption"
                                    color="error.main"
                                    display="block"
                                    data-testid={`stock-error-msg-${item.id}`}
                                    sx={{ mt: 0.5, fontWeight: 600 }}
                                  >
                                    {item.stockStatusMessage ||
                                      `Số lượng trong giỏ (${item.quantity}) vượt quá tồn kho hiện tại (${item.availableStock}). Vui lòng giảm số lượng.`}
                                  </Typography>
                                )}
                                {!hasStockIssue && isMaxStock && (
                                  <Typography
                                    variant="caption"
                                    color="warning.main"
                                    display="block"
                                    sx={{ mt: 0.5 }}
                                  >
                                    Đã đạt giới hạn tồn kho (
                                    {item.availableStock})
                                  </Typography>
                                )}
                              </Box>
                            </Stack>
                          </TableCell>
                          <TableCell align="center">
                            <Typography
                              variant="body2"
                              fontWeight={600}
                              data-testid={`item-price-${item.id}`}
                            >
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
                                border: "1px solid",
                                borderColor: "divider",
                                borderRadius: 1,
                                px: 0.5,
                                py: 0.25,
                                width: "fit-content",
                                mx: "auto",
                              }}
                            >
                              <IconButton
                                size="small"
                                disabled={isMinQuantity || isUpdating}
                                onClick={() =>
                                  handleUpdateQuantity(
                                    item.id,
                                    item.quantity - 1,
                                    item.availableStock,
                                  )
                                }
                                data-testid={`decrease-qty-btn-${item.id}`}
                                aria-label="Giảm số lượng"
                              >
                                <RemoveIcon fontSize="small" />
                              </IconButton>
                              <Typography
                                data-testid={`item-qty-${item.id}`}
                                sx={{
                                  minWidth: 32,
                                  textAlign: "center",
                                  fontWeight: 600,
                                }}
                              >
                                {item.quantity}
                              </Typography>
                              <IconButton
                                size="small"
                                disabled={
                                  isMaxStock || isUpdating || isOutOfStock
                                }
                                onClick={() =>
                                  handleUpdateQuantity(
                                    item.id,
                                    item.quantity + 1,
                                    item.availableStock,
                                  )
                                }
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
                          <TableCell align="center">
                            <Tooltip title="Xoá sản phẩm">
                              <IconButton
                                size="small"
                                color="error"
                                onClick={() => handleOpenDeleteDialog(item)}
                                data-testid={`remove-item-btn-${item.id}`}
                                aria-label={`Xoá ${item.productName}`}
                              >
                                <DeleteOutlineIcon fontSize="small" />
                              </IconButton>
                            </Tooltip>
                          </TableCell>
                        </TableRow>
                      );
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
                    <Typography color="text.secondary">
                      Tổng số lượng:
                    </Typography>
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
                  <Stack
                    direction="row"
                    justifyContent="space-between"
                    alignItems="center"
                  >
                    <Typography color="text.secondary">
                      Phí vận chuyển dự kiến:
                    </Typography>
                    {cart.shippingFee === 0 ? (
                      <Typography
                        fontWeight={600}
                        color="success.main"
                        data-testid="cart-shipping-fee"
                      >
                        Miễn phí
                      </Typography>
                    ) : (
                      <Typography
                        fontWeight={600}
                        data-testid="cart-shipping-fee"
                      >
                        {formatPrice(cart.shippingFee)}
                      </Typography>
                    )}
                  </Stack>

                  {cart.discountAmount > 0 && (
                    <Stack direction="row" justifyContent="space-between">
                      <Typography color="text.secondary">
                        Giảm giá voucher:
                      </Typography>
                      <Typography
                        fontWeight={600}
                        color="error.main"
                        data-testid="cart-discount"
                      >
                        -{formatPrice(cart.discountAmount)}
                      </Typography>
                    </Stack>
                  )}

                  <Divider sx={{ my: 1 }} />
                  <Stack
                    direction="row"
                    justifyContent="space-between"
                    alignItems="baseline"
                  >
                    <Typography variant="subtitle1" fontWeight={700}>
                      Tổng cộng:
                    </Typography>
                    <Typography
                      variant="h6"
                      fontWeight={700}
                      color="primary.main"
                      data-testid="cart-total"
                    >
                      {formatPrice(
                        cart.total ?? cart.subtotal + (cart.shippingFee || 0),
                      )}
                    </Typography>
                  </Stack>
                </Stack>
                <Tooltip
                  title={
                    cart.hasStockIssue
                      ? "Vui lòng điều chỉnh số lượng các sản phẩm vượt tồn kho hoặc xoá sản phẩm hết hàng trước khi thanh toán"
                      : ""
                  }
                >
                  <span>
                    <Button
                      component={isCheckoutDisabled ? "button" : Link}
                      to={isCheckoutDisabled ? undefined : ROUTES.checkout}
                      variant="contained"
                      fullWidth
                      size="large"
                      disabled={isCheckoutDisabled}
                      sx={{ mt: 3 }}
                      data-testid="checkout-btn"
                    >
                      Tiến hành thanh toán
                    </Button>
                  </span>
                </Tooltip>
                {cart.hasStockIssue && (
                  <Typography
                    variant="caption"
                    color="error.main"
                    data-testid="checkout-disabled-reason"
                    sx={{ mt: 1, display: "block", textAlign: "center" }}
                  >
                    Vui lòng xử lý các sản phẩm hết hàng hoặc vượt tồn kho để
                    tiếp tục thanh toán.
                  </Typography>
                )}
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

      {/* Confirmation Dialog */}
      <Dialog
        open={!!itemToDelete}
        onClose={handleCloseDeleteDialog}
        data-testid="delete-confirm-dialog"
      >
        <DialogTitle>Xác nhận xoá sản phẩm</DialogTitle>
        <DialogContent>
          <DialogContentText>
            Bạn có chắc chắn muốn xoá sản phẩm{" "}
            <strong>{itemToDelete?.productName}</strong>
            {itemToDelete && (itemToDelete.color || itemToDelete.storage) ? (
              <>
                {" "}
                (
                {[itemToDelete.color, itemToDelete.storage]
                  .filter(Boolean)
                  .join(" - ")}
                )
              </>
            ) : null}{" "}
            khỏi giỏ hàng không?
          </DialogContentText>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button
            onClick={handleCloseDeleteDialog}
            disabled={deleting}
            data-testid="cancel-delete-btn"
          >
            Huỷ
          </Button>
          <Button
            onClick={handleConfirmDelete}
            color="error"
            variant="contained"
            disabled={deleting}
            data-testid="confirm-delete-btn"
          >
            {deleting ? "Đang xoá..." : "Xác nhận xoá"}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
