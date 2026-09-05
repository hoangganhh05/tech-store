import BlockIcon from '@mui/icons-material/Block'
import { Button, Card, CardContent, Stack, Typography } from '@mui/material'
import { Link } from 'react-router-dom'
import { ROUTES } from '../../constants/routes'
import { useAuth } from '../../hooks/useAuth'

export function ForbiddenPage() {
  const { signOut, user } = useAuth()

  return (
    <Stack alignItems="center" justifyContent="center" py={8} px={2}>
      <Card sx={{ maxWidth: 520, width: '100%', textAlign: 'center', p: { xs: 2, sm: 4 } }}>
        <CardContent>
          <Stack alignItems="center" spacing={2}>
            <BlockIcon color="error" sx={{ fontSize: 64 }} />
            <Typography component="h1" variant="h3" fontWeight={700}>
              Truy cập bị từ chối
            </Typography>
            <Typography color="text.secondary">
              Tài khoản hiện tại ({user?.email || 'người dùng'}) không có quyền truy cập vào khu vực này.
              Trang này chỉ dành riêng cho Quản trị viên hệ thống.
            </Typography>
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} pt={2} width="100%" justifyContent="center">
              <Button component={Link} to={ROUTES.home} variant="contained">
                Về trang chủ
              </Button>
              <Button variant="outlined" color="inherit" onClick={() => signOut()}>
                Đăng xuất
              </Button>
            </Stack>
          </Stack>
        </CardContent>
      </Card>
    </Stack>
  )
}
