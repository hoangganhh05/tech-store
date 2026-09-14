import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControl,
  FormHelperText,
  InputAdornment,
  InputLabel,
  IconButton,
  Menu,
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
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import RefreshIcon from "@mui/icons-material/Refresh";
import SearchIcon from "@mui/icons-material/Search";
import Inventory2OutlinedIcon from "@mui/icons-material/Inventory2Outlined";
import TuneIcon from "@mui/icons-material/Tune";
import CollectionsIcon from "@mui/icons-material/Collections";
import ListAltIcon from "@mui/icons-material/ListAlt";
import DeleteOutlineIcon from "@mui/icons-material/DeleteOutline";
import TaskAltOutlinedIcon from "@mui/icons-material/TaskAltOutlined";
import ArrowForwardIcon from "@mui/icons-material/ArrowForward";
import { isAxiosError } from "axios";
import { useCallback, useEffect, useState, type FormEvent } from "react";
import {
  createAdminProduct,
  deleteAdminProduct,
  getAdminProducts,
  updateAdminProduct,
  updateAdminProductStatus,
  type Product,
  type ProductCreatePayload,
  type ProductStatus,
} from "../../services/productService";
import { getAdminBrands, type Brand } from "../../services/brandService";
import {
  getAdminCategories,
  type Category,
} from "../../services/categoryService";
import { AdminProductVariantsDialog } from "./AdminProductVariantsDialog";
import { AdminProductImagesDialog } from "./AdminProductImagesDialog";
import { AdminProductSpecificationsDialog } from "./AdminProductSpecificationsDialog";
import { AdminPageIntro } from "./components/AdminPageIntro";
import { AdminProductPanel } from "./components/AdminProductPanel";

