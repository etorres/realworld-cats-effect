CREATE TABLE users (
    user_id SERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL CONSTRAINT users_email_must_be_different UNIQUE,
    username VARCHAR(50) NOT NULL CONSTRAINT users_username_must_be_different UNIQUE,
    password CHAR(159) NOT NULL,
    bio TEXT,
    image TEXT
);