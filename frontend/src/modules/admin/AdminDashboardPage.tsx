import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
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
import AssessmentOutlinedIcon from "@mui/icons-material/AssessmentOutlined";
import AttachMoneyOutlinedIcon from "@mui/icons-material/AttachMoneyOutlined";
import RefreshIcon from "@mui/icons-material/Refresh";
import ShoppingBagOutlinedIcon from "@mui/icons-material/ShoppingBagOutlined";
import { useCallback, useEffect, useMemo, useState, type ReactNode } from "react";
import { PageIntro } from "../../components/common/PageIntro";
import {
  getAdminDashboard,
  type AdminDashboard,
  type DashboardPeriod,
} from "../../services/dashboardService";

const pad = (value: number) => String(value).padStart(2, "0");

const currentMonth = () => {
  const now = new Date();
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}`;
};

const currentDate = () => {
  const now = new Date();
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`;
};

const formatCurrency = (value: number) =>
  new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
    maximumFractionDigits: 0,
  }).format(value);

const statusLabels: Record<string, string> = {
  PENDING: "Chờ xác nhận",
  CONFIRMED: "Đã xác nhận",
  SHIPPING: "Đang giao",
  COMPLETED: "Hoàn thành",
};

const statusColors: Record<string, string> = {
  PENDING: "warning.main",
  CONFIRMED: "info.main",
  SHIPPING: "info.main",
  COMPLETED: "success.main",
};

function MetricCard({
  label,
  value,
  icon,
}: {
  label: string;
  value: string;
  icon: ReactNode;
}) {
  return (
    <Card sx={{ height: "100%" }}>
      <CardContent>
        <Stack direction="row" justifyContent="space-between" alignItems="flex-start" gap={1}>
          <Typography color="text.secondary">{label}</Typography>
          <Box color="primary.main">{icon}</Box>
        </Stack>
        <Typography variant="h2" mt={1} data-testid={`metric-${label}`}>
          {value}
        </Typography>
      </CardContent>
    </Card>
  );
}

function RevenueChart({ data }: { data: AdminDashboard["revenueTrend"] }) {
  const maxRevenue = Math.max(...data.map((point) => point.revenue), 0);
  const labelEvery = data.length > 15 ? Math.ceil(data.length / 8) : 1;

  return (
    <Box>
      <Box
        role="img"
        aria-label="Biểu đồ doanh thu theo thời gian"
        sx={{
          height: 240,
          display: "flex",
          alignItems: "stretch",
          gap: { xs: 0.25, sm: 0.75 },
          borderBottom: "1px solid",
          borderColor: "divider",
          px: { xs: 0.5, sm: 1 },
        }}
      >
        {data.map((point, index) => {
          const height = maxRevenue > 0 ? (point.revenue / maxRevenue) * 100 : 0;
          return (
            <Box
              key={`${point.label}-${index}`}
              flex={1}
              minWidth={0}
              display="flex"
              flexDirection="column"
              justifyContent="flex-end"
              alignItems="center"
              title={`${point.label}: ${formatCurrency(point.revenue)} (${point.orderCount} đơn)`}
            >
              <Box
                data-testid="revenue-bar"
                sx={{
                  width: "100%",
                  maxWidth: 34,
                  height: `${height}%`,
                  minHeight: point.revenue > 0 ? 4 : 0,
                  bgcolor: "primary.main",
                  borderRadius: "6px 6px 0 0",
                  transition: "height 0.2s ease",
                }}
              />
            </Box>
          );
        })}
      </Box>
      <Box display="flex" gap={{ xs: 0.25, sm: 0.75 }} px={{ xs: 0.5, sm: 1 }}>
        {data.map((point, index) => (
          <Typography
            key={`${point.label}-label-${index}`}
            flex={1}
            minWidth={0}
            textAlign="center"
            variant="caption"
            color="text.secondary"
            sx={{ overflow: "hidden", whiteSpace: "nowrap" }}
          >
            {index % labelEvery === 0 ? point.label : ""}
          </Typography>
        ))}
      </Box>
      {maxRevenue === 0 && (
        <Typography color="text.secondary" textAlign="center" mt={-15} mb={12}>
          Chưa có doanh thu trong khoảng thời gian này.
        </Typography>
      )}
    </Box>
  );
}

