CREATE TABLE user_accounts (
    id text PRIMARY KEY,
    email text NOT NULL UNIQUE,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE otp_challenges (
    id bigserial PRIMARY KEY,
    email text NOT NULL,
    code_digest text NOT NULL,
    created_at timestamptz NOT NULL,
    expires_at timestamptz NOT NULL,
    attempts integer NOT NULL DEFAULT 0,
    consumed_at timestamptz,
    request_ip text NOT NULL
);
CREATE INDEX otp_email_recent ON otp_challenges (email, created_at DESC);
CREATE INDEX otp_ip_recent ON otp_challenges (request_ip, created_at DESC);

CREATE TABLE sessions (
    id text PRIMARY KEY,
    user_id text NOT NULL REFERENCES user_accounts(id) ON DELETE CASCADE,
    token_digest text NOT NULL UNIQUE,
    created_at timestamptz NOT NULL,
    expires_at timestamptz NOT NULL,
    revoked_at timestamptz
);
CREATE INDEX sessions_user_id ON sessions (user_id);

CREATE TABLE profiles (
    user_id text PRIMARY KEY REFERENCES user_accounts(id) ON DELETE CASCADE,
    name text NOT NULL,
    city_id text NOT NULL,
    sharing_enabled boolean NOT NULL DEFAULT true,
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE rhythms (
    user_id text PRIMARY KEY REFERENCES user_accounts(id) ON DELETE CASCADE,
    value jsonb NOT NULL,
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE temporary_statuses (
    user_id text PRIMARY KEY REFERENCES user_accounts(id) ON DELETE CASCADE,
    available boolean NOT NULL,
    until_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE notes (
    user_id text PRIMARY KEY REFERENCES user_accounts(id) ON DELETE CASCADE,
    text text NOT NULL,
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE invitations (
    id text PRIMARY KEY,
    inviter_id text NOT NULL REFERENCES user_accounts(id) ON DELETE CASCADE,
    code_digest text NOT NULL UNIQUE,
    created_at timestamptz NOT NULL,
    expires_at timestamptz NOT NULL,
    revoked_at timestamptz,
    accepted_by text REFERENCES user_accounts(id),
    accepted_at timestamptz
);
CREATE INDEX invitations_inviter_active ON invitations (inviter_id, expires_at DESC)
    WHERE revoked_at IS NULL AND accepted_at IS NULL;

CREATE TABLE pairs (
    id text PRIMARY KEY,
    created_at timestamptz NOT NULL
);

CREATE TABLE pair_members (
    pair_id text NOT NULL REFERENCES pairs(id) ON DELETE CASCADE,
    user_id text NOT NULL UNIQUE REFERENCES user_accounts(id) ON DELETE CASCADE,
    PRIMARY KEY (pair_id, user_id)
);
CREATE INDEX pair_members_pair_id ON pair_members (pair_id);
