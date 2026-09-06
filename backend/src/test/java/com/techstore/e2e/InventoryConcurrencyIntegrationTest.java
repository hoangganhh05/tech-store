package com.techstore.e2e;

import com.techstore.dto.request.OrderInventoryDeductionRequest;
import com.techstore.dto.request.OrderInventoryRestoreRequest;
import com.techstore.dto.request.OrderItemStockRequest;
import com.techstore.entity.Brand;
import com.techstore.entity.Category;
import com.techstore.entity.Inventory;
import com.techstore.entity.Product;
import com.techstore.entity.ProductVariant;
import com.techstore.entity.Role;
import com.techstore.entity.User;
import com.techstore.enums.ErrorCode;
import com.techstore.enums.InventoryTransactionType;
import com.techstore.enums.ProductStatus;
import com.techstore.enums.RoleCode;
import com.techstore.enums.VariantStatus;
import com.techstore.exception.BusinessException;
import com.techstore.repository.BrandRepository;
import com.techstore.repository.CategoryRepository;
import com.techstore.repository.InventoryRepository;
import com.techstore.repository.InventoryTransactionRepository;
import com.techstore.repository.PasswordResetTokenRepository;
import com.techstore.repository.ProductImageRepository;
import com.techstore.repository.ProductRepository;
import com.techstore.repository.ProductSpecificationRepository;
import com.techstore.repository.ProductVariantRepository;
import com.techstore.repository.RefreshTokenRepository;
import com.techstore.repository.RoleRepository;
import com.techstore.repository.UserRepository;
import com.techstore.service.InventoryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles("test")
@SpringBootTest
class InventoryConcurrencyIntegrationTest {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventoryTransactionRepository inventoryTransactionRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private ProductVariantRepository productVariantRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private ProductSpecificationRepository productSpecificationRepository;

    @Autowired
    private ProductImageRepository productImageRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User admin;
    private ProductVariant flashSaleVariant;

