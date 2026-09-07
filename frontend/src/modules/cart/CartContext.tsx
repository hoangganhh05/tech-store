import { useState, useEffect, useCallback, useRef, type ReactNode } from 'react'
import {
  getCart,
  addToCart as apiAddToCart,
  updateCartItemQuantity as apiUpdateCartItemQuantity,
  removeCartItem as apiRemoveCartItem,
  syncCart as apiSyncCart,
  type Cart,
  type CartSyncResult,
} from '../../services/cartService'
import { getSessionId, rotateSessionId } from '../../utils/sessionStorage'
import { useAuth } from '../../hooks/useAuth'
import { CartContext } from './CartStore'

export function CartProvider({ children }: { children: ReactNode }) {
  const [cart, setCart] = useState<Cart | null>(null)
  const [loading, setLoading] = useState<boolean>(false)
  const [syncNotification, setSyncNotification] = useState<string | null>(null)
  const { user } = useAuth()
  const previousUserRef = useRef<typeof user>(null)

  const clearSyncNotification = useCallback(() => {
    setSyncNotification(null)
  }, [])

  const refreshCart = useCallback(async () => {
    try {
      const data = await getCart()
      setCart(data)
    } catch {
      // Ignore initial get error if network fails
    }
  }, [])

  const syncGuestCart = useCallback(async (): Promise<CartSyncResult | null> => {
    const guestSessionId = getSessionId()
    if (!guestSessionId) return null
    try {
      const res = await apiSyncCart(guestSessionId)
      if (res.mergedItemsCount > 0) {
        setCart(res.cart)
        rotateSessionId()
        if (res.hasStockAdjusted) {
          setSyncNotification('Giỏ hàng tạm đã được gộp. Một số sản phẩm được điều chỉnh theo tồn kho tối đa.')
        } else {
          setSyncNotification(res.message || `Đã gộp ${res.mergedItemsCount} sản phẩm từ giỏ hàng tạm.`)
        }
      } else {
        setCart(res.cart)
      }
      return res
    } catch {
      return null
    }
  }, [])

  useEffect(() => {
    const wasGuest = !previousUserRef.current
    const isNowLoggedIn = Boolean(user)
    const justLoggedOut = Boolean(previousUserRef.current) && !user
    previousUserRef.current = user

    if (wasGuest && isNowLoggedIn) {
      syncGuestCart().then((res) => {
        if (!res) {
          refreshCart()
        }
      })
    } else if (justLoggedOut) {
      rotateSessionId()
      refreshCart()
    } else {
      refreshCart()
    }
  }, [refreshCart, syncGuestCart, user])

  const addToCart = useCallback(async (variantId: number, quantity: number) => {
    setLoading(true)
    try {
      const updatedCart = await apiAddToCart({ variantId, quantity })
      setCart(updatedCart)
      return updatedCart
    } finally {
      setLoading(false)
    }
  }, [])

  const updateQuantity = useCallback(async (itemId: number, quantity: number) => {
    setLoading(true)
    try {
      const updatedCart = await apiUpdateCartItemQuantity(itemId, quantity)
      setCart(updatedCart)
      return updatedCart
    } finally {
      setLoading(false)
    }
  }, [])

  const removeCartItem = useCallback(async (itemId: number) => {
    setLoading(true)
    try {
      const updatedCart = await apiRemoveCartItem(itemId)
      setCart(updatedCart)
      return updatedCart
    } finally {
      setLoading(false)
    }
  }, [])

  const cartCount = cart?.totalItems ?? 0

  return (
    <CartContext.Provider
      value={{
        cart,
        cartCount,
        loading,
        syncNotification,
        clearSyncNotification,
        addToCart,
        updateQuantity,
        removeCartItem,
        refreshCart,
        syncGuestCart,
      }}
    >
      {children}
    </CartContext.Provider>
  )
}

