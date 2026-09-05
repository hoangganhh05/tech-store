import { Alert, Button, Card, CardContent, Container, Link as MuiLink, Stack, TextField, Typography } from '@mui/material'
import { isAxiosError } from 'axios'
import { useState, type ChangeEvent, type FormEvent } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { ROUTES } from '../../constants/routes'
import { resetPassword, type ResetPasswordPayload } from '../../services/authService'

type ResetPasswordField = Exclude<keyof ResetPasswordPayload, 'token'>
type FieldErrors = Partial<Record<ResetPasswordField, string>>

const initialValues: Pick<ResetPasswordPayload, ResetPasswordField> = {
  password: '',
  confirmPassword: '',
}
const invalidLinkMessage = 'Liên kết đặt lại mật khẩu không hợp lệ, đã được sử dụng hoặc đã hết hạn. Vui lòng yêu cầu một liên kết mới.'

export function ResetPasswordPage() {
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const token = searchParams.get('token')?.trim() ?? ''
  const [values, setValues] = useState(initialValues)
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({})
  const [submitError, setSubmitError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  const handleChange = (field: ResetPasswordField) => (event: ChangeEvent<HTMLInputElement>) => {
    setValues((current) => ({ ...current, [field]: event.target.value }))
    setFieldErrors((current) => ({ ...current, [field]: undefined }))
    setSubmitError('')
  }

  const validate = (): FieldErrors => {
    const errors: FieldErrors = {}
    if (values.password.length < 8) errors.password = 'Mật khẩu phải có ít nhất 8 ký tự.'
    if (!values.confirmPassword) errors.confirmPassword = 'Vui lòng xác nhận mật khẩu.'
    else if (values.password !== values.confirmPassword) errors.confirmPassword = 'Mật khẩu xác nhận không khớp.'
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
      await resetPassword({ token, ...values })
      navigate(ROUTES.login, {
        replace: true,
        state: { passwordResetMessage: 'Đặt lại mật khẩu thành công. Bạn có thể đăng nhập bằng mật khẩu mới.' },
      })
    } catch (error: unknown) {
      const message = isAxiosError<{ code?: string; message?: string }>(error)
        && error.response?.data?.code === 'PASSWORD_MUST_BE_DIFFERENT'
        ? error.response.data.message ?? 'Mật khẩu mới phải khác mật khẩu hiện tại.'
        : invalidLinkMessage
      setSubmitError(message)
    } finally {
      setIsSubmitting(false)
    }
  }

  if (!token) {
    return (
      <Container maxWidth="sm">
        <Card>
          <CardContent sx={{ p: { xs: 3, sm: 5 } }}>
            <Typography component="h1" variant="h2" mb={2}>Đặt lại mật khẩu</Typography>
            <Alert severity="error" sx={{ mb: 3 }}>{invalidLinkMessage}</Alert>
            <Button component={Link} to={ROUTES.forgotPassword} variant="contained">
              Yêu cầu liên kết mới
            </Button>
          </CardContent>
        </Card>
      </Container>
    )
  }

  return (
    <Container maxWidth="sm">
      <Card>
        <CardContent sx={{ p: { xs: 3, sm: 5 } }}>
          <Typography component="h1" variant="h2" mb={1}>Đặt lại mật khẩu</Typography>
          <Typography color="text.secondary" mb={3}>Chọn mật khẩu mới có ít nhất 8 ký tự.</Typography>
          <Stack component="form" spacing={2} onSubmit={handleSubmit} noValidate>
            {submitError && <Alert severity="error">{submitError}</Alert>}
            <TextField
              label="Mật khẩu mới"
              type="password"
              value={values.password}
              onChange={handleChange('password')}
              error={Boolean(fieldErrors.password)}
              helperText={fieldErrors.password || 'Tối thiểu 8 ký tự.'}
              required
              autoComplete="new-password"
              disabled={isSubmitting}
            />
            <TextField
              label="Xác nhận mật khẩu mới"
              type="password"
              value={values.confirmPassword}
              onChange={handleChange('confirmPassword')}
              error={Boolean(fieldErrors.confirmPassword)}
              helperText={fieldErrors.confirmPassword}
              required
              autoComplete="new-password"
              disabled={isSubmitting}
            />
            <Button type="submit" variant="contained" size="large" disabled={isSubmitting} aria-busy={isSubmitting}>
              {isSubmitting ? 'Đang đặt lại...' : 'Đặt lại mật khẩu'}
            </Button>
            <Typography textAlign="center" color="text.secondary">
              <MuiLink component={Link} to={ROUTES.login}>Quay lại đăng nhập</MuiLink>
            </Typography>
          </Stack>
        </CardContent>
      </Card>
    </Container>
  )
}
