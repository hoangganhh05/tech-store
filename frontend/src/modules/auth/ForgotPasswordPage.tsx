import { Alert, Button, Card, CardContent, Container, Link as MuiLink, Stack, TextField, Typography } from '@mui/material'
import { useState, type ChangeEvent, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { ROUTES } from '../../constants/routes'
import { requestPasswordReset } from '../../services/authService'

const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const genericSuccessMessage = 'Nếu email này thuộc về một tài khoản, chúng tôi đã gửi hướng dẫn đặt lại mật khẩu. Vui lòng kiểm tra hộp thư của bạn.'

export function ForgotPasswordPage() {
  const [email, setEmail] = useState('')
  const [emailError, setEmailError] = useState('')
  const [submitError, setSubmitError] = useState('')
  const [isComplete, setIsComplete] = useState(false)
  const [isSubmitting, setIsSubmitting] = useState(false)

  const handleChange = (event: ChangeEvent<HTMLInputElement>) => {
    setEmail(event.target.value)
    setEmailError('')
    setSubmitError('')
  }

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const normalizedEmail = email.trim()

    if (!normalizedEmail) {
      setEmailError('Vui lòng nhập email.')
      return
    }
    if (!emailPattern.test(normalizedEmail)) {
      setEmailError('Email không đúng định dạng.')
      return
    }

    setIsSubmitting(true)
    setSubmitError('')
    try {
      await requestPasswordReset({ email: normalizedEmail })
      setIsComplete(true)
    } catch {
      // Do not render an API message here: it could disclose whether an email exists.
      setSubmitError('Không thể gửi yêu cầu lúc này. Vui lòng thử lại sau.')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <Container maxWidth="sm">
      <Card>
        <CardContent sx={{ p: { xs: 3, sm: 5 } }}>
          <Typography component="h1" variant="h2" mb={1}>Quên mật khẩu</Typography>
          <Typography color="text.secondary" mb={3}>
            Nhập email của bạn để nhận hướng dẫn đặt lại mật khẩu.
          </Typography>
          <Stack component="form" spacing={2} onSubmit={handleSubmit} noValidate>
            {isComplete && <Alert severity="success">{genericSuccessMessage}</Alert>}
            {submitError && <Alert severity="error">{submitError}</Alert>}
            <TextField
              label="Email"
              type="email"
              value={email}
              onChange={handleChange}
              error={Boolean(emailError)}
              helperText={emailError}
              required
              autoComplete="email"
              disabled={isSubmitting || isComplete}
            />
            <Button type="submit" variant="contained" size="large" disabled={isSubmitting || isComplete} aria-busy={isSubmitting}>
              {isSubmitting ? 'Đang gửi...' : 'Gửi hướng dẫn'}
            </Button>
            <Typography textAlign="center" color="text.secondary">
              Đã nhớ mật khẩu? <MuiLink component={Link} to={ROUTES.login}>Đăng nhập</MuiLink>
            </Typography>
          </Stack>
        </CardContent>
      </Card>
    </Container>
  )
}
