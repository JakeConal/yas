ALTER TABLE IF EXISTS "checkout"
    ALTER COLUMN customer_id DROP DEFAULT,
    ALTER COLUMN customer_id TYPE varchar(255) USING customer_id::varchar;

ALTER TABLE IF EXISTS "order"
    ALTER COLUMN customer_id DROP DEFAULT,
    ALTER COLUMN customer_id TYPE varchar(255) USING customer_id::varchar;
