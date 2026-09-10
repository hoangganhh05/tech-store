import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  CircularProgress,
  Grid,
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
import AttachMoneyOutlinedIcon from "@mui/icons-material/AttachMoneyOutlined";
import RefreshIcon from "@mui/icons-material/Refresh";
import ShoppingBagOutlinedIcon from "@mui/icons-material/ShoppingBagOutlined";
import TrendingUpOutlinedIcon from "@mui/icons-material/TrendingUpOutlined";
import { useCallback, useEffect, useState, type FormEvent, type ReactNode } from "react";
import { PageIntro } from "../../components/common/PageIntro";
import {
  getRevenueReport,
  type RevenueReport,
} from "../../services/revenueReportService";

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
        <Typography variant="h2" mt={1} data-testid={`revenue-metric-${label}`}>
          {value}
        </Typography>
      </CardContent>
    </Card>
  );
}

export function AdminRevenueReportPage() {
  const [fromDate, setFromDate] = useState(firstDayOfMonth);
  const [toDate, setToDate] = useState(today);
  const [requestedRange, setRequestedRange] = useState({
    fromDate: firstDayOfMonth(),
    toDate: today(),
  });
  const [report, setReport] = useState<RevenueReport | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const loadReport = useCallback(async (range: { fromDate: string; toDate: string }) => {
    setLoading(true);
    setError("");
    try {
      setReport(await getRevenueReport(range));
    } catch {
      setError("Không thể tải báo cáo doanh thu. Vui lòng thử lại.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadReport(requestedRange);
  }, [loadReport, requestedRange]);

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
    setRequestedRange({ fromDate, toDate });
  };

  return (
    <>
      <PageIntro
        eyebrow="Quản trị · Báo cáo"
        title="Báo cáo doanh thu"
        description="Phân tích doanh thu, số đơn hàng và giá trị đơn trung bình theo khoảng thời gian tùy chọn. Đơn đã huỷ không được tính."
        action={
          <Button
            variant="outlined"
            startIcon={<RefreshIcon />}
            onClick={() => void loadReport(requestedRange)}
            aria-label="Làm mới báo cáo doanh thu"
          >
            Làm mới
          </Button>
        }
      />

      <Card sx={{ mb: 3 }}>
        <CardContent component="form" onSubmit={handleSubmit}>
          <Stack direction={{ xs: "column", sm: "row" }} spacing={2} alignItems={{ sm: "center" }}>
            <TextField
              size="small"
              type="date"
              label="Từ ngày"
              value={fromDate}
              onChange={(event) => setFromDate(event.target.value)}
              InputLabelProps={{ shrink: true }}
              inputProps={{ max: today() }}
              required
            />
            <TextField
              size="small"
              type="date"
              label="Đến ngày"
              value={toDate}
              onChange={(event) => setToDate(event.target.value)}
              InputLabelProps={{ shrink: true }}
              inputProps={{ max: today() }}
              required
            />
            <Button type="submit" variant="contained" sx={{ minHeight: 40 }}>
              Xem báo cáo
            </Button>
          </Stack>
        </CardContent>
      </Card>

      {error && (
        <Alert
          severity="error"
          sx={{ mb: 3 }}
          action={
            <Button color="inherit" size="small" onClick={() => void loadReport(requestedRange)}>
              Thử lại
            </Button>
          }
        >
          {error}
        </Alert>
      )}

      {loading && !report ? (
        <Box display="flex" justifyContent="center" py={8}>
          <CircularProgress aria-label="Đang tải báo cáo doanh thu" />
        </Box>
      ) : report ? (
        <>
          <Grid container spacing={2} mb={3}>
            <Grid size={{ xs: 12, sm: 4 }}>
              <MetricCard label="Tổng doanh thu" value={formatCurrency(report.totalRevenue)} icon={<AttachMoneyOutlinedIcon />} />
            </Grid>
            <Grid size={{ xs: 12, sm: 4 }}>
              <MetricCard label="Tổng số đơn hàng" value={report.totalOrders.toLocaleString("vi-VN")} icon={<ShoppingBagOutlinedIcon />} />
            </Grid>
            <Grid size={{ xs: 12, sm: 4 }}>
              <MetricCard label="Giá trị đơn trung bình" value={formatCurrency(report.averageOrderValue)} icon={<TrendingUpOutlinedIcon />} />
            </Grid>
          </Grid>

          <Card>
            <CardContent>
              <Stack direction={{ xs: "column", sm: "row" }} justifyContent="space-between" gap={1} mb={2}>
                <Box>
                  <Typography variant="h3">Chi tiết doanh thu theo ngày</Typography>
                  <Typography color="text.secondary" variant="body2" mt={0.5}>
                    {formatDate(report.fromDate)} – {formatDate(report.toDate)}
                  </Typography>
                </Box>
                <Typography color="text.secondary" variant="body2">
                  {report.dailyRevenue.length} ngày trong khoảng đã chọn
                </Typography>
              </Stack>
              <TableContainer>
                <Table size="small" aria-label="Bảng chi tiết doanh thu theo ngày">
                  <TableHead>
                    <TableRow>
                      <TableCell>Ngày</TableCell>
                      <TableCell align="right">Doanh thu</TableCell>
                      <TableCell align="right">Số đơn hàng</TableCell>
                      <TableCell align="right">Giá trị đơn trung bình</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {report.dailyRevenue.map((day) => (
                      <TableRow key={day.date}>
                        <TableCell>{formatDate(day.date)}</TableCell>
                        <TableCell align="right">{formatCurrency(day.revenue)}</TableCell>
                        <TableCell align="right">{day.orderCount.toLocaleString("vi-VN")}</TableCell>
                        <TableCell align="right">{formatCurrency(day.averageOrderValue)}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            </CardContent>
          </Card>
        </>
      ) : null}
    </>
  );
}
