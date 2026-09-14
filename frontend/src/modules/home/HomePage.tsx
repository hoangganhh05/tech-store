import { useCallback, useEffect, useState } from 'react'
import {
  Alert,
  Box,
  Button,
  Card,
  CardActionArea,
  Container,
  Grid,
  Paper,
  Skeleton,
  Stack,
  Typography,
} from '@mui/material'
import ArrowForwardRoundedIcon from '@mui/icons-material/ArrowForwardRounded'
import AutoAwesomeRoundedIcon from '@mui/icons-material/AutoAwesomeRounded'
import CategoryRoundedIcon from '@mui/icons-material/CategoryRounded'
import LocalOfferRoundedIcon from '@mui/icons-material/LocalOfferRounded'
import NewReleasesRoundedIcon from '@mui/icons-material/NewReleasesRounded'
import PhoneIphoneRoundedIcon from '@mui/icons-material/PhoneIphoneRounded'
import RefreshRoundedIcon from '@mui/icons-material/RefreshRounded'
import SupportAgentRoundedIcon from '@mui/icons-material/SupportAgentRounded'
import { Link } from 'react-router-dom'
import { ProductCard } from '../../components/common/ProductCard'
import { env } from '../../configs/env'
import { ROUTES } from '../../constants/routes'
import { getStorefrontHomeData, type StorefrontHomeData, type StorefrontProduct } from '../../services/storefrontService'

type ProductSectionProps = {
  title: string
  description: string
  icon: React.ReactNode
  products: StorefrontProduct[]
  loading: boolean
  emptyText: string
  id?: string
  accent?: 'default' | 'sale'
}

function ProductSection({ title, description, icon, products, loading, emptyText, id, accent = 'default' }: ProductSectionProps) {
  const sale = accent === 'sale'
  return (
    <Box component="section" id={id} sx={sale ? { p: { xs: 2, sm: 3, md: 4 }, borderRadius: { xs: 3, md: 4 }, bgcolor: '#fff8e8', border: '1px solid #f6df9a' } : undefined}>
      <Stack direction={{ xs: 'column', sm: 'row' }} justifyContent="space-between" alignItems={{ xs: 'flex-start', sm: 'center' }} gap={1.5} mb={2.5}>
        <Stack direction="row" alignItems="center" spacing={1.25}>
          <Box sx={{ width: 40, height: 40, display: 'grid', placeItems: 'center', borderRadius: '12px', bgcolor: sale ? '#ffeebc' : 'primary.light', color: sale ? 'warning.dark' : 'primary.main' }}>{icon}</Box>
          <Box>
            <Typography component="h2" variant="h2">{title}</Typography>
            <Typography variant="body2" color="text.secondary">{description}</Typography>
          </Box>
        </Stack>
        <Button component={Link} to={ROUTES.products} size="small" endIcon={<ArrowForwardRoundedIcon />}>Xem tất cả</Button>
      </Stack>
      {loading ? <Grid container spacing={{ xs: 1.5, sm: 2 }}>{Array.from({ length: 4 }).map((_, index) => <Grid key={index} size={{ xs: 6, sm: 4, md: 3 }}><Skeleton variant="rounded" height={330} sx={{ borderRadius: 3 }} /></Grid>)}</Grid>
        : products.length > 0 ? <Grid container spacing={{ xs: 1.5, sm: 2 }}>{products.slice(0, 8).map((product) => <Grid key={product.id} size={{ xs: 6, sm: 4, md: 3 }}><ProductCard product={product} /></Grid>)}</Grid>
          : <Paper variant="outlined" sx={{ py: 5, textAlign: 'center', borderStyle: 'dashed', bgcolor: 'rgba(255,255,255,.68)' }}><Typography variant="body2" color="text.secondary">{emptyText}</Typography></Paper>}
    </Box>
  )
}

