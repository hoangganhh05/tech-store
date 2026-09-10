import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  CircularProgress,
  FormControl,
  IconButton,
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
  Tooltip,
  Typography,
} from "@mui/material";
import VisibilityOffOutlinedIcon from "@mui/icons-material/VisibilityOffOutlined";
import VisibilityOutlinedIcon from "@mui/icons-material/VisibilityOutlined";
import CheckCircleOutlineIcon from "@mui/icons-material/CheckCircleOutline";
import RefreshIcon from "@mui/icons-material/Refresh";
import { isAxiosError } from "axios";
import { useCallback, useEffect, useState } from "react";
import { PageIntro } from "../../components/common/PageIntro";
import {
  getAdminReviews,
  updateAdminReviewStatus,
  type AdminReview,
} from "../../services/adminReviewService";
import type { ReviewStatus } from "../../services/reviewService";

const statusLabels: Record<ReviewStatus, string> = {
  PENDING: "Chờ duyệt",
  APPROVED: "Đã duyệt",
  HIDDEN: "Đã ẩn",
};

const statusColors: Record<ReviewStatus, "warning" | "success" | "default"> = {
  PENDING: "warning",
  APPROVED: "success",
  HIDDEN: "default",
};

function formatDate(value: string) {
  return new Intl.DateTimeFormat("vi-VN", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}

function errorMessage(error: unknown, fallback: string) {
  if (isAxiosError<{ message?: string }>(error)) {
    return error.response?.data?.message || fallback;
  }
  return fallback;
}

export function AdminReviewsPage() {
  const [reviews, setReviews] = useState<AdminReview[]>([]);
  const [totalElements, setTotalElements] = useState(0);
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(10);
  const [statusFilter, setStatusFilter] = useState<ReviewStatus | "">("");
  const [isLoading, setIsLoading] = useState(false);
  const [updatingReviewId, setUpdatingReviewId] = useState<number | null>(null);
  const [feedback, setFeedback] = useState<{
    type: "success" | "error";
    text: string;
  } | null>(null);

  const fetchReviews = useCallback(async () => {
    setIsLoading(true);
    try {
      const response = await getAdminReviews({
        status: statusFilter || undefined,
        page,
        size: rowsPerPage,
      });
      setReviews(response.items);
      setTotalElements(response.totalElements);
    } catch (error: unknown) {
      setFeedback({
        type: "error",
        text: errorMessage(error, "Không thể tải danh sách đánh giá. Vui lòng thử lại."),
      });
    } finally {
      setIsLoading(false);
    }
  }, [page, rowsPerPage, statusFilter]);

  useEffect(() => {
    void fetchReviews();
  }, [fetchReviews]);

  const handleStatusUpdate = async (review: AdminReview, status: ReviewStatus) => {
    setUpdatingReviewId(review.id);
    try {
      await updateAdminReviewStatus(review.id, status);
      setFeedback({
        type: "success",
        text: `${status === "APPROVED" ? "Duyệt" : status === "HIDDEN" ? "Ẩn" : "Hiện lại"} đánh giá #${review.id} thành công.`,
      });
      await fetchReviews();
    } catch (error: unknown) {
      setFeedback({
        type: "error",
        text: errorMessage(error, "Cập nhật trạng thái đánh giá không thành công."),
      });
    } finally {
      setUpdatingReviewId(null);
    }
  };

  return (
    <Box>
      <PageIntro
        eyebrow="Quản trị nội dung"
        title="Duyệt đánh giá sản phẩm"
        description="Kiểm duyệt nhận xét của khách hàng trước khi hiển thị trên gian hàng công khai."
      />

      {feedback && (
        <Alert severity={feedback.type} onClose={() => setFeedback(null)} sx={{ mb: 3 }}>
          {feedback.text}
        </Alert>
      )}

      <Card sx={{ mb: 3 }}>
        <CardContent sx={{ p: 2.5 }}>
          <Stack direction={{ xs: "column", sm: "row" }} spacing={2} alignItems={{ xs: "stretch", sm: "center" }} justifyContent="space-between">
            <FormControl size="small" sx={{ minWidth: 220 }}>
              <InputLabel id="review-status-filter-label">Lọc trạng thái</InputLabel>
              <Select
                labelId="review-status-filter-label"
                label="Lọc trạng thái"
                value={statusFilter}
                onChange={(event) => {
                  setStatusFilter(event.target.value as ReviewStatus | "");
                  setPage(0);
                }}
              >
                <MenuItem value="">Tất cả trạng thái</MenuItem>
                {(Object.keys(statusLabels) as ReviewStatus[]).map((status) => (
                  <MenuItem key={status} value={status}>{statusLabels[status]}</MenuItem>
                ))}
              </Select>
            </FormControl>
            <Tooltip title="Làm mới dữ liệu">
              <span>
                <IconButton onClick={() => void fetchReviews()} disabled={isLoading} aria-label="Làm mới danh sách đánh giá">
                  <RefreshIcon />
                </IconButton>
              </span>
            </Tooltip>
          </Stack>
        </CardContent>
      </Card>

      <Paper sx={{ width: "100%", overflow: "hidden" }}>
        <TableContainer sx={{ minHeight: 300 }}>
          <Table stickyHeader aria-label="Bảng đánh giá sản phẩm">
            <TableHead>
              <TableRow>
                <TableCell><strong>Đánh giá</strong></TableCell>
                <TableCell><strong>Sản phẩm</strong></TableCell>
                <TableCell><strong>Điểm</strong></TableCell>
                <TableCell><strong>Nhận xét</strong></TableCell>
                <TableCell><strong>Trạng thái</strong></TableCell>
                <TableCell><strong>Thời gian</strong></TableCell>
                <TableCell align="right"><strong>Thao tác</strong></TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {isLoading ? (
                <TableRow>
                  <TableCell colSpan={7} align="center" sx={{ py: 6 }}>
                    <CircularProgress size={36} />
                    <Typography variant="body2" color="text.secondary" mt={1}>Đang tải danh sách đánh giá...</Typography>
                  </TableCell>
                </TableRow>
              ) : reviews.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={7} align="center" sx={{ py: 6 }}>
                    <Typography color="text.secondary">Không có đánh giá nào phù hợp.</Typography>
                  </TableCell>
                </TableRow>
              ) : reviews.map((review) => (
                <TableRow key={review.id} hover>
                  <TableCell>
                    <Typography variant="body2" fontWeight={600}>{review.userFullName}</Typography>
                    <Typography variant="caption" color="text.secondary">#{review.id} · User #{review.userId}</Typography>
                  </TableCell>
                  <TableCell>#{review.productId}</TableCell>
                  <TableCell sx={{ whiteSpace: "nowrap" }}>{"★".repeat(review.rating)} <Typography component="span" variant="caption">({review.rating}/5)</Typography></TableCell>
                  <TableCell sx={{ maxWidth: 280, whiteSpace: "normal", wordBreak: "break-word" }}>{review.comment || "—"}</TableCell>
                  <TableCell><Chip size="small" label={statusLabels[review.status]} color={statusColors[review.status]} variant={review.status === "HIDDEN" ? "outlined" : "filled"} /></TableCell>
                  <TableCell sx={{ whiteSpace: "nowrap" }}>{formatDate(review.createdAt)}</TableCell>
                  <TableCell align="right">
                    <Stack direction="row" spacing={1} justifyContent="flex-end">
                      {review.status === "PENDING" && (
                        <Button size="small" variant="contained" color="success" startIcon={<CheckCircleOutlineIcon />} disabled={updatingReviewId === review.id} onClick={() => void handleStatusUpdate(review, "APPROVED")}>Duyệt</Button>
                      )}
                      {review.status !== "HIDDEN" && (
                        <Button size="small" variant="outlined" color="error" startIcon={<VisibilityOffOutlinedIcon />} disabled={updatingReviewId === review.id} onClick={() => void handleStatusUpdate(review, "HIDDEN")}>Ẩn</Button>
                      )}
                      {review.status === "HIDDEN" && (
                        <Button size="small" variant="outlined" color="success" startIcon={<VisibilityOutlinedIcon />} disabled={updatingReviewId === review.id} onClick={() => void handleStatusUpdate(review, "APPROVED")}>Hiện</Button>
                      )}
                    </Stack>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
        <TablePagination
          rowsPerPageOptions={[5, 10, 25]}
          component="div"
          count={totalElements}
          rowsPerPage={rowsPerPage}
          page={page}
          onPageChange={(_, nextPage) => setPage(nextPage)}
          onRowsPerPageChange={(event) => {
            setRowsPerPage(Number.parseInt(event.target.value, 10));
            setPage(0);
          }}
          labelRowsPerPage="Số hàng mỗi trang:"
          labelDisplayedRows={({ from, to, count }) => `${from}–${to} trên ${count}`}
        />
      </Paper>
    </Box>
  );
}
