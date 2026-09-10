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
import RefreshIcon from "@mui/icons-material/Refresh";
import { useCallback, useEffect, useState, type FormEvent } from "react";
import { PageIntro } from "../../components/common/PageIntro";
import { getAdminCategories, type Category } from "../../services/categoryService";
import {
  getProductInventoryReport,
  type ProductInventoryReport,
  type ProductReportDirection,
  type ProductReportSort,
} from "../../services/productInventoryReportService";

const pad = (value: number) => String(value).padStart(2, "0");

const today = () => {
  const now = new Date();
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`;
};

const firstDayOfMonth = () => {
  const now = new Date();
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-01`;
};

const formatCurrency = (value: number) =>
  new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
    maximumFractionDigits: 0,
  }).format(value);

const formatDate = (value: string) => {
  const [year, month, day] = value.split("-");
  return `${day}/${month}/${year}`;
};

const variantLabel = (color: string | null, storage: string | null) =>
  [color, storage].filter(Boolean).join(" · ") || "Mặc định";

function StockStatusChip({ status }: { status: string }) {
  if (status === "OUT_OF_STOCK") {
    return <Chip label="Hết hàng" color="error" size="small" />;
  }
  return <Chip label="Sắp hết" color="warning" size="small" />;
}

export function AdminProductInventoryReportPage() {
  const [fromDate, setFromDate] = useState(firstDayOfMonth);
  const [toDate, setToDate] = useState(today);
  const [categoryId, setCategoryId] = useState("");
  const [sortBy, setSortBy] = useState<ProductReportSort>("QUANTITY");
  const [sortDirection, setSortDirection] = useState<ProductReportDirection>("DESC");
  const [requestedFilters, setRequestedFilters] = useState({
    fromDate: firstDayOfMonth(),
    toDate: today(),
    categoryId: undefined as number | undefined,
    sortBy: "QUANTITY" as ProductReportSort,
    sortDirection: "DESC" as ProductReportDirection,
  });
  const [categories, setCategories] = useState<Category[]>([]);
  const [report, setReport] = useState<ProductInventoryReport | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const loadReport = useCallback(async (filters: typeof requestedFilters) => {
    setLoading(true);
    setError("");
    try {
      setReport(await getProductInventoryReport(filters));
    } catch {
      setError("Không thể tải báo cáo sản phẩm. Vui lòng thử lại.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadReport(requestedFilters);
  }, [loadReport, requestedFilters]);

  useEffect(() => {
    void getAdminCategories()
      .then(setCategories)
      .catch(() => setCategories([]));
  }, []);

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!fromDate || !toDate) {
      setError("Vui lòng chọn đầy đủ ngày bắt đầu và ngày kết thúc.");
      return;
    }
    if (fromDate > toDate) {
      setError("Ngày bắt đầu không được sau ngày kết thúc.");
      return;
    }
    setRequestedFilters({
      fromDate,
      toDate,
      categoryId: categoryId ? Number(categoryId) : undefined,
      sortBy,
      sortDirection,
    });
  };

  const handleReset = () => {
    const range = { fromDate: firstDayOfMonth(), toDate: today() };
    setFromDate(range.fromDate);
    setToDate(range.toDate);
    setCategoryId("");
    setSortBy("QUANTITY");
    setSortDirection("DESC");
    setRequestedFilters({ ...range, categoryId: undefined, sortBy: "QUANTITY", sortDirection: "DESC" });
  };

  return (
    <>
      <PageIntro
        eyebrow="Quản trị · Báo cáo"
        title="Báo cáo sản phẩm"
        description="Theo dõi sản phẩm bán chạy và các biến thể tồn kho dưới ngưỡng cảnh báo để chủ động nhập hàng và đẩy bán."
        action={
          <Button
            variant="outlined"
            startIcon={<RefreshIcon />}
            onClick={() => void loadReport(requestedFilters)}
            aria-label="Làm mới báo cáo sản phẩm"
          >
            Làm mới
          </Button>
        }
      />

      <Card sx={{ mb: 3 }}>
        <CardContent component="form" onSubmit={handleSubmit}>
          <Grid container spacing={2} alignItems="center">
            <Grid size={{ xs: 12, sm: 6, md: 2.5 }}>
              <TextField
                fullWidth
                size="small"
                type="date"
                label="Từ ngày"
                value={fromDate}
                onChange={(event) => setFromDate(event.target.value)}
                InputLabelProps={{ shrink: true }}
                inputProps={{ max: today() }}
                required
              />
            </Grid>
            <Grid size={{ xs: 12, sm: 6, md: 2.5 }}>
              <TextField
                fullWidth
                size="small"
                type="date"
                label="Đến ngày"
                value={toDate}
                onChange={(event) => setToDate(event.target.value)}
                InputLabelProps={{ shrink: true }}
                inputProps={{ max: today() }}
                required
              />
            </Grid>
            <Grid size={{ xs: 12, sm: 6, md: 2.5 }}>
              <FormControl fullWidth size="small">
                <InputLabel id="product-report-category-label">Danh mục</InputLabel>
                <Select
                  labelId="product-report-category-label"
                  label="Danh mục"
                  value={categoryId}
                  onChange={(event) => setCategoryId(event.target.value)}
                >
                  <MenuItem value="">Tất cả danh mục</MenuItem>
                  {categories.map((category) => (
                    <MenuItem key={category.id} value={String(category.id)}>
                      {category.name}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Grid>
            <Grid size={{ xs: 12, sm: 6, md: 2 }}>
              <FormControl fullWidth size="small">
                <InputLabel id="product-report-sort-label">Sắp xếp top bán chạy</InputLabel>
                <Select
                  labelId="product-report-sort-label"
                  label="Sắp xếp top bán chạy"
                  value={`${sortBy}-${sortDirection}`}
                  onChange={(event) => {
                    const [nextSort, nextDirection] = event.target.value.split("-") as [ProductReportSort, ProductReportDirection];
                    setSortBy(nextSort);
                    setSortDirection(nextDirection);
                  }}
                >
                  <MenuItem value="QUANTITY-DESC">Số lượng giảm dần</MenuItem>
                  <MenuItem value="QUANTITY-ASC">Số lượng tăng dần</MenuItem>
                  <MenuItem value="REVENUE-DESC">Doanh thu giảm dần</MenuItem>
                  <MenuItem value="REVENUE-ASC">Doanh thu tăng dần</MenuItem>
                  <MenuItem value="NAME-ASC">Tên A–Z</MenuItem>
                  <MenuItem value="NAME-DESC">Tên Z–A</MenuItem>
                </Select>
              </FormControl>
            </Grid>
            <Grid size={{ xs: 12, md: 2.5 }}>
              <Stack direction="row" spacing={1}>
                <Button type="submit" variant="contained" sx={{ minHeight: 40 }}>
                  Xem báo cáo
                </Button>
                <Button type="button" variant="text" onClick={handleReset} sx={{ minHeight: 40 }}>
                  Đặt lại
                </Button>
              </Stack>
            </Grid>
          </Grid>
        </CardContent>
      </Card>

      {error && (
        <Alert
          severity="error"
          sx={{ mb: 3 }}
          action={
            <Button color="inherit" size="small" onClick={() => void loadReport(requestedFilters)}>
              Thử lại
            </Button>
          }
        >
          {error}
        </Alert>
      )}

      {loading && !report ? (
        <Box display="flex" justifyContent="center" py={8}>
          <CircularProgress aria-label="Đang tải báo cáo sản phẩm" />
        </Box>
      ) : report ? (
        <Grid container spacing={2}>
          <Grid size={{ xs: 12 }}>
            <Card>
              <CardContent>
                <Stack direction={{ xs: "column", sm: "row" }} justifyContent="space-between" gap={1} mb={2}>
                  <Box>
                    <Typography variant="h3">Top sản phẩm bán chạy</Typography>
                    <Typography color="text.secondary" variant="body2" mt={0.5}>
                      {formatDate(report.fromDate)} – {formatDate(report.toDate)} · Top {report.topSellingProducts.length}
                    </Typography>
                  </Box>
                  <Chip label={`Sắp xếp: ${report.sortBy === "QUANTITY" ? "số lượng" : report.sortBy === "REVENUE" ? "doanh thu" : "tên"}`} size="small" />
                </Stack>
                {report.topSellingProducts.length === 0 ? (
                  <Typography color="text.secondary">Chưa có sản phẩm phát sinh trong khoảng thời gian này.</Typography>
                ) : (
                  <TableContainer>
                    <Table size="small" aria-label="Bảng top sản phẩm bán chạy">
                      <TableHead>
                        <TableRow>
                          <TableCell>Sản phẩm</TableCell>
                          <TableCell>Danh mục</TableCell>
                          <TableCell align="right">Số lượng bán</TableCell>
                          <TableCell align="right">Doanh thu</TableCell>
                        </TableRow>
                      </TableHead>
                      <TableBody>
                        {report.topSellingProducts.map((product) => (
                          <TableRow key={`${product.productName}-${product.categoryId ?? "unknown"}`}>
                            <TableCell>{product.productName}</TableCell>
                            <TableCell>{product.categoryName ?? "Không xác định"}</TableCell>
                            <TableCell align="right">{product.quantitySold.toLocaleString("vi-VN")}</TableCell>
                            <TableCell align="right">{formatCurrency(product.revenue)}</TableCell>
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>
                  </TableContainer>
                )}
              </CardContent>
            </Card>
          </Grid>
          <Grid size={{ xs: 12 }}>
            <Card>
              <CardContent>
                <Stack direction={{ xs: "column", sm: "row" }} justifyContent="space-between" gap={1} mb={2}>
                  <Box>
                    <Typography variant="h3">Sản phẩm tồn kho thấp</Typography>
                    <Typography color="text.secondary" variant="body2" mt={0.5}>
                      Các biến thể có tồn khả dụng nhỏ hơn hoặc bằng ngưỡng cảnh báo.
                    </Typography>
                  </Box>
                  <Chip label={`${report.lowStockVariants.length} biến thể`} color="warning" size="small" />
                </Stack>
                {report.lowStockVariants.length === 0 ? (
                  <Typography color="text.secondary">Không có biến thể nào dưới ngưỡng cảnh báo.</Typography>
                ) : (
                  <TableContainer>
                    <Table size="small" aria-label="Bảng sản phẩm tồn kho thấp">
                      <TableHead>
                        <TableRow>
                          <TableCell>Sản phẩm / biến thể</TableCell>
                          <TableCell>Danh mục</TableCell>
                          <TableCell>SKU</TableCell>
                          <TableCell align="right">Tồn khả dụng</TableCell>
                          <TableCell align="right">Ngưỡng cảnh báo</TableCell>
                          <TableCell>Trạng thái</TableCell>
                        </TableRow>
                      </TableHead>
                      <TableBody>
                        {report.lowStockVariants.map((item) => (
                          <TableRow key={item.variantId}>
                            <TableCell>
                              <Typography variant="body2" fontWeight={600}>{item.productName}</Typography>
                              <Typography variant="caption" color="text.secondary">{variantLabel(item.color, item.storage)}</Typography>
                            </TableCell>
                            <TableCell>{item.categoryName ?? "Không xác định"}</TableCell>
                            <TableCell>{item.sku}</TableCell>
                            <TableCell align="right">{item.availableQuantity.toLocaleString("vi-VN")}</TableCell>
                            <TableCell align="right">{item.lowStockThreshold.toLocaleString("vi-VN")}</TableCell>
                            <TableCell><StockStatusChip status={item.stockStatus} /></TableCell>
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>
                  </TableContainer>
                )}
              </CardContent>
            </Card>
          </Grid>
        </Grid>
      ) : null}
    </>
  );
}