    @BeforeEach
    void setUp() {
        cleanDatabase();

        Role adminRole = roleRepository.findByCode(RoleCode.ADMIN)
                .orElseGet(() -> roleRepository.save(new Role(RoleCode.ADMIN, "Quản trị viên")));

        admin = new User("admin@techstore.com", passwordEncoder.encode("Admin@123"), "Admin User", "0900000001");
        admin.addRole(adminRole);
        admin = userRepository.save(admin);

        Category category = categoryRepository.save(new Category("Điện thoại", "Điện thoại thông minh", null, null));
        Brand brand = brandRepository.save(new Brand("Apple", "Thương hiệu Apple", null));

        Product product = new Product("iPhone 16 Pro Max", "Flagship Apple", brand, category, ProductStatus.ACTIVE);
        product = productRepository.save(product);

        // Flash sale variant with exactly 10 units in stock
        flashSaleVariant = new ProductVariant(
                product, "IP16PM-FLASH-10", "Titan Tự Nhiên", "256GB",
                new BigDecimal("32990000"), new BigDecimal("34990000"), 10, VariantStatus.ACTIVE
        );
        flashSaleVariant = productVariantRepository.save(flashSaleVariant);

        Inventory inventory = new Inventory(flashSaleVariant, 10, 0, 5);
        inventoryRepository.save(inventory);
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    private void cleanDatabase() {
        inventoryTransactionRepository.deleteAll();
        inventoryRepository.deleteAll();
        productSpecificationRepository.deleteAll();
        productImageRepository.deleteAll();
        productVariantRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        brandRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        passwordResetTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("T-04.4.3 (QA): Đặt hàng đồng thời 10 luồng - mỗi luồng 2 chiếc trên kho 10 chiếc -> đúng 5 đơn thành công, 5 đơn thất bại, tồn kho = 0")
    void testConcurrentOrderDeduction_preventsOverselling_andLeavesExactBalance() throws InterruptedException {
        int threadCount = 10;
        int qtyPerOrder = 2; // Total requested = 20, but stock is only 10 -> exactly 5 succeed, 5 fail

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger insufficientStockCount = new AtomicInteger(0);
        AtomicInteger unexpectedErrorCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final long orderId = 1000L + i;
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await(); // Wait for all threads to be ready
                    inventoryService.deductInventoryForOrder(
                            admin.getId(),
                            new OrderInventoryDeductionRequest(
                                    orderId,
                                    "ORD-" + orderId,
                                    List.of(new OrderItemStockRequest(flashSaleVariant.getId(), qtyPerOrder)),
                                    "Đơn hàng flash sale " + orderId
                            )
                    );
                    successCount.incrementAndGet();
                } catch (BusinessException ex) {
                    if (ex.getErrorCode() == ErrorCode.INSUFFICIENT_STOCK) {
                        insufficientStockCount.incrementAndGet();
                    } else {
                        unexpectedErrorCount.incrementAndGet();
                    }
                } catch (Exception ex) {
                    unexpectedErrorCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown(); // Fire all 10 threads concurrently
        boolean finished = doneLatch.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(finished).isTrue();
        assertThat(unexpectedErrorCount.get()).isZero();
        assertThat(successCount.get()).isEqualTo(5);
        assertThat(insufficientStockCount.get()).isEqualTo(5);

        // Verify stock in database is exactly 0 (no overselling, never negative)
        Inventory finalInventory = inventoryRepository.findByVariantId(flashSaleVariant.getId()).orElseThrow();
        assertThat(finalInventory.getQuantityOnHand()).isEqualTo(0);
        assertThat(finalInventory.getAvailableQuantity()).isEqualTo(0);

        ProductVariant finalVariant = productVariantRepository.findById(flashSaleVariant.getId()).orElseThrow();
        assertThat(finalVariant.getStockQuantity()).isEqualTo(0);

        // Verify transactions: exactly 5 SALE transactions of -2 each
        var transactions = inventoryTransactionRepository.findAll();
        assertThat(transactions).hasSize(5);
        for (var tx : transactions) {
            assertThat(tx.getTransactionType()).isEqualTo(InventoryTransactionType.SALE);
            assertThat(tx.getQuantityChange()).isEqualTo(-2);
            assertThat(tx.getReferenceType()).isEqualTo("ORDER");
        }
    }

    @Test
    @DisplayName("T-04.4.2 (Backend): Hoàn tồn kho khi đơn hàng bị huỷ -> hoàn đúng số lượng và ghi nhận CANCEL_RETURN")
    void testOrderCancellation_restoresInventoryAndRecordsCancelReturn() {
        // First, deduct 4 units for order #501
        inventoryService.deductInventoryForOrder(
                admin.getId(),
                new OrderInventoryDeductionRequest(
                        501L,
                        "ORD-501",
                        List.of(new OrderItemStockRequest(flashSaleVariant.getId(), 4)),
                        "Đặt đơn 501"
                )
        );

        Inventory afterDeduct = inventoryRepository.findByVariantId(flashSaleVariant.getId()).orElseThrow();
        assertThat(afterDeduct.getQuantityOnHand()).isEqualTo(6);

        // Now cancel order #501 and restore 4 units
        inventoryService.restoreInventoryForOrder(
                admin.getId(),
                new OrderInventoryRestoreRequest(
                        501L,
                        "ORD-501",
                        List.of(new OrderItemStockRequest(flashSaleVariant.getId(), 4)),
                        "Khách hàng thay đổi nhu cầu huỷ đơn"
                )
        );

        Inventory afterRestore = inventoryRepository.findByVariantId(flashSaleVariant.getId()).orElseThrow();
        assertThat(afterRestore.getQuantityOnHand()).isEqualTo(10);
        assertThat(afterRestore.getAvailableQuantity()).isEqualTo(10);

        ProductVariant variantAfterRestore = productVariantRepository.findById(flashSaleVariant.getId()).orElseThrow();
        assertThat(variantAfterRestore.getStockQuantity()).isEqualTo(10);

        // Verify transactions: 1 SALE (-4) and 1 CANCEL_RETURN (+4)
        var transactions = inventoryTransactionRepository.findAll();
        assertThat(transactions).hasSize(2);

        var cancelTx = transactions.stream()
                .filter(t -> t.getTransactionType() == InventoryTransactionType.CANCEL_RETURN)
                .findFirst()
                .orElseThrow();
        assertThat(cancelTx.getQuantityChange()).isEqualTo(4);
        assertThat(cancelTx.getReferenceType()).isEqualTo("ORDER");
        assertThat(cancelTx.getReferenceId()).isEqualTo(501L);
        assertThat(cancelTx.getNote()).isEqualTo("Khách hàng thay đổi nhu cầu huỷ đơn");
    }

    @Test
    @DisplayName("T-04.4.1 (Backend): Đơn hàng nhiều sản phẩm, nếu có 1 sản phẩm không đủ hàng thì rollback toàn bộ")
    void testMultiVariantOrder_insufficientStockOnOneVariant_rollsBackEntirely() {
        // Create second variant with stock 1
        Product product = productRepository.findAll().get(0);
        ProductVariant variantLow = new ProductVariant(
                product, "IP16PM-LOW-1", "Titan Sa Mạc", "512GB",
                new BigDecimal("37990000"), new BigDecimal("39990000"), 1, VariantStatus.ACTIVE
        );
        variantLow = productVariantRepository.save(variantLow);
        inventoryRepository.save(new Inventory(variantLow, 1, 0, 5));

        final Long lowVariantId = variantLow.getId();
        final Long flashVariantId = flashSaleVariant.getId();

        // Order requests 2 flash sale items (has 10) AND 2 low stock items (has only 1)
        OrderInventoryDeductionRequest request = new OrderInventoryDeductionRequest(
                999L,
                "ORD-999",
                List.of(
                        new OrderItemStockRequest(flashVariantId, 2),
                        new OrderItemStockRequest(lowVariantId, 2)
                ),
                "Đơn hàng gồm 2 sản phẩm"
        );

        assertThatThrownBy(() -> inventoryService.deductInventoryForOrder(admin.getId(), request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INSUFFICIENT_STOCK);

        // Verify flashSaleVariant was NOT deducted (atomic transaction rollback)
        Inventory flashInv = inventoryRepository.findByVariantId(flashVariantId).orElseThrow();
        assertThat(flashInv.getQuantityOnHand()).isEqualTo(10);

        Inventory lowInv = inventoryRepository.findByVariantId(lowVariantId).orElseThrow();
        assertThat(lowInv.getQuantityOnHand()).isEqualTo(1);

        // No SALE transactions recorded
        assertThat(inventoryTransactionRepository.findAll()).isEmpty();
    }
}
