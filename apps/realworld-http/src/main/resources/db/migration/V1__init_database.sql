CREATE TABLE users (
    user_id SERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL
        CONSTRAINT users_email_must_be_different UNIQUE,
    username VARCHAR(50) NOT NULL
        CONSTRAINT users_username_must_be_different UNIQUE,
    password CHAR(159) NOT NULL,
    bio TEXT,
    image TEXT
);

CREATE TABLE followers (
    user_id INTEGER NOT NULL
        REFERENCES users (user_id),
    follower_id INTEGER NOT NULL
        REFERENCES users (user_id),
    PRIMARY KEY (user_id, follower_id)
);

CREATE TABLE articles (
    article_id SERIAL PRIMARY KEY,
    slug TEXT NOT NULL UNIQUE,
    title TEXT NOT NULL UNIQUE,
    description TEXT,
    body TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    author_id INTEGER NOT NULL
        REFERENCES users (user_id)
);

CREATE TABLE tags (
    tag TEXT NOT NULL,
    article_id INTEGER NOT NULL
        REFERENCES articles (article_id) ON UPDATE CASCADE,
    PRIMARY KEY (tag, article_id)
);

CREATE TABLE favorites (
    profile_id INTEGER NOT NULL
        REFERENCES users (user_id),
    article_id INTEGER NOT NULL
        REFERENCES articles (article_id) ON UPDATE CASCADE,
    PRIMARY KEY (profile_id, article_id)
);