-- inventories is the operational source of truth. Keep the legacy variant
-- field as a read projection for endpoints that return product variants.
UPDATE product_variants pv
JOIN inventories i ON i.variant_id = pv.id
SET pv.stock_quantity = GREATEST(i.quantity_on_hand - i.quantity_reserved, 0)
WHERE pv.stock_quantity <> GREATEST(i.quantity_on_hand - i.quantity_reserved, 0);
