-- Insert default to retrofit
ALTER TABLE content_crawl_result
    ADD COLUMN muppet_retries INTEGER not null default 0;

-- Remove default for new entries
ALTER TABLE content_crawl_result
    ALTER COLUMN muppet_retries DROP DEFAULT;