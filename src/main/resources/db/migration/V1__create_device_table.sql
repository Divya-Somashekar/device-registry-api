CREATE TABLE device (
    id            UUID         PRIMARY KEY,
    name          VARCHAR(120) NOT NULL,
    brand         VARCHAR(120) NOT NULL,
    state         VARCHAR(20)  NOT NULL,
    creation_time TIMESTAMPTZ  NOT NULL
);

-- Both columns back a documented filter on the collection endpoint.
CREATE INDEX idx_device_brand ON device (brand);
CREATE INDEX idx_device_state ON device (state);
