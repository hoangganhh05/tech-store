import { Alert, Button, Card, CardContent, Container, Link as MuiLink, Stack, TextField, Typography } from '@mui/material'
import { isAxiosError } from 'axios'
import { useState, type ChangeEvent, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { ROUTES } from '../../constants/routes'
import { registerAccount, type RegisterPayload } from '../../services/authService'

type RegisterField = keyof RegisterPayload
type FieldErrors = Partial<Record<RegisterField, string>>

const initialValues: RegisterPayload = {
  fullName: '',
  email: '',
  phone: '',
  password: '',
  confirmPassword: '',
}

const phonePattern = /^[0-9+() .-]{7,20}$/
const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

export function RegisterPage() {
  const navigate = useNavigate()
  const [values, setValues] = useState(initialValues)
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({})
  const [submitError, setSubmitError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  const handleChange = (field: RegisterField) => (event: ChangeEvent<HTMLInputElement>) => {
    setValues((current) => ({ ...current, [field]: event.target.value }))
    setFieldErrors((current) => ({ ...current, [field]: undefined }))
    setSubmitError('')
  }

  const validate = (): FieldErrors => {
    const errors: FieldErrors = {}
    if (!values.fullName.trim()) errors.fullName = 'Vui lòng nhập họ tên.'
    if (!values.email.trim()) errors.email = 'Vui lòng nhập email.'
    else if (!emailPattern.test(values.email.trim())) errors.email = 'Email không đúng định dạng.'
    if (!values.phone.trim()) errors.phone = 'Vui lòng nhập số điện thoại.'
    else if (!phonePattern.test(values.phone.trim())) errors.phone = 'Số điện thoại không đúng định dạng.'
    if (values.password.length < 8) errors.password = 'Mật khẩu phải có ít nhất 8 ký tự.'
    if (!values.confirmPassword) errors.confirmPassword = 'Vui lòng xác nhận mật khẩu.'
    else if (values.password !== values.confirmPassword) errors.confirmPassword = 'Mật khẩu xác nhận không khớp.'
    return errors
  }

  const serverFieldErrors = (message: string): FieldErrors => {
    const errors: FieldErrors = {}
    message.split(';').forEach((part) => {
      const separator = part.indexOf(':')
      if (separator < 0) return
      const field = part.slice(0, separator).trim() as RegisterField
      if (field in initialValues) errors[field] = part.slice(separator + 1).trim()
    })
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
      await registerAccount({
        ...values,
        fullName: values.fullName.trim(),
        email: values.email.trim(),
        phone: values.phone.trim(),
      })
      navigate(ROUTES.login, {
        replace: true,
        state: { registrationMessage: 'Đăng ký thành công. Bạn có thể đăng nhập ngay bây giờ.' },
      })
    } catch (error: unknown) {
      const message = isAxiosError<{ message?: string }>(error)
        ? error.response?.data?.message
        : undefined
      if (message) {
        const parsedErrors = serverFieldErrors(message)
        if (Object.keys(parsedErrors).length > 0) setFieldErrors(parsedErrors)
        else setSubmitError(message)
      } else {
        setSubmitError('Không thể đăng ký lúc này. Vui lòng thử lại sau.')
      }
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <Container maxWidth="sm">
      <Card>
        <CardContent sx={{ p: { xs: 3, sm: 5 } }}>
          <Typography component="h1" variant="h2" mb={1}>Tạo tài khoản</Typography>
          <Typography color="text.secondary" mb={3}>Đăng ký để mua hàng và theo dõi đơn hàng tại TechStore.</Typography>
          <Stack component="form" spacing={2} onSubmit={handleSubmit} noValidate>
            {submitError && <Alert severity="error">{submitError}</Alert>}
            <TextField label="Họ tên" value={values.fullName} onChange={handleChange('fullName')} error={Boolean(fieldErrors.fullName)} helperText={fieldErrors.fullName} required autoComplete="name" />
            <TextField label="Email" type="email" value={values.email} onChange={handleChange('email')} error={Boolean(fieldErrors.email)} helperText={fieldErrors.email} required autoComplete="email" />
            <TextField label="Số điện thoại" value={values.phone} onChange={handleChange('phone')} error={Boolean(fieldErrors.phone)} helperText={fieldErrors.phone} required autoComplete="tel" />
            <TextField label="Mật khẩu" type="password" value={values.password} onChange={handleChange('password')} error={Boolean(fieldErrors.password)} helperText={fieldErrors.password || 'Tối thiểu 8 ký tự.'} required autoComplete="new-password" />
            <TextField label="Xác nhận mật khẩu" type="password" value={values.confirmPassword} onChange={handleChange('confirmPassword')} error={Boolean(fieldErrors.confirmPassword)} helperText={fieldErrors.confirmPassword} required autoComplete="new-password" />
            <Button type="submit" variant="contained" size="large" disabled={isSubmitting}>
              {isSubmitting ? 'Đang đăng ký...' : 'Đăng ký'}
            </Button>
            <Typography textAlign="center" color="text.secondary">
              Đã có tài khoản? <MuiLink component={Link} to={ROUTES.login}>Đăng nhập</MuiLink>
            </Typography>
          </Stack>
        </CardContent>
      </Card>
    </Container>
  )
}
