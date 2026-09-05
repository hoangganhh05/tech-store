-- US-01.5: add the optional date of birth used by the personal profile.
USE techstore;

ALTER TABLE users
    ADD COLUMN date_of_birth DATE NULL AFTER phone;
