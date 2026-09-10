import { useCallback, useEffect, useState } from "react";
import { Alert, Box, Button, CircularProgress, Grid, Stack, Typography } from "@mui/material";
import { isAxiosError } from "axios";
import { PageIntro } from "../../components/common/PageIntro";
import { ProductCard } from "../../components/common/ProductCard";
import { getWishlist, type WishlistPage as WishlistPageData } from "../../services/wishlistService";

const PAGE_SIZE = 12;

function getErrorMessage(error: unknown): string {
  if (isAxiosError<{ message?: string }>(error)) {
    return error.response?.data?.message || "Không thể tải danh sách yêu thích.";
  }
  return error instanceof Error ? error.message : "Không thể tải danh sách yêu thích.";
}

export function WishlistPage() {
  const [wishlist, setWishlist] = useState<WishlistPageData | null>(null);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  const loadWishlist = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setWishlist(await getWishlist(page, PAGE_SIZE));
    } catch (requestError: unknown) {
      setError(getErrorMessage(requestError));
    } finally {
      setLoading(false);
    }
  }, [page]);

  useEffect(() => {
    void loadWishlist();
  }, [loadWishlist]);

  const handleFavoriteChange = async (isFavorite: boolean) => {
    if (isFavorite) return;
    setMessage("Đã xoá sản phẩm khỏi danh sách yêu thích.");
    await loadWishlist();
  };

  return (
    <Stack spacing={3} data-testid="wishlist-page">
      <PageIntro
        eyebrow="Tài khoản"
        title="Sản phẩm yêu thích"
        description="Lưu lại những sản phẩm bạn quan tâm để xem và mua sau."
      />

      {loading && (
        <Stack alignItems="center" spacing={1} py={8}>
          <CircularProgress size={32} />
          <Typography color="text.secondary">Đang tải danh sách yêu thích...</Typography>
        </Stack>
      )}

      {!loading && error && (
        <Stack alignItems="center" spacing={2} py={6}>
          <Alert severity="error">{error}</Alert>
          <Button variant="outlined" onClick={() => void loadWishlist()}>Thử lại</Button>
        </Stack>
      )}

      {!loading && !error && wishlist?.items.length === 0 && (
        <Box py={8} textAlign="center">
          <Typography color="text.secondary">Bạn chưa có sản phẩm yêu thích nào.</Typography>
        </Box>
      )}

      {!loading && !error && wishlist && wishlist.items.length > 0 && (
        <>
          <Grid container spacing={2.5}>
            {wishlist.items.map((product) => (
              <Grid key={product.id} size={{ xs: 12, sm: 6, md: 4, lg: 3 }}>
                <ProductCard
                  product={product}
                  onFavoriteChange={(isFavorite) =>
                    void handleFavoriteChange(isFavorite)
                  }
                />
              </Grid>
            ))}
          </Grid>
          {wishlist.totalPages > 1 && (
            <Stack direction="row" justifyContent="center">
              <Button
                variant="outlined"
                disabled={wishlist.first}
                onClick={() => setPage((current) => Math.max(0, current - 1))}
              >
                Trang trước
              </Button>
              <Typography sx={{ alignSelf: "center", px: 2 }}>
                Trang {wishlist.page + 1} / {wishlist.totalPages}
              </Typography>
              <Button
                variant="outlined"
                disabled={wishlist.last}
                onClick={() => setPage((current) => current + 1)}
              >
                Trang sau
              </Button>
            </Stack>
          )}
        </>
      )}

      {message && <Alert severity="success" onClose={() => setMessage(null)}>{message}</Alert>}
    </Stack>
  );
}
