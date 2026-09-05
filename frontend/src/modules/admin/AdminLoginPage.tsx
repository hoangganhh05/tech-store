import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  CircularProgress,
  Container,
  Link as MuiLink,
  Stack,
  TextField,
  Typography,
} from '@mui/material'
import ShieldOutlinedIcon from '@mui/icons-material/ShieldOutlined'
import ArrowBackIcon from '@mui/icons-material/ArrowBack'
import { isAxiosError } from 'axios'
import { useEffect, useState, type ChangeEvent, type FormEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { ROUTES } from '../../constants/routes'
import { useAuth } from '../../hooks/useAuth'
import { loginAdminAccount, type LoginPayload } from '../../services/authService'

type LoginField = keyof LoginPayload
type FieldErrors = Partial<Record<LoginField, string>>
type LoginLocationState = {
  from?: string
}

const initialValues: LoginPayload = { email: '', password: '' }
const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

export function AdminLoginPage() {
  const location = useLocation()
  const navigate = useNavigate()
  const { user, isAuthenticated, signIn } = useAuth()
  const locationState = location.state as LoginLocationState | null
  const redirectAfterLogin = locationState?.from ?? ROUTES.admin

  const [values, setValues] = useState<LoginPayload>(initialValues)
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({})
  const [submitError, setSubmitError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  useEffect(() => {
    if (isAuthenticated && user?.roles?.includes('ADMIN')) {
      navigate(redirectAfterLogin, { replace: true })
    }
  }, [isAuthenticated, user, navigate, redirectAfterLogin])

  const handleChange = (field: LoginField) => (event: ChangeEvent<HTMLInputElement>) => {
    setValues((current) => ({ ...current, [field]: event.target.value }))
    setFieldErrors((current) => ({ ...current, [field]: undefined }))
    setSubmitError('')
  }

  const validate = (): FieldErrors => {
    const errors: FieldErrors = {}
    if (!values.email.trim()) {
      errors.email = 'Vui lòng nhập email.'
    } else if (!emailPattern.test(values.email.trim())) {
      errors.email = 'Email không đúng định dạng.'
    }
    if (!values.password) {
      errors.password = 'Vui lòng nhập mật khẩu.'
    }
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
      const result = await loginAdminAccount({
        email: values.email.trim(),
        password: values.password,
      })
      signIn(result)
      navigate(redirectAfterLogin, { replace: true })
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
    <Box
      sx={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        backgroundColor: (theme) =>
          theme.palette.mode === 'dark' ? '#0b1120' : '#f1f5f9',
        p: 2,
      }}
    >
      <Container maxWidth="xs">
        <Card
          sx={{
            boxShadow: '0 10px 25px -5px rgba(0, 0, 0, 0.1), 0 8px 10px -6px rgba(0, 0, 0, 0.1)',
            borderRadius: 3,
            overflow: 'hidden',
          }}
        >
          <Box
            sx={{
              backgroundColor: 'primary.dark',
              color: 'primary.contrastText',
              p: 3,
              textAlign: 'center',
            }}
          >
            <Box
              sx={{
                display: 'inline-flex',
                alignItems: 'center',
                justifyContent: 'center',
                width: 48,
                height: 48,
                borderRadius: '50%',
                backgroundColor: 'rgba(255, 255, 255, 0.15)',
                mb: 1.5,
              }}
            >
              <ShieldOutlinedIcon sx={{ fontSize: 28 }} />
            </Box>
            <Typography variant="h5" component="h1" fontWeight={700}>
              Đăng nhập Quản trị
            </Typography>
            <Chip
              label="Admin Portal"
              size="small"
              sx={{
                mt: 1,
                backgroundColor: 'rgba(255, 255, 255, 0.2)',
                color: 'inherit',
                fontWeight: 600,
              }}
            />
          </Box>

          <CardContent sx={{ p: { xs: 3, sm: 4 } }}>
            <Typography variant="body2" color="text.secondary" textAlign="center" mb={3}>
              Cổng đăng nhập bảo mật dành riêng cho Quản trị viên TechStore.
            </Typography>

            <Stack component="form" spacing={2.5} onSubmit={handleSubmit} noValidate>
              {submitError && (
                <Alert severity="error" sx={{ width: '100%' }}>
                  {submitError}
                </Alert>
              )}

              <TextField
                label="Email quản trị viên"
                type="email"
                value={values.email}
                onChange={handleChange('email')}
                error={Boolean(fieldErrors.email)}
                helperText={fieldErrors.email}
                required
                fullWidth
                autoComplete="email"
                disabled={isSubmitting}
              />

              <TextField
                label="Mật khẩu"
                type="password"
                value={values.password}
                onChange={handleChange('password')}
                error={Boolean(fieldErrors.password)}
                helperText={fieldErrors.password}
                required
                fullWidth
                autoComplete="current-password"
                disabled={isSubmitting}
              />

              <Button
                type="submit"
                variant="contained"
                size="large"
                fullWidth
                disabled={isSubmitting}
                startIcon={
                  isSubmitting ? (
                    <CircularProgress size={20} color="inherit" />
                  ) : (
                    <ShieldOutlinedIcon />
                  )
                }
                sx={{ mt: 1, py: 1.2, fontWeight: 600 }}
              >
                {isSubmitting ? 'Đang xác thực...' : 'Đăng nhập Quản trị'}
              </Button>
            </Stack>

            <Box sx={{ mt: 3, textAlign: 'center' }}>
              <MuiLink
                component={Link}
                to={ROUTES.home}
                variant="body2"
                color="text.secondary"
                sx={{
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: 0.5,
                  textDecoration: 'none',
                  '&:hover': { textDecoration: 'underline' },
                }}
              >
                <ArrowBackIcon fontSize="small" /> Quay lại cửa hàng
              </MuiLink>
            </Box>
          </CardContent>
        </Card>
      </Container>
    </Box>
  )
}