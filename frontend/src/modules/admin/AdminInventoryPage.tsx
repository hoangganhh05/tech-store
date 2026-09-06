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
  Divider,
  FormControl,
  Grid,
  InputAdornment,
  InputLabel,
  MenuItem,
  Paper,
  Select,
  Snackbar,
  Stack,
  Tab,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TablePagination,
  TableRow,
  Tabs,
  TextField,
  Typography,
} from "@mui/material";
import SearchIcon from "@mui/icons-material/Search";
import RefreshIcon from "@mui/icons-material/Refresh";
import Inventory2OutlinedIcon from "@mui/icons-material/Inventory2Outlined";
import WarningAmberOutlinedIcon from "@mui/icons-material/WarningAmberOutlined";
import ErrorOutlineOutlinedIcon from "@mui/icons-material/ErrorOutlineOutlined";
import CheckCircleOutlineOutlinedIcon from "@mui/icons-material/CheckCircleOutlineOutlined";
import AddCircleOutlineIcon from "@mui/icons-material/AddCircleOutline";
import HistoryIcon from "@mui/icons-material/History";
import { useCallback, useEffect, useState } from "react";
import { PageIntro } from "../../components/common/PageIntro";
import {
  getInventories,
  getInventorySummary,
  getInventoryTransactions,
  importInventory,
  type InventoryItem,
  type InventorySummary,
  type InventoryTransactionItem,
  type StockStatus,
} from "../../services/inventoryService";
import {
  getAdminCategories,
  type Category,
} from "../../services/categoryService";

