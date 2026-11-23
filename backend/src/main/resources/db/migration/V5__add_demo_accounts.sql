-- Demo sandboxes ("Try the demo"): accounts created on the fly that are deleted after a while.
-- NULL for every real account, so existing rows need no update.
ALTER TABLE users ADD COLUMN demo_expires_at TIMESTAMPTZ;

-- Partial index: only demo accounts are indexed, which keeps it tiny. It serves the clean-up
-- ("demo accounts that have expired") and the count of active demo accounts.
CREATE INDEX idx_users_demo_expires_at ON users (demo_expires_at) WHERE demo_expires_at IS NOT NULL;
