PRAGMA foreign_keys = ON;
PRAGMA journal_mode = WAL;
PRAGMA busy_timeout = 5000;

CREATE TABLE IF NOT EXISTS restaurant (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    district TEXT NOT NULL,
    business_area TEXT,
    address TEXT,
    average_price REAL NOT NULL DEFAULT 0 CHECK (average_price >= 0),
    cuisine TEXT NOT NULL,
    rating REAL NOT NULL DEFAULT 0 CHECK (rating >= 0 AND rating <= 5),
    taste_tags TEXT NOT NULL DEFAULT '[]',
    scene_tags TEXT NOT NULL DEFAULT '[]',
    avoid_tags TEXT NOT NULL DEFAULT '[]',
    recommended_dishes TEXT NOT NULL DEFAULT '[]',
    description TEXT,
    latitude REAL,
    longitude REAL,
    cover_image TEXT,
    source TEXT NOT NULL DEFAULT 'AMAP_REALTIME',
    source_note TEXT,
    is_demo_data INTEGER NOT NULL DEFAULT 0 CHECK (is_demo_data IN (0, 1)),
    status INTEGER NOT NULL DEFAULT 1 CHECK (status IN (0, 1)),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS recommendation_history (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    request_json TEXT NOT NULL,
    result_json TEXT NOT NULL,
    trigger_type TEXT NOT NULL DEFAULT 'NORMAL'
        CHECK (trigger_type IN ('NORMAL', 'SHAKE_DEVICE', 'SHAKE_BUTTON', 'TASTE_MIRROR')),
    selected_restaurant_id INTEGER,
    client_trigger TEXT
        CHECK (client_trigger IS NULL OR client_trigger IN ('DEVICE_MOTION', 'BUTTON_FALLBACK')),
    ai_used INTEGER NOT NULL DEFAULT 0 CHECK (ai_used IN (0, 1)),
    ai_fallback INTEGER NOT NULL DEFAULT 0 CHECK (ai_fallback IN (0, 1)),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (selected_restaurant_id) REFERENCES restaurant(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS user (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    nickname TEXT,
    role TEXT NOT NULL DEFAULT 'USER' CHECK (role IN ('USER', 'ADMIN')),
    status INTEGER NOT NULL DEFAULT 1 CHECK (status IN (0, 1)),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS favorite (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER,
    restaurant_id INTEGER NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, restaurant_id),
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    FOREIGN KEY (restaurant_id) REFERENCES restaurant(id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_restaurant_name_address
ON restaurant(name, COALESCE(address, ''));

CREATE INDEX IF NOT EXISTS idx_restaurant_status_district
ON restaurant(status, district);

CREATE INDEX IF NOT EXISTS idx_restaurant_status_business_area
ON restaurant(status, business_area);

CREATE INDEX IF NOT EXISTS idx_restaurant_status_cuisine
ON restaurant(status, cuisine);

CREATE INDEX IF NOT EXISTS idx_restaurant_status_price
ON restaurant(status, average_price);

CREATE INDEX IF NOT EXISTS idx_restaurant_rating
ON restaurant(rating);

CREATE INDEX IF NOT EXISTS idx_recommendation_history_trigger
ON recommendation_history(trigger_type, created_at);

CREATE INDEX IF NOT EXISTS idx_favorite_restaurant
ON favorite(restaurant_id);
