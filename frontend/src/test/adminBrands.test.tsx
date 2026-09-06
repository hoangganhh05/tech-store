import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { ThemeProvider } from '@mui/material'
import { MemoryRouter } from 'react-router-dom'
import { appTheme } from '../configs/theme'
import { AuthProvider } from '../modules/auth/AuthContext'
import { AdminBrandsPage } from '../modules/admin/AdminBrandsPage'
import {
  createAdminBrand,
  deleteAdminBrand,
  getAdminBrands,
  updateAdminBrand,
  type Brand,
} from '../services/brandService'

vi.mock('../services/brandService', () => ({
  getAdminBrands: vi.fn(),
  getAdminBrandById: vi.fn(),
  createAdminBrand: vi.fn(),
  updateAdminBrand: vi.fn(),
  deleteAdminBrand: vi.fn(),
}))

const mockedGetAdminBrands = vi.mocked(getAdminBrands)
const mockedCreateAdminBrand = vi.mocked(createAdminBrand)
const mockedUpdateAdminBrand = vi.mocked(updateAdminBrand)
const mockedDeleteAdminBrand = vi.mocked(deleteAdminBrand)

const mockBrands: Brand[] = [
  {
    id: 1,
    name: 'Apple',
    logoUrl: 'https://example.com/apple.png',
    description: 'Thương hiệu Apple',
    createdAt: '2026-09-01T00:00:00Z',
    updatedAt: '2026-09-01T00:00:00Z',
  },
  {
    id: 2,
    name: 'Samsung',
    logoUrl: 'https://example.com/samsung.png',
    description: 'Tập đoàn điện tử Samsung',
    createdAt: '2026-09-02T00:00:00Z',
    updatedAt: '2026-09-02T00:00:00Z',
  },
]

function renderAdminBrandsPage() {
  return render(
    <ThemeProvider theme={appTheme}>
      <AuthProvider>
        <MemoryRouter>
          <AdminBrandsPage />
        </MemoryRouter>
      </AuthProvider>
    </ThemeProvider>,
  )
}

describe('AdminBrandsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockedGetAdminBrands.mockResolvedValue(mockBrands)
  })

  it('renders brands table with items and logos', async () => {
    renderAdminBrandsPage()

    await waitFor(() => {
      expect(screen.getByText('Apple')).toBeInTheDocument()
      expect(screen.getByText('Samsung')).toBeInTheDocument()
    })

    expect(screen.getByText('Thương hiệu Apple')).toBeInTheDocument()
    expect(screen.getByText('Tập đoàn điện tử Samsung')).toBeInTheDocument()
  })

  it('filters brands based on search input', async () => {
    renderAdminBrandsPage()

    await waitFor(() => {
      expect(screen.getByText('Apple')).toBeInTheDocument()
      expect(screen.getByText('Samsung')).toBeInTheDocument()
    })

    const searchInput = screen.getByPlaceholderText(/tìm kiếm thương hiệu/i)
    fireEvent.change(searchInput, { target: { value: 'app' } })

    expect(screen.getByText('Apple')).toBeInTheDocument()
    expect(screen.queryByText('Samsung')).not.toBeInTheDocument()
  })

  it('opens create dialog and submits a new brand successfully', async () => {
    mockedCreateAdminBrand.mockResolvedValue({
      id: 3,
      name: 'Xiaomi',
      logoUrl: 'https://example.com/xiaomi.png',
      description: 'Hãng Xiaomi',
      createdAt: '2026-09-03T00:00:00Z',
      updatedAt: '2026-09-03T00:00:00Z',
    })

    renderAdminBrandsPage()

    await waitFor(() => {
      expect(screen.getByText('Apple')).toBeInTheDocument()
    })

    const addBtn = screen.getByRole('button', { name: /thêm thương hiệu/i })
    fireEvent.click(addBtn)

    expect(screen.getByText('Thêm thương hiệu mới')).toBeInTheDocument()

    const nameInput = screen.getByLabelText(/tên thương hiệu/i)
    fireEvent.change(nameInput, { target: { value: 'Xiaomi' } })

    const submitBtn = screen.getByRole('button', { name: /thêm mới/i })
    fireEvent.click(submitBtn)

    await waitFor(() => {
      expect(mockedCreateAdminBrand).toHaveBeenCalledWith(
        expect.objectContaining({ name: 'Xiaomi' }),
      )
    })
  })

  it('opens edit dialog and updates brand successfully', async () => {
    mockedUpdateAdminBrand.mockResolvedValue({
      id: 1,
      name: 'Apple Inc',
      logoUrl: 'https://example.com/apple.png',
      description: 'Cập nhật',
      createdAt: '2026-09-01T00:00:00Z',
      updatedAt: '2026-09-03T00:00:00Z',
    })

    renderAdminBrandsPage()

    await waitFor(() => {
      expect(screen.getByText('Apple')).toBeInTheDocument()
    })

    const editBtn = screen.getByLabelText('Sửa Apple')
    fireEvent.click(editBtn)

    expect(screen.getByText('Chỉnh sửa thương hiệu')).toBeInTheDocument()

    const nameInput = screen.getByLabelText(/tên thương hiệu/i)
    fireEvent.change(nameInput, { target: { value: 'Apple Inc' } })

    const submitBtn = screen.getByRole('button', { name: /cập nhật/i })
    fireEvent.click(submitBtn)

    await waitFor(() => {
      expect(mockedUpdateAdminBrand).toHaveBeenCalledWith(
        1,
        expect.objectContaining({ name: 'Apple Inc' }),
      )
    })
  })

  it('deletes a brand after confirmation in dialog', async () => {
    mockedDeleteAdminBrand.mockResolvedValue()

    renderAdminBrandsPage()

    await waitFor(() => {
      expect(screen.getByText('Apple')).toBeInTheDocument()
    })

    const deleteBtn = screen.getByLabelText('Xoá Apple')
    fireEvent.click(deleteBtn)

    expect(screen.getByText('Xác nhận xoá thương hiệu?')).toBeInTheDocument()

    const confirmBtn = screen.getByRole('button', { name: 'Xoá thương hiệu' })
    fireEvent.click(confirmBtn)

    await waitFor(() => {
      expect(mockedDeleteAdminBrand).toHaveBeenCalledWith(1)
    })
  })

  it('displays error alert when fetching brands fails', async () => {
    mockedGetAdminBrands.mockRejectedValue(new Error('Network error'))

    renderAdminBrandsPage()

    await waitFor(() => {
      expect(
        screen.getByText(/Không thể tải danh sách thương hiệu/i),
      ).toBeInTheDocument()
    })
  })
})
