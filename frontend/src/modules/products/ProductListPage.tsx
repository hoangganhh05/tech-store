import { useEffect, useState, useCallback, useMemo } from "react";
import {
  Alert,
  Box,
  Button,
  Checkbox,
  Chip,
  Divider,
  FormControl,
  FormControlLabel,
  Grid,
  InputLabel,
  MenuItem,
  Pagination,
  Paper,
  Select,
  Skeleton,
  Stack,
  TextField,
  Typography,
} from "@mui/material";
import RefreshRoundedIcon from "@mui/icons-material/RefreshRounded";
import FilterListRoundedIcon from "@mui/icons-material/FilterListRounded";
import SmartphoneRoundedIcon from "@mui/icons-material/SmartphoneRounded";
import SearchOffRoundedIcon from "@mui/icons-material/SearchOffRounded";
import TuneRoundedIcon from "@mui/icons-material/TuneRounded";
import { useSearchParams } from "react-router-dom";
import { PageIntro } from "../../components/common/PageIntro";
import { ProductCard } from "../../components/common/ProductCard";
import {
  getStorefrontProducts,
  getStorefrontCategories,
  getStorefrontBrands,
  searchStorefrontProducts,
  type StorefrontProduct,
  type StorefrontProductPageResponse,
} from "../../services/storefrontService";
import type { Category } from "../../services/categoryService";
import type { Brand } from "../../services/brandService";

const PRICE_PRESETS = [
  { label: "Dưới 5 triệu", min: null, max: 5000000 },
  { label: "5 - 15 triệu", min: 5000000, max: 15000000 },
  { label: "15 - 25 triệu", min: 15000000, max: 25000000 },
  { label: "Trên 25 triệu", min: 25000000, max: null },
];

function formatPrice(val: number): string {
  return new Intl.NumberFormat("vi-VN").format(val) + "₫";
}

