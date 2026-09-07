import { useState, useEffect, useCallback, type ReactNode } from 'react'
import { getCart, addToCart as apiAddToCart, updateCartItemQuantity as apiUpdateCartItemQuantity, type Cart } from '../../services/cartService'
import { useAuth } from '../../hooks/useAuth'
import { CartContext } from './CartStore'

export function CartProvider({ children }: { children: ReactNode }) {
  const [cart, setCart] = useState<Cart | null>(null)
  const [loading, setLoading] = useState<boolean>(false)
  const { user } = useAuth()

  const refreshCart = useCallback(async () => {
    try {
      const data = await getCart()
      setCart(data)
    } catch {
      // Ignore initial get error if network fails
    }
  }, [])

  useEffect(() => {
    refreshCart()
  }, [refreshCart, user])

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

  const cartCount = cart?.totalItems ?? 0

  return (
    <CartContext.Provider value={{ cart, cartCount, loading, addToCart, updateQuantity, refreshCart }}>
      {children}
    </CartContext.Provider>
  )
}
