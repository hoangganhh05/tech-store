import { useEffect, useState, useMemo, useCallback } from "react";
import {
  Alert,
  Box,
  Breadcrumbs,
  Button,
  Chip,
  CircularProgress,
  Dialog,
  Divider,
  Grid,
  IconButton,
  Paper,
  Skeleton,
  Snackbar,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableRow,
  Typography,
} from "@mui/material";
import axios from "axios";
import { Link, useNavigate, useParams } from "react-router-dom";
import ZoomInRoundedIcon from "@mui/icons-material/ZoomInRounded";
import CloseRoundedIcon from "@mui/icons-material/CloseRounded";
import StarRoundedIcon from "@mui/icons-material/StarRounded";
import CheckCircleOutlineRoundedIcon from "@mui/icons-material/CheckCircleOutlineRounded";
import RemoveCircleOutlineRoundedIcon from "@mui/icons-material/RemoveCircleOutlineRounded";
import WarningAmberRoundedIcon from "@mui/icons-material/WarningAmberRounded";
import ShoppingCartOutlinedIcon from "@mui/icons-material/ShoppingCartOutlined";
import FlashOnRoundedIcon from "@mui/icons-material/FlashOnRounded";
import AddRoundedIcon from "@mui/icons-material/AddRounded";
import RemoveRoundedIcon from "@mui/icons-material/RemoveRounded";
import SmartphoneRoundedIcon from "@mui/icons-material/SmartphoneRounded";
import SearchOffRoundedIcon from "@mui/icons-material/SearchOffRounded";
import ArrowBackRoundedIcon from "@mui/icons-material/ArrowBackRounded";
import RefreshRoundedIcon from "@mui/icons-material/RefreshRounded";
import {
  getStorefrontProductDetail,
  getRelatedProducts,
  type StorefrontProductDetail,
  type ProductVariantDetail,
  type StorefrontProduct,
} from "../../services/storefrontService";
import { ProductCard } from "../../components/common/ProductCard";
import { ROUTES } from "../../constants/routes";
import { useCart } from "../../hooks/useCart";

function formatPrice(val: number): string {
  return new Intl.NumberFormat("vi-VN").format(val) + " ₫";
}

