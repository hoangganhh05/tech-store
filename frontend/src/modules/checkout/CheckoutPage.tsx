import { useState, useEffect, useCallback } from "react";
import {
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  CircularProgress,
  Divider,
  Grid,
  Radio,
  Stack,
  Step,
  StepLabel,
  Stepper,
  Typography,
  Alert,
} from "@mui/material";
import AddIcon from "@mui/icons-material/Add";
import ArrowForwardIcon from "@mui/icons-material/ArrowForward";
import LocalShippingOutlinedIcon from "@mui/icons-material/LocalShippingOutlined";
import ShoppingBagOutlinedIcon from "@mui/icons-material/ShoppingBagOutlined";
import PaymentOutlinedIcon from "@mui/icons-material/PaymentOutlined";
import ReceiptLongOutlinedIcon from "@mui/icons-material/ReceiptLongOutlined";
import { Link } from "react-router-dom";
import { PageIntro } from "../../components/common/PageIntro";
import { ROUTES } from "../../constants/routes";
import { useCart } from "../../hooks/useCart";
import { getMyAddresses, type Address } from "../../services/userService";
import { AddressFormDialog } from "../profile/AddressFormDialog";
import { PaymentMethodStep } from "./PaymentMethodStep";
import type { PaymentOption } from "../../services/checkoutService";
import { OrderReviewStep } from "./OrderReviewStep";

function formatPrice(val: number): string {
  return new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
  }).format(val);
}

const steps = [
  "Địa chỉ giao hàng",
  "Phương thức thanh toán",
  "Xem lại & Đặt hàng",
];

