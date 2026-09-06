import {
  Box,
  Card,
  CardActionArea,
  CardContent,
  Chip,
  Grid,
  Typography,
} from "@mui/material";
import WarningAmberOutlinedIcon from "@mui/icons-material/WarningAmberOutlined";
import { useEffect, useState } from "react";
import { Link as RouterLink } from "react-router-dom";
import { PageIntro } from "../../components/common/PageIntro";
import { ROUTES } from "../../constants/routes";
import {
  getInventorySummary,
  type InventorySummary,
} from "../../services/inventoryService";

export function AdminDashboardPage() {
  const [summary, setSummary] = useState<InventorySummary | null>(null);

  useEffect(() => {
    getInventorySummary()
      .then((data) => setSummary(data))
      .catch(() => {});
  }, []);

  const lowStockCount = summary ? summary.lowStockCount : 0;

  return (
    <>
      <PageIntro
        eyebrow="Quản trị"
        title="Tổng quan"
        description="Theo dõi nhanh tình trạng hoạt động của cửa hàng."
      />
      <Grid container spacing={2}>
        <Grid size={{ xs: 12, md: 4 }}>
          <Card>
            <CardContent>
              <Typography color="text.secondary">Đơn hàng hôm nay</Typography>
              <Typography variant="h2" mt={1}>
                0
              </Typography>
            </CardContent>
          </Card>
        </Grid>

        <Grid size={{ xs: 12, md: 4 }}>
          <Card>
            <CardContent>
              <Typography color="text.secondary">Doanh thu hôm nay</Typography>
              <Typography variant="h2" mt={1}>
                0 ₫
              </Typography>
            </CardContent>
          </Card>
        </Grid>

        <Grid size={{ xs: 12, md: 4 }}>
          <Card
            sx={{
              borderColor: lowStockCount > 0 ? "warning.main" : undefined,
              transition: "all 0.2s",
            }}
          >
            <CardActionArea
              component={RouterLink}
              to={ROUTES.adminInventory}
              data-testid="card-dashboard-low-stock"
            >
              <CardContent>
                <Box
                  display="flex"
                  justifyContent="space-between"
                  alignItems="center"
                >
                  <Typography color="text.secondary">
                    Sản phẩm sắp hết
                  </Typography>
                  {lowStockCount > 0 && (
                    <Chip
                      size="small"
                      color="warning"
                      icon={<WarningAmberOutlinedIcon />}
                      label="Cần nhập hàng"
                      data-testid="badge-low-stock-alert"
                    />
                  )}
                </Box>
                <Typography
                  variant="h2"
                  mt={1}
                  color={lowStockCount > 0 ? "warning.main" : "text.primary"}
                >
                  {lowStockCount}
                </Typography>
              </CardContent>
            </CardActionArea>
          </Card>
        </Grid>
      </Grid>
    </>
  );
}
