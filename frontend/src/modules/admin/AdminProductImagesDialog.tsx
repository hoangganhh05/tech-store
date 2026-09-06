import {
  Alert,
  Box,
  Button,
  Card,
  CardActions,
  CardContent,
  CardMedia,
  Chip,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControl,
  FormControlLabel,
  Grid,
  IconButton,
  InputLabel,
  MenuItem,
  Paper,
  Select,
  Stack,
  Switch,
  Tooltip,
  Typography,
} from "@mui/material";
import CloudUploadIcon from "@mui/icons-material/CloudUpload";
import DeleteOutlineIcon from "@mui/icons-material/DeleteOutline";
import StarIcon from "@mui/icons-material/Star";
import StarBorderIcon from "@mui/icons-material/StarBorder";
import RefreshIcon from "@mui/icons-material/Refresh";
import { isAxiosError } from "axios";
import {
  useCallback,
  useEffect,
  useRef,
  useState,
  type ChangeEvent,
  type DragEvent,
} from "react";
import {
  deleteProductImage,
  getProductImages,
  getProductVariants,
  setPrimaryProductImage,
  updateProductImage,
  uploadProductImage,
  type Product,
  type ProductImage,
  type ProductVariant,
} from "../../services/productService";

interface AdminProductImagesDialogProps {
  open: boolean;
  product: Product | null;
  onClose: () => void;
  onImagesChanged?: () => void;
}

const ALLOWED_EXTENSIONS = ["jpg", "jpeg", "png", "webp"];
const MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024; // 5MB

