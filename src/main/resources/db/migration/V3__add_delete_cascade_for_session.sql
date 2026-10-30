ALTER TABLE sessions
DROP CONSTRAINT fk_campaign_admin; -- or whatever your existing constraint name is

ALTER TABLE sessions
ADD CONSTRAINT fk_campaign_admin
FOREIGN KEY (admin_id) REFERENCES admin_accounts(id)
ON DELETE CASCADE;