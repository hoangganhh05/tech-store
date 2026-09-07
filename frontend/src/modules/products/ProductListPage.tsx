import { useEffect, useState, useCallback, useMemo } from 'react'
import {
  Alert,
  Box,
  Button,
  Chip,
  Grid,
  MenuItem,
  Skeleton,
  Stack,
  TextField,
  Typography,
} from '@mui/material'
import RefreshRoundedIcon from '@mui/icons-material/RefreshRounded'
import FilterListRoundedIcon from '@mui/icons-material/FilterListRounded'
import SmartphoneRoundedIcon from '@mui/icons-material/SmartphoneRounded'
import { useSearchParams } from 'react-router-dom'
import { PageIntro } from '../../components/common/PageIntro'
import { ProductCard } from '../../components/common/ProductCard'
import {
  getStorefrontProducts,
  getStorefrontCategories,
  type StorefrontProduct,
} from '../../services/storefrontService'
import type { Category } from '../../services/categoryService'

export function ProductListPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const categoryIdParam = searchParams.get('categoryId')
  const selectedCategoryId = useMemo(() => {
    if (!categoryIdParam) return null
    const parsed = Number(categoryIdParam)
    return isNaN(parsed) ? null : parsed
  }, [categoryIdParam])

  const [categories, setCategories] = useState<Category[]>([])
  const [products, setProducts] = useState<StorefrontProduct[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  // Load categories list for filter
  useEffect(() => {
    let isMounted = true
    getStorefrontCategories()
      .then((cats) => {
        if (isMounted) {
          setCategories(cats)
        }
      })
      .catch(() => {
        // Fallback or ignore non-critical category load
      })
    return () => {
      isMounted = false
    }
  }, [])

  // Load products based on categoryId
  const loadProducts = useCallback(async () => {
    try {
      setLoading(true)
      setError(null)
      const data = await getStorefrontProducts(selectedCategoryId)
      setProducts(data)
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Không thể tải danh sách sản phẩm'
      setError(msg)
    } finally {
      setLoading(false)
    }
  }, [selectedCategoryId])

  useEffect(() => {
    loadProducts()
  }, [loadProducts])

  const handleCategoryChange = (newCategoryId: number | null) => {
    if (newCategoryId) {
      setSearchParams({ categoryId: String(newCategoryId) })
    } else {
      setSearchParams({})
    }
  }

  // Determine current active category name
  const currentCategory = useMemo(() => {
    if (!selectedCategoryId) return null
    return categories.find((c) => c.id === selectedCategoryId) || null
  }, [categories, selectedCategoryId])

  const selectValue = useMemo(() => {
    if (selectedCategoryId === null) return 'all'
    return categories.some((c) => c.id === selectedCategoryId)
      ? String(selectedCategoryId)
      : 'all'
  }, [categories, selectedCategoryId])

  return (
    <>
      <PageIntro
        title="Sản phẩm"
        description="Tìm điện thoại và phụ kiện phù hợp với nhu cầu của bạn."
      />

      {/* Category filter bar */}
      <Stack
        direction={{ xs: 'column', sm: 'row' }}
        justifyContent="space-between"
        alignItems={{ xs: 'stretch', sm: 'center' }}
        spacing={2}
        mb={3}
      >
        {/* Category quick chips */}
        <Stack
          direction="row"
          spacing={1}
          sx={{
            overflowX: 'auto',
            pb: { xs: 1, sm: 0 },
            '::-webkit-scrollbar': { height: 4 },
          }}
        >
          <Chip
            label="Tất cả"
            clickable
            color={selectedCategoryId === null ? 'primary' : 'default'}
            variant={selectedCategoryId === null ? 'filled' : 'outlined'}
            onClick={() => handleCategoryChange(null)}
            sx={{ fontWeight: 600 }}
          />
          {categories.map((cat) => (
            <Chip
              key={cat.id}
              label={cat.name}
              clickable
              color={selectedCategoryId === cat.id ? 'primary' : 'default'}
              variant={selectedCategoryId === cat.id ? 'filled' : 'outlined'}
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
            const val = e.target.value
            handleCategoryChange(val === 'all' ? null : Number(val))
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

      {/* Results header & count bar */}
      <Stack
        direction="row"
        justifyContent="space-between"
        alignItems="center"
        py={1.5}
        px={2}
        mb={3}
        bgcolor="#f8fafc"
        borderRadius={2}
        border="1px solid #e2e8f0"
      >
        <Typography variant="subtitle1" fontWeight={700} color="text.primary">
          {currentCategory ? currentCategory.name : 'Tất cả sản phẩm'}
        </Typography>
        <Typography variant="body2" color="text.secondary" data-testid="products-count">
          {loading ? 'Đang tải...' : `Tìm thấy ${products.length} sản phẩm`}
        </Typography>
      </Stack>

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
          {Array.from({ length: 8 }).map((_, idx) => (
            <Grid key={idx} size={{ xs: 12, sm: 6, md: 3 }}>
              <Skeleton variant="rounded" height={340} sx={{ borderRadius: 2.5 }} />
            </Grid>
          ))}
        </Grid>
      ) : products.length > 0 ? (
        /* Products Grid */
        <Grid container spacing={2}>
          {products.map((product) => (
            <Grid key={product.id} size={{ xs: 12, sm: 6, md: 3 }}>
              <ProductCard product={product} />
            </Grid>
          ))}
        </Grid>
      ) : (
        /* Empty State */
        <Box
          py={8}
          textAlign="center"
          bgcolor="#f9fafb"
          borderRadius={3}
          border="1px dashed #cbd5e1"
        >
          <SmartphoneRoundedIcon sx={{ fontSize: 64, color: 'text.disabled', mb: 1 }} />
          <Typography variant="h6" fontWeight={600} color="text.primary" mb={1}>
            Chưa có sản phẩm nào
          </Typography>
          <Typography variant="body2" color="text.secondary" mb={3}>
            {currentCategory
              ? `Không tìm thấy sản phẩm nào trong danh mục "${currentCategory.name}".`
              : 'Hiện chưa có sản phẩm nào đang bán.'}
          </Typography>
          {selectedCategoryId !== null && (
            <Button variant="outlined" onClick={() => handleCategoryChange(null)}>
              Xem tất cả sản phẩm
            </Button>
          )}
        </Box>
      )}
    </>
  )
}
