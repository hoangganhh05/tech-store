import { useState } from "react";
import {
  Alert,
  Box,
  Card,
  CardActionArea,
  CardContent,
  Chip,
  CircularProgress,
  IconButton,
  Stack,
  Snackbar,
  Typography,
} from "@mui/material";
import FavoriteBorderRoundedIcon from "@mui/icons-material/FavoriteBorderRounded";
import FavoriteRoundedIcon from "@mui/icons-material/FavoriteRounded";
import StarRoundedIcon from "@mui/icons-material/StarRounded";
import { ProductPlaceholder } from "./ProductPlaceholder";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { useWishlist } from "../../hooks/useWishlist";
import { ROUTES } from "../../constants/routes";
import type { StorefrontProduct } from "../../services/storefrontService";

interface ProductCardProps {
  product: StorefrontProduct;
  onFavoriteChange?: (isFavorite: boolean) => void;
}

function formatPrice(value: number): string {
  return new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
  }).format(value);
}

export function ProductCard({ product, onFavoriteChange }: ProductCardProps) {
  const [imageError, setImageError] = useState(false);
  const [feedback, setFeedback] = useState<string | null>(null);
  const [feedbackSeverity, setFeedbackSeverity] = useState<"success" | "error">("success");
  const navigate = useNavigate();
  const location = useLocation();
  const { isAuthenticated, favoriteIds, loadingIds, toggleFavorite } = useWishlist();
  const isFavorite = favoriteIds.has(product.id);
  const isFavoriteLoading = loadingIds.has(product.id);

  const hasDiscount = product.discountPercent > 0;
  const isOutOfStock = !product.hasStock;

  const displayPrice =
    product.minPrice === product.maxPrice
      ? formatPrice(product.minPrice)
      : `${formatPrice(product.minPrice)} - ${formatPrice(product.maxPrice)}`;

  const handleFavoriteClick = async (event: React.MouseEvent<HTMLButtonElement>) => {
    event.preventDefault();
    event.stopPropagation();
    if (!isAuthenticated) {
      navigate(ROUTES.login, {
        state: {
          from: `${location.pathname}${location.search}${location.hash}`,
        },
      });
      return;
    }

    try {
      const nextValue = await toggleFavorite(product.id);
      onFavoriteChange?.(nextValue);
      setFeedback(
        nextValue
          ? "Đã thêm sản phẩm vào danh sách yêu thích."
          : "Đã xoá sản phẩm khỏi danh sách yêu thích.",
      );
      setFeedbackSeverity("success");
    } catch (error: unknown) {
      setFeedback(
        error instanceof Error
          ? error.message
          : "Không thể cập nhật danh sách yêu thích.",
      );
      setFeedbackSeverity("error");
    }
  };

  return (
    <Card
      data-testid={`product-card-${product.id}`}
      sx={{
        height: "100%",
        position: "relative",
        display: "flex",
        flexDirection: "column",
        borderRadius: "16px",
        border: "1px solid #DBE6F2",
        bgcolor: "#fff",
        boxShadow: "0 1px 4px rgba(7,86,168,0.05)",
        transition: "transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease",
        "&:hover": {
          transform: "translateY(-2px)",
          boxShadow: "0 4px 20px rgba(7,86,168,0.12)",
          borderColor: "#B8CCDF",
          "& .product-card-image": { transform: "scale(1.05)" },
        },
      }}
    >
      <CardActionArea
        component={Link}
        to={`/products/${product.id}`}
        sx={{
          flexGrow: 1,
          display: "flex",
          flexDirection: "column",
          alignItems: "stretch",
          justifyContent: "flex-start",
        }}
      >
        {/* Image & Badges Container */}
        <Box
          position="relative"
          width="100%"
          pt="100%"
          bgcolor="#F7F9FC"
          overflow="hidden"
        >
          {/* Discount Badge */}
          {hasDiscount && (
            <Chip
              label={`-${product.discountPercent}%`}
              size="small"
              color="error"
              sx={{
                position: "absolute",
                top: 10,
                left: 10,
                zIndex: 2,
                fontWeight: 700,
                fontSize: "0.75rem",
                borderRadius: "20px",
                bgcolor: "#FEE2E2",
                color: "#991B1B",
              }}
            />
          )}

          {/* Out of stock badge */}
          {isOutOfStock && (
            <Chip
              label="Tạm hết hàng"
              size="small"
              sx={{
                position: "absolute",
                bottom: 10,
                left: 10,
                zIndex: 2,
                bgcolor: "#526579",
                color: "#fff",
                fontWeight: 600,
                fontSize: "0.72rem",
                borderRadius: "20px",
              }}
            />
          )}

          {/* Product Image or Fallback */}
          <Box
            position="absolute"
            top={0}
            left={0}
            right={0}
            bottom={0}
            display="flex"
            alignItems="center"
            justifyContent="center"
            p={2}
          >
            {product.thumbnailUrl && !imageError ? (
              <Box
                component="img"
                src={product.thumbnailUrl}
                alt={product.name}
                loading="lazy"
                className="product-card-image"
                onError={() => setImageError(true)}
                sx={{
                  maxHeight: "100%",
                  maxWidth: "100%",
                  objectFit: "contain",
                  transition: "transform 0.3s ease",
                  "&:hover": {
                    transform: "scale(1.06)",
                  },
                }}
              />
            ) : (
              <ProductPlaceholder compact />
            )}
          </Box>
        </Box>

        {/* Product Information */}
        <CardContent
          sx={{
            p: { xs: 1.5, sm: 1.75 },
            flexGrow: 1,
            display: "flex",
            flexDirection: "column",
            gap: 0.75,
          }}
        >
          {/* Brand & Category */}
          {(product.brandName || product.categoryName) && (
            <Typography
              variant="caption"
              color="text.secondary"
              fontWeight={500}
            >
              {product.brandName || product.categoryName}
            </Typography>
          )}

          {/* Product Title */}
          <Typography
            variant="subtitle2"
            component="h3"
            fontWeight={600}
            sx={{
              overflow: "hidden",
              textOverflow: "ellipsis",
              display: "-webkit-box",
              WebkitLineClamp: 2,
              WebkitBoxOrient: "vertical",
              lineHeight: 1.35,
              minHeight: "2.7em",
              color: "text.primary",
            }}
          >
            {product.name}
          </Typography>

          {/* Rating & Sales */}
          <Stack direction="row" gap={0.75} alignItems="center" flexWrap="wrap">
            <Stack direction="row" spacing={0.3} alignItems="center">
              <StarRoundedIcon sx={{ fontSize: 16, color: "#F2B705" }} />
              <Typography
                variant="caption"
                fontWeight={700}
                color="text.primary"
              >
                {product.rating ? product.rating.toFixed(1) : "Chưa đánh giá"}
              </Typography>
            </Stack>
            {product.salesCount > 0 && (
              <>
                <Typography variant="caption" color="text.disabled">
                  •
                </Typography>
                <Typography variant="caption" color="text.secondary">
                  Đã bán {product.salesCount}
                </Typography>
              </>
            )}
          </Stack>

          {/* Price Area */}
          <Box mt="auto" pt={1}>
            <Stack
              direction="row"
              spacing={1}
              alignItems="baseline"
              flexWrap="wrap"
            >
              <Typography
                variant="subtitle1"
                component="span"
                fontWeight={700}
                color={isOutOfStock ? "text.secondary" : "primary.main"}
                sx={{ fontSize: { xs: "0.875rem", sm: "1rem" }, lineHeight: 1.6 }}
              >
                {displayPrice}
              </Typography>
              {hasDiscount && product.originalPrice && (
                <Typography
                  variant="caption"
                  component="span"
                  color="text.disabled"
                  sx={{ textDecoration: "line-through" }}
                >
                  {formatPrice(product.originalPrice)}
                </Typography>
              )}
            </Stack>
          </Box>
        </CardContent>
      </CardActionArea>
      <IconButton
        type="button"
        aria-label={isFavorite ? "Xoá khỏi yêu thích" : "Thêm vào yêu thích"}
        aria-pressed={isFavorite}
        data-testid={`favorite-button-${product.id}`}
        onClick={(event) => void handleFavoriteClick(event)}
        disabled={isFavoriteLoading}
        sx={{ position: "absolute", top: 6, right: 6, zIndex: 3, width: 44, height: 44, bgcolor: "rgba(255,255,255,0.94)", color: isFavorite ? "error.main" : "text.secondary", boxShadow: "0 1px 4px rgba(6,46,99,0.06)", "&:hover": { bgcolor: "#fff" } }}
      >
        {isFavoriteLoading ? <CircularProgress size={18} /> : isFavorite ? <FavoriteRoundedIcon fontSize="small" /> : <FavoriteBorderRoundedIcon fontSize="small" />}
      </IconButton>
      <Snackbar
        open={Boolean(feedback)}
        autoHideDuration={3000}
        onClose={() => setFeedback(null)}
      >
        <Alert severity={feedbackSeverity} onClose={() => setFeedback(null)}>
          {feedback}
        </Alert>
      </Snackbar>
    </Card>
  );
}
