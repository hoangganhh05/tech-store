import { useEffect, useState, useCallback } from "react";
import {
  Alert,
  Box,
  Button,
  Card,
  CardActionArea,
  Grid,
  Paper,
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
import CheckCircleRoundedIcon from "@mui/icons-material/CheckCircleRounded";
import HeadsetMicRoundedIcon from "@mui/icons-material/HeadsetMicRounded";
import PhoneIphoneRoundedIcon from "@mui/icons-material/PhoneIphoneRounded";
import VerifiedRoundedIcon from "@mui/icons-material/VerifiedRounded";
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
      setError(err instanceof Error ? err.message : "Không thể tải dữ liệu trang chủ");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadData();
  }, [loadData]);

  return (
    <Stack spacing={{ xs: 4, md: 5 }} pb={4}>
      {/* 1. Hero Banner */}
      <Box sx={{ bgcolor: "secondary.main", color: "white", borderRadius: { xs: 3, md: 4 }, overflow: "hidden", position: "relative", boxShadow: "0 24px 60px rgba(15,23,42,.16)" }}>
        <Grid container alignItems="stretch">
          <Grid size={{ xs: 12, md: 7 }}>
            <Stack spacing={2.5} sx={{ p: { xs: 3.5, sm: 5, md: 7 }, minHeight: { md: 430 }, justifyContent: "center" }}>
          <Stack direction="row" spacing={1} alignItems="center">
                <Box width={34} height={2} bgcolor="primary.main" />
                <Typography variant="overline" sx={{ color: "#fca5a5", fontWeight: 800, letterSpacing: 1.6 }}>PHỤ KIỆN CHÍNH HÃNG</Typography>
          </Stack>
              <Typography component="h1" variant="h1" color="white" maxWidth={650}>{env.brand.name}</Typography>
              <Typography component="p" variant="h2" sx={{ color: "#f87171", fontSize: { xs: "1.55rem", md: "2.2rem" } }}>Phụ kiện phù hợp. Trải nghiệm khác biệt.</Typography>
              <Typography sx={{ fontSize: { xs: "1rem", md: "1.125rem" }, color: "#cbd5e1", lineHeight: 1.7, maxWidth: 570 }}>Khám phá phụ kiện điện thoại được chọn lọc tại {env.brand.name}, minh bạch giá và hỗ trợ tận tâm.</Typography>
          <Stack direction={{ xs: "column", sm: "row" }} spacing={2} pt={1}>
            <Button
              component={Link}
              to={ROUTES.products}
              variant="contained"
              size="large"
              endIcon={<ArrowForwardRoundedIcon />}
              sx={{
                    bgcolor: "primary.main",
                    color: "white",
                fontWeight: 700,
                px: 3.5,
                py: 1.2,
                borderRadius: 2,
                "&:hover": {
                      bgcolor: "primary.dark",
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
                    borderColor: "#475569",
                  color: "#ffffff",
                  fontWeight: 600,
                  px: 3,
                  py: 1.2,
                  borderRadius: 2,
                  "&:hover": {
                        borderColor: "#94a3b8",
                    bgcolor: "rgba(255, 255, 255, 0.1)",
                  },
                }}
              >
                Săn sale giá sốc 🔥
              </Button>
            )}
          </Stack>
            </Stack>
          </Grid>
          <Grid size={{ xs: 12, md: 5 }} sx={{ display: { xs: "none", md: "block" } }}>
            <Box sx={{ height: "100%", minHeight: 430, position: "relative", display: "grid", placeItems: "center", background: "radial-gradient(circle at center, rgba(220,38,38,.28), transparent 62%)" }}>
              <Box sx={{ position: "absolute", inset: 28, border: "1px solid rgba(255,255,255,.1)", borderRadius: 4 }} />
              <Box sx={{ width: 218, height: 330, borderRadius: "34px", border: "8px solid #334155", bgcolor: "#020617", boxShadow: "0 30px 60px rgba(0,0,0,.4)", transform: "rotate(8deg)", display: "grid", placeItems: "center", position: "relative" }}>
                <Box sx={{ position: "absolute", top: 10, width: 72, height: 18, bgcolor: "#334155", borderRadius: 8 }} />
                <PhoneIphoneRoundedIcon sx={{ fontSize: 88, color: "#ef4444", opacity: .9 }} />
              </Box>
              <Paper sx={{ position: "absolute", left: 24, bottom: 38, p: 2, borderRadius: 2.5, minWidth: 190, boxShadow: "0 18px 40px rgba(0,0,0,.25)" }}>
                <Typography variant="caption" color="text.secondary">Cam kết từ cửa hàng</Typography>
                <Typography fontWeight={800} mt={0.5}>Tư vấn đúng nhu cầu</Typography>
              </Paper>
            </Box>
          </Grid>
        </Grid>
      </Box>

      <Grid container spacing={2}>
        {[
          { icon: <VerifiedRoundedIcon />, title: "Sản phẩm chọn lọc", text: "Thông tin rõ ràng" },
          { icon: <CheckCircleRoundedIcon />, title: "Mua hàng an tâm", text: "Hỗ trợ sau bán" },
          { icon: <HeadsetMicRoundedIcon />, title: "Tư vấn tận tình", text: env.brand.contact.phone },
        ].map((benefit) => (
          <Grid key={benefit.title} size={{ xs: 12, sm: 4 }}>
            <Paper variant="outlined" sx={{ p: 2, display: "flex", alignItems: "center", gap: 1.5, borderRadius: 2.5 }}>
              <Box color="primary.main" display="flex">{benefit.icon}</Box>
              <Box><Typography fontWeight={800} variant="body2">{benefit.title}</Typography><Typography variant="caption" color="text.secondary">{benefit.text}</Typography></Box>
            </Paper>
          </Grid>
        ))}
      </Grid>

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
          direction={{ xs: "column", sm: "row" }}
          justifyContent="space-between"
          alignItems={{ xs: "flex-start", sm: "flex-end" }}
          spacing={{ xs: 1, sm: 0 }}
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
        ) : (
          <Paper variant="outlined" sx={{ py: 5, px: 2, textAlign: "center", borderStyle: "dashed", bgcolor: "#fff" }}>
            <CategoryRoundedIcon color="disabled" sx={{ fontSize: 40, mb: 1 }} />
            <Typography fontWeight={700}>Danh mục đang được cập nhật</Typography>
            <Typography variant="body2" color="text.secondary">Quản trị viên có thể thêm danh mục trong trang quản trị.</Typography>
          </Paper>
        )}
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
            direction={{ xs: "column", sm: "row" }}
            justifyContent="space-between"
            alignItems={{ xs: "flex-start", sm: "center" }}
            spacing={{ xs: 1, sm: 0 }}
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
          direction={{ xs: "column", sm: "row" }}
          justifyContent="space-between"
          alignItems={{ xs: "flex-start", sm: "flex-end" }}
          spacing={{ xs: 1, sm: 0 }}
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
          direction={{ xs: "column", sm: "row" }}
          justifyContent="space-between"
          alignItems={{ xs: "flex-start", sm: "flex-end" }}
          spacing={{ xs: 1, sm: 0 }}
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
