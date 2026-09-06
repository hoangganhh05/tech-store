import {
  Alert,
  Box,
  Button,
  Chip,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  FormControl,
  FormHelperText,
  IconButton,
  InputAdornment,
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
  TableRow,
  TextField,
  Tooltip,
  Typography,
} from "@mui/material";
import AddIcon from "@mui/icons-material/Add";
import EditOutlinedIcon from "@mui/icons-material/EditOutlined";
import DeleteOutlineIcon from "@mui/icons-material/DeleteOutline";
import RefreshIcon from "@mui/icons-material/Refresh";
import StyleOutlinedIcon from "@mui/icons-material/StyleOutlined";
import { isAxiosError } from "axios";
import { useCallback, useEffect, useState, type FormEvent } from "react";
import {
  createProductVariant,
  deleteProductVariant,
  getProductVariants,
  updateProductVariant,
  type Product,
  type ProductVariant,
  type ProductVariantPayload,
  type VariantStatus,
} from "../../services/productService";

interface AdminProductVariantsDialogProps {
  open: boolean;
  product: Product | null;
  onClose: () => void;
  onVariantsChanged?: () => void;
}

const formatCurrency = (amount: number): string => {
  return new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
  }).format(amount);
};

export function AdminProductVariantsDialog({
  open,
  product,
  onClose,
  onVariantsChanged,
}: AdminProductVariantsDialogProps) {
  const [variants, setVariants] = useState<ProductVariant[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [feedback, setFeedback] = useState<{
    type: "success" | "error";
    message: string;
  } | null>(null);

  // Add / Edit variant form dialog
  const [formDialogOpen, setFormDialogOpen] = useState(false);
  const [editingVariant, setEditingVariant] = useState<ProductVariant | null>(
    null,
  );
  const [formData, setFormData] = useState<{
    sku: string;
    color: string;
    storage: string;
    price: string;
    originalPrice: string;
    stockQuantity: string;
    status: VariantStatus;
  }>({
    sku: "",
    color: "",
    storage: "",
    price: "",
    originalPrice: "",
    stockQuantity: "0",
    status: "ACTIVE",
  });
  const [formErrors, setFormErrors] = useState<{
    sku?: string;
    price?: string;
    originalPrice?: string;
    stockQuantity?: string;
  }>({});
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Delete confirmation dialog
  const [deleteConfirmOpen, setDeleteConfirmOpen] = useState(false);
  const [variantToDelete, setVariantToDelete] = useState<ProductVariant | null>(
    null,
  );
  const [isDeleting, setIsDeleting] = useState(false);

  const fetchVariants = useCallback(async () => {
    if (!product) return;
    setIsLoading(true);
    try {
      const data = await getProductVariants(product.id);
      setVariants(data);
    } catch (error: unknown) {
      const msg = isAxiosError<{ message?: string }>(error)
        ? error.response?.data?.message
        : undefined;
      setFeedback({
        type: "error",
        message: msg || "Không thể tải danh sách biến thể sản phẩm.",
      });
    } finally {
      setIsLoading(false);
    }
  }, [product]);

  useEffect(() => {
    if (open && product) {
      setFeedback(null);
      fetchVariants();
    } else {
      setVariants([]);
      setFeedback(null);
    }
  }, [open, product, fetchVariants]);

  const handleOpenAdd = () => {
    setEditingVariant(null);
    setFormData({
      sku: "",
      color: "",
      storage: "",
      price: "",
      originalPrice: "",
      stockQuantity: "0",
      status: "ACTIVE",
    });
    setFormErrors({});
    setFormDialogOpen(true);
  };

  const handleOpenEdit = (variant: ProductVariant) => {
    setEditingVariant(variant);
    setFormData({
      sku: variant.sku,
      color: variant.color || "",
      storage: variant.storage || "",
      price: variant.price.toString(),
      originalPrice:
        variant.originalPrice !== null && variant.originalPrice !== undefined
          ? variant.originalPrice.toString()
          : "",
      stockQuantity: variant.stockQuantity.toString(),
      status: variant.status,
    });
    setFormErrors({});
    setFormDialogOpen(true);
  };

  const handleCloseFormDialog = () => {
    if (isSubmitting) return;
    setFormDialogOpen(false);
    setEditingVariant(null);
    setFormErrors({});
  };

  const handleFormSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!product) return;

    const errors: {
      sku?: string;
      price?: string;
      originalPrice?: string;
      stockQuantity?: string;
    } = {};

    const trimmedSku = formData.sku.trim().toUpperCase();
    if (!trimmedSku) {
      errors.sku = "Mã SKU không được để trống.";
    }

    const priceNum = Number(formData.price);
    if (!formData.price || isNaN(priceNum) || priceNum < 0) {
      errors.price = "Giá bán phải là số hợp lệ lớn hơn hoặc bằng 0.";
    }

    let originalPriceNum: number | undefined;
    if (formData.originalPrice.trim() !== "") {
      originalPriceNum = Number(formData.originalPrice);
      if (isNaN(originalPriceNum) || originalPriceNum < 0) {
        errors.originalPrice = "Giá gốc phải là số hợp lệ lớn hơn hoặc bằng 0.";
      } else if (!isNaN(priceNum) && originalPriceNum < priceNum) {
        errors.originalPrice = "Giá gốc phải lớn hơn hoặc bằng giá bán.";
      }
    }

    const stockNum = Number(formData.stockQuantity);
    if (
      formData.stockQuantity.trim() !== "" &&
      (isNaN(stockNum) || stockNum < 0)
    ) {
      errors.stockQuantity = "Số lượng tồn kho không được âm.";
    }

    if (Object.keys(errors).length > 0) {
      setFormErrors(errors);
      return;
    }

    setIsSubmitting(true);
    setFormErrors({});

    const payload: ProductVariantPayload = {
      sku: trimmedSku,
      color: formData.color.trim() || undefined,
      storage: formData.storage.trim() || undefined,
      price: priceNum,
      originalPrice: originalPriceNum,
      stockQuantity: isNaN(stockNum) ? 0 : stockNum,
      status: formData.status,
    };

    try {
      if (editingVariant) {
        await updateProductVariant(product.id, editingVariant.id, payload);
        setFeedback({
          type: "success",
          message: `Đã cập nhật biến thể "${trimmedSku}" thành công.`,
        });
      } else {
        await createProductVariant(product.id, payload);
        setFeedback({
          type: "success",
          message: `Đã thêm biến thể mới "${trimmedSku}" thành công.`,
        });
      }
      setFormDialogOpen(false);
      await fetchVariants();
      onVariantsChanged?.();
    } catch (error: unknown) {
      const msg = isAxiosError<{ message?: string }>(error)
        ? error.response?.data?.message
        : undefined;
      setFormErrors({
        sku:
          msg ||
          "Không thể lưu biến thể. Vui lòng kiểm tra lại thông tin mã SKU và giá.",
      });
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleOpenDelete = (variant: ProductVariant) => {
    setVariantToDelete(variant);
    setDeleteConfirmOpen(true);
  };

  const handleCloseDelete = () => {
    if (isDeleting) return;
    setDeleteConfirmOpen(false);
    setVariantToDelete(null);
  };

  const handleDeleteConfirm = async () => {
    if (!product || !variantToDelete) return;
    setIsDeleting(true);
    try {
      await deleteProductVariant(product.id, variantToDelete.id);
      setFeedback({
        type: "success",
        message: `Đã xoá biến thể "${variantToDelete.sku}" thành công.`,
      });
      setDeleteConfirmOpen(false);
      setVariantToDelete(null);
      await fetchVariants();
      onVariantsChanged?.();
    } catch (error: unknown) {
      const msg = isAxiosError<{ message?: string }>(error)
        ? error.response?.data?.message
        : undefined;
      setFeedback({
        type: "error",
        message:
          msg || "Không thể xoá biến thể này (có thể đã có đơn hàng liên kết).",
      });
      setDeleteConfirmOpen(false);
    } finally {
      setIsDeleting(false);
    }
  };

  return (
    <>
      <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
        <DialogTitle
          sx={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
          }}
        >
          <Box>
            <Typography variant="h6" component="span" fontWeight={600}>
              Biến thể sản phẩm: {product?.name}
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Mã sản phẩm: #{product?.id} · Thương hiệu: {product?.brandName} ·
              Danh mục: {product?.categoryName}
            </Typography>
          </Box>
          <Stack direction="row" spacing={1}>
            <Button
              variant="outlined"
              size="small"
              startIcon={<RefreshIcon />}
              onClick={fetchVariants}
              disabled={isLoading}
            >
              Làm mới
            </Button>
            <Button
              variant="contained"
              size="small"
              startIcon={<AddIcon />}
              onClick={handleOpenAdd}
            >
              Thêm biến thể
            </Button>
          </Stack>
        </DialogTitle>

        <DialogContent dividers sx={{ p: 2 }}>
          {feedback && (
            <Alert
              severity={feedback.type}
              onClose={() => setFeedback(null)}
              sx={{ mb: 2 }}
            >
              {feedback.message}
            </Alert>
          )}

          {isLoading && variants.length === 0 ? (
            <Box sx={{ p: 4, display: "flex", justifyContent: "center" }}>
              <CircularProgress />
            </Box>
          ) : variants.length === 0 ? (
            <Box sx={{ p: 4, textAlign: "center" }}>
              <StyleOutlinedIcon
                sx={{ fontSize: 48, color: "text.secondary", mb: 1 }}
              />
              <Typography variant="subtitle1" fontWeight={600} gutterBottom>
                Chưa có biến thể nào cho sản phẩm này
              </Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                Sản phẩm cần có ít nhất một biến thể hợp lệ (SKU, giá bán) để có
                thể kích hoạt bán hàng.
              </Typography>
              <Button
                variant="contained"
                startIcon={<AddIcon />}
                onClick={handleOpenAdd}
              >
                Thêm biến thể đầu tiên
              </Button>
            </Box>
          ) : (
            <TableContainer component={Paper} elevation={0} variant="outlined">
              <Table size="small">
                <TableHead>
                  <TableRow sx={{ bgcolor: "grey.50" }}>
                    <TableCell sx={{ fontWeight: 600 }}>Mã SKU</TableCell>
                    <TableCell sx={{ fontWeight: 600 }}>Màu sắc</TableCell>
                    <TableCell sx={{ fontWeight: 600 }}>Dung lượng</TableCell>
                    <TableCell sx={{ fontWeight: 600 }} align="right">
                      Giá bán
                    </TableCell>
                    <TableCell sx={{ fontWeight: 600 }} align="right">
                      Giá gốc
                    </TableCell>
                    <TableCell sx={{ fontWeight: 600 }} align="center">
                      Kho
                    </TableCell>
                    <TableCell sx={{ fontWeight: 600 }} align="center">
                      Trạng thái
                    </TableCell>
                    <TableCell sx={{ fontWeight: 600 }} align="center">
                      Thao tác
                    </TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {variants.map((v) => (
                    <TableRow key={v.id} hover>
                      <TableCell sx={{ fontWeight: 600 }}>
                        <Chip
                          label={v.sku}
                          size="small"
                          color="primary"
                          variant="outlined"
                        />
                      </TableCell>
                      <TableCell>{v.color || "—"}</TableCell>
                      <TableCell>{v.storage || "—"}</TableCell>
                      <TableCell
                        align="right"
                        sx={{ fontWeight: 600, color: "primary.main" }}
                      >
                        {formatCurrency(v.price)}
                      </TableCell>
                      <TableCell align="right" sx={{ color: "text.secondary" }}>
                        {v.originalPrice ? (
                          <span
                            style={{
                              textDecoration:
                                v.originalPrice > v.price
                                  ? "line-through"
                                  : "none",
                            }}
                          >
                            {formatCurrency(v.originalPrice)}
                          </span>
                        ) : (
                          "—"
                        )}
                      </TableCell>
                      <TableCell align="center">{v.stockQuantity}</TableCell>
                      <TableCell align="center">
                        {v.status === "ACTIVE" ? (
                          <Chip label="Đang bán" size="small" color="success" />
                        ) : (
                          <Chip
                            label="Ngừng bán"
                            size="small"
                            color="default"
                          />
                        )}
                      </TableCell>
                      <TableCell align="center">
                        <Tooltip title="Chỉnh sửa biến thể">
                          <IconButton
                            size="small"
                            color="primary"
                            onClick={() => handleOpenEdit(v)}
                            aria-label={`Chỉnh sửa biến thể ${v.sku}`}
                          >
                            <EditOutlinedIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>
                        <Tooltip title="Xoá biến thể">
                          <IconButton
                            size="small"
                            color="error"
                            onClick={() => handleOpenDelete(v)}
                            aria-label={`Xoá biến thể ${v.sku}`}
                          >
                            <DeleteOutlineIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </DialogContent>

        <DialogActions sx={{ px: 3, py: 1.5 }}>
          <Button onClick={onClose} variant="outlined">
            Đóng
          </Button>
        </DialogActions>
      </Dialog>

      {/* Add / Edit Variant Modal */}
      <Dialog
        open={formDialogOpen}
        onClose={handleCloseFormDialog}
        maxWidth="sm"
        fullWidth
      >
        <form onSubmit={handleFormSubmit} noValidate>
          <DialogTitle>
            {editingVariant
              ? `Chỉnh sửa biến thể: ${editingVariant.sku}`
              : "Thêm biến thể mới"}
          </DialogTitle>
          <DialogContent dividers>
            <Stack spacing={2} sx={{ pt: 1 }}>
              <TextField
                label="Mã SKU"
                required
                fullWidth
                placeholder="VD: IP16PM-256-DESERT"
                value={formData.sku}
                onChange={(e) =>
                  setFormData({
                    ...formData,
                    sku: e.target.value.toUpperCase(),
                  })
                }
                error={Boolean(formErrors.sku)}
                helperText={
                  formErrors.sku ||
                  "Mã SKU là duy nhất trên toàn hệ thống (tự động viết hoa)."
                }
                disabled={isSubmitting}
                autoFocus
              />

              <Stack direction="row" spacing={2}>
                <TextField
                  label="Màu sắc"
                  fullWidth
                  placeholder="VD: Sa mạc titan, Titan tự nhiên"
                  value={formData.color}
                  onChange={(e) =>
                    setFormData({ ...formData, color: e.target.value })
                  }
                  disabled={isSubmitting}
                />
                <TextField
                  label="Dung lượng / Phiên bản"
                  fullWidth
                  placeholder="VD: 128GB, 256GB, 512GB, 1TB"
                  value={formData.storage}
                  onChange={(e) =>
                    setFormData({ ...formData, storage: e.target.value })
                  }
                  disabled={isSubmitting}
                />
              </Stack>

              <Stack direction="row" spacing={2}>
                <TextField
                  label="Giá bán (VNĐ)"
                  required
                  type="number"
                  fullWidth
                  placeholder="VD: 28990000"
                  value={formData.price}
                  onChange={(e) =>
                    setFormData({ ...formData, price: e.target.value })
                  }
                  error={Boolean(formErrors.price)}
                  helperText={formErrors.price || "Giá bán thực tế cho khách"}
                  disabled={isSubmitting}
                  InputProps={{
                    endAdornment: (
                      <InputAdornment position="end">đ</InputAdornment>
                    ),
                  }}
                />
                <TextField
                  label="Giá gốc (VNĐ)"
                  type="number"
                  fullWidth
                  placeholder="VD: 31990000"
                  value={formData.originalPrice}
                  onChange={(e) =>
                    setFormData({ ...formData, originalPrice: e.target.value })
                  }
                  error={Boolean(formErrors.originalPrice)}
                  helperText={
                    formErrors.originalPrice ||
                    "Giá niêm yết trước giảm (nếu có, >= giá bán)"
                  }
                  disabled={isSubmitting}
                  InputProps={{
                    endAdornment: (
                      <InputAdornment position="end">đ</InputAdornment>
                    ),
                  }}
                />
              </Stack>

              <Stack direction="row" spacing={2}>
                <TextField
                  label="Số lượng tồn kho"
                  type="number"
                  fullWidth
                  placeholder="VD: 50"
                  value={formData.stockQuantity}
                  onChange={(e) =>
                    setFormData({ ...formData, stockQuantity: e.target.value })
                  }
                  error={Boolean(formErrors.stockQuantity)}
                  helperText={formErrors.stockQuantity}
                  disabled={isSubmitting}
                />
                <FormControl fullWidth disabled={isSubmitting}>
                  <InputLabel id="variant-status-label">Trạng thái</InputLabel>
                  <Select
                    labelId="variant-status-label"
                    label="Trạng thái"
                    value={formData.status}
                    onChange={(e) =>
                      setFormData({
                        ...formData,
                        status: e.target.value as VariantStatus,
                      })
                    }
                  >
                    <MenuItem value="ACTIVE">Đang bán (ACTIVE)</MenuItem>
                    <MenuItem value="INACTIVE">
                      Ngừng bán / Tạm ẩn (INACTIVE)
                    </MenuItem>
                  </Select>
                  <FormHelperText>
                    Chọn ngừng bán để tạm ẩn biến thể
                  </FormHelperText>
                </FormControl>
              </Stack>
            </Stack>
          </DialogContent>
          <DialogActions sx={{ px: 3, py: 2 }}>
            <Button onClick={handleCloseFormDialog} disabled={isSubmitting}>
              Huỷ
            </Button>
            <Button
              type="submit"
              variant="contained"
              disabled={isSubmitting}
              startIcon={isSubmitting ? <CircularProgress size={16} /> : null}
            >
              {editingVariant ? "Lưu thay đổi" : "Tạo biến thể"}
            </Button>
          </DialogActions>
        </form>
      </Dialog>

      {/* Delete Confirmation Modal */}
      <Dialog
        open={deleteConfirmOpen}
        onClose={handleCloseDelete}
        maxWidth="xs"
        fullWidth
      >
        <DialogTitle>Xác nhận xoá biến thể</DialogTitle>
        <DialogContent>
          <DialogContentText>
            Bạn có chắc chắn muốn xoá biến thể{" "}
            <strong>{variantToDelete?.sku}</strong> (
            {variantToDelete?.color ? `${variantToDelete.color} - ` : ""}
            {variantToDelete?.storage || ""})?
          </DialogContentText>
          <Typography
            variant="caption"
            color="error"
            display="block"
            sx={{ mt: 1 }}
          >
            Lưu ý: Nếu biến thể đã từng phát sinh đơn hàng, hệ thống sẽ từ chối
            xoá để bảo toàn lịch sử dữ liệu. Khi đó bạn có thể chuyển trạng thái
            sang 'Ngừng bán'.
          </Typography>
        </DialogContent>
        <DialogActions sx={{ px: 3, py: 2 }}>
          <Button onClick={handleCloseDelete} disabled={isDeleting}>
            Huỷ
          </Button>
          <Button
            onClick={handleDeleteConfirm}
            color="error"
            variant="contained"
            disabled={isDeleting}
            startIcon={isDeleting ? <CircularProgress size={16} /> : null}
          >
            Xác nhận xoá
          </Button>
        </DialogActions>
      </Dialog>
    </>
  );
}