export function ProductListPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const searchQuery = (searchParams.get("q") || "").trim();

  // Category filter
  const categoryIdParam = searchParams.get("categoryId");
  const selectedCategoryId = useMemo(() => {
    if (!categoryIdParam) return null;
    const parsed = Number(categoryIdParam);
    return isNaN(parsed) ? null : parsed;
  }, [categoryIdParam]);

  // Brands filter (multi-select)
  const brandIdsParam = searchParams.get("brandIds");
  const selectedBrandIds = useMemo(() => {
    if (!brandIdsParam) return [];
    return brandIdsParam
      .split(",")
      .map((s) => Number(s.trim()))
      .filter((n) => !isNaN(n) && n > 0);
  }, [brandIdsParam]);

  // Price filter
  const priceMinParam = searchParams.get("priceMin");
  const priceMin = useMemo(() => {
    if (!priceMinParam) return null;
    const n = Number(priceMinParam);
    return isNaN(n) ? null : n;
  }, [priceMinParam]);

  const priceMaxParam = searchParams.get("priceMax");
  const priceMax = useMemo(() => {
    if (!priceMaxParam) return null;
    const n = Number(priceMaxParam);
    return isNaN(n) ? null : n;
  }, [priceMaxParam]);

  // Sorting filter
  const sortByParam = searchParams.get("sortBy");
  const sortDirParam = searchParams.get("sortDir");

  const sortBy = sortByParam || "createdAt";
  const sortDir =
    sortDirParam === "asc" || sortDirParam === "desc" ? sortDirParam : "desc";

  const sortValue = useMemo(() => {
    if (sortBy === "price") {
      return sortDir === "asc" ? "price_asc" : "price_desc";
    }
    if (sortBy === "salesCount" || sortBy === "sales") {
      return "salesCount_desc";
    }
    return "createdAt_desc";
  }, [sortBy, sortDir]);

  // Pagination state (1-indexed in URL, 0-indexed in backend API)
  const pageParam = searchParams.get("page");
  const currentPage = useMemo(() => {
    if (!pageParam) return 1;
    const p = parseInt(pageParam, 10);
    return isNaN(p) || p < 1 ? 1 : p;
  }, [pageParam]);

  const pageSize = 12;
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);

  // Local state for custom price inputs
  const [customMin, setCustomMin] = useState(
    priceMin != null ? String(priceMin) : "",
  );
  const [customMax, setCustomMax] = useState(
    priceMax != null ? String(priceMax) : "",
  );
  const [priceError, setPriceError] = useState<string | null>(null);

  useEffect(() => {
    setCustomMin(priceMin != null ? String(priceMin) : "");
    setCustomMax(priceMax != null ? String(priceMax) : "");
  }, [priceMin, priceMax]);

  const [categories, setCategories] = useState<Category[]>([]);
  const [brands, setBrands] = useState<Brand[]>([]);
  const [products, setProducts] = useState<StorefrontProduct[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Load categories and brands
  useEffect(() => {
    let isMounted = true;
    getStorefrontCategories()
      .then((cats) => {
        if (isMounted) setCategories(cats);
      })
      .catch(() => {});

    getStorefrontBrands()
      .then((bList) => {
        if (isMounted) setBrands(bList);
      })
      .catch(() => {});

    return () => {
      isMounted = false;
    };
  }, []);

  // Load products based on query or combined filters
  const loadProducts = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      if (searchQuery) {
        const data = await searchStorefrontProducts(searchQuery);
        const sortedData = [...data];
        if (sortBy === "price") {
          sortedData.sort((a, b) =>
            sortDir === "asc"
              ? a.minPrice - b.minPrice
              : b.minPrice - a.minPrice,
          );
        } else if (sortBy === "salesCount" || sortBy === "sales") {
          sortedData.sort((a, b) => (b.salesCount || 0) - (a.salesCount || 0));
        } else if (sortBy === "createdAt") {
          if (sortDir === "asc") {
            sortedData.sort(
              (a, b) =>
                new Date(a.createdAt || 0).getTime() -
                new Date(b.createdAt || 0).getTime(),
            );
          } else {
            sortedData.sort(
              (a, b) =>
                new Date(b.createdAt || 0).getTime() -
                new Date(a.createdAt || 0).getTime(),
            );
          }
        }
        setTotalElements(sortedData.length);
        setTotalPages(Math.max(1, Math.ceil(sortedData.length / pageSize)));
        const startIdx = (currentPage - 1) * pageSize;
        setProducts(sortedData.slice(startIdx, startIdx + pageSize));
      } else {
        const res = await getStorefrontProducts({
          categoryId: selectedCategoryId,
          brandIds: selectedBrandIds,
          priceMin,
          priceMax,
          sortBy: sortByParam || undefined,
          sortDir:
            sortDirParam === "asc" || sortDirParam === "desc"
              ? sortDirParam
              : undefined,
          page: currentPage - 1,
          size: pageSize,
        });
        if (Array.isArray(res)) {
          setProducts(res);
          setTotalElements(res.length);
          setTotalPages(Math.max(1, Math.ceil(res.length / pageSize)));
        } else if (res && typeof res === "object" && "items" in res) {
          const pageRes = res as StorefrontProductPageResponse;
          setProducts(pageRes.items);
          setTotalElements(pageRes.totalElements);
          setTotalPages(pageRes.totalPages);
        }
      }
    } catch (err: unknown) {
      const msg =
        err instanceof Error ? err.message : "Không thể tải danh sách sản phẩm";
      setError(msg);
    } finally {
      setLoading(false);
    }
  }, [
    searchQuery,
    selectedCategoryId,
    selectedBrandIds,
    priceMin,
    priceMax,
    sortBy,
    sortDir,
    sortByParam,
    sortDirParam,
    currentPage,
    pageSize,
  ]);

  useEffect(() => {
    loadProducts();
  }, [loadProducts]);

  // Update URL helper (resets page to 1 on filter changes)
  const updateFilterParams = useCallback(
    (updates: {
      categoryId?: number | null;
      brandIds?: number[];
      priceMin?: number | null;
      priceMax?: number | null;
    }) => {
      const newParams = new URLSearchParams(searchParams);

      if (updates.categoryId !== undefined) {
        if (updates.categoryId) {
          newParams.set("categoryId", String(updates.categoryId));
        } else {
          newParams.delete("categoryId");
        }
      }

      if (updates.brandIds !== undefined) {
        if (updates.brandIds.length > 0) {
          newParams.set("brandIds", updates.brandIds.join(","));
        } else {
          newParams.delete("brandIds");
        }
      }

      if (updates.priceMin !== undefined) {
        if (updates.priceMin != null) {
          newParams.set("priceMin", String(updates.priceMin));
        } else {
          newParams.delete("priceMin");
        }
      }

      if (updates.priceMax !== undefined) {
        if (updates.priceMax != null) {
          newParams.set("priceMax", String(updates.priceMax));
        } else {
          newParams.delete("priceMax");
        }
      }

      // Reset to page 1 on filter changes
      newParams.delete("page");

      setSearchParams(newParams);
    },
    [searchParams, setSearchParams],
  );

  const handleCategoryChange = (newCategoryId: number | null) => {
    updateFilterParams({ categoryId: newCategoryId });
  };

  const handleToggleBrand = (brandId: number) => {
    const next = selectedBrandIds.includes(brandId)
      ? selectedBrandIds.filter((id) => id !== brandId)
      : [...selectedBrandIds, brandId];
    updateFilterParams({ brandIds: next });
  };

  const handleApplyPresetPrice = (min: number | null, max: number | null) => {
    if (priceMin === min && priceMax === max) {
      // Toggle off
      updateFilterParams({ priceMin: null, priceMax: null });
    } else {
      updateFilterParams({ priceMin: min, priceMax: max });
    }
  };

  const handleApplyCustomPrice = () => {
    const minVal = customMin.trim() ? Number(customMin.trim()) : null;
    const maxVal = customMax.trim() ? Number(customMax.trim()) : null;

    if (minVal != null && (isNaN(minVal) || minVal < 0)) {
      setPriceError("Giá tối thiểu không hợp lệ");
      return;
    }
    if (maxVal != null && (isNaN(maxVal) || maxVal < 0)) {
      setPriceError("Giá tối đa không hợp lệ");
      return;
    }
    if (minVal != null && maxVal != null && minVal > maxVal) {
      setPriceError("Giá tối thiểu không được lớn hơn giá tối đa");
      return;
    }

    setPriceError(null);
    updateFilterParams({ priceMin: minVal, priceMax: maxVal });
  };

  const handleClearAllFilters = () => {
    const newParams = new URLSearchParams();
    if (searchQuery) {
      newParams.set("q", searchQuery);
    }
    if (sortByParam) {
      newParams.set("sortBy", sortByParam);
    }
    if (sortDirParam) {
      newParams.set("sortDir", sortDirParam);
    }
    setSearchParams(newParams);
  };

  const handleClearSearch = () => {
    const newParams = new URLSearchParams(searchParams);
    newParams.delete("q");
    newParams.delete("page");
    setSearchParams(newParams);
  };

  const handleSortChange = (newSortValue: string) => {
    const newParams = new URLSearchParams(searchParams);
    if (newSortValue === "price_asc") {
      newParams.set("sortBy", "price");
      newParams.set("sortDir", "asc");
    } else if (newSortValue === "price_desc") {
      newParams.set("sortBy", "price");
      newParams.set("sortDir", "desc");
    } else if (newSortValue === "salesCount_desc") {
      newParams.set("sortBy", "salesCount");
      newParams.set("sortDir", "desc");
    } else {
      newParams.delete("sortBy");
      newParams.delete("sortDir");
    }
    // Reset to page 1 on sort changes
    newParams.delete("page");
    setSearchParams(newParams);
  };

  const handlePageChange = (
    _event: React.ChangeEvent<unknown>,
    value: number,
  ) => {
    const newParams = new URLSearchParams(searchParams);
    if (value > 1) {
      newParams.set("page", String(value));
    } else {
      newParams.delete("page");
    }
    setSearchParams(newParams);
    window.scrollTo({ top: 0, behavior: "smooth" });
  };

  const hasBrandOrPriceFilters = Boolean(
    selectedBrandIds.length > 0 || priceMin != null || priceMax != null,
  );

  const hasActiveFilters = Boolean(
    selectedCategoryId != null || hasBrandOrPriceFilters,
  );

  const currentCategory = useMemo(() => {
    if (!selectedCategoryId) return null;
    return categories.find((c) => c.id === selectedCategoryId) || null;
  }, [categories, selectedCategoryId]);

  const selectValue = useMemo(() => {
    if (selectedCategoryId === null) return "all";
    return categories.some((c) => c.id === selectedCategoryId)
      ? String(selectedCategoryId)
      : "all";
  }, [categories, selectedCategoryId]);

  return (
    <>
      <PageIntro
        title={
          searchQuery ? `Kết quả tìm kiếm cho: "${searchQuery}"` : "Sản phẩm"
        }
        description={
          searchQuery
            ? `Các sản phẩm phù hợp với từ khóa "${searchQuery}".`
            : "Tìm điện thoại và phụ kiện phù hợp với nhu cầu của bạn."
        }
      />

      {/* Active search filter badge */}
      {searchQuery && (
        <Stack direction="row" alignItems="center" spacing={1} mb={2.5}>
          <Typography variant="body2" color="text.secondary">
            Tìm kiếm:
          </Typography>
          <Chip
            label={`"${searchQuery}"`}
            onDelete={handleClearSearch}
            color="primary"
            variant="filled"
            size="small"
            sx={{ fontWeight: 600 }}
          />
          <Button
            size="small"
            onClick={handleClearSearch}
            sx={{
              textTransform: "none",
              color: "text.secondary",
              fontSize: "0.8125rem",
            }}
          >
            Xóa tìm kiếm
          </Button>
        </Stack>
      )}

      {/* Category quick chips */}
      <Stack
        direction={{ xs: "column", sm: "row" }}
        justifyContent="space-between"
        alignItems={{ xs: "stretch", sm: "center" }}
        spacing={2}
        mb={3}
      >
        <Stack
          direction="row"
          spacing={1}
          sx={{
            overflowX: "auto",
            pb: { xs: 1, sm: 0 },
            "::-webkit-scrollbar": { height: 4 },
          }}
        >
          <Chip
            label="Tất cả"
            clickable
            color={
              !searchQuery && selectedCategoryId === null
                ? "primary"
                : "default"
            }
            variant={
              !searchQuery && selectedCategoryId === null
                ? "filled"
                : "outlined"
            }
            onClick={() => handleCategoryChange(null)}
            sx={{ fontWeight: 600 }}
          />
          {categories.map((cat) => (
            <Chip
              key={cat.id}
              label={cat.name}
              clickable
              color={
                !searchQuery && selectedCategoryId === cat.id
                  ? "primary"
                  : "default"
              }
              variant={
                !searchQuery && selectedCategoryId === cat.id
                  ? "filled"
                  : "outlined"
              }
              onClick={() => handleCategoryChange(cat.id)}
              sx={{ fontWeight: 600 }}
            />
          ))}
        </Stack>

        {/* Dropdown selector for compact / mobile */}
        <TextField
          select
          label="Danh mục"
          size="small"
          value={selectValue}
          onChange={(e) => {
            const val = e.target.value;
            handleCategoryChange(val === "all" ? null : Number(val));
          }}
          sx={{ minWidth: 200 }}
          slotProps={{
            select: {
              IconComponent: FilterListRoundedIcon,
            },
          }}
        >
          <MenuItem value="all">Tất cả danh mục</MenuItem>
          {categories.map((cat) => (
            <MenuItem key={cat.id} value={String(cat.id)}>
              {cat.name}
            </MenuItem>
          ))}
        </TextField>
      </Stack>

      <Grid container spacing={3}>
        {/* Left Filter Sidebar */}
        <Grid size={{ xs: 12, md: 3.5, lg: 3 }}>
          <Paper
            elevation={0}
            sx={{
              p: 2.5,
              borderRadius: 2.5,
              border: "1px solid #e2e8f0",
              bgcolor: "#ffffff",
            }}
          >
            {/* Sidebar header */}
            <Stack
              direction="row"
              justifyContent="space-between"
              alignItems="center"
              mb={2}
            >
              <Stack direction="row" spacing={1} alignItems="center">
                <TuneRoundedIcon color="primary" fontSize="small" />
                <Typography variant="subtitle1" fontWeight={700}>
                  Bộ lọc
                </Typography>
              </Stack>
              {hasActiveFilters && (
                <Button
                  size="small"
                  onClick={handleClearAllFilters}
                  data-testid="clear-all-filters-btn"
                  sx={{
                    textTransform: "none",
                    color: "error.main",
                    fontSize: "0.8125rem",
                    p: 0,
                    minWidth: "auto",
                  }}
                >
                  Xóa tất cả
                </Button>
              )}
            </Stack>

            {/* Brands Section */}
            <Box mb={2.5}>
              <Typography
                variant="subtitle2"
                fontWeight={700}
                color="text.primary"
                mb={1.5}
              >
                Thương hiệu
              </Typography>
              {brands.length === 0 ? (
                <Typography variant="body2" color="text.secondary">
                  Đang tải thương hiệu...
                </Typography>
              ) : (
                <Stack spacing={0.5}>
                  {brands.map((brand) => (
                    <FormControlLabel
                      key={brand.id}
                      control={
                        <Checkbox
                          size="small"
                          checked={selectedBrandIds.includes(brand.id)}
                          onChange={() => handleToggleBrand(brand.id)}
                          inputProps={
                            {
                              "data-testid": `brand-checkbox-${brand.id}`,
                            } as React.InputHTMLAttributes<HTMLInputElement>
                          }
                        />
                      }
                      label={
                        <Typography variant="body2">{brand.name}</Typography>
                      }
                    />
                  ))}
                </Stack>
              )}
            </Box>

            <Divider sx={{ mb: 2.5 }} />

            {/* Price Range Section */}
            <Box mb={2}>
              <Typography
                variant="subtitle2"
                fontWeight={700}
                color="text.primary"
                mb={1.5}
              >
                Khoảng giá
              </Typography>

              {/* Price presets */}
              <Stack spacing={1} mb={2}>
                {PRICE_PRESETS.map((preset, idx) => {
                  const isSelected =
                    priceMin === preset.min && priceMax === preset.max;
                  return (
                    <Button
                      key={preset.label}
                      size="small"
                      variant={isSelected ? "contained" : "outlined"}
                      color={isSelected ? "primary" : "inherit"}
                      onClick={() =>
                        handleApplyPresetPrice(preset.min, preset.max)
                      }
                      data-testid={`preset-price-${idx}`}
                      sx={{
                        justifyContent: "flex-start",
                        textTransform: "none",
                        fontSize: "0.8125rem",
                        borderRadius: 2,
                        py: 0.75,
                      }}
                    >
                      {preset.label}
                    </Button>
                  );
                })}
              </Stack>

              {/* Custom Min / Max inputs */}
              <Typography
                variant="caption"
                color="text.secondary"
                display="block"
                mb={1}
                fontWeight={600}
              >
                Hoặc nhập khoảng giá (VNĐ):
              </Typography>
              <Stack direction="row" spacing={1} alignItems="center" mb={1}>
                <TextField
                  size="small"
                  placeholder="Từ"
                  value={customMin}
                  onChange={(e) => setCustomMin(e.target.value)}
                  type="number"
                  inputProps={{
                    "data-testid": "custom-price-min",
                    min: 0,
                  }}
                  sx={{ flex: 1 }}
                />
                <Typography variant="body2" color="text.secondary">
                  -
                </Typography>
                <TextField
                  size="small"
                  placeholder="Đến"
                  value={customMax}
                  onChange={(e) => setCustomMax(e.target.value)}
                  type="number"
                  inputProps={{
                    "data-testid": "custom-price-max",
                    min: 0,
                  }}
                  sx={{ flex: 1 }}
                />
              </Stack>

              {priceError && (
                <Typography
                  variant="caption"
                  color="error"
                  display="block"
                  mb={1}
                >
                  {priceError}
                </Typography>
              )}

              <Button
                fullWidth
                size="small"
                variant="outlined"
                onClick={handleApplyCustomPrice}
                data-testid="apply-price-filter-btn"
                sx={{ textTransform: "none", borderRadius: 2 }}
              >
                Áp dụng
              </Button>
            </Box>
          </Paper>
        </Grid>

        {/* Right Content Area: Active Chips, Count, Products Grid, Pagination */}
        <Grid size={{ xs: 12, md: 8.5, lg: 9 }}>
          {/* Results header, count bar & sort dropdown */}
          <Stack
            direction={{ xs: "column", sm: "row" }}
            justifyContent="space-between"
            alignItems={{ xs: "flex-start", sm: "center" }}
            spacing={2}
            py={1.5}
            px={2}
            mb={2}
            bgcolor="#f8fafc"
            borderRadius={2}
            border="1px solid #e2e8f0"
          >
            <Box>
              <Typography
                variant="subtitle1"
                fontWeight={700}
                color="text.primary"
              >
                {searchQuery
                  ? `Từ khóa: "${searchQuery}"`
                  : currentCategory
                    ? currentCategory.name
                    : "Tất cả sản phẩm"}
              </Typography>
              <Typography
                variant="body2"
                color="text.secondary"
                data-testid="products-count"
              >
                {loading ? "Đang tải..." : `Tìm thấy ${totalElements} sản phẩm`}
              </Typography>
            </Box>

            {/* Dropdown chọn tiêu chí sắp xếp */}
            <FormControl
              size="small"
              sx={{ minWidth: 170, width: { xs: "100%", sm: "auto" } }}
            >
              <InputLabel id="sort-select-label">Sắp xếp theo</InputLabel>
              <Select
                labelId="sort-select-label"
                id="sort-select"
                value={sortValue}
                label="Sắp xếp theo"
                onChange={(e) => handleSortChange(e.target.value)}
                data-testid="sort-select"
                sx={{
                  bgcolor: "#ffffff",
                  borderRadius: 1.5,
                  fontSize: "0.875rem",
                }}
              >
                <MenuItem
                  value="createdAt_desc"
                  data-testid="sort-option-newest"
                >
                  Mới nhất
                </MenuItem>
                <MenuItem value="price_asc" data-testid="sort-option-price-asc">
                  Giá tăng dần
                </MenuItem>
                <MenuItem
                  value="price_desc"
                  data-testid="sort-option-price-desc"
                >
                  Giá giảm dần
                </MenuItem>
                <MenuItem
                  value="salesCount_desc"
                  data-testid="sort-option-sales-desc"
                >
                  Bán chạy nhất
                </MenuItem>
              </Select>
            </FormControl>
          </Stack>

          {/* Active Filter Badges */}
          {hasActiveFilters && (
            <Stack
              direction="row"
              flexWrap="wrap"
              gap={1}
              alignItems="center"
              mb={2.5}
            >
              <Typography
                variant="body2"
                color="text.secondary"
                fontWeight={600}
              >
                Đang lọc:
              </Typography>
              {currentCategory && (
                <Chip
                  size="small"
                  label={`Danh mục: ${currentCategory.name}`}
                  onDelete={() => handleCategoryChange(null)}
                  color="primary"
                  variant="outlined"
                />
              )}
              {selectedBrandIds.map((bId) => {
                const b = brands.find((br) => br.id === bId);
                return (
                  <Chip
                    key={bId}
                    size="small"
                    label={`Hãng: ${b ? b.name : bId}`}
                    onDelete={() => handleToggleBrand(bId)}
                    color="primary"
                    variant="outlined"
                  />
                );
              })}
              {(priceMin != null || priceMax != null) && (
                <Chip
                  size="small"
                  label={
                    priceMin != null && priceMax != null
                      ? `Giá: ${formatPrice(priceMin)} - ${formatPrice(priceMax)}`
                      : priceMin != null
                        ? `Giá từ: ${formatPrice(priceMin)}`
                        : `Giá đến: ${formatPrice(priceMax!)}`
                  }
                  onDelete={() =>
                    updateFilterParams({ priceMin: null, priceMax: null })
                  }
                  color="primary"
                  variant="outlined"
                />
              )}
              <Button
                size="small"
                onClick={handleClearAllFilters}
                sx={{
                  textTransform: "none",
                  color: "error.main",
                  fontSize: "0.8125rem",
                }}
              >
                Xóa lọc
              </Button>
            </Stack>
          )}

          {/* Error state */}
          {error && (
            <Alert
              severity="error"
              action={
                <Button
                  color="inherit"
                  size="small"
                  startIcon={<RefreshRoundedIcon />}
                  onClick={loadProducts}
                >
                  Thử lại
                </Button>
              }
              sx={{ mb: 3, borderRadius: 2 }}
            >
              {error}
            </Alert>
          )}

          {/* Loading state: Skeleton Grid */}
          {loading ? (
            <Grid container spacing={2}>
              {Array.from({ length: 6 }).map((_, idx) => (
                <Grid key={idx} size={{ xs: 12, sm: 6, md: 4 }}>
                  <Skeleton
                    variant="rounded"
                    height={340}
                    sx={{ borderRadius: 2.5 }}
                  />
                </Grid>
              ))}
            </Grid>
          ) : products.length > 0 ? (
            <>
              {/* Products Grid */}
              <Grid container spacing={2}>
                {products.map((product) => (
                  <Grid key={product.id} size={{ xs: 12, sm: 6, md: 4 }}>
                    <ProductCard product={product} />
                  </Grid>
                ))}
              </Grid>

              {/* Pagination Control */}
              {totalPages > 1 && (
                <Box
                  display="flex"
                  justifyContent="center"
                  mt={4}
                  mb={2}
                  data-testid="pagination-container"
                >
                  <Pagination
                    count={totalPages}
                    page={currentPage}
                    onChange={handlePageChange}
                    color="primary"
                    shape="rounded"
                    showFirstButton
                    showLastButton
                    data-testid="pagination-control"
                  />
                </Box>
              )}
            </>
          ) : searchQuery ? (
            /* Empty Search State with Suggestions */
            <Box
              py={8}
              px={3}
              textAlign="center"
              bgcolor="#f9fafb"
              borderRadius={3}
              border="1px dashed #cbd5e1"
            >
              <SearchOffRoundedIcon
                sx={{ fontSize: 64, color: "text.disabled", mb: 1.5 }}
              />
              <Typography
                variant="h6"
                fontWeight={600}
                color="text.primary"
                mb={1}
                data-testid="empty-search-heading"
              >
                Không tìm thấy sản phẩm nào
              </Typography>
              <Typography variant="body2" color="text.secondary" mb={3}>
                Không có kết quả nào khớp với từ khóa "{searchQuery}".
              </Typography>

              <Box
                sx={{
                  maxWidth: 460,
                  mx: "auto",
                  mb: 3.5,
                  p: 2.5,
                  bgcolor: "#ffffff",
                  borderRadius: 2,
                  border: "1px solid #e2e8f0",
                  textAlign: "left",
                }}
              >
                <Typography
                  variant="subtitle2"
                  fontWeight={700}
                  color="text.primary"
                  mb={1}
                >
                  Gợi ý mở rộng tìm kiếm:
                </Typography>
                <Typography
                  component="ul"
                  variant="body2"
                  color="text.secondary"
                  sx={{ pl: 2.5, m: 0, "& li": { mb: 0.5 } }}
                >
                  <li>Kiểm tra lại chính tả của từ khóa đã nhập.</li>
                  <li>
                    Thử sử dụng từ khóa ngắn gọn hoặc tổng quát hơn (ví dụ:
                    iPhone, Samsung, tai nghe, sạc...).
                  </li>
                  <li>
                    Thử duyệt theo danh mục sản phẩm ở thanh lọc phía trên.
                  </li>
                </Typography>
              </Box>

              <Button
                variant="contained"
                onClick={handleClearSearch}
                data-testid="clear-search-btn"
              >
                Xem tất cả sản phẩm
              </Button>
            </Box>
          ) : hasBrandOrPriceFilters ? (
            /* Empty Filter State */
            <Box
              py={8}
              px={3}
              textAlign="center"
              bgcolor="#f9fafb"
              borderRadius={3}
              border="1px dashed #cbd5e1"
            >
              <TuneRoundedIcon
                sx={{ fontSize: 64, color: "text.disabled", mb: 1.5 }}
              />
              <Typography
                variant="h6"
                fontWeight={600}
                color="text.primary"
                mb={1}
                data-testid="empty-filter-heading"
              >
                Không có sản phẩm phù hợp
              </Typography>
              <Typography variant="body2" color="text.secondary" mb={3}>
                Không tìm thấy sản phẩm nào khớp với bộ lọc bạn đã chọn.
              </Typography>
              <Button
                variant="contained"
                onClick={handleClearAllFilters}
                data-testid="clear-filters-btn"
              >
                Xóa tất cả bộ lọc
              </Button>
            </Box>
          ) : (
            /* Empty Category / Catalog State */
            <Box
              py={8}
              textAlign="center"
              bgcolor="#f9fafb"
              borderRadius={3}
              border="1px dashed #cbd5e1"
            >
              <SmartphoneRoundedIcon
                sx={{ fontSize: 64, color: "text.disabled", mb: 1 }}
              />
              <Typography
                variant="h6"
                fontWeight={600}
                color="text.primary"
                mb={1}
              >
                Chưa có sản phẩm nào
              </Typography>
              <Typography variant="body2" color="text.secondary" mb={3}>
                {currentCategory
                  ? `Không tìm thấy sản phẩm nào trong danh mục "${currentCategory.name}".`
                  : "Hiện chưa có sản phẩm nào đang bán."}
              </Typography>
              {selectedCategoryId !== null && (
                <Button
                  variant="outlined"
                  onClick={() => handleCategoryChange(null)}
                >
                  Xem tất cả sản phẩm
                </Button>
              )}
            </Box>
          )}
        </Grid>
      </Grid>
    </>
  );
}
