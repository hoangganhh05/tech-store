import { Button, Card, CardContent, Typography } from "@mui/material";
import { PageIntro } from "../../components/common/PageIntro";
import { Button, Card, CardContent, Typography } from "@mui/material";
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
  Typography,
} from "@mui/material";
import AddIcon from "@mui/icons-material/Add";
import RefreshIcon from "@mui/icons-material/Refresh";
import SearchIcon from "@mui/icons-material/Search";
import Inventory2OutlinedIcon from "@mui/icons-material/Inventory2Outlined";
import { isAxiosError } from "axios";
import { useCallback, useEffect, useState, type FormEvent } from "react";
import { PageIntro } from "../../components/common/PageIntro";
import {
  createAdminProduct,
  getAdminProducts,
  type Product,
  type ProductCreatePayload,
  type ProductStatus,
} from "../../services/productService";
import { getAdminBrands, type Brand } from "../../services/brandService";
import {
  getAdminCategories,
  type Category,
} from "../../services/categoryService";

export function AdminProductsPage() {
  return (
    <>
      <PageIntro
        eyebrow="Quản trị"
        title="Quản lý sản phẩm"
        description="Thêm, cập nhật và theo dõi danh mục sản phẩm."
        action={<Button variant="contained">Thêm sản phẩm</Button>}
      />
      <Card>
        <CardContent>
          <Typography color="text.secondary">
            Chưa có dữ liệu sản phẩm.
          </Typography>
        </CardContent>
      </Card>
    </>
  );
  return (
    <>
      <PageIntro
        eyebrow="Quản trị"
        title="Quản lý sản phẩm"
        description="Thêm, cập nhật và theo dõi danh mục sản phẩm."
        action={<Button variant="contained">Thêm sản phẩm</Button>}
      />
      <Card>
        <CardContent>
          <Typography color="text.secondary">
            Chưa có dữ liệu sản phẩm.
          </Typography>
        </CardContent>
      </Card>
    </>
  );
  const [products, setProducts] = useState<Product[]>([]);
  const [brands, setBrands] = useState<Brand[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [searchKeyword, setSearchKeyword] = useState("");
  const [feedbackMessage, setFeedbackMessage] = useState<{
    type: "success" | "error";
    text: string;
  } | null>(null);

  // Dialog state
  const [isDialogOpen, setIsDialogOpen] = useState(false);
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
    setFormData({
      name: "",
      description: "",
      brandId: brands.length > 0 ? brands[0].id : 0,
      categoryId: categories.length > 0 ? categories[0].id : 0,
      status: "DRAFT",
    });
    setFormErrors({});
    setIsDialogOpen(true);
  };

  const handleCloseDialog = () => {
    if (isSubmitting) return;
    setIsDialogOpen(false);
    setFormErrors({});
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
      const created = await createAdminProduct({
        name: trimmedName,
        description: formData.description?.trim() || undefined,
        brandId: Number(formData.brandId),
        categoryId: Number(formData.categoryId),
        status: formData.status,
      });

      setFeedbackMessage({
        type: "success",
        text: `Đã tạo sản phẩm "${created.name}" thành công ở trạng thái nháp.`,
      });
      setIsDialogOpen(false);
      await fetchData();
    } catch (error: unknown) {
      const message = isAxiosError<{ message?: string }>(error)
        ? error.response?.data?.message
        : undefined;
      setFeedbackMessage({
        type: "error",
        text:
          message || "Không thể tạo sản phẩm. Vui lòng kiểm tra lại thông tin.",
      });
    } finally {
      setIsSubmitting(false);
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

  const getStatusChip = (status: ProductStatus) => {
    switch (status) {
      case "ACTIVE":
        return <Chip label="Đang bán" size="small" color="success" />;
      case "INACTIVE":
        return <Chip label="Ngừng bán" size="small" color="default" />;
      case "DRAFT":
      default:
        return (
          <Chip label="Nháp" size="small" color="warning" variant="outlined" />
        );
    }
  };

  return (
    <Stack spacing={3}>
      <PageIntro
        eyebrow="Quản trị"
        title="Quản lý sản phẩm"
        description="Thêm mới sản phẩm với thông tin cơ bản, quản lý trạng thái hiển thị và phân loại theo thương hiệu, danh mục."
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
                        {getStatusChip(p.status)}
                      </TableCell>
                      <TableCell sx={{ maxWidth: 300 }}>
                        <Typography
                          variant="body2"
                          color="text.secondary"
                          noWrap
                          title={p.description || ""}
                        >
                          {p.description || "—"}
                        </Typography>
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

      {/* Dialog tạo sản phẩm mới */}
      <Dialog
        open={isDialogOpen}
        onClose={handleCloseDialog}
        maxWidth="sm"
        fullWidth
      >
        <form onSubmit={handleSubmit}>
          <DialogTitle>Tạo sản phẩm mới</DialogTitle>
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
                >
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
                >
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
                  Trạng thái ban đầu
                </InputLabel>
                <Select
                  labelId="status-select-label"
                  label="Trạng thái ban đầu"
                  value={formData.status || "DRAFT"}
                  onChange={(e) =>
                    setFormData({
                      ...formData,
                      status: e.target.value as ProductStatus,
                    })
                  }
                >
                  <MenuItem value="DRAFT">Nháp (DRAFT - Khuyên dùng)</MenuItem>
                  <MenuItem value="ACTIVE">
                    Đang bán (ACTIVE - Yêu cầu có ít nhất 1 biến thể)
                  </MenuItem>
                </Select>
                <FormHelperText>
                  Sản phẩm mới tạo sẽ ở trạng thái 'Nháp' cho đến khi có ít nhất
                  một biến thể hợp lệ.
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
              Tạo sản phẩm
            </Button>
          </DialogActions>
        </form>
      </Dialog>
    </Stack>
  );
}
