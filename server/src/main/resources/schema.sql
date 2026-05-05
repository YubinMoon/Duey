CREATE TABLE IF NOT EXISTS users (
    id TEXT PRIMARY KEY,
    google_subject TEXT NOT NULL UNIQUE,
    email TEXT NOT NULL,
    name TEXT,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS categories (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL,
    name TEXT NOT NULL,
    sort_order INTEGER NOT NULL,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL,
    deleted_at TEXT,
    UNIQUE(user_id, name),
    FOREIGN KEY(user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS todos (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL,
    title TEXT NOT NULL,
    description TEXT NOT NULL,
    category_id TEXT,
    start_date TEXT NOT NULL,
    end_date TEXT NOT NULL,
    is_completed INTEGER NOT NULL,
    recurring_template_id TEXT,
    recurring_occurrence_date TEXT,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL,
    deleted_at TEXT,
    FOREIGN KEY(user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS recurring_templates (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL,
    title TEXT NOT NULL,
    description TEXT NOT NULL,
    category_id TEXT,
    repeat_start_date TEXT NOT NULL,
    repeat_end_date TEXT NOT NULL,
    repeat_type TEXT NOT NULL,
    weekly_days TEXT NOT NULL,
    monthly_day INTEGER NOT NULL,
    period_length_days INTEGER NOT NULL,
    last_generated_until TEXT,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL,
    deleted_at TEXT,
    FOREIGN KEY(user_id) REFERENCES users(id)
);
