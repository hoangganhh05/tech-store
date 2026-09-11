package com.techstore.service;

import com.techstore.dto.request.AddToCartRequest;
import com.techstore.dto.request.UpdateCartItemRequest;
import com.techstore.entity.Cart;
import com.techstore.entity.CartItem;
import com.techstore.entity.Inventory;
import com.techstore.entity.Product;
import com.techstore.entity.ProductVariant;
import com.techstore.enums.ProductStatus;
import com.techstore.enums.VariantStatus;
import com.techstore.exception.BusinessException;
import com.techstore.repository.CartItemRepository;
import com.techstore.repository.CartRepository;
import com.techstore.repository.InventoryRepository;
import com.techstore.repository.ProductImageRepository;
import com.techstore.repository.ProductVariantRepository;
import com.techstore.repository.UserRepository;
import com.techstore.service.impl.CartServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CartServiceImplTest {
    private final CartRepository carts = mock(CartRepository.class);
    private final CartItemRepository items = mock(CartItemRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final ProductVariantRepository variants = mock(ProductVariantRepository.class);
    private final InventoryRepository inventories = mock(InventoryRepository.class);
    private final ProductImageRepository images = mock(ProductImageRepository.class);
    private final PromotionService promotions = mock(PromotionService.class);
    private final CartServiceImpl service = new CartServiceImpl(carts, items, users, variants, inventories, images, promotions);

    private final Cart cart = new Cart();
    private final Product product = mock(Product.class);
    private final ProductVariant variant = mock(ProductVariant.class);
    private final Inventory inventory = new Inventory(variant, 6, 2, 5);
    private final List<CartItem> storedItems = new ArrayList<>();

    @BeforeEach
    void setUp() throws Exception {
        setId(cart, 10L);
        cart.setItems(storedItems);
        when(carts.findByUserId(7L)).thenReturn(Optional.of(cart));
        when(variant.getId()).thenReturn(101L);
        when(variant.getProduct()).thenReturn(product);
        when(variant.getPrice()).thenReturn(new BigDecimal("100000"));
        when(variant.getOriginalPrice()).thenReturn(new BigDecimal("120000"));
        when(variant.getSku()).thenReturn("PHONE-101");
        when(variant.getColor()).thenReturn("Black");
        when(variant.getStorage()).thenReturn("128GB");
        when(variant.getStatus()).thenReturn(VariantStatus.ACTIVE);
        when(variant.isDeleted()).thenReturn(false);
        when(variants.findById(101L)).thenReturn(Optional.of(variant));
        when(product.getId()).thenReturn(11L);
        when(product.getName()).thenReturn("Phone");
        when(product.getStatus()).thenReturn(ProductStatus.ACTIVE);
        when(product.isDeleted()).thenReturn(false);
        when(inventories.findByVariantId(101L)).thenReturn(Optional.of(inventory));
        when(images.findByVariantId(101L)).thenReturn(List.of());
        when(images.findByProductIdOrderByIsPrimaryDescDisplayOrderAscIdAsc(11L)).thenReturn(List.of());
        when(promotions.getEffectivePrices(any())).thenReturn(java.util.Map.of());
        when(items.findByCartId(10L)).thenReturn(storedItems);
        when(items.save(any(CartItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void addsNewItemAndCalculatesSubtotalAndShipping() {
        var response = service.addToCart(7L, null, new AddToCartRequest(101L, 2));

        assertThat(response.totalItems()).isEqualTo(2);
        assertThat(response.subtotal()).isEqualByComparingTo("200000");
        assertThat(response.shippingFee()).isEqualByComparingTo("30000");
        assertThat(response.total()).isEqualByComparingTo("230000");
        assertThat(response.canCheckout()).isTrue();
        assertThat(storedItems).hasSize(1).first().extracting(CartItem::getQuantity).isEqualTo(2);
        verify(items).save(any(CartItem.class));
    }

    @Test
    void incrementsExistingItemAndRejectsQuantityBeyondAvailableStock() throws Exception {
        CartItem existing = new CartItem(cart, variant, 2);
        setId(existing, 20L);
        storedItems.add(existing);
        when(items.findByCartIdAndVariantId(10L, 101L)).thenReturn(Optional.of(existing));

        var response = service.addToCart(7L, null, new AddToCartRequest(101L, 2));
        assertThat(response.totalItems()).isEqualTo(4);
        assertThat(existing.getQuantity()).isEqualTo(4);

        assertThatThrownBy(() -> service.addToCart(7L, null, new AddToCartRequest(101L, 1)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("vượt quá tồn kho");
        verify(items, times(1)).save(existing);
    }

    @Test
    void updatesAndRemovesOnlyItemsBelongingToCurrentCart() throws Exception {
        CartItem existing = new CartItem(cart, variant, 2);
        setId(existing, 20L);
        storedItems.add(existing);
        when(items.findById(20L)).thenReturn(Optional.of(existing));

        var response = service.updateCartItemQuantity(7L, null, 20L, new UpdateCartItemRequest(3));
        assertThat(existing.getQuantity()).isEqualTo(3);
        assertThat(response.totalItems()).isEqualTo(3);

        service.removeCartItem(7L, null, 20L);
        verify(items).delete(existing);
        assertThat(storedItems).isEmpty();
    }

    private static void setId(Object entity, long id) throws Exception {
        Field field = entity.getClass().getDeclaredField("id");
        field.setAccessible(true);
        field.set(entity, id);
    }
}
