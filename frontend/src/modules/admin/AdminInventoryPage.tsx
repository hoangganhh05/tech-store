import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  CircularProgress,
  FormControl,
  Grid,
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
  TablePagination,
  TableRow,
  TextField,
  Typography,
} from "@mui/material";
import SearchIcon from "@mui/icons-material/Search";
import RefreshIcon from "@mui/icons-material/Refresh";
import Inventory2OutlinedIcon from "@mui/icons-material/Inventory2Outlined";
import WarningAmberOutlinedIcon from "@mui/icons-material/WarningAmberOutlined";
import ErrorOutlineOutlinedIcon from "@mui/icons-material/ErrorOutlineOutlined";
import CheckCircleOutlineOutlinedIcon from "@mui/icons-material/CheckCircleOutlineOutlined";
import { useCallback, useEffect, useState } from "react";
import { PageIntro } from "../../components/common/PageIntro";
import {
  getInventories,
  getInventorySummary,
  type InventoryItem,
  type InventorySummary,
  type StockStatus,
} from "../../services/inventoryService";
import {
  getAdminCategories,
  type Category,
} from "../../services/categoryService";

export function AdminInventoryPage() {
  const [items, setItems] = useState<InventoryItem[]>([]);
  const [summary, setSummary] = useState<InventorySummary | null>(null);
  const [categories, setCategories] = useState<Category[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Filters & Pagination
  const [searchTerm, setSearchTerm] = useState("");
  const [selectedCategory, setSelectedCategory] = useState<string>("");
  const [selectedStatus, setSelectedStatus] = useState<StockStatus>("ALL");
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(10);
  const [totalElements, setTotalElements] = useState(0);

  const fetchCategories = useCallback(async () => {
    try {
      const data = await getAdminCategories();
      setCategories(data);
    } catch {
      // Ignore category load error in inventory view
    }
  }, []);

  const fetchSummary = useCallback(async () => {
    try {
      const data = await getInventorySummary();
      setSummary(data);
    } catch {
      // Non-blocking summary error
    }
  }, []);

  const fetchInventories = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await getInventories({
        search: searchTerm || undefined,
        categoryId: selectedCategory ? Number(selectedCategory) : undefined,
        stockStatus: selectedStatus,
        page,
        size: rowsPerPage,
      });
      setItems(response.items);
      setTotalElements(response.totalElements);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "Không thể tải dữ liệu tồn kho";
      setError(msg);
    } finally {
      setLoading(false);
    }
  }, [searchTerm, selectedCategory, selectedStatus, page, rowsPerPage]);

  useEffect(() => {
    fetchCategories();
    fetchSummary();
  }, [fetchCategories, fetchSummary]);

  useEffect(() => {
    fetchInventories();
  }, [fetchInventories]);

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setPage(0);
    fetchInventories();
  };

  const handleResetFilters = () => {
    setSearchTerm("");
    setSelectedCategory("");
    setSelectedStatus("ALL");
    setPage(0);
  };

  const handlePageChange = (_: unknown, newPage: number) => {
    setPage(newPage);
  };

  const handleRowsPerPageChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    setRowsPerPage(parseInt(event.target.value, 10));
    setPage(0);
  };

  const renderStockBadge = (status: StockStatus) => {
    switch (status) {
      case "IN_STOCK":
        return <Chip label="Còn hàng" color="success" size="small" variant="filled" />;
      case "LOW_STOCK":
        return <Chip label="Sắp hết" color="warning" size="small" variant="filled" />;
      case "OUT_OF_STOCK":
        return <Chip label="Hết hàng" color="error" size="small" variant="filled" />;
      default:
        return <Chip label={status} size="small" />;
    }
  };

  return (
    <Box sx={{ p: { xs: 2, md: 3 } }}>
      <PageIntro
        title="Quản lý tồn kho"
        description="Theo dõi và kiểm soát số lượng tồn kho theo từng biến thể sản phẩm theo thời gian thực."
      />

      {/* Overview Stat Cards */}
      <Grid container spacing={2} sx={{ mb: 3 }}>
        <Grid size={{ xs: 12, sm: 6, md: 3 }}>
          <Card variant="outlined" sx={{ borderRadius: 2 }}>
            <CardContent>
              <Stack direction="row" alignItems="center" justifyContent="space-between">
                <Box>
                  <Typography variant="caption" color="text.secondary" fontWeight={700}>
                    TỔNG BIẾN THỂ
                  </Typography>
                  <Typography variant="h4" fontWeight={800} sx={{ mt: 0.5 }}>
                    {summary ? summary.totalVariants : "-"}
                  </Typography>
                </Box>
                <Inventory2OutlinedIcon color="primary" sx={{ fontSize: 36, opacity: 0.8 }} />
              </Stack>
            </CardContent>
          </Card>
        </Grid>

        <Grid size={{ xs: 12, sm: 6, md: 3 }}>
          <Card variant="outlined" sx={{ borderRadius: 2 }}>
            <CardContent>
              <Stack direction="row" alignItems="center" justifyContent="space-between">
                <Box>
                  <Typography variant="caption" color="text.secondary" fontWeight={700}>
                    CÒN HÀNG
                  </Typography>
                  <Typography variant="h4" fontWeight={800} color="success.main" sx={{ mt: 0.5 }}>
                    {summary ? summary.inStockCount : "-"}
                  </Typography>
                </Box>
                <CheckCircleOutlineOutlinedIcon color="success" sx={{ fontSize: 36, opacity: 0.8 }} />
              </Stack>
            </CardContent>
          </Card>
        </Grid>

        <Grid size={{ xs: 12, sm: 6, md: 3 }}>
          <Card variant="outlined" sx={{ borderRadius: 2 }}>
            <CardContent>
              <Stack direction="row" alignItems="center" justifyContent="space-between">
                <Box>
                  <Typography variant="caption" color="text.secondary" fontWeight={700}>
                    SẮP HẾT HÀNG
                  </Typography>
                  <Typography variant="h4" fontWeight={800} color="warning.main" sx={{ mt: 0.5 }}>
                    {summary ? summary.lowStockCount : "-"}
                  </Typography>
                </Box>
                <WarningAmberOutlinedIcon color="warning" sx={{ fontSize: 36, opacity: 0.8 }} />
              </Stack>
            </CardContent>
          </Card>
        </Grid>

        <Grid size={{ xs: 12, sm: 6, md: 3 }}>
          <Card variant="outlined" sx={{ borderRadius: 2 }}>
            <CardContent>
              <Stack direction="row" alignItems="center" justifyContent="space-between">
                <Box>
                  <Typography variant="caption" color="text.secondary" fontWeight={700}>
                    HẾT HÀNG
                  </Typography>
                  <Typography variant="h4" fontWeight={800} color="error.main" sx={{ mt: 0.5 }}>
                    {summary ? summary.outOfStockCount : "-"}
                  </Typography>
                </Box>
                <ErrorOutlineOutlinedIcon color="error" sx={{ fontSize: 36, opacity: 0.8 }} />
              </Stack>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* Filter and Search Panel */}
      <Paper variant="outlined" sx={{ p: 2, mb: 3, borderRadius: 2 }}>
        <Box component="form" onSubmit={handleSearchSubmit}>
          <Grid container spacing={2} alignItems="center">
            <Grid size={{ xs: 12, md: 4 }}>
              <TextField
                fullWidth
                size="small"
                label="Tìm kiếm sản phẩm, SKU..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                InputProps={{
                  startAdornment: (
                    <InputAdornment position="start">
                      <SearchIcon fontSize="small" />
                    </InputAdornment>
                  ),
                }}
              />
            </Grid>

            <Grid size={{ xs: 12, sm: 6, md: 3 }}>
              <FormControl fullWidth size="small">
                <InputLabel id="category-filter-label">Danh mục</InputLabel>
                <Select
                  labelId="category-filter-label"
                  label="Danh mục"
                  value={selectedCategory}
                  onChange={(e) => {
                    setSelectedCategory(e.target.value);
                    setPage(0);
                  }}
                >
                  <MenuItem value="">Tất cả danh mục</MenuItem>
                  {categories.map((c) => (
                    <MenuItem key={c.id} value={String(c.id)}>
                      {c.name}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Grid>

            <Grid size={{ xs: 12, sm: 6, md: 3 }}>
              <FormControl fullWidth size="small">
                <InputLabel id="status-filter-label">Mức tồn</InputLabel>
                <Select
                  labelId="status-filter-label"
                  label="Mức tồn"
                  value={selectedStatus}
                  onChange={(e) => {
                    setSelectedStatus(e.target.value as StockStatus);
                    setPage(0);
                  }}
                >
                  <MenuItem value="ALL">Tất cả trạng thái</MenuItem>
                  <MenuItem value="IN_STOCK">Còn hàng</MenuItem>
                  <MenuItem value="LOW_STOCK">Sắp hết hàng</MenuItem>
                  <MenuItem value="OUT_OF_STOCK">Hết hàng</MenuItem>
                </Select>
              </FormControl>
            </Grid>

            <Grid size={{ xs: 12, md: 2 }}>
              <Stack direction="row" spacing={1}>
                <Button
                  type="submit"
                  variant="contained"
                  fullWidth
                  startIcon={<SearchIcon />}
                >
                  Lọc
                </Button>
                <Button
                  variant="outlined"
                  onClick={handleResetFilters}
                  title="Đặt lại bộ lọc"
                >
                  <RefreshIcon />
                </Button>
              </Stack>
            </Grid>
          </Grid>
        </Box>
      </Paper>

      {/* Error display */}
      {error && (
        <Alert
          severity="error"
          sx={{ mb: 2 }}
          action={
            <Button color="inherit" size="small" onClick={fetchInventories}>
              Thử lại
            </Button>
          }
        >
          {error}
        </Alert>
      )}

      {/* Inventory Table */}
      <TableContainer component={Paper} variant="outlined" sx={{ borderRadius: 2 }}>
        <Table sx={{ minWidth: 750 }}>
          <TableHead sx={{ bgcolor: "action.hover" }}>
            <TableRow>
              <TableCell sx={{ fontWeight: 700 }}>SẢN PHẨM / SKU</TableCell>
              <TableCell sx={{ fontWeight: 700 }}>BIẾN THỂ</TableCell>
              <TableCell align="right" sx={{ fontWeight: 700 }}>
                KHẢ DỤNG
              </TableCell>
              <TableCell align="right" sx={{ fontWeight: 700 }}>
                ĐANG GIỮ
              </TableCell>
              <TableCell align="right" sx={{ fontWeight: 700 }}>
                TỔNG TỒN
              </TableCell>
              <TableCell align="right" sx={{ fontWeight: 700 }}>
                NGƯỠNG
              </TableCell>
              <TableCell align="center" sx={{ fontWeight: 700 }}>
                TRẠNG THÁI
              </TableCell>
            </TableRow>
          </TableHead>

          <TableBody>
            {loading ? (
              <TableRow>
                <TableCell colSpan={7} align="center" sx={{ py: 6 }}>
                  <CircularProgress size={36} />
                  <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                    Đang tải dữ liệu tồn kho...
                  </Typography>
                </TableCell>
              </TableRow>
            ) : items.length === 0 ? (
              <TableRow>
                <TableCell colSpan={7} align="center" sx={{ py: 6 }}>
                  <Typography variant="body1" color="text.secondary">
                    Không tìm thấy biến thể nào phù hợp với điều kiện tìm kiếm.
                  </Typography>
                </TableCell>
              </TableRow>
            ) : (
              items.map((item) => (
                <TableRow key={item.id} hover>
                  <TableCell>
                    <Typography variant="body2" fontWeight={700}>
                      {item.productName}
                    </Typography>
                    <Typography
                      variant="caption"
                      sx={{
                        fontFamily: "monospace",
                        bgcolor: "grey.100",
                        px: 0.8,
                        py: 0.2,
                        borderRadius: 1,
                        color: "text.secondary",
                      }}
                    >
                      {item.sku}
                    </Typography>
                  </TableCell>

                  <TableCell>
                    <Typography variant="body2">
                      {[item.color, item.storage].filter(Boolean).join(" · ") || "Mặc định"}
                    </Typography>
                    {item.categoryName && (
                      <Typography variant="caption" color="text.secondary">
                        {item.categoryName}
                      </Typography>
                    )}
                  </TableCell>

                  <TableCell align="right">
                    <Typography
                      variant="body2"
                      fontWeight={800}
                      color={
                        item.availableQuantity === 0
                          ? "error.main"
                          : item.availableQuantity <= item.lowStockThreshold
                          ? "warning.main"
                          : "text.primary"
                      }
                    >
                      {item.availableQuantity.toLocaleString("vi-VN")}
                    </Typography>
                  </TableCell>

                  <TableCell align="right">
                    <Typography variant="body2" color="text.secondary">
                      {item.quantityReserved.toLocaleString("vi-VN")}
                    </Typography>
                  </TableCell>

                  <TableCell align="right">
                    <Typography variant="body2">
                      {item.quantityOnHand.toLocaleString("vi-VN")}
                    </Typography>
                  </TableCell>

                  <TableCell align="right">
                    <Typography variant="body2" color="text.secondary">
                      {item.lowStockThreshold}
                    </Typography>
                  </TableCell>

                  <TableCell align="center">
                    {renderStockBadge(item.stockStatus)}
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>

        <TablePagination
          rowsPerPageOptions={[5, 10, 20, 50]}
          component="div"
          count={totalElements}
          rowsPerPage={rowsPerPage}
          page={page}
          onPageChange={handlePageChange}
          onRowsPerPageChange={handleRowsPerPageChange}
          labelRowsPerPage="Số hàng mỗi trang:"
          labelDisplayedRows={({ from, to, count }) =>
            `${from}–${to} trong số ${count !== -1 ? count : `hơn ${to}`}`
          }
        />
      </TableContainer>
    </Box>
  );
}
