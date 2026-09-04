import { Alert, Button, Card, CardContent, Container, Link as MuiLink, Stack, TextField, Typography } from '@mui/material'
import { Link, useLocation } from 'react-router-dom'
import { ROUTES } from '../../constants/routes'

export function LoginPage() {
  const location = useLocation()
  const registrationMessage = (location.state as { registrationMessage?: string } | null)?.registrationMessage

  return (
    <Container maxWidth="sm">
      <Card><CardContent sx={{ p: { xs: 3, sm: 5 } }}>
        <Typography component="h1" variant="h2" mb={1}>Đăng nhập</Typography>
        <Typography color="text.secondary" mb={3}>Truy cập tài khoản TechStore của bạn.</Typography>
        {registrationMessage && <Alert severity="success" sx={{ mb: 2 }}>{registrationMessage}</Alert>}
        <Stack component="form" spacing={2}>
          <TextField label="Email" type="email" required autoComplete="email" />
          <TextField label="Mật khẩu" type="password" required autoComplete="current-password" />
          <Button type="submit" variant="contained" size="large">Đăng nhập</Button>
          <Typography textAlign="center" color="text.secondary">
            Chưa có tài khoản? <MuiLink component={Link} to={ROUTES.register}>Đăng ký ngay</MuiLink>
          </Typography>
        </Stack>
      </CardContent></Card>
    </Container>
  )
}
