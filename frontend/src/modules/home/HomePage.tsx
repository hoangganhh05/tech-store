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
import BatteryChargingFullRoundedIcon from '@mui/icons-material/BatteryChargingFullRounded'
import CableRoundedIcon from '@mui/icons-material/CableRounded'
import CategoryRoundedIcon from '@mui/icons-material/CategoryRounded'
import HeadphonesRoundedIcon from '@mui/icons-material/HeadphonesRounded'
import LocalOfferRoundedIcon from '@mui/icons-material/LocalOfferRounded'
import NewReleasesRoundedIcon from '@mui/icons-material/NewReleasesRounded'
import PhoneIphoneRoundedIcon from '@mui/icons-material/PhoneIphoneRounded'
import RefreshRoundedIcon from '@mui/icons-material/RefreshRounded'
import ReplayRoundedIcon from '@mui/icons-material/ReplayRounded'
import ShieldOutlinedIcon from '@mui/icons-material/ShieldOutlined'
import ShoppingBagOutlinedIcon from '@mui/icons-material/ShoppingBagOutlined'
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

type QuickCategory = {
  label: string
  icon: React.ReactNode
  categoryId?: number
  imageUrl?: string | null
}

const fallbackCategories: QuickCategory[] = [
  { label: 'iPhone', icon: <PhoneIphoneRoundedIcon /> },
  { label: 'Samsung', icon: <PhoneIphoneRoundedIcon /> },
  { label: 'Xiaomi', icon: <PhoneIphoneRoundedIcon /> },
  { label: 'Điện thoại khác', icon: <PhoneIphoneRoundedIcon /> },
  { label: 'Tai nghe', icon: <HeadphonesRoundedIcon /> },
  { label: 'Củ sạc & Cáp', icon: <CableRoundedIcon /> },
  { label: 'Ốp lưng & Bảo vệ', icon: <CategoryRoundedIcon /> },
  { label: 'Pin dự phòng', icon: <BatteryChargingFullRoundedIcon /> },
]

function formatPrice(value: number) {
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 }).format(value)
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

