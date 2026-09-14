import type { ReactNode } from 'react'
import { Box, Stack, Typography } from '@mui/material'
import ArrowBackIcon from '@mui/icons-material/ArrowBack'
import Inventory2OutlinedIcon from '@mui/icons-material/Inventory2Outlined'
import FavoriteBorderIcon from '@mui/icons-material/FavoriteBorder'
import LocationOnOutlinedIcon from '@mui/icons-material/LocationOnOutlined'
import { Link } from 'react-router-dom'
import { env } from '../../configs/env'
import { ROUTES } from '../../constants/routes'

export function AuthPanel({ children }: { children: ReactNode }) {
  return (
    <Box sx={{ maxWidth: 1000, mx: 'auto', display: 'grid', gridTemplateColumns: { xs: 'minmax(0, 1fr)', md: '0.9fr 1.1fr' }, border: '1px solid', borderColor: 'divider', borderRadius: '24px', overflow: 'hidden', bgcolor: 'background.paper', boxShadow: '0 12px 40px rgba(16,42,67,0.05)' }}>
      <Stack sx={{ p: { xs: 3, md: 5 }, color: '#fff', background: 'radial-gradient(ellipse at 100% 0%, #1269B9 0%, transparent 60%), linear-gradient(145deg, #062E63, #0756A8)', position: 'relative', overflow: 'hidden', justifyContent: 'space-between', gap: 5 }}>
        <Box component={Link} to={ROUTES.home} sx={{ display: 'inline-flex', alignItems: 'center', gap: 1, color: 'inherit', fontSize: 13, textDecoration: 'none', alignSelf: 'flex-start', '&:hover': { color: '#F2B705' } }}><ArrowBackIcon fontSize="small" /> Về trang chủ</Box>
        <Box>
          <Typography sx={{ color: '#F2B705', fontWeight: 800, letterSpacing: 1, fontSize: 12, mb: 2, textTransform: 'uppercase' }}>{env.brand.name}</Typography>
          <Typography component="p" sx={{ fontSize: { xs: 26, md: 36 }, lineHeight: 1.3, fontWeight: 800, maxWidth: 320 }}>Công nghệ bạn thích.<br />Trải nghiệm của bạn.</Typography>
          <Typography sx={{ mt: 2, color: '#D6E6F7', fontSize: 14, lineHeight: 1.8 }}>Một tài khoản để lưu sản phẩm yêu thích, đặt hàng và theo dõi mọi giao dịch.</Typography>
        </Box>
        <Stack spacing={2.5} sx={{ display: { xs: 'none', md: 'flex' } }}>
          {[{ Icon: Inventory2OutlinedIcon, text: 'Theo dõi đơn hàng dễ dàng' }, { Icon: FavoriteBorderIcon, text: 'Lưu những sản phẩm bạn yêu thích' }, { Icon: LocationOnOutlinedIcon, text: 'Quản lý địa chỉ nhận hàng' }].map(({ Icon, text }) => <Stack key={text} direction="row" spacing={1.5} alignItems="center"><Box sx={{ bgcolor: 'rgba(255,255,255,0.1)', p: 1, borderRadius: '10px', display: 'flex' }}><Icon sx={{ color: '#F2B705', fontSize: 21 }} /></Box><Typography variant="body2">{text}</Typography></Stack>)}
        </Stack>
      </Stack>
      <Box sx={{ minWidth: 0, p: { xs: 3, sm: 4, md: 5 }, alignSelf: 'center', '& h1': { fontSize: { xs: 26, sm: 30 }, fontWeight: 800 }, '& form > .MuiButton-root': { mt: 1, minHeight: 48 }, '& .MuiOutlinedInput-root': { bgcolor: '#FBFCFE' } }}>{children}</Box>
    </Box>
  )
}
