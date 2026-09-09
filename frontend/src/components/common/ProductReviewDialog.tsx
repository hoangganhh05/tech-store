import { useEffect, useRef, useState } from "react";
import {
  Alert,
  Box,
  Button,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Rating,
  Stack,
  TextField,
  Typography,
} from "@mui/material";
import StarRoundedIcon from "@mui/icons-material/StarRounded";
import { isAxiosError } from "axios";
import {
  getMyProductReview,
  submitProductReview,
  type ReviewResponse,
} from "../../services/reviewService";

interface ProductReviewDialogProps {
  open: boolean;
  onClose: () => void;
  productId: number;
  productName: string;
  variantLabel?: string;
  onSuccess?: (review: ReviewResponse) => void;
}

const ratingLabels: Record<number, string> = {
  1: "Rất tệ",
  2: "Tệ",
  3: "Bình thường",
  4: "Hài lòng",
  5: "Tuyệt vời",
};

export function ProductReviewDialog({
  open,
  onClose,
  productId,
  productName,
  variantLabel,
  onSuccess,
}: ProductReviewDialogProps) {
  const [rating, setRating] = useState<number>(5);
  const [comment, setComment] = useState<string>("");
  const [loadingCheck, setLoadingCheck] = useState<boolean>(false);
  const [canReview, setCanReview] = useState<boolean>(true);
  const [isEditMode, setIsEditMode] = useState<boolean>(false);
  const [submitting, setSubmitting] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);
  const closeTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    return () => {
      if (closeTimerRef.current) {
        clearTimeout(closeTimerRef.current);
      }
    };
  }, []);

  useEffect(() => {
    if (!open || !productId) return;

    let isMounted = true;
    setLoadingCheck(true);
    setError(null);
    setSuccessMsg(null);

    getMyProductReview(productId)
      .then((data) => {
        if (!isMounted) return;
        setCanReview(data.canReview);
        if (data.myReview) {
          setIsEditMode(true);
          setRating(data.myReview.rating);
          setComment(data.myReview.comment || "");
        } else {
          setIsEditMode(false);
          setRating(5);
          setComment("");
        }
      })
      .catch(() => {
        if (!isMounted) return;
        // Default to allow submitting if check fails, server will validate
        setCanReview(true);
        setIsEditMode(false);
      })
      .finally(() => {
        if (isMounted) setLoadingCheck(false);
      });

    return () => {
      isMounted = false;
    };
  }, [open, productId]);

  const handleSubmit = async () => {
    if (rating < 1 || rating > 5) {
      setError("Vui lòng chọn từ 1 đến 5 sao đánh giá.");
      return;
    }

    setSubmitting(true);
    setError(null);
    setSuccessMsg(null);

    try {
      const result = await submitProductReview(productId, {
        rating,
        comment: comment.trim() || undefined,
      });
      setSuccessMsg(
        isEditMode
          ? "Cập nhật đánh giá thành công!"
          : "Gửi đánh giá sản phẩm thành công!",
      );
      onSuccess?.(result);
      closeTimerRef.current = setTimeout(() => {
        onClose();
      }, 1200);
    } catch (err: unknown) {
      const msg = isAxiosError<{ message?: string }>(err)
        ? err.response?.data?.message
        : undefined;
      setError(msg || "Không thể gửi đánh giá. Vui lòng thử lại sau.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Dialog
      open={open}
      onClose={() => !submitting && onClose()}
      fullWidth
      maxWidth="sm"
      aria-labelledby="review-dialog-title"
    >
      <DialogTitle id="review-dialog-title">
        {isEditMode ? "Chỉnh sửa đánh giá sản phẩm" : "Đánh giá sản phẩm"}
      </DialogTitle>
      <DialogContent dividers>
        <Stack spacing={2.5}>
          <Box>
            <Typography variant="subtitle1" fontWeight={600}>
              {productName}
            </Typography>
            {variantLabel && (
              <Typography variant="body2" color="text.secondary">
                Phân loại: {variantLabel}
              </Typography>
            )}
          </Box>

          {loadingCheck ? (
            <Stack alignItems="center" py={3} spacing={1}>
              <CircularProgress size={28} />
              <Typography variant="body2" color="text.secondary">
                Đang kiểm tra quyền đánh giá...
              </Typography>
            </Stack>
          ) : !canReview ? (
            <Alert severity="warning">
              Chỉ khách hàng có đơn hàng đã hoàn thành chứa sản phẩm này mới
              được đánh giá.
            </Alert>
          ) : (
            <>
              {error && <Alert severity="error">{error}</Alert>}
              {successMsg && <Alert severity="success">{successMsg}</Alert>}

              <Stack spacing={1} alignItems="center">
                <Typography variant="body2" fontWeight={500}>
                  Chất lượng sản phẩm
                </Typography>
                <Rating
                  name="product-rating"
                  value={rating}
                  precision={1}
                  size="large"
                  onChange={(_, val) => {
                    if (val) setRating(val);
                  }}
                  emptyIcon={<StarRoundedIcon fontSize="inherit" />}
                />
                <Typography variant="caption" color="text.secondary">
                  {ratingLabels[rating] || ""}
                </Typography>
              </Stack>

              <TextField
                label="Nhận xét của bạn (tuỳ chọn)"
                multiline
                minRows={3}
                maxRows={6}
                value={comment}
                onChange={(e) => setComment(e.target.value)}
                placeholder="Hãy chia sẻ cảm nhận về chất lượng sản phẩm, đóng gói, giao hàng..."
                inputProps={{ maxLength: 2000 }}
                helperText={`${comment.length}/2000 ký tự`}
                fullWidth
                disabled={submitting}
              />
            </>
          )}
        </Stack>
      </DialogContent>
      <DialogActions sx={{ px: 3, py: 2 }}>
        <Button onClick={onClose} disabled={submitting}>
          Đóng
        </Button>
        {canReview && !loadingCheck && (
          <Button
            variant="contained"
            onClick={handleSubmit}
            disabled={submitting || rating < 1}
          >
            {submitting
              ? "Đang gửi..."
              : isEditMode
                ? "Cập nhật đánh giá"
                : "Gửi đánh giá"}
          </Button>
        )}
      </DialogActions>
    </Dialog>
  );
}