export function HomePage() {
  const [data, setData] = useState<StorefrontHomeData | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const loadData = useCallback(async () => {
    try {
      setLoading(true)
      setError(null)
      setData(await getStorefrontHomeData(8))
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Không thể tải dữ liệu trang chủ')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    void loadData()
  }, [loadData])

  const featuredProducts = data?.featuredProducts ?? []
  const newArrivals = data?.newArrivals ?? []
  const onSaleProducts = data?.onSaleProducts ?? []
  const featuredCategories = data?.featuredCategories ?? []
  const heroProduct = featuredProducts.find((product) => product.thumbnailUrl)
    ?? newArrivals.find((product) => product.thumbnailUrl)
    ?? null

  return (
    <Box pb={{ xs: 5, md: 7 }}>
      <Box component="section" sx={{ position: 'relative', overflow: 'hidden', color: 'white', bgcolor: 'secondary.main', background: 'linear-gradient(125deg, #062e63 0%, #0756a8 62%, #0d73d1 100%)' }}>
        <Box sx={{ position: 'absolute', inset: 0, opacity: .2, backgroundImage: 'radial-gradient(circle at 83% 12%, #f2b705 0, transparent 22%), radial-gradient(circle at 10% 90%, white 0, transparent 26%)' }} />
        <Container maxWidth="xl" sx={{ position: 'relative' }}>
          <Grid container alignItems="center" minHeight={{ xs: 380, md: 450 }}>
            <Grid size={{ xs: 12, md: 7 }}>
              <Stack spacing={2.5} py={{ xs: 6, sm: 7, md: 8 }} maxWidth={640}>
                <Typography variant="caption" sx={{ width: 'fit-content', px: 1.25, py: .6, borderRadius: '999px', fontWeight: 700, color: 'warning.main', bgcolor: 'rgba(255,255,255,.1)', letterSpacing: '.035em' }}>ĐĂNG TÙNG MOBILE</Typography>
                <Box>
                  <Typography component="h1" sx={{ fontSize: { xs: '2.15rem', md: '3.35rem' }, lineHeight: 1.16, fontWeight: 800, letterSpacing: '-.035em', maxWidth: 620 }}>Điện thoại và phụ kiện<br /><Box component="span" color="warning.main">phù hợp nhu cầu của bạn</Box></Typography>
                  <Typography mt={2} maxWidth={540} sx={{ color: '#d7e7fa', fontSize: { xs: 15, md: 17 }, lineHeight: 1.7 }}>Khám phá sản phẩm với thông tin rõ ràng, lựa chọn phiên bản phù hợp và quản lý đơn hàng thuận tiện tại {env.brand.name}.</Typography>
                </Box>
                <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1.5} alignItems={{ xs: 'stretch', sm: 'center' }}>
                  <Button component={Link} to={ROUTES.products} variant="contained" size="large" endIcon={<ArrowForwardRoundedIcon />} sx={{ bgcolor: 'warning.main', color: 'warning.contrastText', '&:hover': { bgcolor: 'warning.dark' } }}>Khám phá sản phẩm</Button>
                  {onSaleProducts.length ? <Button component="a" href="#on-sale-section" size="large" variant="outlined" sx={{ color: 'white', borderColor: 'rgba(255,255,255,.42)', '&:hover': { borderColor: 'white', bgcolor: 'rgba(255,255,255,.1)' } }}>Xem ưu đãi</Button> : null}
                </Stack>
              </Stack>
            </Grid>
            <Grid size={{ xs: 12, md: 5 }} sx={{ display: { xs: 'none', md: 'block' }, alignSelf: 'stretch' }}>
              <Box height="100%" minHeight={450} position="relative" sx={{ display: 'grid', placeItems: 'center' }}>
                <Box sx={{ width: 310, height: 310, position: 'absolute', borderRadius: '50%', bgcolor: 'rgba(255,255,255,.08)', border: '1px solid rgba(255,255,255,.12)' }} />
                {heroProduct?.thumbnailUrl ? <Box component="img" src={heroProduct.thumbnailUrl} alt={heroProduct.name} sx={{ zIndex: 1, width: 310, height: 355, objectFit: 'contain', filter: 'drop-shadow(0 24px 24px rgba(0,0,0,.25))' }} /> : <PhoneIphoneRoundedIcon sx={{ zIndex: 1, fontSize: 180, color: 'warning.main', filter: 'drop-shadow(0 24px 24px rgba(0,0,0,.25))' }} />}
                {heroProduct && <Paper elevation={0} sx={{ position: 'absolute', left: 0, bottom: 48, zIndex: 2, py: 1.25, px: 1.5, borderRadius: 3, minWidth: 190, boxShadow: '0 16px 40px rgba(0,0,0,.2)' }}><Typography variant="caption" color="text.secondary" noWrap>Gợi ý nổi bật</Typography><Typography variant="body2" fontWeight={700} noWrap maxWidth={180}>{heroProduct.name}</Typography></Paper>}
              </Box>
            </Grid>
          </Grid>
        </Container>
      </Box>

      <Container maxWidth="xl">
        <Stack spacing={{ xs: 5, md: 7 }} pt={{ xs: 3, md: 4 }}>
          <Grid container spacing={{ xs: 1, sm: 1.5 }}>
            {[{ icon: <CategoryRoundedIcon />, title: 'Dễ tìm sản phẩm', text: 'Lọc theo danh mục, thương hiệu và giá' }, { icon: <LocalOfferRoundedIcon />, title: 'Giá và ưu đãi rõ ràng', text: 'Xem giá theo từng phiên bản' }, { icon: <SupportAgentRoundedIcon />, title: 'Cần hỗ trợ?', text: `Liên hệ ${env.brand.contact.phone}` }].map((item) => <Grid key={item.title} size={{ xs: 12, sm: 4 }}><Paper variant="outlined" sx={{ height: '100%', display: 'flex', alignItems: 'center', gap: 1.25, p: { xs: 1.5, sm: 2 }, bgcolor: 'white' }}><Box sx={{ width: 40, height: 40, flexShrink: 0, display: 'grid', placeItems: 'center', color: 'primary.main', borderRadius: '12px', bgcolor: 'primary.light' }}>{item.icon}</Box><Box><Typography variant="body2" fontWeight={700}>{item.title}</Typography><Typography variant="caption" color="text.secondary">{item.text}</Typography></Box></Paper></Grid>)}
          </Grid>

          {error && <Alert severity="error" action={<Button color="inherit" size="small" startIcon={<RefreshRoundedIcon />} onClick={() => void loadData()}>Thử lại</Button>}>{error}</Alert>}

          <Box component="section">
            <Stack direction={{ xs: 'column', sm: 'row' }} justifyContent="space-between" alignItems={{ xs: 'flex-start', sm: 'center' }} gap={1.5} mb={2.5}><Box><Typography component="h2" variant="h2">Danh mục nổi bật</Typography><Typography variant="body2" color="text.secondary">Chọn nhanh dòng sản phẩm bạn quan tâm</Typography></Box><Button component={Link} to={ROUTES.products} size="small" endIcon={<ArrowForwardRoundedIcon />}>Tất cả sản phẩm</Button></Stack>
            {loading ? <Grid container spacing={1.5}>{Array.from({ length: 6 }).map((_, index) => <Grid key={index} size={{ xs: 4, sm: 3, md: 2 }}><Skeleton variant="rounded" height={112} sx={{ borderRadius: 3 }} /></Grid>)}</Grid>
              : featuredCategories.length ? <Grid container spacing={1.5}>{featuredCategories.slice(0, 8).map((category) => <Grid key={category.id} size={{ xs: 4, sm: 3, md: 1.5 }}><Card sx={{ height: '100%' }}><CardActionArea component={Link} to={`${ROUTES.products}?categoryId=${category.id}`} sx={{ height: '100%', p: 1.5, textAlign: 'center' }}><Box sx={{ height: 48, display: 'grid', placeItems: 'center', mb: .75, color: 'primary.main' }}>{category.imageUrl ? <Box component="img" src={category.imageUrl} alt="" sx={{ width: 42, height: 42, objectFit: 'contain' }} /> : <CategoryRoundedIcon sx={{ fontSize: 31 }} />}</Box><Typography variant="caption" fontWeight={600} color="text.primary" lineHeight={1.35}>{category.name}</Typography></CardActionArea></Card></Grid>)}</Grid>
                : <Paper variant="outlined" sx={{ py: 4, px: 2, textAlign: 'center', borderStyle: 'dashed' }}><Typography variant="body2" color="text.secondary">Danh mục đang được cập nhật</Typography></Paper>}
          </Box>

          {onSaleProducts.length ? <ProductSection id="on-sale-section" accent="sale" title="Săn Sale Giá Sốc" description="Ưu đãi đang áp dụng cho các sản phẩm được chọn" icon={<LocalOfferRoundedIcon />} products={onSaleProducts} loading={loading} emptyText="" /> : null}
          <ProductSection title="Sản phẩm nổi bật" description="Được khách hàng quan tâm và lựa chọn hàng đầu" icon={<AutoAwesomeRoundedIcon />} products={featuredProducts} loading={loading} emptyText="Chưa có sản phẩm nổi bật nào." />
          <ProductSection title="Sản phẩm mới về" description="Cập nhật sản phẩm mới từ cửa hàng" icon={<NewReleasesRoundedIcon />} products={newArrivals} loading={loading} emptyText="Chưa có sản phẩm mới nào." />
        </Stack>
      </Container>
    </Box>
  )
}
