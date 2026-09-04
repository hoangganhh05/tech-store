import { Button, Card, CardContent, Container, Stack, TextField, Typography } from '@mui/material'

export function LoginPage() {
  return (
    <Container maxWidth="sm">
      <Card><CardContent sx={{ p: { xs: 3, sm: 5 } }}><Typography component="h1" variant="h2" mb={1}>Đăng nhập</Typography><Typography color="text.secondary" mb={3}>Truy cập tài khoản TechStore của bạn.</Typography><Stack component="form" spacing={2}><TextField label="Email" type="email" required /><TextField label="Mật khẩu" type="password" required /><Button type="submit" variant="contained" size="large">Đăng nhập</Button></Stack></CardContent></Card>
    </Container>
  )
}
