import { fireEvent, render, screen, waitFor } from "@testing-library/react"
import { ThemeProvider } from "@mui/material"
import { MemoryRouter } from "react-router-dom"
import { appTheme } from "../configs/theme"
import { AdminReviewsPage } from "../modules/admin/AdminReviewsPage"
import { getAdminReviews, updateAdminReviewStatus } from "../services/adminReviewService"

vi.mock("../services/adminReviewService", () => ({
  getAdminReviews: vi.fn(),
  updateAdminReviewStatus: vi.fn(),
}))

const mockedGetAdminReviews = vi.mocked(getAdminReviews)
const mockedUpdateAdminReviewStatus = vi.mocked(updateAdminReviewStatus)

const pendingReview = {
  id: 10,
  productId: 42,
  userId: 7,
  userFullName: "Nguyễn Minh Anh",
  rating: 5,
  comment: "Sản phẩm rất tốt",
  status: "PENDING" as const,
  createdAt: "2026-09-05T10:00:00Z",
  updatedAt: "2026-09-05T10:00:00Z",
}

const hiddenReview = {
  ...pendingReview,
  id: 11,
  status: "HIDDEN" as const,
  comment: "Nội dung không phù hợp",
}

function renderPage() {
  return render(
    <ThemeProvider theme={appTheme}>
      <MemoryRouter><AdminReviewsPage /></MemoryRouter>
    </ThemeProvider>,
  )
}

describe("AdminReviewsPage", () => {
  beforeEach(() => {
    mockedGetAdminReviews.mockResolvedValue({
      items: [pendingReview, hiddenReview],
      page: 0,
      size: 10,
      totalElements: 2,
      totalPages: 1,
      first: true,
      last: true,
    })
  })

  afterEach(() => {
    mockedGetAdminReviews.mockReset()
    mockedUpdateAdminReviewStatus.mockReset()
  })

  it("renders reviews with status and moderation actions", async () => {
    renderPage()

    expect((await screen.findAllByText("Nguyễn Minh Anh")).length).toBe(2)
    expect(screen.getByText("Chờ duyệt")).toBeInTheDocument()
    expect(screen.getByText("Đã ẩn")).toBeInTheDocument()
    expect(screen.getByRole("button", { name: "Duyệt" })).toBeInTheDocument()
    expect(screen.getByRole("button", { name: "Hiện" })).toBeInTheDocument()
  })

  it("filters reviews by selected status", async () => {
    renderPage()
    await screen.findByText("Sản phẩm rất tốt")

    fireEvent.mouseDown(screen.getAllByRole("combobox")[0])
    fireEvent.click(await screen.findByRole("option", { name: "Đã ẩn" }))

    await waitFor(() => {
      expect(mockedGetAdminReviews).toHaveBeenLastCalledWith({ status: "HIDDEN", page: 0, size: 10 })
    })
  })

  it("updates review status and reloads the list", async () => {
    mockedUpdateAdminReviewStatus.mockResolvedValue({ ...pendingReview, status: "APPROVED" })
    renderPage()
    await screen.findByText("Sản phẩm rất tốt")

    fireEvent.click(screen.getByRole("button", { name: "Duyệt" }))

    await waitFor(() => {
      expect(mockedUpdateAdminReviewStatus).toHaveBeenCalledWith(10, "APPROVED")
      expect(mockedGetAdminReviews).toHaveBeenCalledTimes(2)
    })
    expect(await screen.findByText(/Duyệt đánh giá #10 thành công/i)).toBeInTheDocument()
  })

  it("shows API errors and allows retry", async () => {
    mockedGetAdminReviews.mockRejectedValueOnce(new Error("network"))
    renderPage()

    expect(await screen.findByText(/Không thể tải danh sách đánh giá/i)).toBeInTheDocument()
    mockedGetAdminReviews.mockResolvedValueOnce({
      items: [pendingReview], page: 0, size: 10, totalElements: 1, totalPages: 1, first: true, last: true,
    })
    fireEvent.click(screen.getByRole("button", { name: /làm mới danh sách đánh giá/i }))
    expect(await screen.findByText("Sản phẩm rất tốt")).toBeInTheDocument()
  })
})