export function ProductDetailPage() {
  const { slug } = useParams<{ slug: string }>();
  const navigate = useNavigate();

  const [product, setProduct] = useState<StorefrontProductDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isNotFound, setIsNotFound] = useState(false);
  const [isDiscontinued, setIsDiscontinued] = useState(false);

  // Variant selection state
  const [selectedVariant, setSelectedVariant] =
    useState<ProductVariantDetail | null>(null);
  const [selectedColor, setSelectedColor] = useState<string | null>(null);
  const [selectedStorage, setSelectedStorage] = useState<string | null>(null);
  const [quantity, setQuantity] = useState<number>(1);

  // Gallery state
  const [selectedImage, setSelectedImage] = useState<string | null>(null);
  const [zoomOpen, setZoomOpen] = useState(false);

  // Related products state
  const [relatedProducts, setRelatedProducts] = useState<StorefrontProduct[]>(
    [],
  );
  const [relatedLoading, setRelatedLoading] = useState(false);

  // Cart & Toast state
  const { addToCart } = useCart();
  const [isAddingToCart, setIsAddingToCart] = useState(false);
  const [toastOpen, setToastOpen] = useState(false);
  const [toastMessage, setToastMessage] = useState("");
  const [toastSeverity, setToastSeverity] = useState<"success" | "error">(
    "success",
  );

  const productId = useMemo(() => {
    if (!slug) return null;
    const n = Number(slug);
    return isNaN(n) || n <= 0 ? null : n;
  }, [slug]);

  const syncVariantImage = useCallback(
    (
      variant: ProductVariantDetail,
      images: StorefrontProductDetail["images"],
    ) => {
      if (!images || images.length === 0) return;
      // 1. Prioritize image matching variantId
      const imgById = images.find((img) => img.variantId === variant.id);
      if (imgById) {
        setSelectedImage(imgById.imageUrl);
        return;
      }
      // 2. Image matching color
      if (variant.color) {
        const imgByColor = images.find(
          (img) =>
            img.variantColor?.toLowerCase() === variant.color?.toLowerCase(),
        );
        if (imgByColor) {
          setSelectedImage(imgByColor.imageUrl);
          return;
        }
      }
    },
    [],
  );

  const loadProduct = useCallback(async () => {
    if (!productId) {
      setLoading(false);
      setIsNotFound(true);
      return;
    }

    try {
      setLoading(true);
      setError(null);
      setIsNotFound(false);
      setIsDiscontinued(false);

      const data = await getStorefrontProductDetail(productId);
      setProduct(data);

      // Default variant selection: pick first in-stock variant, or first active variant
      const variants = data.variants || [];
      const defaultVariant =
        variants.find((v) => v.stockQuantity > 0) || variants[0] || null;

      setSelectedVariant(defaultVariant);
      setSelectedColor(defaultVariant?.color || null);
      setSelectedStorage(defaultVariant?.storage || null);

      // Default selected image: check if default variant has associated image, otherwise primary image
      let chosenImg: string | null = null;
      if (defaultVariant && data.images && data.images.length > 0) {
        const variantImg =
          data.images.find((img) => img.variantId === defaultVariant.id) ||
          (defaultVariant.color
            ? data.images.find(
                (img) =>
                  img.variantColor?.toLowerCase() ===
                  defaultVariant.color?.toLowerCase(),
              )
            : null);
        if (variantImg) {
          chosenImg = variantImg.imageUrl;
        }
      }

      if (!chosenImg && data.images && data.images.length > 0) {
        const primaryImg = data.images.find((img) => img.isPrimary);
        chosenImg = primaryImg ? primaryImg.imageUrl : data.images[0].imageUrl;
      }

      setSelectedImage(chosenImg);
    } catch (err: unknown) {
      const msg =
        err instanceof Error ? err.message : "Không thể tải thông tin sản phẩm";
      setError(msg);

      // Check if not found or discontinued
      const axiosErr = err as {
        response?: {
          status?: number;
          data?: { code?: string; message?: string };
        };
      };
      if (axiosErr?.response?.status === 404) {
        if (axiosErr.response.data?.message?.includes("ngừng kinh doanh")) {
          setIsDiscontinued(true);
        } else {
          setIsNotFound(true);
        }
      }
    } finally {
      setLoading(false);
    }
  }, [productId]);

  useEffect(() => {
    loadProduct();
  }, [loadProduct]);

  useEffect(() => {
    if (!productId) {
      setRelatedProducts([]);
      return;
    }

    let active = true;
    const fetchRelated = async () => {
      try {
        setRelatedLoading(true);
        const data = await getRelatedProducts(productId, 8);
        if (active) {
          setRelatedProducts(Array.isArray(data) ? data : []);
        }
      } catch {
        if (active) {
          setRelatedProducts([]);
        }
      } finally {
        if (active) {
          setRelatedLoading(false);
        }
      }
    };

    fetchRelated();
    return () => {
      active = false;
    };
  }, [productId]);

  const availableColors = useMemo(() => {
    if (product?.availableColors && product.availableColors.length > 0) {
      return product.availableColors;
    }
    if (!product?.variants) return [];
    return Array.from(
      new Set(
        product.variants
          .map((v) => v.color)
          .filter((c): c is string => Boolean(c && c.trim())),
      ),
    );
  }, [product]);

  const availableStorages = useMemo(() => {
    if (product?.availableStorages && product.availableStorages.length > 0) {
      return product.availableStorages;
    }
    if (!product?.variants) return [];
    return Array.from(
      new Set(
        product.variants
          .map((v) => v.storage)
          .filter((s): s is string => Boolean(s && s.trim())),
      ),
    );
  }, [product]);

  const isColorDisabled = useCallback(
    (color: string) => {
      if (!product?.variants) return false;
      return !product.variants.some((v) => v.color === color);
    },
    [product],
  );

  const isStorageDisabled = useCallback(
    (storage: string) => {
      if (!product?.variants) return false;
      if (!selectedColor) {
        return !product.variants.some((v) => v.storage === storage);
      }
      return !product.variants.some(
        (v) => v.storage === storage && v.color === selectedColor,
      );
    },
    [product, selectedColor],
  );

  const handleColorSelect = useCallback(
    (color: string) => {
      if (isColorDisabled(color)) return;
      setSelectedColor(color);

      if (!product?.variants) return;
      let match = product.variants.find(
        (v) =>
          v.color === color &&
          (!selectedStorage || v.storage === selectedStorage),
      );
      if (!match) {
        match = product.variants.find((v) => v.color === color);
      }
      if (match) {
        setSelectedVariant(match);
        if (match.storage) {
          setSelectedStorage(match.storage);
        }
        syncVariantImage(match, product.images);
        setQuantity(1);
      }
    },
    [isColorDisabled, product, selectedStorage, syncVariantImage],
  );

  const handleStorageSelect = useCallback(
    (storage: string) => {
      if (isStorageDisabled(storage)) return;
      setSelectedStorage(storage);

      if (!product?.variants) return;
      let match = product.variants.find(
        (v) =>
          (!selectedColor || v.color === selectedColor) &&
          v.storage === storage,
      );
      if (!match) {
        match = product.variants.find((v) => v.storage === storage);
      }
      if (match) {
        setSelectedVariant(match);
        if (match.color) {
          setSelectedColor(match.color);
        }
        syncVariantImage(match, product.images);
        setQuantity(1);
      }
    },
    [isStorageDisabled, product, selectedColor, syncVariantImage],
  );

  const displayPrice = useMemo(() => {
    if (selectedVariant) {
      return formatPrice(selectedVariant.price);
    }
    if (!product) return "0 ₫";
    return product.minPrice === product.maxPrice
      ? formatPrice(product.minPrice)
      : `${formatPrice(product.minPrice)} - ${formatPrice(product.maxPrice)}`;
  }, [product, selectedVariant]);

  const displayOriginalPrice = useMemo(() => {
    if (selectedVariant) {
      return selectedVariant.originalPrice;
    }
    return product?.originalPrice;
  }, [product, selectedVariant]);

  const displayDiscountPercent = useMemo(() => {
    if (selectedVariant) {
      if (
        selectedVariant.originalPrice &&
        selectedVariant.originalPrice > selectedVariant.price
      ) {
        return Math.round(
          ((selectedVariant.originalPrice - selectedVariant.price) /
            selectedVariant.originalPrice) *
            100,
        );
      }
      return 0;
    }
    return product?.discountPercent || 0;
  }, [product, selectedVariant]);

  const hasDiscount =
    displayDiscountPercent > 0 && displayOriginalPrice != null;

  const currentStock = useMemo(() => {
    if (selectedVariant) {
      return selectedVariant.stockQuantity;
    }
    return product?.totalStock || 0;
  }, [product, selectedVariant]);

  const handleAddToCart = async () => {
    if (!selectedVariant) {
      setToastMessage("Vui lòng chọn biến thể sản phẩm");
      setToastSeverity("error");
      setToastOpen(true);
      return;
    }
    if (currentStock <= 0) {
      setToastMessage("Sản phẩm hiện đang hết hàng");
      setToastSeverity("error");
      setToastOpen(true);
      return;
    }

    setIsAddingToCart(true);
    try {
      await addToCart(selectedVariant.id, quantity);
      setToastMessage("Đã thêm sản phẩm vào giỏ hàng thành công!");
      setToastSeverity("success");
      setToastOpen(true);
    } catch (err: unknown) {
      let errorMsg = "Không thể thêm sản phẩm vào giỏ hàng. Vui lòng thử lại.";
      if (axios.isAxiosError(err) && err.response?.data?.message) {
        errorMsg = err.response.data.message;
      } else if (err instanceof Error) {
        errorMsg = err.message;
      }
      setToastMessage(errorMsg);
      setToastSeverity("error");
      setToastOpen(true);
    } finally {
      setIsAddingToCart(false);
    }
  };

  // Loading skeleton state
  if (loading) {
    return (
      <Box sx={{ py: 3 }} data-testid="product-detail-skeleton">
        {/* Breadcrumbs Skeleton */}
        <Skeleton variant="text" width={240} height={28} sx={{ mb: 3 }} />

        <Grid container spacing={4}>
          {/* Gallery Skeleton */}
          <Grid size={{ xs: 12, md: 6 }}>
            <Skeleton
              variant="rounded"
              height={420}
              sx={{ borderRadius: 3, mb: 2 }}
            />
            <Stack direction="row" spacing={1.5}>
              {Array.from({ length: 4 }).map((_, idx) => (
                <Skeleton
                  key={idx}
                  variant="rounded"
                  width={80}
                  height={80}
                  sx={{ borderRadius: 2 }}
                />
              ))}
            </Stack>
          </Grid>

          {/* Details Skeleton */}
          <Grid size={{ xs: 12, md: 6 }}>
            <Skeleton variant="text" width="40%" height={24} sx={{ mb: 1 }} />
            <Skeleton variant="text" width="90%" height={44} sx={{ mb: 2 }} />
            <Skeleton variant="text" width="30%" height={32} sx={{ mb: 2 }} />
            <Skeleton
              variant="rounded"
              width="50%"
              height={48}
              sx={{ mb: 3 }}
            />
            <Skeleton
              variant="rounded"
              width="100%"
              height={120}
              sx={{ mb: 3 }}
            />
            <Skeleton variant="rounded" width="100%" height={50} />
          </Grid>
        </Grid>
      </Box>
    );
  }

  // Not Found / Discontinued state
  if (isNotFound || isDiscontinued) {
    return (
      <Box
        py={10}
        px={3}
        textAlign="center"
        bgcolor="#f9fafb"
        borderRadius={3}
        border="1px dashed #cbd5e1"
        my={4}
        data-testid="product-not-found-state"
      >
        <SearchOffRoundedIcon
          sx={{ fontSize: 72, color: "text.disabled", mb: 2 }}
        />
        <Typography
          variant="h5"
          fontWeight={700}
          color="text.primary"
          mb={1.5}
          data-testid="not-found-title"
        >
          {isDiscontinued
            ? "Sản phẩm đã ngừng kinh doanh"
            : "Không tìm thấy sản phẩm"}
        </Typography>
        <Typography
          variant="body1"
          color="text.secondary"
          maxWidth={480}
          mx="auto"
          mb={4}
        >
          {isDiscontinued
            ? "Sản phẩm này hiện tại đã ngừng kinh doanh hoặc tạm ngừng phân phối. Xin quý khách vui lòng tham khảo các dòng sản phẩm tương đương khác."
            : "Sản phẩm bạn đang tìm kiếm không tồn tại hoặc đã bị gỡ khỏi hệ thống."}
        </Typography>
        <Button
          variant="contained"
          size="large"
          startIcon={<ArrowBackRoundedIcon />}
          onClick={() => navigate(ROUTES.products)}
          data-testid="back-to-products-btn"
          sx={{ borderRadius: 2, textTransform: "none", px: 3 }}
        >
          Quay lại danh sách sản phẩm
        </Button>
      </Box>
    );
  }

  // Generic Error state
  if (error || !product) {
    return (
      <Box py={4}>
        <Alert
          severity="error"
          action={
            <Button
              color="inherit"
              size="small"
              startIcon={<RefreshRoundedIcon />}
              onClick={loadProduct}
            >
              Thử lại
            </Button>
          }
          sx={{ borderRadius: 2 }}
        >
          {error || "Đã xảy ra lỗi khi tải chi tiết sản phẩm."}
        </Alert>
      </Box>
    );
  }

  const galleryImages =
    product?.images && product.images.length > 0 ? product.images : [];

  return (
    <Box sx={{ py: 3 }} data-testid="product-detail-container">
      {/* Breadcrumbs Navigation */}
      <Breadcrumbs aria-label="breadcrumb" sx={{ mb: 3, fontSize: "0.875rem" }}>
        <Link
          to={ROUTES.home}
          style={{ textDecoration: "none", color: "#64748b" }}
        >
          Trang chủ
        </Link>
        <Link
          to={ROUTES.products}
          style={{ textDecoration: "none", color: "#64748b" }}
        >
          Sản phẩm
        </Link>
        {product.categoryId && (
          <Link
            to={`${ROUTES.products}?categoryId=${product.categoryId}`}
            style={{ textDecoration: "none", color: "#64748b" }}
          >
            {product.categoryName || "Danh mục"}
          </Link>
        )}
        <Typography
          color="text.primary"
          fontWeight={600}
          noWrap
          sx={{ maxWidth: 320 }}
        >
          {product.name}
        </Typography>
      </Breadcrumbs>

      <Grid container spacing={4}>
        {/* Left Column: Image Gallery */}
        <Grid size={{ xs: 12, md: 6 }}>
          <Paper
            elevation={0}
            sx={{
              position: "relative",
              borderRadius: 3,
              border: "1px solid #e2e8f0",
              bgcolor: "#ffffff",
              p: 2,
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              minHeight: 400,
              maxHeight: 520,
              overflow: "hidden",
            }}
          >
            {/* Discount Badge */}
            {hasDiscount && (
              <Chip
                label={`-${displayDiscountPercent}%`}
                size="small"
                color="error"
                sx={{
                  position: "absolute",
                  top: 16,
                  left: 16,
                  fontWeight: 700,
                  fontSize: "0.8125rem",
                  borderRadius: 1.5,
                  zIndex: 2,
                }}
              />
            )}

            {/* Zoom Action Button */}
            {selectedImage && (
              <IconButton
                onClick={() => setZoomOpen(true)}
                data-testid="zoom-image-btn"
                sx={{
                  position: "absolute",
                  top: 16,
                  right: 16,
                  bgcolor: "rgba(255, 255, 255, 0.85)",
                  backdropFilter: "blur(4px)",
                  boxShadow: "0 2px 8px rgba(0,0,0,0.12)",
                  "&:hover": { bgcolor: "#ffffff" },
                  zIndex: 2,
                }}
                title="Phóng to ảnh"
              >
                <ZoomInRoundedIcon />
              </IconButton>
            )}

            {/* Main Active Image */}
            {selectedImage ? (
              <Box
                component="img"
                src={selectedImage}
                alt={product.name}
                data-testid="main-product-image"
                onClick={() => setZoomOpen(true)}
                sx={{
                  maxHeight: 460,
                  maxWidth: "100%",
                  objectFit: "contain",
                  cursor: "zoom-in",
                  transition: "transform 0.3s ease",
                  "&:hover": { transform: "scale(1.02)" },
                }}
              />
            ) : (
              <Box
                display="flex"
                flexDirection="column"
                alignItems="center"
                justifyContent="center"
                color="text.disabled"
                py={8}
              >
                <SmartphoneRoundedIcon
                  sx={{ fontSize: 96, opacity: 0.4, mb: 1 }}
                />
                <Typography variant="body2">Chưa có ảnh sản phẩm</Typography>
              </Box>
            )}
          </Paper>

          {/* Thumbnails Row */}
          {galleryImages.length > 1 && (
            <Stack
              direction="row"
              spacing={1.5}
              sx={{
                mt: 2,
                overflowX: "auto",
                pb: 1,
                "::-webkit-scrollbar": { height: 6 },
              }}
              data-testid="image-thumbnails-container"
            >
              {galleryImages.map((img, idx) => {
                const isSelected = selectedImage === img.imageUrl;
                return (
                  <Paper
                    key={img.id || idx}
                    elevation={0}
                    onClick={() => setSelectedImage(img.imageUrl)}
                    data-testid={`thumbnail-image-${idx}`}
                    sx={{
                      width: 76,
                      height: 76,
                      minWidth: 76,
                      borderRadius: 2,
                      border: isSelected
                        ? "2px solid #2563eb"
                        : "1px solid #e2e8f0",
                      p: 0.5,
                      cursor: "pointer",
                      display: "flex",
                      alignItems: "center",
                      justifyContent: "center",
                      overflow: "hidden",
                      transition: "all 0.2s ease",
                      "&:hover": {
                        borderColor: isSelected ? "#2563eb" : "#94a3b8",
                        transform: "translateY(-2px)",
                      },
                    }}
                  >
                    <Box
                      component="img"
                      src={img.imageUrl}
                      alt={`${product.name} - ${idx + 1}`}
                      sx={{
                        maxWidth: "100%",
                        maxHeight: "100%",
                        objectFit: "contain",
                      }}
                    />
                  </Paper>
                );
              })}
            </Stack>
          )}
        </Grid>

        {/* Right Column: Product Information */}
        <Grid size={{ xs: 12, md: 6 }}>
          {/* Brand & Category badges */}
          <Stack direction="row" spacing={1} alignItems="center" mb={1.5}>
            {product.brandName && (
              <Chip
                label={product.brandName}
                size="small"
                variant="outlined"
                color="primary"
                data-testid="brand-badge"
                sx={{ fontWeight: 600, fontSize: "0.75rem" }}
              />
            )}
            {product.categoryName && (
              <Chip
                label={product.categoryName}
                size="small"
                variant="outlined"
                data-testid="category-badge"
                sx={{ fontWeight: 500, fontSize: "0.75rem" }}
              />
            )}
          </Stack>

          {/* Title */}
          <Typography
            variant="h4"
            component="h1"
            fontWeight={700}
            color="text.primary"
            mb={1.5}
            data-testid="product-title"
          >
            {product.name}
          </Typography>

          {/* Rating & Sales */}
          <Stack direction="row" spacing={2} alignItems="center" mb={2}>
            <Stack direction="row" spacing={0.5} alignItems="center">
              <StarRoundedIcon sx={{ fontSize: 20, color: "#f59e0b" }} />
              <Typography variant="body2" fontWeight={700} color="text.primary">
                {product.rating ? product.rating.toFixed(1) : "5.0"}
              </Typography>
            </Stack>
            <Divider
              orientation="vertical"
              flexItem
              sx={{ height: 16, my: "auto" }}
            />
            <Typography variant="body2" color="text.secondary">
              Đã bán <strong>{product.salesCount || 0}</strong>
            </Typography>
            <Divider
              orientation="vertical"
              flexItem
              sx={{ height: 16, my: "auto" }}
            />
            {/* Stock status */}
            <Stack direction="row" spacing={0.5} alignItems="center">
              {currentStock > 5 ? (
                <>
                  <CheckCircleOutlineRoundedIcon
                    sx={{ fontSize: 18, color: "success.main" }}
                  />
                  <Typography
                    variant="body2"
                    color="success.main"
                    fontWeight={600}
                    data-testid="stock-status"
                  >
                    Còn hàng ({currentStock} sản phẩm)
                  </Typography>
                </>
              ) : currentStock > 0 ? (
                <>
                  <WarningAmberRoundedIcon
                    sx={{ fontSize: 18, color: "warning.main" }}
                  />
                  <Typography
                    variant="body2"
                    color="warning.main"
                    fontWeight={600}
                    data-testid="stock-status"
                  >
                    Sắp hết hàng (Chỉ còn {currentStock} sản phẩm)
                  </Typography>
                </>
              ) : (
                <>
                  <RemoveCircleOutlineRoundedIcon
                    sx={{ fontSize: 18, color: "error.main" }}
                  />
                  <Typography
                    variant="body2"
                    color="error.main"
                    fontWeight={600}
                    data-testid="stock-status"
                  >
                    Tạm hết hàng
                  </Typography>
                </>
              )}
            </Stack>
          </Stack>

          {/* Selected Variant SKU */}
          {selectedVariant?.sku && (
            <Typography
              variant="caption"
              color="text.secondary"
              data-testid="variant-sku"
              sx={{ display: "block", mb: 2 }}
            >
              Mã SKU: <strong>{selectedVariant.sku}</strong>
            </Typography>
          )}

          {/* Price Box */}
          <Paper
            elevation={0}
            sx={{
              p: 2.5,
              borderRadius: 2.5,
              bgcolor: "#f8fafc",
              border: "1px solid #e2e8f0",
              mb: 3,
            }}
          >
            <Stack
              direction="row"
              spacing={2}
              alignItems="baseline"
              flexWrap="wrap"
            >
              <Typography
                variant="h4"
                fontWeight={800}
                color="primary.main"
                data-testid="product-price"
              >
                {displayPrice}
              </Typography>
              {hasDiscount && displayOriginalPrice && (
                <Typography
                  variant="body1"
                  color="text.disabled"
                  sx={{ textDecoration: "line-through" }}
                  data-testid="original-price"
                >
                  {formatPrice(displayOriginalPrice)}
                </Typography>
              )}
            </Stack>
          </Paper>

          {/* Variant Option Selector: Color Swatches */}
          {availableColors.length > 0 && (
            <Box mb={2.5} data-testid="variant-colors-container">
              <Typography
                variant="subtitle2"
                fontWeight={700}
                color="text.primary"
                mb={1}
              >
                Màu sắc:{" "}
                <Typography
                  component="span"
                  fontWeight={600}
                  color="primary.main"
                  data-testid="selected-color-label"
                >
                  {selectedColor || "Chưa chọn"}
                </Typography>
              </Typography>
              <Stack direction="row" spacing={1.5} flexWrap="wrap" useFlexGap>
                {availableColors.map((color) => {
                  const isSelected = selectedColor === color;
                  const disabled = isColorDisabled(color);
                  return (
                    <Button
                      key={color}
                      variant={isSelected ? "contained" : "outlined"}
                      disabled={disabled}
                      onClick={() => handleColorSelect(color)}
                      data-testid={`color-option-${color}`}
                      sx={{
                        textTransform: "none",
                        borderRadius: 2,
                        px: 2,
                        py: 0.75,
                        fontWeight: 600,
                        fontSize: "0.875rem",
                        borderColor: isSelected ? "primary.main" : "#cbd5e1",
                        bgcolor: isSelected
                          ? "primary.main"
                          : disabled
                            ? "#f1f5f9"
                            : "#ffffff",
                        color: isSelected
                          ? "#ffffff"
                          : disabled
                            ? "text.disabled"
                            : "text.primary",
                        boxShadow: isSelected
                          ? "0 2px 6px rgba(37, 99, 235, 0.3)"
                          : "none",
                        "&:hover": {
                          borderColor: isSelected
                            ? "primary.dark"
                            : "primary.main",
                          bgcolor: isSelected ? "primary.dark" : "#f8fafc",
                        },
                      }}
                    >
                      {color}
                    </Button>
                  );
                })}
              </Stack>
            </Box>
          )}

          {/* Variant Option Selector: Storage */}
          {availableStorages.length > 0 && (
            <Box mb={3} data-testid="variant-storages-container">
              <Typography
                variant="subtitle2"
                fontWeight={700}
                color="text.primary"
                mb={1}
              >
                Dung lượng:{" "}
                <Typography
                  component="span"
                  fontWeight={600}
                  color="primary.main"
                  data-testid="selected-storage-label"
                >
                  {selectedStorage || "Chưa chọn"}
                </Typography>
              </Typography>
              <Stack direction="row" spacing={1.5} flexWrap="wrap" useFlexGap>
                {availableStorages.map((storage) => {
                  const isSelected = selectedStorage === storage;
                  const disabled = isStorageDisabled(storage);
                  return (
                    <Button
                      key={storage}
                      variant={isSelected ? "contained" : "outlined"}
                      disabled={disabled}
                      onClick={() => handleStorageSelect(storage)}
                      data-testid={`storage-option-${storage}`}
                      sx={{
                        textTransform: "none",
                        borderRadius: 2,
                        px: 2,
                        py: 0.75,
                        fontWeight: 600,
                        fontSize: "0.875rem",
                        borderColor: isSelected ? "primary.main" : "#cbd5e1",
                        bgcolor: isSelected
                          ? "primary.main"
                          : disabled
                            ? "#f1f5f9"
                            : "#ffffff",
                        color: isSelected
                          ? "#ffffff"
                          : disabled
                            ? "text.disabled"
                            : "text.primary",
                        boxShadow: isSelected
                          ? "0 2px 6px rgba(37, 99, 235, 0.3)"
                          : "none",
                        "&:hover": {
                          borderColor: isSelected
                            ? "primary.dark"
                            : "primary.main",
                          bgcolor: isSelected ? "primary.dark" : "#f8fafc",
                        },
                      }}
                    >
                      {storage}
                    </Button>
                  );
                })}
              </Stack>
            </Box>
          )}

          {/* Quantity Selector and Stock Alerts */}
          <Box mb={3} data-testid="quantity-section">
            <Typography
              variant="subtitle2"
              fontWeight={700}
              color="text.primary"
              mb={1.5}
            >
              Số lượng:
            </Typography>
            <Stack
              direction="row"
              spacing={2}
              alignItems="center"
              flexWrap="wrap"
              useFlexGap
            >
              <Box
                sx={{
                  display: "inline-flex",
                  alignItems: "center",
                  border: "1px solid #cbd5e1",
                  borderRadius: 2,
                  bgcolor: "#ffffff",
                  overflow: "hidden",
                }}
              >
                <IconButton
                  size="small"
                  onClick={() => setQuantity((prev) => Math.max(1, prev - 1))}
                  disabled={quantity <= 1 || currentStock <= 0}
                  data-testid="decrease-quantity-btn"
                  sx={{
                    borderRadius: 0,
                    p: 1,
                    color: "text.primary",
                    "&:disabled": { color: "text.disabled" },
                  }}
                >
                  <RemoveRoundedIcon fontSize="small" />
                </IconButton>
                <Typography
                  sx={{
                    minWidth: 44,
                    textAlign: "center",
                    fontWeight: 700,
                    fontSize: "0.95rem",
                    userSelect: "none",
                  }}
                  data-testid="quantity-value"
                >
                  {currentStock <= 0 ? 0 : quantity}
                </Typography>
                <IconButton
                  size="small"
                  onClick={() =>
                    setQuantity((prev) => Math.min(currentStock, prev + 1))
                  }
                  disabled={quantity >= currentStock || currentStock <= 0}
                  data-testid="increase-quantity-btn"
                  sx={{
                    borderRadius: 0,
                    p: 1,
                    color: "text.primary",
                    "&:disabled": { color: "text.disabled" },
                  }}
                >
                  <AddRoundedIcon fontSize="small" />
                </IconButton>
              </Box>

              {currentStock > 0 && currentStock <= 5 && (
                <Chip
                  icon={
                    <WarningAmberRoundedIcon
                      sx={{ fontSize: "1rem !important" }}
                    />
                  }
                  label={`Chỉ còn ${currentStock} sản phẩm trong kho`}
                  size="small"
                  color="warning"
                  variant="outlined"
                  data-testid="low-stock-alert"
                  sx={{ fontWeight: 600, fontSize: "0.8125rem" }}
                />
              )}

              {currentStock <= 0 && (
                <Chip
                  icon={
                    <RemoveCircleOutlineRoundedIcon
                      sx={{ fontSize: "1rem !important" }}
                    />
                  }
                  label="Hết hàng"
                  size="small"
                  color="error"
                  variant="outlined"
                  data-testid="out-of-stock-alert"
                  sx={{ fontWeight: 600, fontSize: "0.8125rem" }}
                />
              )}
            </Stack>
          </Box>

          {/* Action Buttons: Thêm vào giỏ & Mua ngay */}
          <Stack
            direction={{ xs: "column", sm: "row" }}
            spacing={2}
            mb={3.5}
            data-testid="purchase-actions"
          >
            <Button
              variant="contained"
              color="primary"
              size="large"
              startIcon={
                isAddingToCart ? (
                  <CircularProgress size={20} color="inherit" />
                ) : (
                  <ShoppingCartOutlinedIcon />
                )
              }
              disabled={currentStock <= 0 || isAddingToCart}
              onClick={handleAddToCart}
              data-testid="add-to-cart-btn"
              sx={{
                flex: 1,
                py: 1.5,
                borderRadius: 2.5,
                fontWeight: 700,
                fontSize: "1rem",
                textTransform: "none",
                boxShadow:
                  currentStock > 0
                    ? "0 4px 12px rgba(37, 99, 235, 0.25)"
                    : "none",
              }}
            >
              {isAddingToCart ? "Đang thêm..." : "Thêm vào giỏ"}
            </Button>
            <Button
              variant="contained"
              color="error"
              size="large"
              startIcon={<FlashOnRoundedIcon />}
              disabled={currentStock <= 0}
              data-testid="buy-now-btn"
              sx={{
                flex: 1,
                py: 1.5,
                borderRadius: 2.5,
                fontWeight: 700,
                fontSize: "1rem",
                textTransform: "none",
                bgcolor: currentStock > 0 ? "#dc2626" : undefined,
                "&:hover": {
                  bgcolor: currentStock > 0 ? "#b91c1c" : undefined,
                },
                boxShadow:
                  currentStock > 0
                    ? "0 4px 12px rgba(220, 38, 38, 0.25)"
                    : "none",
              }}
            >
              Mua ngay
            </Button>
          </Stack>

          {/* Short Description */}
          {product.description && (
            <Box mb={3}>
              <Typography
                variant="subtitle2"
                fontWeight={700}
                color="text.primary"
                mb={1}
              >
                Đặc điểm nổi bật:
              </Typography>
              <Typography
                variant="body2"
                color="text.secondary"
                sx={{ lineHeight: 1.7, whiteSpace: "pre-line" }}
                data-testid="product-short-description"
              >
                {product.description}
              </Typography>
            </Box>
          )}

          <Divider sx={{ my: 3 }} />

          {/* Specifications Table (Summary) */}
          {product.specifications && product.specifications.length > 0 && (
            <Box mb={3}>
              <Typography
                variant="h6"
                fontWeight={700}
                color="text.primary"
                mb={2}
              >
                Thông số kỹ thuật
              </Typography>
              <Paper
                elevation={0}
                sx={{
                  borderRadius: 2,
                  border: "1px solid #e2e8f0",
                  overflow: "hidden",
                }}
              >
                <Table size="small" data-testid="specifications-table">
                  <TableBody>
                    {product.specifications.map((spec, index) => (
                      <TableRow
                        key={spec.id || index}
                        sx={{
                          bgcolor: index % 2 === 0 ? "#f8fafc" : "#ffffff",
                        }}
                      >
                        <TableCell
                          sx={{
                            fontWeight: 600,
                            color: "text.primary",
                            width: "35%",
                            borderBottom: "1px solid #e2e8f0",
                            py: 1.25,
                          }}
                        >
                          {spec.specKey}
                        </TableCell>
                        <TableCell
                          sx={{
                            color: "text.secondary",
                            borderBottom: "1px solid #e2e8f0",
                            py: 1.25,
                          }}
                        >
                          {spec.specValue}
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </Paper>
            </Box>
          )}
        </Grid>
      </Grid>

      {/* Related Products Section */}
      {(relatedLoading ||
        Boolean(relatedProducts && relatedProducts.length > 0)) && (
        <Box sx={{ mt: 6 }} data-testid="related-products-section">
          <Typography
            variant="h5"
            fontWeight={700}
            color="text.primary"
            mb={3}
            data-testid="related-products-title"
          >
            Sản phẩm liên quan
          </Typography>

          {relatedLoading ? (
            <Grid container spacing={3} data-testid="related-products-skeleton">
              {Array.from({ length: 4 }).map((_, idx) => (
                <Grid size={{ xs: 12, sm: 6, md: 3 }} key={idx}>
                  <Skeleton
                    variant="rounded"
                    height={320}
                    sx={{ borderRadius: 2.5 }}
                  />
                </Grid>
              ))}
            </Grid>
          ) : (
            <Grid container spacing={3} data-testid="related-products-grid">
              {relatedProducts.map((relProduct) => (
                <Grid size={{ xs: 12, sm: 6, md: 3 }} key={relProduct.id}>
                  <ProductCard product={relProduct} />
                </Grid>
              ))}
            </Grid>
          )}
        </Box>
      )}

      {/* Lightbox Zoom Dialog */}
      <Dialog
        open={zoomOpen}
        onClose={() => setZoomOpen(false)}
        maxWidth="md"
        fullWidth
        data-testid="zoom-dialog"
        PaperProps={{
          sx: {
            borderRadius: 3,
            p: 2,
            bgcolor: "#ffffff",
            position: "relative",
          },
        }}
      >
        <IconButton
          onClick={() => setZoomOpen(false)}
          data-testid="close-zoom-btn"
          sx={{
            position: "absolute",
            top: 12,
            right: 12,
            zIndex: 3,
            bgcolor: "rgba(0,0,0,0.06)",
            "&:hover": { bgcolor: "rgba(0,0,0,0.12)" },
          }}
        >
          <CloseRoundedIcon />
        </IconButton>
        {selectedImage && (
          <Box
            component="img"
            src={selectedImage}
            alt="Zoomed product view"
            data-testid="zoomed-image"
            sx={{
              width: "100%",
              maxHeight: "75vh",
              objectFit: "contain",
              display: "block",
              mx: "auto",
            }}
          />
        )}
      </Dialog>

      <Snackbar
        open={toastOpen}
        autoHideDuration={3500}
        onClose={() => setToastOpen(false)}
        anchorOrigin={{ vertical: "bottom", horizontal: "center" }}
      >
        <Alert
          onClose={() => setToastOpen(false)}
          severity={toastSeverity}
          variant="filled"
          data-testid="cart-toast"
          sx={{ width: "100%", borderRadius: 2, fontWeight: 600 }}
        >
          {toastMessage}
        </Alert>
      </Snackbar>
    </Box>
  );
}