export function AdminProductsPage() {
  const [products, setProducts] = useState<Product[]>([]);
  const [brands, setBrands] = useState<Brand[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [searchKeyword, setSearchKeyword] = useState("");
  const [feedbackMessage, setFeedbackMessage] = useState<{
    type: "success" | "error";
    text: string;
  } | null>(null);

  // Add / Edit Product Dialog state
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const [editingProduct, setEditingProduct] = useState<Product | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [formData, setFormData] = useState<ProductCreatePayload>({
    name: "",
    description: "",
    brandId: 0,
    categoryId: 0,
    status: "DRAFT",
  });
  const [formErrors, setFormErrors] = useState<{
    name?: string;
    brandId?: string;
    categoryId?: string;
  }>({});

  // Quick status menu state
  const [statusMenuAnchorEl, setStatusMenuAnchorEl] =
    useState<null | HTMLElement>(null);
  const [selectedProductForStatus, setSelectedProductForStatus] =
    useState<Product | null>(null);

  // Product Variants Dialog state
  const [selectedProductForVariants, setSelectedProductForVariants] =
    useState<Product | null>(null);
  const [isVariantsDialogOpen, setIsVariantsDialogOpen] = useState(false);

  // Product Images Dialog state
  const [selectedProductForImages, setSelectedProductForImages] =
    useState<Product | null>(null);
  const [isImagesDialogOpen, setIsImagesDialogOpen] = useState(false);

  // Product Specifications Dialog state
  const [selectedProductForSpecifications, setSelectedProductForSpecifications] =
    useState<Product | null>(null);
  const [isSpecificationsDialogOpen, setIsSpecificationsDialogOpen] = useState(false);

  // The workspace keeps the three API-backed setup dialogs in one discoverable flow.
  const [selectedProductForWorkspace, setSelectedProductForWorkspace] =
    useState<Product | null>(null);
  const [isWorkspaceOpen, setIsWorkspaceOpen] = useState(false);
  const [returnToWorkspaceAfterEditor, setReturnToWorkspaceAfterEditor] =
    useState(false);
  const [returnToWorkspaceAfterProductForm, setReturnToWorkspaceAfterProductForm] =
    useState(false);

  // Delete Product Dialog state
  const [isDeleteDialogOpen, setIsDeleteDialogOpen] = useState(false);
  const [deletingProduct, setDeletingProduct] = useState<Product | null>(null);
  const [isDeleting, setIsDeleting] = useState(false);

  const fetchData = useCallback(async () => {
    setIsLoading(true);
    try {
      const [productList, brandList, categoryList] = await Promise.all([
        getAdminProducts(),
        getAdminBrands(),
        getAdminCategories(),
      ]);
      setProducts(productList);
      setBrands(brandList);
      setCategories(categoryList);
    } catch (error: unknown) {
      const message = isAxiosError<{ message?: string }>(error)
        ? error.response?.data?.message
        : undefined;
      setFeedbackMessage({
        type: "error",
        text: message || "Không thể tải danh sách sản phẩm. Vui lòng thử lại.",
      });
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const handleOpenAddDialog = () => {
    setReturnToWorkspaceAfterProductForm(false);
    setEditingProduct(null);
    setFormData({
      name: "",
      description: "",
      // A brand and category affect reporting, storefront placement and filters.
      // Never silently assign the first value in either list.
      brandId: 0,
      categoryId: 0,
      status: "DRAFT",
    });
    setFormErrors({});
    setIsDialogOpen(true);
  };

  const handleOpenEditDialog = (
    product: Product,
    options: { returnToWorkspace?: boolean } = {},
  ) => {
    setReturnToWorkspaceAfterProductForm(Boolean(options.returnToWorkspace));
    setEditingProduct(product);
    setFormData({
      name: product.name,
      description: product.description || "",
      brandId: product.brandId,
      categoryId: product.categoryId,
      status: product.status,
    });
    setFormErrors({});
    setIsDialogOpen(true);
  };

  const handleCloseDialog = () => {
    if (isSubmitting) return;
    const shouldReturnToWorkspace = returnToWorkspaceAfterProductForm;
    setIsDialogOpen(false);
    setEditingProduct(null);
    setFormErrors({});
    setReturnToWorkspaceAfterProductForm(false);
    if (shouldReturnToWorkspace) {
      setIsWorkspaceOpen(true);
    }
  };

  const handleOpenProductWorkspace = (product: Product) => {
    setSelectedProductForWorkspace(product);
    setReturnToWorkspaceAfterEditor(false);
    setIsWorkspaceOpen(true);
  };

  const handleCloseProductWorkspace = () => {
    setIsWorkspaceOpen(false);
    setSelectedProductForWorkspace(null);
    setReturnToWorkspaceAfterEditor(false);
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();

    const errors: { name?: string; brandId?: string; categoryId?: string } = {};
    const trimmedName = formData.name.trim();

    if (!trimmedName) {
      errors.name = "Tên sản phẩm không được để trống.";
    }
    if (!formData.brandId) {
      errors.brandId = "Vui lòng chọn thương hiệu.";
    }
    if (!formData.categoryId) {
      errors.categoryId = "Vui lòng chọn danh mục.";
    }

    if (Object.keys(errors).length > 0) {
      setFormErrors(errors);
      return;
    }

    setIsSubmitting(true);
    setFormErrors({});

    try {
      let completedProduct: Product;
      if (editingProduct) {
        const updated = await updateAdminProduct(editingProduct.id, {
          name: trimmedName,
          description: formData.description?.trim() || undefined,
          brandId: Number(formData.brandId),
          categoryId: Number(formData.categoryId),
          status: formData.status,
        });
        completedProduct = updated;

        setFeedbackMessage({
          type: "success",
          text: `Đã cập nhật sản phẩm "${updated.name}" thành công.`,
        });
      } else {
        const created = await createAdminProduct({
          name: trimmedName,
          description: formData.description?.trim() || undefined,
          brandId: Number(formData.brandId),
          categoryId: Number(formData.categoryId),
          status: formData.status,
        });
        completedProduct = created;

        setFeedbackMessage({
          type: "success",
          text: `Đã tạo sản phẩm "${created.name}" thành công ở trạng thái nháp.`,
        });
      }

      setIsDialogOpen(false);
      setEditingProduct(null);
      setReturnToWorkspaceAfterProductForm(false);
      await fetchData();
      handleOpenProductWorkspace(completedProduct);
    } catch (error: unknown) {
      const message = isAxiosError<{ message?: string }>(error)
        ? error.response?.data?.message
        : undefined;
      setFeedbackMessage({
        type: "error",
        text:
          message ||
          (editingProduct
            ? "Không thể cập nhật sản phẩm. Vui lòng kiểm tra lại thông tin."
            : "Không thể tạo sản phẩm. Vui lòng kiểm tra lại thông tin."),
      });
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleOpenStatusMenu = (
    event: React.MouseEvent<HTMLElement>,
    product: Product,
  ) => {
    event.stopPropagation();
    setStatusMenuAnchorEl(event.currentTarget);
    setSelectedProductForStatus(product);
  };

  const handleCloseStatusMenu = () => {
    setStatusMenuAnchorEl(null);
    setSelectedProductForStatus(null);
  };

  const handleSelectStatus = async (newStatus: ProductStatus) => {
    if (!selectedProductForStatus) return;

    if (selectedProductForStatus.status === newStatus) {
      handleCloseStatusMenu();
      return;
    }

    const targetProduct = selectedProductForStatus;
    handleCloseStatusMenu();

    try {
      const updated = await updateAdminProductStatus(targetProduct.id, {
        status: newStatus,
      });

      const statusLabels: Record<ProductStatus, string> = {
        ACTIVE: "Đang bán",
        INACTIVE: "Ngừng bán",
        DRAFT: "Nháp",
      };

      setFeedbackMessage({
        type: "success",
        text: `Đã chuyển trạng thái sản phẩm "${updated.name}" sang "${statusLabels[newStatus]}" thành công.`,
      });

      setProducts((prev) =>
        prev.map((p) =>
          p.id === updated.id ? { ...p, status: updated.status } : p,
        ),
      );
    } catch (error: unknown) {
      const message = isAxiosError<{ message?: string }>(error)
        ? error.response?.data?.message
        : undefined;
      setFeedbackMessage({
        type: "error",
        text: message || "Không thể cập nhật trạng thái sản phẩm.",
      });
    }
  };

  const handleOpenVariantsDialog = (product: Product) => {
    setSelectedProductForVariants(product);
    setIsVariantsDialogOpen(true);
  };

  const handleCloseVariantsDialog = () => {
    setIsVariantsDialogOpen(false);
    setSelectedProductForVariants(null);
    if (returnToWorkspaceAfterEditor) {
      setIsWorkspaceOpen(true);
      setReturnToWorkspaceAfterEditor(false);
    }
  };

  const handleOpenImagesDialog = (product: Product) => {
    setSelectedProductForImages(product);
    setIsImagesDialogOpen(true);
  };

  const handleCloseImagesDialog = () => {
    setIsImagesDialogOpen(false);
    setSelectedProductForImages(null);
    if (returnToWorkspaceAfterEditor) {
      setIsWorkspaceOpen(true);
      setReturnToWorkspaceAfterEditor(false);
    }
  };

  const handleOpenSpecificationsDialog = (product: Product) => {
    setSelectedProductForSpecifications(product);
    setIsSpecificationsDialogOpen(true);
  };

  const handleCloseSpecificationsDialog = () => {
    setIsSpecificationsDialogOpen(false);
    setSelectedProductForSpecifications(null);
    if (returnToWorkspaceAfterEditor) {
      setIsWorkspaceOpen(true);
      setReturnToWorkspaceAfterEditor(false);
    }
  };

  const handleOpenWorkspaceEditor = (
    editor: "variants" | "images" | "specifications",
  ) => {
    const product = selectedProductForWorkspace;
    if (!product) return;

    setIsWorkspaceOpen(false);
    setReturnToWorkspaceAfterEditor(true);

    if (editor === "variants") {
      handleOpenVariantsDialog(product);
      return;
    }
    if (editor === "images") {
      handleOpenImagesDialog(product);
      return;
    }
    handleOpenSpecificationsDialog(product);
  };

  const handleEditFromWorkspace = () => {
    if (!selectedProductForWorkspace) return;
    setIsWorkspaceOpen(false);
    handleOpenEditDialog(selectedProductForWorkspace, {
      returnToWorkspace: true,
    });
  };

  const handleOpenDeleteDialog = (product: Product) => {
    setDeletingProduct(product);
    setIsDeleteDialogOpen(true);
  };

  const handleCloseDeleteDialog = () => {
    if (isDeleting) return;
    setIsDeleteDialogOpen(false);
    setDeletingProduct(null);
  };

  const handleConfirmDelete = async () => {
    if (!deletingProduct) return;
    try {
      setIsDeleting(true);
      await deleteAdminProduct(deletingProduct.id);
      setFeedbackMessage({
        type: "success",
        text: `Đã xoá sản phẩm "${deletingProduct.name}" thành công.`,
      });
      setProducts((prev) => prev.filter((p) => p.id !== deletingProduct.id));
      setIsDeleteDialogOpen(false);
      setDeletingProduct(null);
    } catch (err: unknown) {
      if (isAxiosError<{ message?: string }>(err) && err.response?.data?.message) {
        setFeedbackMessage({
          type: "error",
          text: err.response.data.message,
        });
      } else {
        setFeedbackMessage({
          type: "error",
          text: "Không thể xoá sản phẩm. Vui lòng thử lại sau.",
        });
      }
      setIsDeleteDialogOpen(false);
      setDeletingProduct(null);
    } finally {
      setIsDeleting(false);
    }
  };

  const filteredProducts = products.filter((p) => {
    const q = searchKeyword.toLowerCase().trim();
    if (!q) return true;
    return (
      p.name.toLowerCase().includes(q) ||
      p.brandName.toLowerCase().includes(q) ||
      p.categoryName.toLowerCase().includes(q)
    );
  });

  return (
    <Stack spacing={3}>
      <AdminPageIntro
        eyebrow="Quản trị"
        title="Quản lý sản phẩm"
        description="Tạo thông tin cơ bản trước, rồi hoàn thiện biến thể, hình ảnh và thông số trong cùng một quy trình rõ ràng."
        action={
          <Stack direction="row" spacing={1.5}>
            <Button
              variant="outlined"
              startIcon={<RefreshIcon />}
              onClick={fetchData}
              disabled={isLoading}
            >
              Làm mới
            </Button>
            <Button
              variant="contained"
              startIcon={<AddIcon />}
              onClick={handleOpenAddDialog}
            >
              Thêm sản phẩm
            </Button>
          </Stack>
        }
      />

      {feedbackMessage && (
        <Alert
          severity={feedbackMessage.type}
          onClose={() => setFeedbackMessage(null)}
          sx={{ mb: 1 }}
        >
          {feedbackMessage.text}
        </Alert>
      )}

      {/* Tìm kiếm */}
      <Card elevation={0} sx={{ border: "1px solid", borderColor: "divider" }}>
        <CardContent sx={{ py: 2 }}>
          <TextField
            size="small"
            placeholder="Tìm kiếm sản phẩm theo tên, thương hiệu, danh mục..."
            value={searchKeyword}
            onChange={(e) => setSearchKeyword(e.target.value)}
            fullWidth
            InputProps={{
              startAdornment: (
                <InputAdornment position="start">
                  <SearchIcon color="action" fontSize="small" />
                </InputAdornment>
              ),
            }}
          />
        </CardContent>
      </Card>

      {/* Bảng danh sách */}
      <Card elevation={0} sx={{ border: "1px solid", borderColor: "divider" }}>
        <CardContent sx={{ p: 0 }}>
          {isLoading && products.length === 0 ? (
            <Box sx={{ p: 6, display: "flex", justifyContent: "center" }}>
              <CircularProgress />
            </Box>
          ) : filteredProducts.length === 0 ? (
            <Box sx={{ p: 6, textAlign: "center" }}>
              <Inventory2OutlinedIcon
                sx={{ fontSize: 48, color: "text.secondary", mb: 1.5 }}
              />
              <Typography variant="h6" fontWeight={600} gutterBottom>
                {products.length === 0
                  ? "Chưa có sản phẩm nào"
                  : "Không tìm thấy sản phẩm phù hợp"}
              </Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                {products.length === 0
                  ? "Bắt đầu bằng việc tạo sản phẩm cơ bản đầu tiên vào catalog."
                  : "Thử tìm kiếm với từ khoá khác."}
              </Typography>
              {products.length === 0 && (
                <Button
                  variant="contained"
                  startIcon={<AddIcon />}
                  onClick={handleOpenAddDialog}
                >
                  Thêm sản phẩm đầu tiên
                </Button>
              )}
            </Box>
          ) : (
            <TableContainer component={Paper} elevation={0}>
              <Table sx={{ minWidth: 650 }}>
                <TableHead>
                  <TableRow sx={{ bgcolor: "grey.50" }}>
                    <TableCell sx={{ fontWeight: 600 }}>Tên sản phẩm</TableCell>
                    <TableCell sx={{ fontWeight: 600 }}>Thương hiệu</TableCell>
                    <TableCell sx={{ fontWeight: 600 }}>Danh mục</TableCell>
                    <TableCell sx={{ fontWeight: 600 }} align="center">
                      Trạng thái
                    </TableCell>
                    <TableCell sx={{ fontWeight: 600 }}>Mô tả</TableCell>
                    <TableCell sx={{ fontWeight: 600 }} align="center">
                      Thao tác
                    </TableCell>
                    <TableCell sx={{ fontWeight: 600 }} align="right">
                      Ngày tạo
                    </TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {filteredProducts.map((p) => (
                    <TableRow key={p.id} hover>
                      <TableCell sx={{ fontWeight: 600 }}>{p.name}</TableCell>
                      <TableCell>
                        <Chip
                          label={p.brandName}
                          size="small"
                          variant="outlined"
                        />
                      </TableCell>
                      <TableCell>{p.categoryName}</TableCell>
                      <TableCell align="center">
                        <Tooltip title="Bấm để đổi nhanh trạng thái" arrow>
                          <Chip
                            label={
                              p.status === "ACTIVE"
                                ? "Đang bán"
                                : p.status === "INACTIVE"
                                ? "Ngừng bán"
                                : "Nháp"
                            }
                            size="small"
                            color={
                              p.status === "ACTIVE"
                                ? "success"
                                : p.status === "INACTIVE"
                                ? "default"
                                : "warning"
                            }
                            variant={
                              p.status === "DRAFT" ? "outlined" : "filled"
                            }
                            onClick={(e) => handleOpenStatusMenu(e, p)}
                            deleteIcon={<ExpandMoreIcon fontSize="small" />}
                            onDelete={(e) =>
                              handleOpenStatusMenu(
                                e as unknown as React.MouseEvent<HTMLElement>,
                                p,
                              )
                            }
                            sx={{
                              cursor: "pointer",
                              fontWeight: 600,
                              "& .MuiChip-deleteIcon": {
                                color: "inherit",
                                marginRight: "4px",
                                marginLeft: "-2px",
                              },
                            }}
                            data-testid={`status-chip-${p.id}`}
                            aria-label={`Trạng thái: ${
                              p.status === "ACTIVE"
                                ? "Đang bán"
                                : p.status === "INACTIVE"
                                ? "Ngừng bán"
                                : "Nháp"
                            }`}
                          />
                        </Tooltip>
                      </TableCell>
                      <TableCell sx={{ maxWidth: 250 }}>
                        <Typography
                          variant="body2"
                          color="text.secondary"
                          noWrap
                          title={p.description || ""}
                        >
                          {p.description || "—"}
                        </Typography>
                      </TableCell>
                      <TableCell align="center">
                        <Stack direction="row" spacing={0.25} justifyContent="center" alignItems="center">
                          <Tooltip title="Hoàn thiện sản phẩm" arrow>
                            <Button
                              size="small"
                              variant="contained"
                              onClick={() => handleOpenProductWorkspace(p)}
                              sx={{ minWidth: 0, px: 1.25, whiteSpace: "nowrap" }}
                            >
                              Hoàn thiện
                            </Button>
                          </Tooltip>
                          <Tooltip title="Chỉnh sửa thông tin cơ bản" arrow>
                            <IconButton
                              size="small"
                              color="primary"
                              aria-label={`Sửa ${p.name}`}
                              onClick={() => handleOpenEditDialog(p)}
                            >
                              <EditOutlinedIcon fontSize="small" />
                            </IconButton>
                          </Tooltip>
                          <Tooltip title="Quản lý biến thể" arrow>
                            <IconButton
                              size="small"
                              color="primary"
                              aria-label={`Biến thể ${p.name}`}
                              onClick={() => handleOpenVariantsDialog(p)}
                            >
                              <TuneIcon fontSize="small" />
                            </IconButton>
                          </Tooltip>
                          <Tooltip title="Quản lý hình ảnh" arrow>
                            <IconButton
                              size="small"
                              color="info"
                              aria-label={`Hình ảnh ${p.name}`}
                              onClick={() => handleOpenImagesDialog(p)}
                            >
                              <CollectionsIcon fontSize="small" />
                            </IconButton>
                          </Tooltip>
                          <Tooltip title="Quản lý thông số" arrow>
                            <IconButton
                              size="small"
                              color="secondary"
                              aria-label={`Thông số ${p.name}`}
                              onClick={() => handleOpenSpecificationsDialog(p)}
                            >
                              <ListAltIcon fontSize="small" />
                            </IconButton>
                          </Tooltip>
                          <Tooltip title="Xoá sản phẩm" arrow>
                            <IconButton
                              size="small"
                              color="error"
                              aria-label={`Xoá ${p.name}`}
                              onClick={() => handleOpenDeleteDialog(p)}
                            >
                              <DeleteOutlineIcon fontSize="small" />
                            </IconButton>
                          </Tooltip>
                        </Stack>
                      </TableCell>
                      <TableCell align="right">
                        <Typography variant="caption" color="text.secondary">
                          {new Date(p.createdAt).toLocaleDateString("vi-VN")}
                        </Typography>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </CardContent>
      </Card>

      {/* Menu chuyển nhanh trạng thái */}
      <Menu
        anchorEl={statusMenuAnchorEl}
        open={Boolean(statusMenuAnchorEl)}
        onClose={handleCloseStatusMenu}
        anchorOrigin={{ vertical: "bottom", horizontal: "center" }}
        transformOrigin={{ vertical: "top", horizontal: "center" }}
      >
        <MenuItem
          onClick={() => handleSelectStatus("ACTIVE")}
          selected={selectedProductForStatus?.status === "ACTIVE"}
        >
          <Stack direction="row" spacing={1.5} alignItems="center">
            <Box
              sx={{
                width: 8,
                height: 8,
                borderRadius: "50%",
                bgcolor: "success.main",
              }}
            />
            <Typography
              variant="body2"
              fontWeight={
                selectedProductForStatus?.status === "ACTIVE" ? 600 : 400
              }
            >
              Đang bán (ACTIVE)
            </Typography>
          </Stack>
        </MenuItem>
        <MenuItem
          onClick={() => handleSelectStatus("INACTIVE")}
          selected={selectedProductForStatus?.status === "INACTIVE"}
        >
          <Stack direction="row" spacing={1.5} alignItems="center">
            <Box
              sx={{
                width: 8,
                height: 8,
                borderRadius: "50%",
                bgcolor: "grey.500",
              }}
            />
            <Typography
              variant="body2"
              fontWeight={
                selectedProductForStatus?.status === "INACTIVE" ? 600 : 400
              }
            >
              Ngừng bán (INACTIVE)
            </Typography>
          </Stack>
        </MenuItem>
        <MenuItem
          onClick={() => handleSelectStatus("DRAFT")}
          selected={selectedProductForStatus?.status === "DRAFT"}
        >
          <Stack direction="row" spacing={1.5} alignItems="center">
            <Box
              sx={{
                width: 8,
                height: 8,
                borderRadius: "50%",
                bgcolor: "warning.main",
              }}
            />
            <Typography
              variant="body2"
              fontWeight={
                selectedProductForStatus?.status === "DRAFT" ? 600 : 400
              }
            >
              Nháp (DRAFT)
            </Typography>
          </Stack>
        </MenuItem>
      </Menu>

      {/* Dialog tạo / sửa sản phẩm */}
      <Dialog
        open={isDialogOpen}
        onClose={handleCloseDialog}
        maxWidth="sm"
        fullWidth
      >
        <form onSubmit={handleSubmit} noValidate>
          <DialogTitle sx={{ pb: 1 }}>
            <Typography component="span" variant="h6" fontWeight={800} display="block">
              {editingProduct
                ? "Chỉnh sửa thông tin sản phẩm"
                : "Tạo sản phẩm mới"}
            </Typography>
            <Typography component="span" variant="body2" color="text.secondary" display="block" sx={{ mt: 0.5 }}>
              {editingProduct
                ? "Lưu thay đổi xong, bạn có thể tiếp tục hoàn thiện biến thể, ảnh và thông số ở một nơi."
                : "Chỉ cần điền thông tin cơ bản. Các bước biến thể, ảnh và thông số sẽ xuất hiện sau khi tạo."}
            </Typography>
          </DialogTitle>
          <DialogContent dividers>
            <Stack spacing={2.5} sx={{ pt: 1 }}>
              <TextField
                label="Tên sản phẩm"
                required
                fullWidth
                value={formData.name}
                onChange={(e) =>
                  setFormData({ ...formData, name: e.target.value })
                }
                error={Boolean(formErrors.name)}
                helperText={
                  formErrors.name ||
                  "Tên sản phẩm không được trùng trong cùng một thương hiệu."
                }
                disabled={isSubmitting}
                autoFocus
              />

              <FormControl
                fullWidth
                required
                error={Boolean(formErrors.brandId)}
                disabled={isSubmitting}
              >
                <InputLabel id="brand-select-label">Thương hiệu</InputLabel>
                <Select
                  labelId="brand-select-label"
                  label="Thương hiệu *"
                  value={formData.brandId || ""}
                  onChange={(e) =>
                    setFormData({
                      ...formData,
                      brandId: Number(e.target.value),
                    })
                  }
                  inputProps={{ "data-testid": "brand-select" }}
                >
                  <MenuItem value="" disabled>
                    Chọn thương hiệu
                  </MenuItem>
                  {brands.map((b) => (
                    <MenuItem key={b.id} value={b.id}>
                      {b.name}
                    </MenuItem>
                  ))}
                </Select>
                {formErrors.brandId && (
                  <FormHelperText>{formErrors.brandId}</FormHelperText>
                )}
              </FormControl>

              <FormControl
                fullWidth
                required
                error={Boolean(formErrors.categoryId)}
                disabled={isSubmitting}
              >
                <InputLabel id="category-select-label">Danh mục</InputLabel>
                <Select
                  labelId="category-select-label"
                  label="Danh mục *"
                  value={formData.categoryId || ""}
                  onChange={(e) =>
                    setFormData({
                      ...formData,
                      categoryId: Number(e.target.value),
                    })
                  }
                  inputProps={{ "data-testid": "category-select" }}
                >
                  <MenuItem value="" disabled>
                    Chọn danh mục
                  </MenuItem>
                  {categories.map((c) => (
                    <MenuItem key={c.id} value={c.id}>
                      {c.parentName ? `${c.parentName} ➔ ${c.name}` : c.name}
                    </MenuItem>
                  ))}
                </Select>
                {formErrors.categoryId && (
                  <FormHelperText>{formErrors.categoryId}</FormHelperText>
                )}
              </FormControl>

              <FormControl fullWidth disabled={isSubmitting}>
                <InputLabel id="status-select-label">
                  {editingProduct ? "Trạng thái sản phẩm" : "Trạng thái ban đầu"}
                </InputLabel>
                <Select
                  labelId="status-select-label"
                  label={
                    editingProduct
                      ? "Trạng thái sản phẩm"
                      : "Trạng thái ban đầu"
                  }
                  value={formData.status || "DRAFT"}
                  onChange={(e) =>
                    setFormData({
                      ...formData,
                      status: e.target.value as ProductStatus,
                    })
                  }
                  inputProps={{ "data-testid": "status-select" }}
                >
                  <MenuItem value="DRAFT">Nháp (DRAFT)</MenuItem>
                  <MenuItem value="ACTIVE">
                    Đang bán (ACTIVE - Yêu cầu có ít nhất 1 biến thể)
                  </MenuItem>
                  {editingProduct && (
                    <MenuItem value="INACTIVE">Ngừng bán (INACTIVE)</MenuItem>
                  )}
                </Select>
                <FormHelperText>
                  {editingProduct
                    ? "Chuyển sang Đang bán yêu cầu sản phẩm phải có ít nhất một biến thể hợp lệ."
                    : "Sản phẩm mới tạo sẽ ở trạng thái 'Nháp' cho đến khi có ít nhất một biến thể hợp lệ."}
                </FormHelperText>
              </FormControl>

              <TextField
                label="Mô tả sản phẩm"
                fullWidth
                multiline
                rows={3}
                placeholder="Nhập mô tả giới thiệu tổng quan về sản phẩm..."
                value={formData.description || ""}
                onChange={(e) =>
                  setFormData({ ...formData, description: e.target.value })
                }
                disabled={isSubmitting}
              />
            </Stack>
          </DialogContent>
          <DialogActions sx={{ px: 3, py: 2 }}>
            <Button onClick={handleCloseDialog} disabled={isSubmitting}>
              Huỷ
            </Button>
            <Button
              type="submit"
              variant="contained"
              disabled={isSubmitting}
              startIcon={isSubmitting ? <CircularProgress size={16} /> : null}
            >
              {editingProduct ? "Cập nhật sản phẩm" : "Tạo sản phẩm"}
            </Button>
          </DialogActions>
        </form>
      </Dialog>

      {/* A guided workspace replaces the old hunt through five separate row actions. */}
      <AdminProductPanel
        open={isWorkspaceOpen}
        onClose={handleCloseProductWorkspace}
        maxWidth="md"
        fullWidth
        aria-labelledby="product-workspace-title"
      >
        <DialogTitle id="product-workspace-title" sx={{ pb: 1.25 }}>
          <Stack direction={{ xs: "column", sm: "row" }} justifyContent="space-between" spacing={1.25}>
            <Box>
              <Typography variant="overline" color="primary.main" fontWeight={800}>
                QUY TRÌNH HOÀN THIỆN
              </Typography>
              <Typography variant="h6" fontWeight={800}>
                {selectedProductForWorkspace?.name}
              </Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mt: 0.25 }}>
                Hoàn thành lần lượt các phần bên dưới. Bạn có thể đóng và quay lại bất cứ lúc nào.
              </Typography>
            </Box>
            {selectedProductForWorkspace && (
              <Chip
                label={
                  selectedProductForWorkspace.status === "ACTIVE"
                    ? "Đang bán"
                    : selectedProductForWorkspace.status === "INACTIVE"
                      ? "Ngừng bán"
                      : "Nháp"
                }
                color={
                  selectedProductForWorkspace.status === "ACTIVE"
                    ? "success"
                    : selectedProductForWorkspace.status === "INACTIVE"
                      ? "default"
                      : "warning"
                }
                size="small"
                sx={{ alignSelf: { xs: "flex-start", sm: "center" }, fontWeight: 700 }}
              />
            )}
          </Stack>
        </DialogTitle>
        <DialogContent dividers sx={{ bgcolor: "background.default", py: 2.5 }}>
          <Alert severity="info" sx={{ mb: 2.5 }}>
            Muốn đưa sản phẩm lên bán? Hãy tạo ít nhất một biến thể hợp lệ trước, sau đó đổi trạng thái thành <strong>Đang bán</strong>.
          </Alert>
          <Box
            sx={{
              display: "grid",
              gridTemplateColumns: { xs: "1fr", sm: "repeat(2, minmax(0, 1fr))" },
              gap: 1.5,
            }}
          >
            <Card
              variant="outlined"
              sx={{ borderColor: "success.light", bgcolor: "rgba(46, 125, 50, 0.04)" }}
            >
              <CardContent>
                <Stack direction="row" spacing={1.25} alignItems="flex-start">
                  <TaskAltOutlinedIcon color="success" />
                  <Box minWidth={0} flex={1}>
                    <Typography fontWeight={800}>1. Thông tin cơ bản</Typography>
                    <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
                      Tên, thương hiệu, danh mục, mô tả và trạng thái đã được lưu.
                    </Typography>
                  </Box>
                </Stack>
                <Button
                  size="small"
                  fullWidth
                  sx={{ mt: 2 }}
                  endIcon={<ArrowForwardIcon fontSize="small" />}
                  onClick={handleEditFromWorkspace}
                >
                  Chỉnh sửa thông tin
                </Button>
              </CardContent>
            </Card>

            <Card variant="outlined">
              <CardContent>
                <Stack direction="row" spacing={1.25} alignItems="flex-start">
                  <Box
                    sx={{
                      width: 24,
                      height: 24,
                      display: "grid",
                      placeItems: "center",
                      borderRadius: "50%",
                      bgcolor: "primary.main",
                      color: "primary.contrastText",
                      fontSize: 12,
                      fontWeight: 800,
                      flexShrink: 0,
                    }}
                  >
                    2
                  </Box>
                  <Box minWidth={0} flex={1}>
                    <Typography fontWeight={800}>Biến thể và tồn kho</Typography>
                    <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
                      Thêm SKU, giá, màu/dung lượng và số lượng tồn. Đây là bước bắt buộc để bán.
                    </Typography>
                  </Box>
                </Stack>
                <Button
                  variant="contained"
                  size="small"
                  fullWidth
                  sx={{ mt: 2 }}
                  endIcon={<ArrowForwardIcon fontSize="small" />}
                  onClick={() => handleOpenWorkspaceEditor("variants")}
                >
                  Thêm biến thể
                </Button>
              </CardContent>
            </Card>

            <Card variant="outlined">
              <CardContent>
                <Stack direction="row" spacing={1.25} alignItems="flex-start">
                  <Box
                    sx={{
                      width: 24,
                      height: 24,
                      display: "grid",
                      placeItems: "center",
                      borderRadius: "50%",
                      bgcolor: "primary.main",
                      color: "primary.contrastText",
                      fontSize: 12,
                      fontWeight: 800,
                      flexShrink: 0,
                    }}
                  >
                    3
                  </Box>
                  <Box minWidth={0} flex={1}>
                    <Typography fontWeight={800}>Hình ảnh sản phẩm</Typography>
                    <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
                      Tải ảnh đại diện và ảnh chi tiết để khách hàng nhận biết sản phẩm nhanh hơn.
                    </Typography>
                  </Box>
                </Stack>
                <Button
                  size="small"
                  fullWidth
                  sx={{ mt: 2 }}
                  endIcon={<ArrowForwardIcon fontSize="small" />}
                  onClick={() => handleOpenWorkspaceEditor("images")}
                >
                  Thêm hình ảnh
                </Button>
              </CardContent>
            </Card>

            <Card variant="outlined">
              <CardContent>
                <Stack direction="row" spacing={1.25} alignItems="flex-start">
                  <Box
                    sx={{
                      width: 24,
                      height: 24,
                      display: "grid",
                      placeItems: "center",
                      borderRadius: "50%",
                      bgcolor: "primary.main",
                      color: "primary.contrastText",
                      fontSize: 12,
                      fontWeight: 800,
                      flexShrink: 0,
                    }}
                  >
                    4
                  </Box>
                  <Box minWidth={0} flex={1}>
                    <Typography fontWeight={800}>Thông số kỹ thuật</Typography>
                    <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
                      Bổ sung màn hình, chipset, kết nối hoặc các thông số đúng với từng loại sản phẩm.
                    </Typography>
                  </Box>
                </Stack>
                <Button
                  size="small"
                  fullWidth
                  sx={{ mt: 2 }}
                  endIcon={<ArrowForwardIcon fontSize="small" />}
                  onClick={() => handleOpenWorkspaceEditor("specifications")}
                >
                  Bổ sung thông số
                </Button>
              </CardContent>
            </Card>
          </Box>
        </DialogContent>
        <DialogActions sx={{ px: 3, py: 2 }}>
          <Button onClick={handleCloseProductWorkspace}>Hoàn tất sau</Button>
        </DialogActions>
      </AdminProductPanel>

      {/* Dialog xác nhận xoá sản phẩm */}
      <Dialog
        open={isDeleteDialogOpen}
        onClose={isDeleting ? undefined : handleCloseDeleteDialog}
        maxWidth="xs"
        fullWidth
      >
        <DialogTitle sx={{ fontWeight: 600 }}>Xác nhận xoá sản phẩm</DialogTitle>
        <DialogContent>
          <Typography variant="body2" sx={{ mb: 1 }}>
            Bạn có chắc chắn muốn xoá sản phẩm{" "}
            <strong>{deletingProduct?.name}</strong> không?
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Sản phẩm và các biến thể liên quan sẽ được ẩn khỏi hệ thống (xoá mềm). Nếu sản phẩm đã phát sinh đơn hàng, hệ thống sẽ ngăn chặn xoá và bạn cần chuyển trạng thái sang &quot;Ngừng bán&quot;.
          </Typography>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button
            onClick={handleCloseDeleteDialog}
            disabled={isDeleting}
            color="inherit"
          >
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

      {/* Dialog quản lý biến thể */}
      <AdminProductVariantsDialog
        open={isVariantsDialogOpen}
        product={selectedProductForVariants}
        onClose={handleCloseVariantsDialog}
        onVariantsChanged={fetchData}
      />

      {/* Dialog quản lý hình ảnh */}
      <AdminProductImagesDialog
        open={isImagesDialogOpen}
        product={selectedProductForImages}
        onClose={handleCloseImagesDialog}
        onImagesChanged={fetchData}
      />

      {/* Dialog quản lý thông số kỹ thuật */}
      <AdminProductSpecificationsDialog
        open={isSpecificationsDialogOpen}
        product={selectedProductForSpecifications}
        onClose={handleCloseSpecificationsDialog}
        onSpecificationsChanged={fetchData}
      />
    </Stack>
  );
}
