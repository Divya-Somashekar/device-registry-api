CREATE TABLE device (
    id            UUID         PRIMARY KEY,
    name          VARCHAR(120) NOT NULL,
    brand         VARCHAR(120) NOT NULL,
    state         VARCHAR(20)  NOT NULL,
    creation_time TIMESTAMPTZ  NOT NULL,

    -- Backs @Version on the entity. The domain rules are read-check-write, so Hibernate
    -- matches this column in the WHERE clause of every update and delete: a write built on a
    -- stale read then affects no rows and fails, instead of overwriting a newer one.
    version       BIGINT       NOT NULL DEFAULT 0
);

-- Brand matching is case-insensitive, so the generated predicate is upper(brand) = upper(?),
-- which a plain btree on brand cannot answer. The expression has to be indexed instead.
-- A btree is scannable on a prefix of its columns, so this one composite serves the brand
-- filter and the combined brand-and-state filter both.
CREATE INDEX idx_device_brand_state ON device (upper(brand), state);

-- state is the composite's trailing column, and a suffix is not scannable, so the state-only
-- filter needs its own index.
CREATE INDEX idx_device_state ON device (state);

-- The unfiltered collection is the most common query, ordered by creation time with the
-- primary key breaking ties. Matching that order here keeps the default listing off a sort.
CREATE INDEX idx_device_creation_time ON device (creation_time DESC, id DESC);
