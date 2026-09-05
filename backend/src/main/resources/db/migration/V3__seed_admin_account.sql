-- V3: Seed initial admin account for administration access (US-01.8)
-- Default credentials:
-- Email: admin@techstore.local
-- Password: Admin@123456

INSERT INTO users (email, password_hash, full_name, phone, status, email_verified, created_at, updated_at)
VALUES (
    'admin@techstore.local',
    '$2a$10$0skPp2Q4.UoAz3TmVIXftec2RDfD.k8Klx6xWI7wQIUkXZhKZtEGy',
    'Quản trị viên hệ thống',
    '0909999999',
    'ACTIVE',
    TRUE,
    NOW(),
    NOW()
);

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.email = 'admin@techstore.local' AND r.code = 'ADMIN';
