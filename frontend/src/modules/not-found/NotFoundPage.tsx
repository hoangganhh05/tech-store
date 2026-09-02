import { Button, Stack, Typography } from '@mui/material'
import { Link } from 'react-router-dom'
import { ROUTES } from '../../constants/routes'

export function NotFoundPage() {
  return <Stack alignItems="flex-start" spacing={2}><Typography component="h1" variant="h1">Không tìm thấy trang</Typography><Typography color="text.secondary">Đường dẫn bạn truy cập không tồn tại.</Typography><Button component={Link} to={ROUTES.home} variant="contained">Về trang chủ</Button></Stack>
}
