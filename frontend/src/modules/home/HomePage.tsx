import { useEffect, useState, useCallback } from "react";
import {
  Alert,
  Box,
  Button,
  Card,
  CardActionArea,
  Grid,
  Skeleton,
  Stack,
  Typography,
} from "@mui/material";
import ArrowForwardRoundedIcon from "@mui/icons-material/ArrowForwardRounded";
import LocalFireDepartmentRoundedIcon from "@mui/icons-material/LocalFireDepartmentRounded";
import AutoAwesomeRoundedIcon from "@mui/icons-material/AutoAwesomeRounded";
import NewReleasesRoundedIcon from "@mui/icons-material/NewReleasesRounded";
import CategoryRoundedIcon from "@mui/icons-material/CategoryRounded";
import RefreshRoundedIcon from "@mui/icons-material/RefreshRounded";
import { Link } from "react-router-dom";
import { ROUTES } from "../../constants/routes";
import { env } from "../../configs/env";
import { ProductCard } from "../../components/common/ProductCard";
import {
  getStorefrontHomeData,
  type StorefrontHomeData,
} from "../../services/storefrontService";

export function HomePage() {
  const [data, setData] = useState<StorefrontHomeData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadData = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const result = await getStorefrontHomeData(8);
      setData(result);
    } catch (err: unknown) {
      const msg =
        err instanceof Error ? err.message : "Không thể tải dữ liệu trang chủ";
      setError(msg);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadData();
  }, [loadData]);

  return (
    <Stack spacing={{ xs: 4, md: 6 }} pb={4}>
      {/* 1. Hero Banner */}
      <Box
        sx={{
          background:
            "linear-gradient(135deg, #0d47a1 0%, #1565c0 50%, #1976d2 100%)",
          color: "#ffffff",
          borderRadius: { xs: 3, md: 4 },
          p: { xs: 3.5, sm: 5, md: 7 },
          position: "relative",
          overflow: "hidden",
          boxShadow: "0 12px 32px rgba(21, 101, 192, 0.25)",
        }}
      >
        <Box
          sx={{
            position: "absolute",
            top: -40,
            right: -40,
            width: 260,
            height: 260,
            borderRadius: "50%",
            background:
              "radial-gradient(circle, rgba(255,255,255,0.15) 0%, rgba(255,255,255,0) 70%)",
            pointerEvents: "none",
          }}
        />
        <Stack spacing={2.5} maxWidth={680} position="relative" zIndex={1}>
          <Stack direction="row" spacing={1} alignItems="center">
            <AutoAwesomeRoundedIcon sx={{ color: "#ffd54f", fontSize: 20 }} />
            <Typography
              variant="subtitle2"
              sx={{ color: "#ffd54f", fontWeight: 700, letterSpacing: 1 }}
            >
              {env.brand.name.toLocaleUpperCase("vi-VN")}
            </Typography>
          </Stack>
          <Typography
            component="h1"
            variant="h1"
            sx={{
              fontSize: { xs: "1.85rem", sm: "2.4rem", md: "2.85rem" },
              fontWeight: 800,
              lineHeight: 1.2,
              letterSpacing: "-0.02em",
              color: "#ffffff",
            }}
          >
            {env.brand.name}
          </Typography>
          <Typography
            sx={{
              fontSize: { xs: "0.95rem", md: "1.1rem" },
              color: "rgba(255, 255, 255, 0.88)",
              lineHeight: 1.6,
            }}
          >
            {env.brand.industry}
          </Typography>
          <Stack direction={{ xs: "column", sm: "row" }} spacing={2} pt={1}>
            <Button
              component={Link}
              to={ROUTES.products}
              variant="contained"
              size="large"
              endIcon={<ArrowForwardRoundedIcon />}
              sx={{
                bgcolor: "#ffffff",
                color: "#0d47a1",
                fontWeight: 700,
                px: 3.5,
                py: 1.2,
                borderRadius: 2,
                "&:hover": {
                  bgcolor: "#f5f5f5",
                },
              }}
            >
              Khám phá sản phẩm
            </Button>
            {data?.onSaleProducts && data.onSaleProducts.length > 0 && (
              <Button
                component="a"
                href="#on-sale-section"
                variant="outlined"
                size="large"
                sx={{
                  borderColor: "rgba(255, 255, 255, 0.6)",
                  color: "#ffffff",
                  fontWeight: 600,
                  px: 3,
                  py: 1.2,
                  borderRadius: 2,
                  "&:hover": {
                    borderColor: "#ffffff",
                    bgcolor: "rgba(255, 255, 255, 0.1)",
                  },
                }}
              >
                Săn sale giá sốc 🔥
              </Button>
            )}
          </Stack>
        </Stack>
      </Box>

      {/* Error Alert */}
      {error && (
        <Alert
          severity="error"
          action={
            <Button
              color="inherit"
              size="small"
              startIcon={<RefreshRoundedIcon />}
              onClick={loadData}
            >
              Thử lại
            </Button>
          }
          sx={{ borderRadius: 2 }}
        >
          {error}
        </Alert>
      )}

      {/* 2. Featured Categories */}
      <Box>
        <Stack
          direction="row"
          justifyContent="space-between"
          alignItems="flex-end"
          mb={2.5}
        >
          <Box>
            <Typography
              component="h2"
              variant="h2"
              sx={{ fontSize: { xs: "1.3rem", md: "1.6rem" }, fontWeight: 700 }}
            >
              Danh mục nổi bật
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Lựa chọn nhanh dòng sản phẩm bạn quan tâm
            </Typography>
          </Box>
          <Button
            component={Link}
            to={ROUTES.products}
            endIcon={<ArrowForwardRoundedIcon />}
            size="small"
          >
            Tất cả
          </Button>
        </Stack>

        {loading ? (
          <Grid container spacing={2}>
            {Array.from({ length: 6 }).map((_, idx) => (
              <Grid key={idx} size={{ xs: 6, sm: 4, md: 2 }}>
                <Skeleton
                  variant="rounded"
                  height={100}
                  sx={{ borderRadius: 2 }}
                />
              </Grid>
            ))}
          </Grid>
        ) : data?.featuredCategories && data.featuredCategories.length > 0 ? (
          <Grid container spacing={2}>
            {data.featuredCategories.map((cat) => (
              <Grid key={cat.id} size={{ xs: 6, sm: 4, md: 2 }}>
                <Card
                  variant="outlined"
                  sx={{
                    borderRadius: 2.5,
                    textAlign: "center",
                    transition: "all 0.2s ease",
                    "&:hover": {
                      borderColor: "primary.main",
                      transform: "translateY(-3px)",
                      boxShadow: "0 6px 16px rgba(0,0,0,0.06)",
                    },
                  }}
                >
                  <CardActionArea
                    component={Link}
                    to={`${ROUTES.products}?categoryId=${cat.id}`}
                    sx={{ p: 2 }}
                  >
                    <Box
                      display="flex"
                      alignItems="center"
                      justifyContent="center"
                      height={56}
                      width={56}
                      mx="auto"
                      mb={1}
                      borderRadius="50%"
                      bgcolor="#e3f2fd"
                      color="primary.main"
                    >
                      {cat.imageUrl ? (
                        <Box
                          component="img"
                          src={cat.imageUrl}
                          alt={cat.name}
                          sx={{ width: 36, height: 36, objectFit: "contain" }}
                        />
                      ) : (
                        <CategoryRoundedIcon sx={{ fontSize: 30 }} />
                      )}
                    </Box>
                    <Typography variant="body2" fontWeight={600} noWrap>
                      {cat.name}
                    </Typography>
                  </CardActionArea>
                </Card>
              </Grid>
            ))}
          </Grid>
        ) : null}
      </Box>

      {/* 3. Flash Sale / On Sale Section */}
      {loading ? (
        <Box>
          <Skeleton variant="text" width={220} height={36} sx={{ mb: 2 }} />
          <Grid container spacing={2}>
            {Array.from({ length: 4 }).map((_, idx) => (
              <Grid key={idx} size={{ xs: 12, sm: 6, md: 3 }}>
                <Skeleton
                  variant="rounded"
                  height={320}
                  sx={{ borderRadius: 2.5 }}
                />
              </Grid>
            ))}
          </Grid>
        </Box>
      ) : data?.onSaleProducts && data.onSaleProducts.length > 0 ? (
        <Box
          id="on-sale-section"
          sx={{
            p: { xs: 2.5, md: 3.5 },
            bgcolor: "#fff5f5",
            borderRadius: 3.5,
            border: "1px solid #fed7d7",
          }}
        >
          <Stack
            direction="row"
            justifyContent="space-between"
            alignItems="center"
            mb={2.5}
          >
            <Stack direction="row" spacing={1} alignItems="center">
              <LocalFireDepartmentRoundedIcon
                sx={{ color: "#e53e3e", fontSize: 28 }}
              />
              <Box>
                <Typography
                  component="h2"
                  variant="h2"
                  sx={{
                    fontSize: { xs: "1.25rem", md: "1.5rem" },
                    fontWeight: 700,
                    color: "#c53030",
                  }}
                >
                  Săn Sale Giá Sốc
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  Ưu đãi giảm giá có hạn dành riêng hôm nay
                </Typography>
              </Box>
            </Stack>
            <Button
              component={Link}
              to={`${ROUTES.products}?onSale=true`}
              endIcon={<ArrowForwardRoundedIcon />}
              size="small"
              sx={{ color: "#c53030" }}
            >
              Xem tất cả
            </Button>
          </Stack>
          <Grid container spacing={2}>
            {data.onSaleProducts.map((product) => (
              <Grid key={product.id} size={{ xs: 12, sm: 6, md: 3 }}>
                <ProductCard product={product} />
              </Grid>
            ))}
          </Grid>
        </Box>
      ) : null}

      {/* 4. Featured Products Section */}
      <Box>
        <Stack
          direction="row"
          justifyContent="space-between"
          alignItems="flex-end"
          mb={2.5}
        >
          <Stack direction="row" spacing={1} alignItems="center">
            <AutoAwesomeRoundedIcon
              sx={{ color: "primary.main", fontSize: 24 }}
            />
            <Box>
              <Typography
                component="h2"
                variant="h2"
                sx={{
                  fontSize: { xs: "1.3rem", md: "1.6rem" },
                  fontWeight: 700,
                }}
              >
                Sản phẩm nổi bật
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Được khách hàng quan tâm và lựa chọn hàng đầu
              </Typography>
            </Box>
          </Stack>
          <Button
            component={Link}
            to={ROUTES.products}
            endIcon={<ArrowForwardRoundedIcon />}
            size="small"
          >
            Xem thêm
          </Button>
        </Stack>

        {loading ? (
          <Grid container spacing={2}>
            {Array.from({ length: 4 }).map((_, idx) => (
              <Grid key={idx} size={{ xs: 12, sm: 6, md: 3 }}>
                <Skeleton
                  variant="rounded"
                  height={320}
                  sx={{ borderRadius: 2.5 }}
                />
              </Grid>
            ))}
          </Grid>
        ) : data?.featuredProducts && data.featuredProducts.length > 0 ? (
          <Grid container spacing={2}>
            {data.featuredProducts.map((product) => (
              <Grid key={product.id} size={{ xs: 12, sm: 6, md: 3 }}>
                <ProductCard product={product} />
              </Grid>
            ))}
          </Grid>
        ) : (
          <Box py={6} textAlign="center">
            <Typography color="text.secondary">
              Chưa có sản phẩm nổi bật nào.
            </Typography>
          </Box>
        )}
      </Box>

      {/* 5. New Arrivals Section */}
      <Box>
        <Stack
          direction="row"
          justifyContent="space-between"
          alignItems="flex-end"
          mb={2.5}
        >
          <Stack direction="row" spacing={1} alignItems="center">
            <NewReleasesRoundedIcon
              sx={{ color: "secondary.main", fontSize: 24 }}
            />
            <Box>
              <Typography
                component="h2"
                variant="h2"
                sx={{
                  fontSize: { xs: "1.3rem", md: "1.6rem" },
                  fontWeight: 700,
                }}
              >
                Sản phẩm mới về
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Cập nhật thiết bị công nghệ mới nhất vừa ra mắt
              </Typography>
            </Box>
          </Stack>
          <Button
            component={Link}
            to={ROUTES.products}
            endIcon={<ArrowForwardRoundedIcon />}
            size="small"
          >
            Xem thêm
          </Button>
        </Stack>

        {loading ? (
          <Grid container spacing={2}>
            {Array.from({ length: 4 }).map((_, idx) => (
              <Grid key={idx} size={{ xs: 12, sm: 6, md: 3 }}>
                <Skeleton
                  variant="rounded"
                  height={320}
                  sx={{ borderRadius: 2.5 }}
                />
              </Grid>
            ))}
          </Grid>
        ) : data?.newArrivals && data.newArrivals.length > 0 ? (
          <Grid container spacing={2}>
            {data.newArrivals.map((product) => (
              <Grid key={product.id} size={{ xs: 12, sm: 6, md: 3 }}>
                <ProductCard product={product} />
              </Grid>
            ))}
          </Grid>
        ) : (
          <Box py={6} textAlign="center">
            <Typography color="text.secondary">
              Chưa có sản phẩm mới nào.
            </Typography>
          </Box>
        )}
      </Box>

    </Stack>
  );
}
