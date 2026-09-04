import { Alert, Button, Card, CardContent, Container, Link as MuiLink, Stack, TextField, Typography } from '@mui/material'
import { isAxiosError } from 'axios'
import { useState, type ChangeEvent, type FormEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { ROUTES } from '../../constants/routes'
import { useAuth } from '../../hooks/useAuth'
import { loginAccount, type LoginPayload } from '../../services/authService'

type LoginField = keyof LoginPayload
type FieldErrors = Partial<Record<LoginField, string>>

const initialValues: LoginPayload = { email: '', password: '' }
const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

export function LoginPage() {
  const location = useLocation()
  const navigate = useNavigate()
  const { signIn } = useAuth()
  const registrationMessage = (location.state as { registrationMessage?: string } | null)?.registrationMessage
  const [values, setValues] = useState(initialValues)
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({})
  const [submitError, setSubmitError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  const handleChange = (field: LoginField) => (event: ChangeEvent<HTMLInputElement>) => {
    setValues((current) => ({ ...current, [field]: event.target.value }))
    setFieldErrors((current) => ({ ...current, [field]: undefined }))
    setSubmitError('')
  }

  const validate = (): FieldErrors => {
    const errors: FieldErrors = {}
    if (!values.email.trim()) errors.email = 'Vui lòng nhập email.'
    else if (!emailPattern.test(values.email.trim())) errors.email = 'Email không đúng định dạng.'
    if (!values.password) errors.password = 'Vui lòng nhập mật khẩu.'
    return errors
  }

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const errors = validate()
    setFieldErrors(errors)
    if (Object.keys(errors).length > 0) return

    setIsSubmitting(true)
    setSubmitError('')
    try {
      const result = await loginAccount({ email: values.email.trim(), password: values.password })
      signIn(result)
      navigate(ROUTES.home, { replace: true })
    } catch (error: unknown) {
      const message = isAxiosError<{ message?: string }>(error)
        ? error.response?.data?.message
        : undefined
      setSubmitError(message || 'Không thể đăng nhập lúc này. Vui lòng thử lại sau.')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <Container maxWidth="sm">
      <Card><CardContent sx={{ p: { xs: 3, sm: 5 } }}>
        <Typography component="h1" variant="h2" mb={1}>Đăng nhập</Typography>
        <Typography color="text.secondary" mb={3}>Truy cập tài khoản TechStore của bạn.</Typography>
        {registrationMessage && <Alert severity="success" sx={{ mb: 2 }}>{registrationMessage}</Alert>}
        <Stack component="form" spacing={2} onSubmit={handleSubmit} noValidate>
          {submitError && <Alert severity="error">{submitError}</Alert>}
          <TextField label="Email" type="email" value={values.email} onChange={handleChange('email')} error={Boolean(fieldErrors.email)} helperText={fieldErrors.email} required autoComplete="email" />
          <TextField label="Mật khẩu" type="password" value={values.password} onChange={handleChange('password')} error={Boolean(fieldErrors.password)} helperText={fieldErrors.password} required autoComplete="current-password" />
          <Button type="submit" variant="contained" size="large" disabled={isSubmitting} aria-busy={isSubmitting}>
            {isSubmitting ? 'Đang đăng nhập...' : 'Đăng nhập'}
          </Button>
          <Typography textAlign="center" color="text.secondary">
            Chưa có tài khoản? <MuiLink component={Link} to={ROUTES.register}>Đăng ký ngay</MuiLink>
          </Typography>
        </Stack>
      </CardContent></Card>
    </Container>
  )
}