export function CheckoutPage() {
  const { cart } = useCart();
  const [activeStep, setActiveStep] = useState<number>(0);
  const [paymentOption, setPaymentOption] = useState<PaymentOption | null>(null);

  // Addresses state
  const [addresses, setAddresses] = useState<Address[]>([]);
  const [selectedAddressId, setSelectedAddressId] = useState<number | null>(
    null,
  );
  const [isLoadingAddresses, setIsLoadingAddresses] = useState<boolean>(true);
  const [addressError, setAddressError] = useState<string | null>(null);

  // Address dialog
  const [formOpen, setFormOpen] = useState<boolean>(false);

  const loadAddresses = useCallback(async () => {
    setIsLoadingAddresses(true);
    setAddressError(null);
    try {
      const list = await getMyAddresses();
      setAddresses(list);
      if (list.length > 0) {
        // Tự động chọn địa chỉ mặc định, hoặc địa chỉ đầu tiên nếu không có mặc định
        const defaultAddr = list.find((a) => a.isDefault);
        const initialSelected = defaultAddr ? defaultAddr.id : list[0].id;
        setSelectedAddressId(initialSelected);
      } else {
        setSelectedAddressId(null);
      }
    } catch {
      setAddressError("Không thể tải danh sách địa chỉ. Vui lòng thử lại sau.");
    } finally {
      setIsLoadingAddresses(false);
    }
  }, []);

  useEffect(() => {
    loadAddresses();
  }, [loadAddresses]);

  const handleOpenAdd = () => {
    setFormOpen(true);
  };

  const handleAddressSaved = (newAddress: Address) => {
    setFormOpen(false);
    setAddresses((prev) => {
      const exists = prev.some((a) => a.id === newAddress.id);
      if (exists) {
        return prev.map((a) => (a.id === newAddress.id ? newAddress : a));
      }
      return newAddress.isDefault
        ? [newAddress, ...prev]
        : [...prev, newAddress];
    });
    // Tự động chọn địa chỉ mới vừa tạo
    setSelectedAddressId(newAddress.id);
  };

  const handleContinue = () => {
    if (!selectedAddressId) return;
    setActiveStep((prev) => Math.min(prev + 1, steps.length - 1));
  };

  const handleBack = () => {
    setActiveStep((prev) => Math.max(prev - 1, 0));
  };

  const selectedAddress =
    addresses.find((a) => a.id === selectedAddressId) || null;
  const isCartEmpty = !cart || cart.totalItems === 0;

  return (
    <>
      <PageIntro
        title="Thanh toán đơn hàng"
        description="Vui lòng kiểm tra địa chỉ giao hàng và hoàn tất đơn đặt hàng của bạn."
      />

      {/* Stepper Header */}
      <Card sx={{ mb: 4 }}>
        <CardContent sx={{ py: 3 }}>
          <Stepper activeStep={activeStep} alternativeLabel>
            {steps.map((label) => (
              <Step key={label}>
                <StepLabel>{label}</StepLabel>
              </Step>
            ))}
          </Stepper>
        </CardContent>
      </Card>

      {isCartEmpty ? (
        <Card sx={{ p: 4, textAlign: "center" }}>
          <Stack spacing={2} alignItems="center">
            <ShoppingBagOutlinedIcon
              sx={{ fontSize: 64, color: "text.disabled" }}
            />
            <Typography variant="h5">Giỏ hàng của bạn đang trống</Typography>
            <Typography color="text.secondary">
              Vui lòng thêm sản phẩm vào giỏ hàng trước khi tiến hành thanh
              toán.
            </Typography>
            <Button
              component={Link}
              to={ROUTES.products}
              variant="contained"
              size="large"
            >
              Xem danh sách sản phẩm
            </Button>
          </Stack>
        </Card>
      ) : (
        <Grid container spacing={4}>
          {/* Main Step Content (Left Column) */}
          <Grid size={{ xs: 12, md: 8 }}>
            {activeStep === 0 && (
              <Card data-testid="checkout-step-address">
                <CardContent sx={{ p: { xs: 2, sm: 3 } }}>
                  <Stack
                    direction="row"
                    justifyContent="space-between"
                    alignItems="center"
                    mb={2}
                  >
                    <Stack direction="row" spacing={1} alignItems="center">
                      <LocalShippingOutlinedIcon color="primary" />
                      <Typography variant="h6" fontWeight={600}>
                        1. Địa chỉ giao hàng
                      </Typography>
                    </Stack>
                    <Button
                      variant="outlined"
                      size="small"
                      startIcon={<AddIcon />}
                      onClick={handleOpenAdd}
                      data-testid="add-new-address-btn"
                    >
                      Thêm địa chỉ mới
                    </Button>
                  </Stack>

                  <Divider sx={{ mb: 3 }} />

                  {isLoadingAddresses && (
                    <Stack alignItems="center" py={4} spacing={2}>
                      <CircularProgress size={36} />
                      <Typography color="text.secondary">
                        Đang tải danh sách địa chỉ giao hàng...
                      </Typography>
                    </Stack>
                  )}

                  {addressError && (
                    <Alert
                      severity="error"
                      sx={{ mb: 2 }}
                      action={
                        <Button
                          color="inherit"
                          size="small"
                          onClick={loadAddresses}
                        >
                          Thử lại
                        </Button>
                      }
                    >
                      {addressError}
                    </Alert>
                  )}

                  {!isLoadingAddresses &&
                    !addressError &&
                    addresses.length === 0 && (
                      <Alert
                        severity="info"
                        sx={{ mb: 3 }}
                        action={
                          <Button
                            color="primary"
                            variant="contained"
                            size="small"
                            onClick={handleOpenAdd}
                            data-testid="add-first-address-btn"
                          >
                            Thêm địa chỉ
                          </Button>
                        }
                      >
                        Bạn chưa có địa chỉ giao hàng nào trong tài khoản. Vui
                        lòng thêm địa chỉ để tiếp tục đặt hàng.
                      </Alert>
                    )}

                  {!isLoadingAddresses && addresses.length > 0 && (
                    <Stack spacing={2} data-testid="saved-addresses-list">
                      {addresses.map((address) => {
                        const isSelected = selectedAddressId === address.id;
                        return (
                          <Card
                            key={address.id}
                            variant={isSelected ? "outlined" : "elevation"}
                            onClick={() => setSelectedAddressId(address.id)}
                            data-testid={`address-card-${address.id}`}
                            sx={{
                              p: 2,
                              cursor: "pointer",
                              border: isSelected ? "2px solid" : "1px solid",
                              borderColor: isSelected
                                ? "primary.main"
                                : "divider",
                              bgcolor: isSelected
                                ? "action.hover"
                                : "background.paper",
                              transition: "all 0.2s ease-in-out",
                              "&:hover": {
                                borderColor: "primary.main",
                              },
                            }}
                          >
                            <Stack
                              direction="row"
                              alignItems="flex-start"
                              spacing={1.5}
                            >
                              <Radio
                                checked={isSelected}
                                onChange={() =>
                                  setSelectedAddressId(address.id)
                                }
                                value={address.id}
                                name="shipping-address-radio"
                                data-testid={`address-radio-${address.id}`}
                                sx={{ p: 0.5 }}
                              />
                              <Box sx={{ flexGrow: 1 }}>
                                <Stack
                                  direction="row"
                                  alignItems="center"
                                  spacing={1}
                                  mb={0.5}
                                >
                                  <Typography
                                    variant="subtitle1"
                                    fontWeight={600}
                                  >
                                    {address.recipientName}
                                  </Typography>
                                  <Typography
                                    variant="body2"
                                    color="text.secondary"
                                  >
                                    ({address.phone})
                                  </Typography>
                                  {address.isDefault && (
                                    <Chip
                                      label="Mặc định"
                                      size="small"
                                      color="primary"
                                      variant="filled"
                                      data-testid={`default-chip-${address.id}`}
                                      sx={{ height: 20, fontSize: "0.75rem" }}
                                    />
                                  )}
                                </Stack>
                                <Typography
                                  variant="body2"
                                  color="text.secondary"
                                >
                                  {address.streetAddress}, {address.ward},{" "}
                                  {address.district}, {address.province}
                                </Typography>
                              </Box>
                            </Stack>
                          </Card>
                        );
                      })}
                    </Stack>
                  )}

                  <Box
                    sx={{
                      display: "flex",
                      justifyContent: "flex-end",
                      mt: 3,
                    }}
                  >
                    <Button
                      variant="contained"
                      size="large"
                      endIcon={<ArrowForwardIcon />}
                      disabled={!selectedAddressId || isCartEmpty}
                      onClick={handleContinue}
                      data-testid="continue-to-payment-btn"
                    >
                      Tiếp tục đến thanh toán
                    </Button>
                  </Box>
                </CardContent>
              </Card>
            )}

            {activeStep === 1 && (
              <Card data-testid="checkout-step-payment">
                <CardContent sx={{ p: { xs: 2, sm: 3 } }}>
                  <Stack direction="row" spacing={1} alignItems="center" mb={2}>
                    <PaymentOutlinedIcon color="primary" />
                    <Typography variant="h6" fontWeight={600}>
                      2. Phương thức thanh toán
                    </Typography>
                  </Stack>
                  <Divider sx={{ mb: 3 }} />

                  {selectedAddress && (
                    <Alert severity="success" sx={{ mb: 3 }}>
                      <Typography variant="subtitle2">
                        Đã chọn địa chỉ:
                      </Typography>
                      <Typography variant="body2">
                        {selectedAddress.recipientName} -{" "}
                        {selectedAddress.phone} ({selectedAddress.streetAddress}
                        , {selectedAddress.ward}, {selectedAddress.district},{" "}
                        {selectedAddress.province})
                      </Typography>
                    </Alert>
                  )}

                  <PaymentMethodStep
                    selected={paymentOption}
                    onSelectionChange={setPaymentOption}
                    onBack={handleBack}
                    onContinue={(option) => {
                      setPaymentOption(option);
                      handleContinue();
                    }}
                  />
                </CardContent>
              </Card>
            )}

            {activeStep === 2 && (
              <Card data-testid="checkout-step-review">
                <CardContent sx={{ p: { xs: 2, sm: 3 } }}>
                  <Stack direction="row" spacing={1} alignItems="center" mb={2}>
                    <ReceiptLongOutlinedIcon color="primary" />
                    <Typography variant="h6" fontWeight={600}>
                      3. Xem lại & Đặt hàng
                    </Typography>
                  </Stack>
                  <Divider sx={{ mb: 3 }} />
                  {selectedAddressId && paymentOption ? (
                    <OrderReviewStep
                      addressId={selectedAddressId}
                      paymentOption={paymentOption}
                      onEditAddress={() => setActiveStep(0)}
                      onEditPayment={() => setActiveStep(1)}
                    />
                  ) : (
                    <Alert severity="warning">Vui lòng hoàn tất địa chỉ và phương thức thanh toán.</Alert>
                  )}
                </CardContent>
              </Card>
            )}
          </Grid>

          {/* Right Column: Order Summary */}
          <Grid size={{ xs: 12, md: 4 }}>
            <Card
              sx={{ position: "sticky", top: 80 }}
              data-testid="checkout-order-summary"
            >
              <CardContent sx={{ p: 3 }}>
                <Typography variant="h6" fontWeight={600} gutterBottom>
                  Tóm tắt đơn hàng
                </Typography>
                <Divider sx={{ my: 2 }} />

                <Stack spacing={1.5} mb={2}>
                  <Stack
                    direction="row"
                    justifyContent="space-between"
                    alignItems="center"
                  >
                    <Typography color="text.secondary">
                      Tạm tính ({cart?.totalItems || 0} sản phẩm)
                    </Typography>
                    <Typography fontWeight={500}>
                      {formatPrice(cart?.subtotal || 0)}
                    </Typography>
                  </Stack>

                  <Stack
                    direction="row"
                    justifyContent="space-between"
                    alignItems="center"
                  >
                    <Typography color="text.secondary">
                      Phí vận chuyển
                    </Typography>
                    <Typography
                      fontWeight={500}
                      color={
                        cart?.shippingFee === 0 ? "success.main" : "inherit"
                      }
                    >
                      {cart?.shippingFee === 0
                        ? "Miễn phí"
                        : formatPrice(cart?.shippingFee || 0)}
                    </Typography>
                  </Stack>

                  {Boolean(cart?.discountAmount && cart.discountAmount > 0) && (
                    <Stack
                      direction="row"
                      justifyContent="space-between"
                      alignItems="center"
                    >
                      <Typography color="text.secondary">Giảm giá</Typography>
                      <Typography fontWeight={500} color="error.main">
                        -{formatPrice(cart?.discountAmount || 0)}
                      </Typography>
                    </Stack>
                  )}
                </Stack>

                <Divider sx={{ my: 2 }} />

                <Stack
                  direction="row"
                  justifyContent="space-between"
                  alignItems="baseline"
                  mb={3}
                >
                  <Typography variant="subtitle1" fontWeight={700}>
                    Tổng cộng
                  </Typography>
                  <Typography
                    variant="h5"
                    color="primary.main"
                    fontWeight={700}
                    data-testid="checkout-total-amount"
                  >
                    {formatPrice(cart?.total || 0)}
                  </Typography>
                </Stack>

                <Button
                  component={Link}
                  to={ROUTES.cart}
                  size="small"
                  fullWidth
                  variant="text"
                >
                  Chỉnh sửa giỏ hàng
                </Button>
              </CardContent>
            </Card>
          </Grid>
        </Grid>
      )}

      {/* Dialog thêm nhanh địa chỉ */}
      <AddressFormDialog
        open={formOpen}
        editing={null}
        onClose={() => setFormOpen(false)}
        onSaved={handleAddressSaved}
      />
    </>
  );
}