export function AdminDashboardPage() {
  const [period, setPeriod] = useState<DashboardPeriod>("MONTH");
  const [selectedDate, setSelectedDate] = useState(currentMonth);
  const [dashboard, setDashboard] = useState<AdminDashboard | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const requestDate = useMemo(
    () => (period === "MONTH" ? `${selectedDate}-01` : selectedDate),
    [period, selectedDate],
  );

  const loadDashboard = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      setDashboard(await getAdminDashboard({ period, date: requestDate }));
    } catch {
      setError("Không thể tải dữ liệu dashboard. Vui lòng thử lại.");
    } finally {
      setLoading(false);
    }
  }, [period, requestDate]);

  useEffect(() => {
    void loadDashboard();
  }, [loadDashboard]);

  const handlePeriodChange = (nextPeriod: DashboardPeriod) => {
    setPeriod(nextPeriod);
    setSelectedDate(nextPeriod === "MONTH" ? currentMonth() : currentDate());
  };

  return (
    <>
      <PageIntro
        eyebrow="Quản trị · Báo cáo"
        title="Dashboard tổng quan"
        description="Theo dõi nhanh doanh thu, đơn hàng và sản phẩm bán chạy theo ngày, tuần hoặc tháng."
        action={
          <Button
            variant="outlined"
            startIcon={<RefreshIcon />}
            onClick={() => void loadDashboard()}
            aria-label="Làm mới dashboard"
          >
            Làm mới
          </Button>
        }
      />

      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Stack direction={{ xs: "column", sm: "row" }} spacing={2} alignItems={{ sm: "center" }}>
            <FormControl size="small" sx={{ minWidth: 180 }}>
              <InputLabel id="dashboard-period-label">Khoảng xem</InputLabel>
              <Select
                labelId="dashboard-period-label"
                label="Khoảng xem"
                value={period}
                onChange={(event) => handlePeriodChange(event.target.value as DashboardPeriod)}
              >
                <MenuItem value="DAY">Theo ngày</MenuItem>
                <MenuItem value="WEEK">Theo tuần</MenuItem>
                <MenuItem value="MONTH">Theo tháng</MenuItem>
              </Select>
            </FormControl>
            <TextField
              size="small"
              type={period === "MONTH" ? "month" : "date"}
              label={period === "MONTH" ? "Tháng" : period === "WEEK" ? "Ngày trong tuần" : "Ngày"}
              value={selectedDate}
              onChange={(event) => setSelectedDate(event.target.value)}
              InputLabelProps={{ shrink: true }}
              inputProps={{ max: period === "MONTH" ? undefined : currentDate() }}
            />
          </Stack>
        </CardContent>
      </Card>

      {error && (
        <Alert
          severity="error"
          sx={{ mb: 3 }}
          action={
            <Button color="inherit" size="small" onClick={() => void loadDashboard()}>
              Thử lại
            </Button>
          }
        >
          {error}
        </Alert>
      )}

      {loading && !dashboard ? (
        <Box display="flex" justifyContent="center" py={8}>
          <CircularProgress aria-label="Đang tải dashboard" />
        </Box>
      ) : dashboard ? (
        <>
          <Grid container spacing={2} mb={3}>
            <Grid size={{ xs: 12, sm: 6, md: 4 }}>
              <MetricCard label="Tổng doanh thu" value={formatCurrency(dashboard.totalRevenue)} icon={<AttachMoneyOutlinedIcon />} />
            </Grid>
            <Grid size={{ xs: 12, sm: 6, md: 4 }}>
              <MetricCard label="Tổng số đơn hàng" value={dashboard.totalOrders.toLocaleString("vi-VN")} icon={<ShoppingBagOutlinedIcon />} />
            </Grid>
            <Grid size={{ xs: 12, md: 4 }}>
              <MetricCard label="Trạng thái đơn hàng" value={`${dashboard.ordersByStatus.filter((item) => item.count > 0).length} trạng thái`} icon={<AssessmentOutlinedIcon />} />
            </Grid>
          </Grid>

          <Grid container spacing={2}>
            <Grid size={{ xs: 12, lg: 8 }}>
              <Card sx={{ height: "100%" }}>
                <CardContent>
                  <Typography variant="h3" mb={2}>Doanh thu theo thời gian</Typography>
                  <RevenueChart data={dashboard.revenueTrend} />
                </CardContent>
              </Card>
            </Grid>
            <Grid size={{ xs: 12, lg: 4 }}>
              <Card sx={{ height: "100%" }}>
                <CardContent>
                  <Typography variant="h3" mb={2}>Đơn hàng theo trạng thái</Typography>
                  <Stack spacing={1.5}>
                    {dashboard.ordersByStatus.map((item) => (
                      <Stack key={item.status} direction="row" justifyContent="space-between" alignItems="center">
                        <Typography color="text.secondary">{statusLabels[item.status] ?? item.status}</Typography>
                        <Typography fontWeight={700} color={statusColors[item.status] ?? "text.primary"}>{item.count.toLocaleString("vi-VN")}</Typography>
                      </Stack>
                    ))}
                  </Stack>
                </CardContent>
              </Card>
            </Grid>
            <Grid size={{ xs: 12 }}>
              <Card>
                <CardContent>
                  <Typography variant="h3" mb={2}>Sản phẩm bán chạy nhất</Typography>
                  {dashboard.topSellingProducts.length === 0 ? (
                    <Typography color="text.secondary">Chưa có sản phẩm phát sinh trong khoảng thời gian này.</Typography>
                  ) : (
                    <TableContainer>
                      <Table size="small" aria-label="Danh sách sản phẩm bán chạy">
                        <TableHead>
                          <TableRow>
                            <TableCell>Sản phẩm</TableCell>
                            <TableCell align="right">Số lượng bán</TableCell>
                            <TableCell align="right">Doanh thu</TableCell>
                          </TableRow>
                        </TableHead>
                        <TableBody>
                          {dashboard.topSellingProducts.map((product) => (
                            <TableRow key={product.productName}>
                              <TableCell>{product.productName}</TableCell>
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
          </Grid>
        </>
      ) : null}
    </>
  );
}
