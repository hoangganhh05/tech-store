import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { ThemeProvider } from '@mui/material'
import { MemoryRouter } from 'react-router-dom'
import { appTheme } from '../configs/theme'
import { AuthProvider } from '../modules/auth/AuthContext'
import { AdminCategoriesPage } from '../modules/admin/AdminCategoriesPage'
import {
  createAdminCategory,
  deleteAdminCategory,
  getAdminCategories,
  getAdminCategoryTree,
  updateAdminCategory,
  type Category,
  type CategoryTree,
} from '../services/categoryService'

vi.mock('../services/categoryService', () => ({
  getAdminCategories: vi.fn(),
  getAdminCategoryTree: vi.fn(),
  getAdminCategoryById: vi.fn(),
  createAdminCategory: vi.fn(),
  updateAdminCategory: vi.fn(),
  deleteAdminCategory: vi.fn(),
}))

const mockedGetAdminCategories = vi.mocked(getAdminCategories)
const mockedGetAdminCategoryTree = vi.mocked(getAdminCategoryTree)
const mockedCreateAdminCategory = vi.mocked(createAdminCategory)
const mockedUpdateAdminCategory = vi.mocked(updateAdminCategory)
const mockedDeleteAdminCategory = vi.mocked(deleteAdminCategory)

const mockFlatCategories: Category[] = [
  {
    id: 1,
    name: 'Điện thoại',
    description: 'Smartphone các loại',
    parentId: null,
    imageUrl: 'https://example.com/phone.png',
    createdAt: '2026-09-01T00:00:00Z',
    updatedAt: '2026-09-01T00:00:00Z',
  },
  {
    id: 2,
    name: 'iPhone',
    description: 'Điện thoại iPhone',
    parentId: 1,
    parentName: 'Điện thoại',
    imageUrl: 'https://example.com/iphone.png',
    createdAt: '2026-09-02T00:00:00Z',
    updatedAt: '2026-09-02T00:00:00Z',
  },
]

const mockTreeCategories: CategoryTree[] = [
  {
    id: 1,
    name: 'Điện thoại',
    description: 'Smartphone các loại',
    parentId: null,
    imageUrl: 'https://example.com/phone.png',
    children: [
      {
        id: 2,
        name: 'iPhone',
        description: 'Điện thoại iPhone',
        parentId: 1,
        imageUrl: 'https://example.com/iphone.png',
        children: [],
      },
    ],
  },
]

function renderAdminCategoriesPage() {
  return render(
    <ThemeProvider theme={appTheme}>
      <AuthProvider>
        <MemoryRouter>
          <AdminCategoriesPage />
        </MemoryRouter>
      </AuthProvider>
    </ThemeProvider>,
  )
}

describe('AdminCategoriesPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockedGetAdminCategories.mockResolvedValue(mockFlatCategories)
    mockedGetAdminCategoryTree.mockResolvedValue(mockTreeCategories)
  })

  it('renders categories tree table with root and child items', async () => {
    renderAdminCategoriesPage()

    await waitFor(() => {
      expect(screen.getByText('Điện thoại')).toBeInTheDocument()
      expect(screen.getByText('iPhone')).toBeInTheDocument()
    })

    expect(screen.getByText('Gốc')).toBeInTheDocument()
    expect(screen.getByText('1 con')).toBeInTheDocument()
  })

  it('opens create dialog and submits a new category successfully', async () => {
    mockedCreateAdminCategory.mockResolvedValue({
      id: 3,
      name: 'Samsung',
      description: 'Điện thoại Samsung',
      parentId: 1,
      parentName: 'Điện thoại',
      imageUrl: '',
      createdAt: '2026-09-03T00:00:00Z',
      updatedAt: '2026-09-03T00:00:00Z',
    })

    renderAdminCategoriesPage()

    await waitFor(() => {
      expect(screen.getByText('Điện thoại')).toBeInTheDocument()
    })

    const addButton = screen.getByRole('button', { name: /thêm danh mục/i })
    fireEvent.click(addButton)

    expect(screen.getByText('Thêm danh mục sản phẩm mới')).toBeInTheDocument()

    const nameInput = screen.getByLabelText(/tên danh mục/i)
    fireEvent.change(nameInput, { target: { value: 'Samsung' } })

    const submitBtn = screen.getByRole('button', { name: /thêm mới/i })
    fireEvent.click(submitBtn)

    await waitFor(() => {
      expect(mockedCreateAdminCategory).toHaveBeenCalledWith(
        expect.objectContaining({ name: 'Samsung' }),
      )
    })
  })

  it('opens edit dialog and updates category successfully', async () => {
    mockedUpdateAdminCategory.mockResolvedValue({
      id: 2,
      name: 'iPhone Pro',
      description: 'Điện thoại iPhone Pro',
      parentId: 1,
      parentName: 'Điện thoại',
      imageUrl: '',
      createdAt: '2026-09-02T00:00:00Z',
      updatedAt: '2026-09-03T00:00:00Z',
    })

    renderAdminCategoriesPage()

    await waitFor(() => {
      expect(screen.getByText('iPhone')).toBeInTheDocument()
    })

    const editBtn = screen.getByLabelText('Sửa iPhone')
    fireEvent.click(editBtn)

    expect(screen.getByText('Chỉnh sửa danh mục sản phẩm')).toBeInTheDocument()

    const nameInput = screen.getByLabelText(/tên danh mục/i)
    fireEvent.change(nameInput, { target: { value: 'iPhone Pro' } })

    const submitBtn = screen.getByRole('button', { name: /cập nhật/i })
    fireEvent.click(submitBtn)

    await waitFor(() => {
      expect(mockedUpdateAdminCategory).toHaveBeenCalledWith(
        2,
        expect.objectContaining({ name: 'iPhone Pro' }),
      )
    })
  })

  it('disables delete button for category having children', async () => {
    renderAdminCategoriesPage()

    await waitFor(() => {
      expect(screen.getByText('Điện thoại')).toBeInTheDocument()
    })

    const deletePhoneBtn = screen.getByLabelText('Xoá Điện thoại')
    expect(deletePhoneBtn).toBeDisabled()

    const deleteIPhoneBtn = screen.getByLabelText('Xoá iPhone')
    expect(deleteIPhoneBtn).not.toBeDisabled()
  })

  it('deletes a leaf category after confirmation in dialog', async () => {
    mockedDeleteAdminCategory.mockResolvedValue()

    renderAdminCategoriesPage()

    await waitFor(() => {
      expect(screen.getByText('iPhone')).toBeInTheDocument()
    })

    const deleteIPhoneBtn = screen.getByLabelText('Xoá iPhone')
    fireEvent.click(deleteIPhoneBtn)

    expect(screen.getByText('Xác nhận xoá danh mục?')).toBeInTheDocument()

    const confirmBtn = screen.getByRole('button', { name: 'Xoá danh mục' })
    fireEvent.click(confirmBtn)

    await waitFor(() => {
      expect(mockedDeleteAdminCategory).toHaveBeenCalledWith(2)
    })
  })

  it('displays error alert when fetching categories fails', async () => {
    mockedGetAdminCategories.mockRejectedValue(new Error('Network error'))
    mockedGetAdminCategoryTree.mockRejectedValue(new Error('Network error'))

    renderAdminCategoriesPage()

    await waitFor(() => {
      expect(
        screen.getByText(/Không thể tải danh sách danh mục/i),
      ).toBeInTheDocument()
    })
  })
})