export function AdminInventoryPage() {
  const [tabValue, setTabValue] = useState(0);

  // Inventories state
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

  // Transactions state
  const [transactions, setTransactions] = useState<InventoryTransactionItem[]>(
    [],
  );
  const [txLoading, setTxLoading] = useState(false);
  const [txPage, setTxPage] = useState(0);
  const [txRowsPerPage, setTxRowsPerPage] = useState(10);
  const [txTotalElements, setTxTotalElements] = useState(0);

  // Import Dialog state
  const [openImportDialog, setOpenImportDialog] = useState(false);
  const [selectedVariantId, setSelectedVariantId] = useState<number | "">("");
  const [importQuantity, setImportQuantity] = useState<number | "">(1);
  const [importNote, setImportNote] = useState("");
  const [importSubmitting, setImportSubmitting] = useState(false);
  const [importError, setImportError] = useState<string | null>(null);

  // Snackbar feedback
  const [snackbarMessage, setSnackbarMessage] = useState<string | null>(null);
  const [snackbarSeverity, setSnackbarSeverity] = useState<"success" | "error">(
    "success",
  );

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
      const msg =
        err instanceof Error ? err.message : "Không thể tải dữ liệu tồn kho";
      setError(msg);
    } finally {
      setLoading(false);
    }
  }, [searchTerm, selectedCategory, selectedStatus, page, rowsPerPage]);

  const fetchTransactions = useCallback(async () => {
    setTxLoading(true);
    try {
      const response = await getInventoryTransactions({
        type: "IMPORT",
        page: txPage,
        size: txRowsPerPage,
      });
      setTransactions(response.items);
      setTxTotalElements(response.totalElements);
    } catch {
      // Non-blocking
    } finally {
      setTxLoading(false);
    }
  }, [txPage, txRowsPerPage]);

  useEffect(() => {
    fetchCategories();
    fetchSummary();
  }, [fetchCategories, fetchSummary]);

  useEffect(() => {
    if (tabValue === 0) {
      fetchInventories();
    } else {
      fetchTransactions();
    }
  }, [tabValue, fetchInventories, fetchTransactions]);

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

  const handleRowsPerPageChange = (
    event: React.ChangeEvent<HTMLInputElement>,
  ) => {
    setRowsPerPage(parseInt(event.target.value, 10));
    setPage(0);
  };

  const handleOpenImportDialog = (item?: InventoryItem) => {
    if (item) {
      setSelectedVariantId(item.variantId);
    } else if (items.length > 0) {
      setSelectedVariantId(items[0].variantId);
    } else {
      setSelectedVariantId("");
    }
    setImportQuantity(1);
    setImportNote("");
    setImportError(null);
    setOpenImportDialog(true);
  };

  const handleCloseImportDialog = () => {
    if (importSubmitting) return;
    setOpenImportDialog(false);
    setImportError(null);
  };

  const handleImportSubmit = async (e?: React.FormEvent) => {
    if (e) {
      e.preventDefault();
    }
    if (!selectedVariantId) {
      setImportError("Vui lòng chọn biến thể sản phẩm");
      return;
    }
    const qty = Number(importQuantity);
    if (!qty || qty <= 0) {
      setImportError("Số lượng nhập phải lớn hơn 0");
      return;
    }

    setImportSubmitting(true);
    setImportError(null);
    try {
      await importInventory({
        variantId: Number(selectedVariantId),
        quantity: qty,
        note: importNote.trim() || undefined,
      });

      setSnackbarSeverity("success");
      setSnackbarMessage(`Nhập kho thành công ${qty} sản phẩm!`);
      setOpenImportDialog(false);

      // Refresh data
      fetchInventories();
      fetchSummary();
      if (tabValue === 1) {
        fetchTransactions();
      }
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "Nhập kho thất bại";
      setImportError(msg);
    } finally {
      setImportSubmitting(false);
    }
  };

  const currentSelectedItem = items.find(
    (i) => i.variantId === selectedVariantId,
  );

  const renderStockBadge = (status: StockStatus) => {
    switch (status) {
      case "IN_STOCK":
        return (
          <Chip
            label="Còn hàng"
            color="success"
            size="small"
            variant="filled"
          />
        );
      case "LOW_STOCK":
        return (
          <Chip label="Sắp hết" color="warning" size="small" variant="filled" />
        );
      case "OUT_OF_STOCK":
        return (
          <Chip label="Hết hàng" color="error" size="small" variant="filled" />
        );
      default:
        return <Chip label={status} size="small" />;
    }
  };

  return (
    <Box sx={{ p: { xs: 2, md: 3 } }}>
      <Stack
        direction={{ xs: "column", sm: "row" }}
        justifyContent="space-between"
        alignItems={{ xs: "flex-start", sm: "center" }}
        spacing={2}
        sx={{ mb: 2 }}
      >
        <PageIntro
          title="Quản lý tồn kho"
          description="Theo dõi và kiểm soát số lượng tồn kho theo từng biến thể sản phẩm theo thời gian thực."
        />
        <Button
          variant="contained"
          startIcon={<AddCircleOutlineIcon />}
          onClick={() => handleOpenImportDialog()}
          data-testid="btn-header-import"
          sx={{ fontWeight: 700, px: 2.5, py: 1 }}
        >
          Nhập kho
        </Button>
      </Stack>

      {/* Overview Stat Cards */}
      <Grid container spacing={2} sx={{ mb: 3 }}>
        <Grid size={{ xs: 12, sm: 6, md: 3 }}>
          <Card variant="outlined" sx={{ borderRadius: 2 }}>
            <CardContent>
              <Stack
                direction="row"
                alignItems="center"
                justifyContent="space-between"
              >
                <Box>
                  <Typography
                    variant="caption"
                    color="text.secondary"
                    fontWeight={700}
                  >
                    TỔNG BIẾN THỂ
                  </Typography>
                  <Typography variant="h4" fontWeight={800} sx={{ mt: 0.5 }}>
                    {summary ? summary.totalVariants : "-"}
                  </Typography>
                </Box>
                <Inventory2OutlinedIcon
                  color="primary"
                  sx={{ fontSize: 36, opacity: 0.8 }}
                />
              </Stack>
            </CardContent>
          </Card>
        </Grid>

        <Grid size={{ xs: 12, sm: 6, md: 3 }}>
          <Card variant="outlined" sx={{ borderRadius: 2 }}>
            <CardContent>
              <Stack
                direction="row"
                alignItems="center"
                justifyContent="space-between"
              >
                <Box>
                  <Typography
                    variant="caption"
                    color="text.secondary"
                    fontWeight={700}
                  >
                    CÒN HÀNG
                  </Typography>
                  <Typography
                    variant="h4"
                    fontWeight={800}
                    color="success.main"
                    sx={{ mt: 0.5 }}
                  >
                    {summary ? summary.inStockCount : "-"}
                  </Typography>
                </Box>
                <CheckCircleOutlineOutlinedIcon
                  color="success"
                  sx={{ fontSize: 36, opacity: 0.8 }}
                />
              </Stack>
            </CardContent>
          </Card>
        </Grid>

        <Grid size={{ xs: 12, sm: 6, md: 3 }}>
          <Card variant="outlined" sx={{ borderRadius: 2 }}>
            <CardContent>
              <Stack
                direction="row"
                alignItems="center"
                justifyContent="space-between"
              >
                <Box>
                  <Typography
                    variant="caption"
                    color="text.secondary"
                    fontWeight={700}
                  >
                    SẮP HẾT HÀNG
                  </Typography>
                  <Typography
                    variant="h4"
                    fontWeight={800}
                    color="warning.main"
                    sx={{ mt: 0.5 }}
                  >
                    {summary ? summary.lowStockCount : "-"}
                  </Typography>
                </Box>
                <WarningAmberOutlinedIcon
                  color="warning"
                  sx={{ fontSize: 36, opacity: 0.8 }}
                />
              </Stack>
            </CardContent>
          </Card>
        </Grid>

        <Grid size={{ xs: 12, sm: 6, md: 3 }}>
          <Card variant="outlined" sx={{ borderRadius: 2 }}>
            <CardContent>
              <Stack
                direction="row"
                alignItems="center"
                justifyContent="space-between"
              >
                <Box>
                  <Typography
                    variant="caption"
                    color="text.secondary"
                    fontWeight={700}
                  >
                    HẾT HÀNG
                  </Typography>
                  <Typography
                    variant="h4"
                    fontWeight={800}
                    color="error.main"
                    sx={{ mt: 0.5 }}
                  >
                    {summary ? summary.outOfStockCount : "-"}
                  </Typography>
                </Box>
                <ErrorOutlineOutlinedIcon
                  color="error"
                  sx={{ fontSize: 36, opacity: 0.8 }}
                />
              </Stack>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* Tabs Navigation */}
      <Tabs
        value={tabValue}
        onChange={(_, val) => setTabValue(val)}
        sx={{ borderBottom: 1, borderColor: "divider", mb: 3 }}
      >
        <Tab
          icon={<Inventory2OutlinedIcon />}
          iconPosition="start"
          label="Danh sách tồn kho"
          data-testid="tab-inventory-list"
        />
        <Tab
          icon={<HistoryIcon />}
          iconPosition="start"
          label="Lịch sử nhập kho"
          data-testid="tab-inventory-history"
        />
      </Tabs>

      {tabValue === 0 && (
        <>
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
                          <SearchIcon color="action" />
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
                      onChange={(e) => setSelectedCategory(e.target.value)}
                    >
                      <MenuItem value="">Tất cả danh mục</MenuItem>
                      {categories.map((cat) => (
                        <MenuItem key={cat.id} value={String(cat.id)}>
                          {cat.name}
                        </MenuItem>
                      ))}
                    </Select>
                  </FormControl>
                </Grid>

                <Grid size={{ xs: 12, sm: 6, md: 3 }}>
                  <FormControl fullWidth size="small">
                    <InputLabel id="status-filter-label">
                      Trạng thái tồn
                    </InputLabel>
                    <Select
                      labelId="status-filter-label"
                      label="Trạng thái tồn"
                      value={selectedStatus}
                      onChange={(e) =>
                        setSelectedStatus(e.target.value as StockStatus)
                      }
                    >
                      <MenuItem value="ALL">Tất cả mức tồn</MenuItem>
                      <MenuItem value="IN_STOCK">Còn hàng</MenuItem>
                      <MenuItem value="LOW_STOCK">Sắp hết hàng</MenuItem>
                      <MenuItem value="OUT_OF_STOCK">Hết hàng</MenuItem>
                    </Select>
                  </FormControl>
                </Grid>

                <Grid size={{ xs: 12, md: 2 }}>
                  <Stack direction="row" spacing={1}>
                    <Button
                      fullWidth
                      variant="contained"
                      type="submit"
                      sx={{ height: 40 }}
                    >
                      Lọc
                    </Button>
                    <Button
                      variant="outlined"
                      onClick={handleResetFilters}
                      sx={{ minWidth: 40, px: 1, height: 40 }}
                      title="Đặt lại bộ lọc"
                    >
                      <RefreshIcon />
                    </Button>
                  </Stack>
                </Grid>
              </Grid>
            </Box>
          </Paper>

          {/* Error message */}
          {error && (
            <Alert
              severity="error"
              sx={{ mb: 3 }}
              onClose={() => setError(null)}
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
          <TableContainer
            component={Paper}
            variant="outlined"
            sx={{ borderRadius: 2 }}
          >
            <Table>
              <TableHead sx={{ bgcolor: "grey.50" }}>
                <TableRow>
                  <TableCell sx={{ fontWeight: 700 }}>SẢN PHẨM / SKU</TableCell>
                  <TableCell sx={{ fontWeight: 700 }}>THUỘC TÍNH</TableCell>
                  <TableCell align="right" sx={{ fontWeight: 700 }}>
                    KHẢ DỤNG
                  </TableCell>
                  <TableCell align="right" sx={{ fontWeight: 700 }}>
                    ĐANG GIỮ
                  </TableCell>
                  <TableCell align="right" sx={{ fontWeight: 700 }}>
                    TỒN THỰC TẾ
                  </TableCell>
                  <TableCell align="right" sx={{ fontWeight: 700 }}>
                    NGƯỠNG CẢNH BÁO
                  </TableCell>
                  <TableCell align="center" sx={{ fontWeight: 700 }}>
                    TRẠNG THÁI
                  </TableCell>
                  <TableCell align="center" sx={{ fontWeight: 700 }}>
                    THAO TÁC
                  </TableCell>
                </TableRow>
              </TableHead>

              <TableBody>
                {loading ? (
                  <TableRow>
                    <TableCell colSpan={8} align="center" sx={{ py: 6 }}>
                      <CircularProgress size={36} />
                      <Typography
                        variant="body2"
                        color="text.secondary"
                        sx={{ mt: 1 }}
                      >
                        Đang tải dữ liệu tồn kho...
                      </Typography>
                    </TableCell>
                  </TableRow>
                ) : items.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={8} align="center" sx={{ py: 6 }}>
                      <Typography variant="body1" color="text.secondary">
                        Không tìm thấy biến thể nào phù hợp với điều kiện tìm
                        kiếm.
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
                          {[item.color, item.storage]
                            .filter(Boolean)
                            .join(" · ") || "Mặc định"}
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

                      <TableCell align="center">
                        <Button
                          size="small"
                          variant="outlined"
                          startIcon={<AddCircleOutlineIcon />}
                          onClick={() => handleOpenImportDialog(item)}
                          data-testid={`btn-import-row-${item.variantId}`}
                          sx={{ textTransform: "none", fontWeight: 600 }}
                        >
                          Nhập kho
                        </Button>
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
        </>
      )}

      {tabValue === 1 && (
        <TableContainer
          component={Paper}
          variant="outlined"
          sx={{ borderRadius: 2 }}
        >
          <Table>
            <TableHead sx={{ bgcolor: "grey.50" }}>
              <TableRow>
                <TableCell sx={{ fontWeight: 700 }}>THỜI GIAN</TableCell>
                <TableCell sx={{ fontWeight: 700 }}>
                  MÃ SKU / SẢN PHẨM
                </TableCell>
                <TableCell align="center" sx={{ fontWeight: 700 }}>
                  LOẠI GIAO DỊCH
                </TableCell>
                <TableCell align="right" sx={{ fontWeight: 700 }}>
                  SỐ LƯỢNG NHẬP
                </TableCell>
                <TableCell sx={{ fontWeight: 700 }}>NGƯỜI THỰC HIỆN</TableCell>
                <TableCell sx={{ fontWeight: 700 }}>
                  GHI CHÚ / THAM CHIẾU
                </TableCell>
              </TableRow>
            </TableHead>

            <TableBody>
              {txLoading ? (
                <TableRow>
                  <TableCell colSpan={6} align="center" sx={{ py: 6 }}>
                    <CircularProgress size={36} />
                    <Typography
                      variant="body2"
                      color="text.secondary"
                      sx={{ mt: 1 }}
                    >
                      Đang tải lịch sử nhập kho...
                    </Typography>
                  </TableCell>
                </TableRow>
              ) : transactions.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={6} align="center" sx={{ py: 6 }}>
                    <Typography variant="body1" color="text.secondary">
                      Chưa có lịch sử giao dịch nhập kho nào.
                    </Typography>
                  </TableCell>
                </TableRow>
              ) : (
                transactions.map((tx) => (
                  <TableRow key={tx.id} hover>
                    <TableCell>
                      <Typography variant="body2">
                        {new Date(tx.createdAt).toLocaleString("vi-VN")}
                      </Typography>
                    </TableCell>

                    <TableCell>
                      <Typography variant="body2" fontWeight={700}>
                        {tx.productName || "Sản phẩm"}
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
                        {tx.sku}
                      </Typography>
                      {(tx.color || tx.storage) && (
                        <Typography
                          variant="caption"
                          color="text.secondary"
                          sx={{ display: "block" }}
                        >
                          {[tx.color, tx.storage].filter(Boolean).join(" · ")}
                        </Typography>
                      )}
                    </TableCell>

                    <TableCell align="center">
                      <Chip
                        label="Nhập kho"
                        color="success"
                        size="small"
                        variant="filled"
                      />
                    </TableCell>

                    <TableCell align="right">
                      <Typography
                        variant="body2"
                        fontWeight={800}
                        color="success.main"
                      >
                        +{tx.quantityChange}
                      </Typography>
                    </TableCell>

                    <TableCell>
                      <Typography variant="body2" fontWeight={600}>
                        {tx.createdByName || "Quản trị viên"}
                      </Typography>
                      {tx.createdByEmail && (
                        <Typography variant="caption" color="text.secondary">
                          {tx.createdByEmail}
                        </Typography>
                      )}
                    </TableCell>

                    <TableCell>
                      <Typography variant="body2">
                        {tx.note ||
                          (tx.referenceType
                            ? `Tham chiếu: ${tx.referenceType}`
                            : "-")}
                      </Typography>
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>

          <TablePagination
            rowsPerPageOptions={[5, 10, 20, 50]}
            component="div"
            count={txTotalElements}
            rowsPerPage={txRowsPerPage}
            page={txPage}
            onPageChange={(_, newPage) => setTxPage(newPage)}
            onRowsPerPageChange={(e) => {
              setTxRowsPerPage(parseInt(e.target.value, 10));
              setTxPage(0);
            }}
            labelRowsPerPage="Số hàng mỗi trang:"
            labelDisplayedRows={({ from, to, count }) =>
              `${from}–${to} trong số ${count !== -1 ? count : `hơn ${to}`}`
            }
          />
        </TableContainer>
      )}

      {/* Dialog Nhập kho */}
      <Dialog
        open={openImportDialog}
        onClose={handleCloseImportDialog}
        maxWidth="sm"
        fullWidth
        data-testid="dialog-import-inventory"
      >
        <Box component="form" onSubmit={handleImportSubmit}>
          <DialogTitle sx={{ fontWeight: 700, pb: 1 }}>
            Nhập kho biến thể sản phẩm
          </DialogTitle>
          <Divider />

          <DialogContent sx={{ pt: 2.5 }}>
            {importError && (
              <Alert severity="error" sx={{ mb: 2 }}>
                {importError}
              </Alert>
            )}

            <Stack spacing={2.5}>
              <FormControl fullWidth size="small">
                <InputLabel id="select-variant-label">Chọn biến thể</InputLabel>
                <Select
                  labelId="select-variant-label"
                  label="Chọn biến thể"
                  value={selectedVariantId}
                  onChange={(e) => setSelectedVariantId(Number(e.target.value))}
                  data-testid="select-import-variant"
                >
                  {items.map((it) => (
                    <MenuItem key={it.variantId} value={it.variantId}>
                      {it.productName} ({it.sku}) - Tồn: {it.quantityOnHand}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>

              {currentSelectedItem && (
                <Paper
                  variant="outlined"
                  sx={{ p: 2, bgcolor: "grey.50", borderRadius: 1.5 }}
                >
                  <Typography variant="subtitle2" fontWeight={700}>
                    {currentSelectedItem.productName}
                  </Typography>
                  <Typography
                    variant="body2"
                    color="text.secondary"
                    sx={{ mt: 0.5 }}
                  >
                    Mã SKU: <strong>{currentSelectedItem.sku}</strong> | Thuộc
                    tính:{" "}
                    {[currentSelectedItem.color, currentSelectedItem.storage]
                      .filter(Boolean)
                      .join(" · ") || "Mặc định"}
                  </Typography>
                  <Stack direction="row" spacing={3} sx={{ mt: 1.5 }}>
                    <Typography variant="caption" color="text.secondary">
                      Tồn hiện tại:{" "}
                      <strong>{currentSelectedItem.quantityOnHand}</strong>
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      Khả dụng:{" "}
                      <strong>{currentSelectedItem.availableQuantity}</strong>
                    </Typography>
                    <Typography
                      variant="caption"
                      color="primary.main"
                      fontWeight={700}
                    >
                      Dự kiến sau nhập:{" "}
                      <strong>
                        {currentSelectedItem.quantityOnHand +
                          (Number(importQuantity) || 0)}
                      </strong>
                    </Typography>
                  </Stack>
                </Paper>
              )}

              <TextField
                fullWidth
                size="small"
                type="number"
                label="Số lượng nhập"
                value={importQuantity}
                onChange={(e) => {
                  const val =
                    e.target.value === "" ? "" : Number(e.target.value);
                  setImportQuantity(val);
                }}
                inputProps={{ min: 1 }}
                required
                data-testid="input-import-quantity"
              />

              <TextField
                fullWidth
                size="small"
                label="Ghi chú nhập hàng (nhà cung cấp, giá nhập...)"
                value={importNote}
                onChange={(e) => setImportNote(e.target.value)}
                placeholder="VD: Nhập hàng từ nhà phân phối FPT Synnex, giá 21.000.000đ"
                multiline
                rows={2}
                data-testid="input-import-note"
              />
            </Stack>
          </DialogContent>

          <Divider />
          <DialogActions sx={{ px: 3, py: 2 }}>
            <Button
              onClick={handleCloseImportDialog}
              disabled={importSubmitting}
            >
              Hủy
            </Button>
            <Button
              variant="contained"
              type="submit"
              onClick={handleImportSubmit}
              disabled={importSubmitting}
              data-testid="btn-submit-import"
              sx={{ fontWeight: 700, minWidth: 120 }}
            >
              {importSubmitting ? (
                <CircularProgress size={22} color="inherit" />
              ) : (
                "Xác nhận nhập kho"
              )}
            </Button>
          </DialogActions>
        </Box>
      </Dialog>

      {/* Snackbar feedback */}
      <Snackbar
        open={Boolean(snackbarMessage)}
        autoHideDuration={4000}
        onClose={() => setSnackbarMessage(null)}
        anchorOrigin={{ vertical: "bottom", horizontal: "right" }}
      >
        <Alert
          severity={snackbarSeverity}
          onClose={() => setSnackbarMessage(null)}
          variant="filled"
          sx={{ width: "100%" }}
        >
          {snackbarMessage}
        </Alert>
      </Snackbar>
    </Box>
  );
}
