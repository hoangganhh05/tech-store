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
  IconButton,
  InputAdornment,
  Paper,
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
import ListAltIcon from "@mui/icons-material/ListAlt";
import NumbersIcon from "@mui/icons-material/Numbers";
import { isAxiosError } from "axios";
import { useCallback, useEffect, useState, type FormEvent } from "react";
import {
  createProductSpecification,
  deleteProductSpecification,
  getProductSpecifications,
  updateProductSpecification,
  type Product,
  type ProductSpecification,
} from "../../services/productService";

interface AdminProductSpecificationsDialogProps {
  open: boolean;
  product: Product | null;
  onClose: () => void;
  onSpecificationsChanged?: () => void;
}

const COMMON_SPEC_SUGGESTIONS = [
  "Màn hình",
  "Hệ điều hành",
  "CPU",
  "GPU",
  "RAM",
  "Bộ nhớ trong",
  "Camera sau",
  "Camera trước",
  "Dung lượng pin",
  "Cổng sạc",
  "Trọng lượng",
  "Kết nối",
];

export function AdminProductSpecificationsDialog({
  open,
  product,
  onClose,
  onSpecificationsChanged,
}: AdminProductSpecificationsDialogProps) {
  const [specifications, setSpecifications] = useState<ProductSpecification[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [feedback, setFeedback] = useState<{
    type: "success" | "error";
    message: string;
  } | null>(null);

  // Add / Edit form dialog state
  const [formDialogOpen, setFormDialogOpen] = useState(false);
  const [editingSpec, setEditingSpec] = useState<ProductSpecification | null>(null);
  const [formData, setFormData] = useState<{
    specKey: string;
    specValue: string;
    displayOrder: string;
  }>({
    specKey: "",
    specValue: "",
    displayOrder: "0",
  });
  const [formErrors, setFormErrors] = useState<{
    specKey?: string;
    specValue?: string;
    displayOrder?: string;
  }>({});
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Delete confirmation dialog state
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [deletingSpec, setDeletingSpec] = useState<ProductSpecification | null>(null);
  const [isDeleting, setIsDeleting] = useState(false);

  const fetchSpecifications = useCallback(async () => {
    if (!product) return;
    setIsLoading(true);
    try {
      const data = await getProductSpecifications(product.id);
      setSpecifications(data);
    } catch (err: unknown) {
      const message = isAxiosError<{ message?: string }>(err)
        ? err.response?.data?.message
        : undefined;
      setFeedback({
        type: "error",
        message: message || "Không thể tải danh sách thông số kỹ thuật.",
      });
    } finally {
      setIsLoading(false);
    }
  }, [product]);

  useEffect(() => {
    if (open && product) {
      setFeedback(null);
      fetchSpecifications();
    } else {
      setSpecifications([]);
      setFeedback(null);
    }
  }, [open, product, fetchSpecifications]);

  // Open Add Dialog
  const handleOpenAdd = () => {
    setEditingSpec(null);
    const nextOrder =
      specifications.length > 0
        ? Math.max(...specifications.map((s) => s.displayOrder)) + 1
        : 1;
    setFormData({
      specKey: "",
      specValue: "",
      displayOrder: String(nextOrder),
    });
    setFormErrors({});
    setFormDialogOpen(true);
  };

  // Open Edit Dialog
  const handleOpenEdit = (spec: ProductSpecification) => {
    setEditingSpec(spec);
    setFormData({
      specKey: spec.specKey,
      specValue: spec.specValue,
      displayOrder: String(spec.displayOrder),
    });
    setFormErrors({});
    setFormDialogOpen(true);
  };

  // Close form dialog
  const handleCloseForm = () => {
    if (isSubmitting) return;
    setFormDialogOpen(false);
    setEditingSpec(null);
    setFormErrors({});
  };

  // Submit form
  const handleSubmitForm = async (e: FormEvent) => {
    e.preventDefault();
    if (!product) return;

    const errors: { specKey?: string; specValue?: string; displayOrder?: string } = {};
    const trimmedKey = formData.specKey.trim();
    const trimmedValue = formData.specValue.trim();
    const orderNum = Number(formData.displayOrder);

    if (!trimmedKey) {
      errors.specKey = "Tên thông số kỹ thuật không được để trống.";
    } else if (trimmedKey.length > 100) {
      errors.specKey = "Tên thông số không vượt quá 100 ký tự.";
    }

    if (!trimmedValue) {
      errors.specValue = "Giá trị thông số kỹ thuật không được để trống.";
    } else if (trimmedValue.length > 500) {
      errors.specValue = "Giá trị thông số không vượt quá 500 ký tự.";
    }

    if (isNaN(orderNum) || orderNum < 0) {
      errors.displayOrder = "Thứ tự hiển thị phải là số không âm.";
    }

    if (Object.keys(errors).length > 0) {
      setFormErrors(errors);
      return;
    }

    setIsSubmitting(true);
    setFormErrors({});

    try {
      if (editingSpec) {
        await updateProductSpecification(product.id, editingSpec.id, {
          specKey: trimmedKey,
          specValue: trimmedValue,
          displayOrder: orderNum,
        });
        setFeedback({
          type: "success",
          message: `Đã cập nhật thông số "${trimmedKey}" thành công.`,
        });
      } else {
        await createProductSpecification(product.id, {
          specKey: trimmedKey,
          specValue: trimmedValue,
          displayOrder: orderNum,
        });
        setFeedback({
          type: "success",
          message: `Đã thêm thông số "${trimmedKey}" thành công.`,
        });
      }

      setFormDialogOpen(false);
      setEditingSpec(null);
      await fetchSpecifications();
      onSpecificationsChanged?.();
    } catch (err: unknown) {
      const message = isAxiosError<{ message?: string }>(err)
        ? err.response?.data?.message
        : undefined;
      setFeedback({
        type: "error",
        message: message || "Không thể lưu thông số kỹ thuật. Vui lòng thử lại.",
      });
    } finally {
      setIsSubmitting(false);
    }
  };

  // Open delete dialog
  const handleOpenDelete = (spec: ProductSpecification) => {
    setDeletingSpec(spec);
    setDeleteDialogOpen(true);
  };

  // Close delete dialog
  const handleCloseDelete = () => {
    if (isDeleting) return;
    setDeleteDialogOpen(false);
    setDeletingSpec(null);
  };

  // Confirm delete
  const handleConfirmDelete = async () => {
    if (!product || !deletingSpec) return;
    setIsDeleting(true);
    try {
      await deleteProductSpecification(product.id, deletingSpec.id);
      setFeedback({
        type: "success",
        message: `Đã xoá thông số "${deletingSpec.specKey}" thành công.`,
      });
      setDeleteDialogOpen(false);
      setDeletingSpec(null);
      await fetchSpecifications();
      onSpecificationsChanged?.();
    } catch (err: unknown) {
      const message = isAxiosError<{ message?: string }>(err)
        ? err.response?.data?.message
        : undefined;
      setFeedback({
        type: "error",
        message: message || "Không thể xoá thông số kỹ thuật. Vui lòng thử lại.",
      });
      setDeleteDialogOpen(false);
      setDeletingSpec(null);
    } finally {
      setIsDeleting(false);
    }
  };

  return (
    <>
      <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
        <DialogTitle sx={{ pb: 1 }}>
          <Stack
            direction="row"
            justifyContent="space-between"
            alignItems="center"
            spacing={2}
          >
            <Box>
              <Typography variant="h6" fontWeight={700}>
                Thông số kỹ thuật sản phẩm
              </Typography>
              {product && (
                <Stack direction="row" spacing={1} alignItems="center" sx={{ mt: 0.5 }}>
                  <Typography variant="body2" color="text.secondary">
                    {product.name}
                  </Typography>
                  <Chip
                    label={product.categoryName}
                    size="small"
                    variant="outlined"
                  />
                  <Chip
                    label={product.brandName}
                    size="small"
                    variant="outlined"
                  />
                </Stack>
              )}
            </Box>
            <Stack direction="row" spacing={1}>
              <Tooltip title="Làm mới">
                <span>
                  <IconButton
                    onClick={fetchSpecifications}
                    disabled={isLoading}
                    size="small"
                  >
                    <RefreshIcon />
                  </IconButton>
                </span>
              </Tooltip>
              <Button
                variant="contained"
                size="small"
                startIcon={<AddIcon />}
                onClick={handleOpenAdd}
              >
                Thêm thông số
              </Button>
            </Stack>
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

          {isLoading && specifications.length === 0 ? (
            <Box sx={{ p: 6, display: "flex", justifyContent: "center" }}>
              <CircularProgress />
            </Box>
          ) : specifications.length === 0 ? (
            <Box sx={{ p: 6, textAlign: "center" }}>
              <ListAltIcon
                sx={{ fontSize: 48, color: "text.secondary", mb: 1.5 }}
              />
              <Typography variant="h6" fontWeight={600} gutterBottom>
                Chưa có thông số kỹ thuật nào
              </Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                Thêm các thông số như CPU, RAM, Màn hình, Dung lượng pin... để khách hàng tham khảo khi xem sản phẩm.
              </Typography>
              <Button
                variant="contained"
                startIcon={<AddIcon />}
                onClick={handleOpenAdd}
              >
                Thêm thông số đầu tiên
              </Button>
            </Box>
          ) : (
            <TableContainer
              component={Paper}
              elevation={0}
              sx={{ border: "1px solid", borderColor: "divider" }}
            >
              <Table size="small">
                <TableHead>
                  <TableRow sx={{ bgcolor: "grey.50" }}>
                    <TableCell sx={{ fontWeight: 600, width: 90 }} align="center">
                      Thứ tự
                    </TableCell>
                    <TableCell sx={{ fontWeight: 600, width: 220 }}>
                      Tên thông số
                    </TableCell>
                    <TableCell sx={{ fontWeight: 600 }}>Giá trị</TableCell>
                    <TableCell sx={{ fontWeight: 600, width: 140 }} align="center">
                      Thao tác
                    </TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {specifications.map((spec) => (
                    <TableRow key={spec.id} hover>
                      <TableCell align="center">
                        <Chip
                          label={spec.displayOrder}
                          size="small"
                          variant="outlined"
                          sx={{ fontWeight: 600 }}
                        />
                      </TableCell>
                      <TableCell sx={{ fontWeight: 600 }}>{spec.specKey}</TableCell>
                      <TableCell>{spec.specValue}</TableCell>
                      <TableCell align="center">
                        <Stack
                          direction="row"
                          spacing={0.5}
                          justifyContent="center"
                        >
                          <IconButton
                            size="small"
                            color="primary"
                            onClick={() => handleOpenEdit(spec)}
                            title="Sửa thông số"
                            aria-label={`Sửa thông số ${spec.specKey}`}
                          >
                            <EditOutlinedIcon fontSize="small" />
                          </IconButton>
                          <IconButton
                            size="small"
                            color="error"
                            onClick={() => handleOpenDelete(spec)}
                            title="Xoá thông số"
                            aria-label={`Xoá thông số ${spec.specKey}`}
                          >
                            <DeleteOutlineIcon fontSize="small" />
                          </IconButton>
                        </Stack>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </DialogContent>

        <DialogActions sx={{ px: 3, py: 1.5 }}>
          <Button onClick={onClose} color="inherit">
            Đóng
          </Button>
        </DialogActions>
      </Dialog>

      {/* Dialog Thêm / Sửa Thông số */}
      <Dialog
        open={formDialogOpen}
        onClose={handleCloseForm}
        maxWidth="sm"
        fullWidth
      >
        <form onSubmit={handleSubmitForm} noValidate>
          <DialogTitle>
            {editingSpec ? "Chỉnh sửa thông số kỹ thuật" : "Thêm thông số kỹ thuật"}
          </DialogTitle>
          <DialogContent dividers>
            <Stack spacing={2.5} sx={{ pt: 1 }}>
              {!editingSpec && (
                <Box>
                  <Typography variant="caption" color="text.secondary" gutterBottom>
                    Gợi ý nhanh tên thông số phổ biến:
                  </Typography>
                  <Stack direction="row" spacing={0.8} flexWrap="wrap" sx={{ gap: 0.8, mt: 0.5 }}>
                    {COMMON_SPEC_SUGGESTIONS.map((suggestion) => (
                      <Chip
                        key={suggestion}
                        label={suggestion}
                        size="small"
                        clickable
                        onClick={() =>
                          setFormData((prev) => ({ ...prev, specKey: suggestion }))
                        }
                        color={formData.specKey === suggestion ? "primary" : "default"}
                        variant={formData.specKey === suggestion ? "filled" : "outlined"}
                      />
                    ))}
                  </Stack>
                </Box>
              )}

              <TextField
                label="Tên thông số kỹ thuật"
                required
                fullWidth
                value={formData.specKey}
                onChange={(e) =>
                  setFormData({ ...formData, specKey: e.target.value })
                }
                error={Boolean(formErrors.specKey)}
                helperText={
                  formErrors.specKey ||
                  "Ví dụ: CPU, RAM, Màn hình, Dung lượng pin..."
                }
                disabled={isSubmitting}
                autoFocus
              />

              <TextField
                label="Giá trị thông số"
                required
                fullWidth
                multiline
                rows={2}
                value={formData.specValue}
                onChange={(e) =>
                  setFormData({ ...formData, specValue: e.target.value })
                }
                error={Boolean(formErrors.specValue)}
                helperText={
                  formErrors.specValue ||
                  "Ví dụ: Apple A18 Pro 6 nhân, 8 GB, 6.3 inch Super Retina XDR..."
                }
                disabled={isSubmitting}
              />

              <TextField
                label="Thứ tự hiển thị"
                fullWidth
                type="number"
                value={formData.displayOrder}
                onChange={(e) =>
                  setFormData({ ...formData, displayOrder: e.target.value })
                }
                error={Boolean(formErrors.displayOrder)}
                helperText={
                  formErrors.displayOrder ||
                  "Số nhỏ hơn sẽ hiển thị trước trên trang chi tiết sản phẩm."
                }
                InputProps={{
                  startAdornment: (
                    <InputAdornment position="start">
                      <NumbersIcon fontSize="small" />
                    </InputAdornment>
                  ),
                }}
                disabled={isSubmitting}
              />
            </Stack>
          </DialogContent>
          <DialogActions sx={{ px: 3, py: 2 }}>
            <Button onClick={handleCloseForm} disabled={isSubmitting}>
              Huỷ
            </Button>
            <Button
              type="submit"
              variant="contained"
              disabled={isSubmitting}
              startIcon={isSubmitting ? <CircularProgress size={16} /> : null}
            >
              {editingSpec ? "Lưu thay đổi" : "Thêm thông số"}
            </Button>
          </DialogActions>
        </form>
      </Dialog>

      {/* Dialog Xác nhận xoá thông số */}
      <Dialog
        open={deleteDialogOpen}
        onClose={handleCloseDelete}
        maxWidth="xs"
        fullWidth
      >
        <DialogTitle sx={{ fontWeight: 600 }}>Xác nhận xoá thông số</DialogTitle>
        <DialogContent>
          <DialogContentText>
            Bạn có chắc chắn muốn xoá thông số{" "}
            <strong>{deletingSpec?.specKey}</strong> khỏi sản phẩm này không?
          </DialogContentText>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={handleCloseDelete} disabled={isDeleting} color="inherit">
            Huỷ
          </Button>
          <Button
            variant="contained"
            color="error"
            onClick={handleConfirmDelete}
            disabled={isDeleting}
          >
            {isDeleting ? "Đang xoá..." : "Xác nhận xoá"}
          </Button>
        </DialogActions>
      </Dialog>
    </>
  );
}