function PhoneMock() {
  return (
    <Box aria-label="Minh hoạ điện thoại" sx={{ position: 'relative', width: 184, height: 302, borderRadius: '34px', border: '9px solid #f2b705', bgcolor: '#0b2142', boxShadow: '0 24px 40px rgba(0,0,0,.3)', transform: 'rotate(8deg)', display: 'grid', placeItems: 'center' }}>
      <Box sx={{ position: 'absolute', top: 10, width: 68, height: 16, borderRadius: 8, bgcolor: '#f2b705' }} />
      <PhoneIphoneRoundedIcon sx={{ color: '#f2b705', fontSize: 90, opacity: .8 }} />
      <Box sx={{ position: 'absolute', top: 26, left: 23, display: 'grid', gridTemplateColumns: 'repeat(2, 9px)', gap: .5 }}>
        {[0, 1, 2].map((item) => <Box key={item} sx={{ width: 9, height: 9, borderRadius: '50%', bgcolor: '#b7c9e0', gridColumn: item === 2 ? '1 / span 2' : undefined, justifySelf: item === 2 ? 'center' : undefined }} />)}
      </Box>
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
  const heroProduct = featuredProducts.find((product) => product.thumbnailUrl) ?? newArrivals.find((product) => product.thumbnailUrl) ?? null
  const categories: QuickCategory[] = featuredCategories.length
    ? featuredCategories.slice(0, 8).map((category) => ({ label: category.name, categoryId: category.id, imageUrl: category.imageUrl, icon: <CategoryRoundedIcon /> }))
    : fallbackCategories

  return (
    <Box pb={{ xs: 5, md: 7 }}>
      <Box component="section" sx={{ position: 'relative', overflow: 'hidden', color: 'white', bgcolor: 'secondary.main', background: 'linear-gradient(118deg, #062e63 0%, #0756a8 58%, #1479d4 100%)' }}>
        <Box sx={{ position: 'absolute', inset: 0, opacity: .2, backgroundImage: 'radial-gradient(circle at 84% 10%, #f2b705 0, transparent 23%), radial-gradient(circle at 5% 90%, white 0, transparent 28%)' }} />
        <Container maxWidth="xl" sx={{ position: 'relative' }}>
          <Grid container alignItems="center" minHeight={{ xs: 420, md: 480 }}>
            <Grid size={{ xs: 12, md: 7 }}>
              <Stack spacing={2.4} py={{ xs: 6, sm: 7, md: 8 }} maxWidth={650}>
                <Typography variant="caption" sx={{ width: 'fit-content', px: 1.35, py: .65, borderRadius: '999px', fontWeight: 800, color: 'warning.main', bgcolor: 'rgba(255,255,255,.11)', letterSpacing: '.02em' }}>
                  {heroProduct ? `${heroProduct.name} · Vừa về hàng` : 'ĐĂNG TÙNG MOBILE · CHÍNH HÃNG'}
                </Typography>
                <Box>
                  <Typography component="h1" sx={{ fontSize: { xs: '2.25rem', md: '3.5rem' }, lineHeight: 1.12, fontWeight: 800, letterSpacing: '-.04em', maxWidth: 640 }}>
                    Công nghệ đỉnh cao<br /><Box component="span" color="warning.main">Giá tốt nhất thị trường</Box>
                  </Typography>
                  <Typography mt={2} maxWidth={550} sx={{ color: '#d7e7fa', fontSize: { xs: 15, md: 17 }, lineHeight: 1.7 }}>
                    Điện thoại, tai nghe và phụ kiện chính hãng. Bảo hành đầy đủ, giao hàng toàn quốc.
                  </Typography>
                </Box>
                <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1.5} alignItems={{ xs: 'stretch', sm: 'center' }}>
                  <Button component={Link} to={ROUTES.products} variant="contained" size="large" endIcon={<ArrowForwardRoundedIcon />} sx={{ bgcolor: 'warning.main', color: 'warning.contrastText', '&:hover': { bgcolor: 'warning.dark' } }}>Khám phá sản phẩm</Button>
                  <Button component={Link} to={ROUTES.products} size="large" variant="contained" sx={{ bgcolor: 'white', color: 'secondary.main', '&:hover': { bgcolor: '#edf4fb' } }}>Xem sản phẩm nổi bật</Button>
                </Stack>
              </Stack>
            </Grid>
            <Grid size={{ xs: 12, md: 5 }} sx={{ display: { xs: 'none', md: 'block' }, alignSelf: 'stretch' }}>
              <Box height="100%" minHeight={480} position="relative" sx={{ display: 'grid', placeItems: 'center' }}>
                <Box sx={{ width: 350, height: 350, position: 'absolute', borderRadius: '50%', bgcolor: 'rgba(255,255,255,.08)', border: '1px solid rgba(255,255,255,.14)' }} />
                {heroProduct?.thumbnailUrl ? <Box component="img" src={heroProduct.thumbnailUrl} alt={heroProduct.name} sx={{ zIndex: 1, width: 310, height: 350, objectFit: 'contain', filter: 'drop-shadow(0 24px 24px rgba(0,0,0,.25))' }} /> : <PhoneMock />}
                {heroProduct && <Paper elevation={0} sx={{ position: 'absolute', left: 0, bottom: 66, zIndex: 2, py: 1.25, px: 1.75, borderRadius: 3, minWidth: 185, bgcolor: 'rgba(255,255,255,.78)', backdropFilter: 'blur(12px)', boxShadow: '0 16px 40px rgba(0,0,0,.2)' }}><Typography variant="caption" color="text.secondary" noWrap>Từ</Typography><Typography variant="h6" color="warning.dark" noWrap>{formatPrice(heroProduct.minPrice)}</Typography></Paper>}
              </Box>
            </Grid>
          </Grid>
        </Container>
      </Box>

      <Container maxWidth="xl">
        <Stack spacing={{ xs: 5, md: 7 }} pt={{ xs: 3, md: 4 }}>
          <Grid container spacing={{ xs: 1, sm: 1.5 }}>
            {[
              { icon: <ShieldOutlinedIcon />, title: 'Bảo hành rõ ràng', text: 'Thông tin minh bạch theo sản phẩm' },
              { icon: <ShoppingBagOutlinedIcon />, title: 'Giao hàng toàn quốc', text: 'Theo dõi đơn hàng thuận tiện' },
              { icon: <ReplayRoundedIcon />, title: 'Đổi trả dễ dàng', text: 'Hỗ trợ khi sản phẩm có vấn đề' },
              { icon: <SupportAgentRoundedIcon />, title: 'Tư vấn tận tâm', text: `Liên hệ ${env.brand.contact.phone}` },
            ].map((item) => <Grid key={item.title} size={{ xs: 12, sm: 6, md: 3 }}><Paper variant="outlined" sx={{ height: '100%', display: 'flex', alignItems: 'center', gap: 1.25, p: { xs: 1.5, sm: 2 }, bgcolor: 'white' }}><Box sx={{ width: 42, height: 42, flexShrink: 0, display: 'grid', placeItems: 'center', color: 'primary.main', borderRadius: '50%', bgcolor: 'primary.light' }}>{item.icon}</Box><Box><Typography variant="body2" fontWeight={700}>{item.title}</Typography><Typography variant="caption" color="text.secondary">{item.text}</Typography></Box></Paper></Grid>)}
          </Grid>

          {error && <Alert severity="error" action={<Button color="inherit" size="small" startIcon={<RefreshRoundedIcon />} onClick={() => void loadData()}>Thử lại</Button>}>{error}</Alert>}

          <Box component="section">
            <Stack direction={{ xs: 'column', sm: 'row' }} justifyContent="space-between" alignItems={{ xs: 'flex-start', sm: 'center' }} gap={1.5} mb={2.5}><Box><Typography component="h2" variant="h2">Danh mục nổi bật</Typography><Typography variant="body2" color="text.secondary">Chọn nhanh dòng sản phẩm bạn quan tâm</Typography></Box><Button component={Link} to={ROUTES.products} size="small" endIcon={<ArrowForwardRoundedIcon />}>Tất cả sản phẩm</Button></Stack>
            {loading ? <Grid container spacing={1.5}>{Array.from({ length: 8 }).map((_, index) => <Grid key={index} size={{ xs: 6, sm: 3, md: 1.5 }}><Skeleton variant="rounded" height={104} sx={{ borderRadius: 3 }} /></Grid>)}</Grid>
              : <Grid container spacing={1.5}>{categories.map((category) => <Grid key={category.label} size={{ xs: 6, sm: 3, md: 1.5 }}><Card sx={{ height: '100%' }}><CardActionArea component={Link} to={category.categoryId ? `${ROUTES.products}?categoryId=${category.categoryId}` : ROUTES.products} sx={{ height: '100%', p: 1.5, textAlign: 'center' }}><Box sx={{ height: 44, display: 'grid', placeItems: 'center', mb: .75, color: 'primary.main' }}>{category.imageUrl ? <Box component="img" src={category.imageUrl} alt="" sx={{ width: 40, height: 40, objectFit: 'contain' }} /> : category.icon}</Box><Typography variant="caption" fontWeight={600} color="text.primary" lineHeight={1.35}>{category.label}</Typography></CardActionArea></Card></Grid>)}</Grid>}
          </Box>

          {onSaleProducts.length ? <ProductSection id="on-sale-section" accent="sale" title="Săn Sale Giá Sốc" description="Ưu đãi đang áp dụng cho các sản phẩm được chọn" icon={<LocalOfferRoundedIcon />} products={onSaleProducts} loading={loading} emptyText="" /> : null}
          <ProductSection title="Sản phẩm nổi bật" description="Được khách hàng quan tâm và lựa chọn hàng đầu" icon={<AutoAwesomeRoundedIcon />} products={featuredProducts} loading={loading} emptyText="Chưa có sản phẩm nổi bật nào." />
          <ProductSection title="Sản phẩm mới về" description="Cập nhật sản phẩm mới từ cửa hàng" icon={<NewReleasesRoundedIcon />} products={newArrivals} loading={loading} emptyText="Chưa có sản phẩm mới nào." />
        </Stack>
      </Container>
    </Box>
  )
}
