-- The V3 bootstrap account had public credentials. It is retained only for
-- migration history and is never permitted to authenticate after this release.
UPDATE users
SET status = 'LOCKED',
    updated_at = CURRENT_TIMESTAMP
WHERE LOWER(email) = 'admin@techstore.local'
  AND status <> 'LOCKED';