export function AdminProductImagesDialog({
  open,
  product,
  onClose,
  onImagesChanged,
}: AdminProductImagesDialogProps) {
  const [images, setImages] = useState<ProductImage[]>([]);
  const [variants, setVariants] = useState<ProductVariant[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [isUploading, setIsUploading] = useState(false);
  const [feedback, setFeedback] = useState<{
    type: "success" | "error";
    message: string;
  } | null>(null);

  // Upload options
  const [selectedVariantId, setSelectedVariantId] = useState<string>("");
  const [setAsPrimaryOnUpload, setSetAsPrimaryOnUpload] = useState(false);
  const [isDragging, setIsDragging] = useState(false);

  // Delete confirmation
  const [imageToDelete, setImageToDelete] = useState<ProductImage | null>(null);
  const [isDeleting, setIsDeleting] = useState(false);

  const fileInputRef = useRef<HTMLInputElement | null>(null);

  const fetchData = useCallback(async () => {
    if (!product) return;
    setIsLoading(true);
    try {
      const [imageList, variantList] = await Promise.all([
        getProductImages(product.id),
        getProductVariants(product.id),
      ]);
      setImages(imageList);
      setVariants(variantList);
    } catch (error: unknown) {
      const message = isAxiosError<{ message?: string }>(error)
        ? error.response?.data?.message
        : undefined;
      setFeedback({
        type: "error",
        message: message || "Không thể tải danh sách hình ảnh của sản phẩm.",
      });
    } finally {
      setIsLoading(false);
    }
  }, [product]);

  useEffect(() => {
    if (open && product) {
      setFeedback(null);
      setSelectedVariantId("");
      setSetAsPrimaryOnUpload(false);
      setImageToDelete(null);
      fetchData();
    }
  }, [open, product, fetchData]);

  const validateFile = (file: File): string | null => {
    const ext = file.name.split(".").pop()?.toLowerCase();
    if (!ext || !ALLOWED_EXTENSIONS.includes(ext)) {
      return `File "${file.name}" không đúng định dạng. Chỉ hỗ trợ JPG, JPEG, PNG, WEBP.`;
    }
    if (file.size > MAX_FILE_SIZE_BYTES) {
      return `File "${file.name}" dung lượng quá lớn (${(file.size / (1024 * 1024)).toFixed(1)}MB). Giới hạn tối đa là 5MB.`;
    }
    return null;
  };

  const handleUploadFiles = async (files: FileList | File[]) => {
    if (!product || files.length === 0) return;

    // Validate all files first
    const fileList = Array.from(files);
    for (const file of fileList) {
      const errorMsg = validateFile(file);
      if (errorMsg) {
        setFeedback({ type: "error", message: errorMsg });
        return;
      }
    }

    setIsUploading(true);
    setFeedback(null);

    const variantId =
      selectedVariantId && selectedVariantId !== ""
        ? Number(selectedVariantId)
        : null;

    let successCount = 0;
    let failureCount = 0;

    try {
      for (let i = 0; i < fileList.length; i++) {
        const file = fileList[i];
        const isPrimary = i === 0 && setAsPrimaryOnUpload;
        try {
          await uploadProductImage(product.id, file, variantId, isPrimary);
          successCount++;
        } catch {
          failureCount++;
        }
      }

      if (failureCount === 0) {
        setFeedback({
          type: "success",
          message: `Đã tải lên thành công ${successCount} hình ảnh.`,
        });
      } else {
        setFeedback({
          type: "error",
          message: `Tải lên thành công ${successCount} ảnh, thất bại ${failureCount} ảnh.`,
        });
      }

      await fetchData();
      onImagesChanged?.();
    } finally {
      setIsUploading(false);
      if (fileInputRef.current) {
        fileInputRef.current.value = "";
      }
    }
  };

  const handleFileInputChange = (e: ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) {
      handleUploadFiles(e.target.files);
    }
  };

  const handleDragOver = (e: DragEvent) => {
    e.preventDefault();
    setIsDragging(true);
  };

  const handleDragLeave = (e: DragEvent) => {
    e.preventDefault();
    setIsDragging(false);
  };

  const handleDrop = (e: DragEvent) => {
    e.preventDefault();
    setIsDragging(false);
    if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
      handleUploadFiles(e.dataTransfer.files);
    }
  };

  const handleSetPrimary = async (imageId: number) => {
    if (!product) return;
    try {
      await setPrimaryProductImage(product.id, imageId);
      setFeedback({
        type: "success",
        message: "Đã thiết lập ảnh đại diện mới cho sản phẩm.",
      });
      await fetchData();
      onImagesChanged?.();
    } catch (error: unknown) {
      const message = isAxiosError<{ message?: string }>(error)
        ? error.response?.data?.message
        : undefined;
      setFeedback({
        type: "error",
        message: message || "Không thể đặt ảnh đại diện.",
      });
    }
  };

  const handleUpdateVariant = async (
    imageId: number,
    newVariantIdStr: string,
  ) => {
    if (!product) return;
    const newVariantId =
      newVariantIdStr && newVariantIdStr !== ""
        ? Number(newVariantIdStr)
        : null;

    try {
      await updateProductImage(product.id, imageId, {
        variantId: newVariantId,
      });
      setFeedback({
        type: "success",
        message: "Cập nhật gắn biến thể cho ảnh thành công.",
      });
      await fetchData();
    } catch (error: unknown) {
      const message = isAxiosError<{ message?: string }>(error)
        ? error.response?.data?.message
        : undefined;
      setFeedback({
        type: "error",
        message: message || "Không thể cập nhật thông tin ảnh.",
      });
    }
  };

  const handleDeleteConfirm = async () => {
    if (!product || !imageToDelete) return;
    setIsDeleting(true);
    try {
      await deleteProductImage(product.id, imageToDelete.id);
      setFeedback({
        type: "success",
        message: "Đã xoá hình ảnh thành công khỏi hệ thống.",
      });
      setImageToDelete(null);
      await fetchData();
      onImagesChanged?.();
    } catch (error: unknown) {
      const message = isAxiosError<{ message?: string }>(error)
        ? error.response?.data?.message
        : undefined;
      setFeedback({
        type: "error",
        message: message || "Không thể xoá hình ảnh.",
      });
    } finally {
      setIsDeleting(false);
    }
  };

  return (
    <>
      <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
        <DialogTitle>
          <Stack
            direction="row"
            justifyContent="space-between"
            alignItems="center"
          >
            <Box>
              <Typography variant="h6" component="span" fontWeight={600}>
                Quản lý hình ảnh
              </Typography>
              {product && (
                <Typography
                  variant="body2"
                  color="text.secondary"
                  sx={{ mt: 0.5 }}
                >
                  Sản phẩm: <strong>{product.name}</strong> ({product.brandName})
                </Typography>
              )}
            </Box>
            <IconButton onClick={fetchData} disabled={isLoading || isUploading}>
              <RefreshIcon fontSize="small" />
            </IconButton>
          </Stack>
        </DialogTitle>

        <DialogContent dividers>
          <Stack spacing={3}>
            {feedback && (
              <Alert
                severity={feedback.type}
                onClose={() => setFeedback(null)}
              >
                {feedback.message}
              </Alert>
            )}

            {/* Upload Zone */}
            <Paper
              variant="outlined"
              onDragOver={handleDragOver}
              onDragLeave={handleDragLeave}
              onDrop={handleDrop}
              sx={{
                p: 3,
                textAlign: "center",
                cursor: "pointer",
                border: "2px dashed",
                borderColor: isDragging ? "primary.main" : "divider",
                bgcolor: isDragging ? "action.hover" : "background.paper",
                transition: "all 0.2s ease",
              }}
              onClick={() => fileInputRef.current?.click()}
            >
              <input
                ref={fileInputRef}
                type="file"
                multiple
                accept=".jpg,.jpeg,.png,.webp"
                style={{ display: "none" }}
                onChange={handleFileInputChange}
                disabled={isUploading}
              />
              <CloudUploadIcon
                sx={{ fontSize: 48, color: "primary.main", mb: 1 }}
              />
              <Typography variant="subtitle1" fontWeight={600}>
                Kéo thả nhiều ảnh vào đây hoặc nhấp để tải lên
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Hỗ trợ định dạng JPG, JPEG, PNG, WEBP. Tối đa 5MB mỗi file.
              </Typography>

              {isUploading && (
                <Box
                  sx={{
                    mt: 2,
                    display: "flex",
                    justifyContent: "center",
                    alignItems: "center",
                    gap: 1,
                  }}
                >
                  <CircularProgress size={20} />
                  <Typography variant="body2">
                    Đang tải lên hình ảnh...
                  </Typography>
                </Box>
              )}
            </Paper>

            {/* Upload Settings Bar */}
            <Stack
              direction={{ xs: "column", sm: "row" }}
              spacing={2}
              alignItems="center"
              justifyContent="space-between"
              sx={{ px: 1 }}
            >
              <FormControl size="small" sx={{ minWidth: 220 }}>
                <InputLabel id="upload-variant-label">
                  Gán vào biến thể
                </InputLabel>
                <Select
                  labelId="upload-variant-label"
                  label="Gán vào biến thể"
                  value={selectedVariantId}
                  onChange={(e) => setSelectedVariantId(e.target.value)}
                  disabled={isUploading}
                >
                  <MenuItem value="">
                    <em>Ảnh chung (không gắn biến thể)</em>
                  </MenuItem>
                  {variants.map((v) => (
                    <MenuItem key={v.id} value={String(v.id)}>
                      {v.color ? `${v.color} - ` : ""}
                      {v.storage ? `${v.storage} ` : ""}({v.sku})
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>

              <FormControlLabel
                control={
                  <Switch
                    checked={setAsPrimaryOnUpload}
                    onChange={(e) => setSetAsPrimaryOnUpload(e.target.checked)}
                    disabled={isUploading}
                    size="small"
                  />
                }
                label="Đặt ảnh đầu tiên làm ảnh đại diện"
              />
            </Stack>

            {/* Images Grid Gallery */}
            <Box>
              <Typography
                variant="subtitle2"
                color="text.secondary"
                sx={{ mb: 1.5 }}
              >
                Danh sách ảnh đã tải lên ({images.length})
              </Typography>

              {isLoading && images.length === 0 ? (
                <Box sx={{ p: 4, display: "flex", justifyContent: "center" }}>
                  <CircularProgress />
                </Box>
              ) : images.length === 0 ? (
                <Box
                  sx={{
                    p: 4,
                    textAlign: "center",
                    bgcolor: "grey.50",
                    borderRadius: 1,
                  }}
                >
                  <Typography variant="body2" color="text.secondary">
                    Chưa có hình ảnh nào cho sản phẩm này. Hãy tải lên ảnh đầu
                    tiên!
                  </Typography>
                </Box>
              ) : (
                <Grid container spacing={2}>
                  {images.map((img) => (
                    <Grid key={img.id} size={{ xs: 12, sm: 6, md: 4 }}>
                      <Card
                        variant="outlined"
                        sx={{
                          position: "relative",
                          border: img.isPrimary ? "2px solid" : "1px solid",
                          borderColor: img.isPrimary
                            ? "warning.main"
                            : "divider",
                          height: "100%",
                          display: "flex",
                          flexDirection: "column",
                        }}
                      >
                        {/* Badges on image */}
                        <Box
                          sx={{
                            position: "absolute",
                            top: 8,
                            left: 8,
                            zIndex: 1,
                            display: "flex",
                            flexDirection: "column",
                            gap: 0.5,
                          }}
                        >
                          {img.isPrimary && (
                            <Chip
                              size="small"
                              icon={
                                <StarIcon sx={{ "&&": { color: "#fff" } }} />
                              }
                              label="Ảnh đại diện"
                              color="warning"
                              sx={{ fontWeight: 600 }}
                            />
                          )}
                          {img.variantId && (
                            <Chip
                              size="small"
                              label={
                                img.variantColor || img.variantSku || "Biến thể"
                              }
                              color="info"
                              variant="filled"
                            />
                          )}
                        </Box>

                        <CardMedia
                          component="img"
                          height="180"
                          image={img.imageUrl}
                          alt={product?.name || "Ảnh sản phẩm"}
                          sx={{
                            objectFit: "contain",
                            bgcolor: "grey.100",
                            p: 1,
                          }}
                        />

                        <CardContent sx={{ p: 1.5, flexGrow: 1 }}>
                          <FormControl fullWidth size="small" sx={{ mt: 0.5 }}>
                            <InputLabel id={`img-variant-${img.id}`}>
                              Biến thể
                            </InputLabel>
                            <Select
                              labelId={`img-variant-${img.id}`}
                              label="Biến thể"
                              inputProps={{
                                "data-testid": `image-variant-select-${img.id}`,
                              }}
                              value={img.variantId ? String(img.variantId) : ""}
                              onChange={(e) =>
                                handleUpdateVariant(img.id, e.target.value)
                              }
                            >
                              <MenuItem value="">
                                <em>Ảnh chung</em>
                              </MenuItem>
                              {variants.map((v) => (
                                <MenuItem key={v.id} value={String(v.id)}>
                                  {v.color ? `${v.color} - ` : ""}
                                  {v.storage ? `${v.storage} ` : ""}({v.sku})
                                </MenuItem>
                              ))}
                            </Select>
                          </FormControl>
                        </CardContent>

                        <CardActions
                          sx={{
                            justifyContent: "space-between",
                            px: 1.5,
                            pb: 1.5,
                            pt: 0,
                          }}
                        >
                          {img.isPrimary ? (
                            <Typography
                              variant="caption"
                              color="warning.dark"
                              fontWeight={600}
                              sx={{
                                display: "flex",
                                alignItems: "center",
                                gap: 0.5,
                              }}
                            >
                              <StarIcon fontSize="small" /> Chính
                            </Typography>
                          ) : (
                            <Button
                              size="small"
                              startIcon={<StarBorderIcon />}
                              onClick={() => handleSetPrimary(img.id)}
                            >
                              Đặt đại diện
                            </Button>
                          )}

                          <Tooltip title="Xoá ảnh này">
                            <IconButton
                              size="small"
                              color="error"
                              aria-label="Xoá ảnh này"
                              onClick={() => setImageToDelete(img)}
                            >
                              <DeleteOutlineIcon fontSize="small" />
                            </IconButton>
                          </Tooltip>
                        </CardActions>
                      </Card>
                    </Grid>
                  ))}
                </Grid>
              )}
            </Box>
          </Stack>
        </DialogContent>

        <DialogActions sx={{ px: 3, py: 2 }}>
          <Button onClick={onClose} variant="contained">
            Đóng
          </Button>
        </DialogActions>
      </Dialog>

      {/* Delete Confirmation Dialog */}
      <Dialog
        open={Boolean(imageToDelete)}
        onClose={() => !isDeleting && setImageToDelete(null)}
        maxWidth="xs"
      >
        <DialogTitle>Xác nhận xoá hình ảnh</DialogTitle>
        <DialogContent>
          <Typography variant="body2">
            Bạn có chắc chắn muốn xoá hình ảnh này không? Thao tác này sẽ xoá file
            khỏi hệ thống lưu trữ và không thể hoàn tác.
          </Typography>
          {imageToDelete?.isPrimary && (
            <Alert severity="warning" sx={{ mt: 2 }}>
              Đây là <strong>ảnh đại diện</strong> hiện tại. Khi xoá, hệ thống
              sẽ tự động chọn ảnh kế tiếp làm ảnh đại diện.
            </Alert>
          )}
        </DialogContent>
        <DialogActions sx={{ px: 3, py: 2 }}>
          <Button onClick={() => setImageToDelete(null)} disabled={isDeleting}>
            Huỷ
          </Button>
          <Button
            color="error"
            variant="contained"
            onClick={handleDeleteConfirm}
            disabled={isDeleting}
            startIcon={isDeleting ? <CircularProgress size={16} /> : null}
          >
            Xoá ảnh
          </Button>
        </DialogActions>
      </Dialog>
    </>
  );
}
