import { useState } from "react";
import {
  Box,
  Card,
  CardActionArea,
  CardContent,
  Chip,
  Stack,
  Typography,
} from "@mui/material";
import StarRoundedIcon from "@mui/icons-material/StarRounded";
import SmartphoneRoundedIcon from "@mui/icons-material/SmartphoneRounded";
import { Link } from "react-router-dom";
import type { StorefrontProduct } from "../../services/storefrontService";

interface ProductCardProps {
  product: StorefrontProduct;
}

function formatPrice(value: number): string {
  return new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
  }).format(value);
}

export function ProductCard({ product }: ProductCardProps) {
  const [imageError, setImageError] = useState(false);

  const hasDiscount = product.discountPercent > 0;
  const isOutOfStock = !product.hasStock;

  const displayPrice =
    product.minPrice === product.maxPrice
      ? formatPrice(product.minPrice)
      : `${formatPrice(product.minPrice)} - ${formatPrice(product.maxPrice)}`;

  return (
    <Card
      data-testid={`product-card-${product.id}`}
      sx={{
        height: "100%",
        display: "flex",
        flexDirection: "column",
        borderRadius: 2.5,
        border: "1px solid #e0e0e0",
        boxShadow: "none",
        transition: "all 0.25s ease-in-out",
        "&:hover": {
          transform: "translateY(-4px)",
          boxShadow: "0 10px 24px rgba(0,0,0,0.08)",
          borderColor: "primary.main",
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
          pt="85%" /* aspect ratio ~ 1.15 */
          bgcolor="#f9fafb"
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
                borderRadius: 1.5,
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
                bgcolor: "rgba(0, 0, 0, 0.65)",
                color: "#fff",
                fontWeight: 600,
                fontSize: "0.72rem",
                borderRadius: 1.5,
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
              <Box
                display="flex"
                alignItems="center"
                justifyContent="center"
                width="100%"
                height="100%"
                color="text.disabled"
              >
                <SmartphoneRoundedIcon sx={{ fontSize: 64, opacity: 0.6 }} />
              </Box>
            )}
          </Box>
        </Box>

        {/* Product Information */}
        <CardContent
          sx={{
            p: 2,
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
              {[product.brandName, product.categoryName]
                .filter(Boolean)
                .join(" • ")}
            </Typography>
          )}

          {/* Product Title */}
          <Typography
            variant="subtitle1"
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
          <Stack direction="row" spacing={1} alignItems="center">
            <Stack direction="row" spacing={0.3} alignItems="center">
              <StarRoundedIcon sx={{ fontSize: 16, color: "#f59e0b" }} />
              <Typography
                variant="caption"
                fontWeight={700}
                color="text.primary"
              >
                {product.rating ? product.rating.toFixed(1) : "5.0"}
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
                color={isOutOfStock ? "text.secondary" : "error.main"}
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
    </Card>
  );
}
