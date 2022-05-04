-- New table request will take over dns_crawl_result.
CREATE TABLE dns_crawler.request
(
    id              SERIAL PRIMARY KEY,
    visit_id        UUID                     NOT NULL,
    domain_name     VARCHAR(128)             NOT NULL, -- abc.be
    prefix          VARCHAR(63)              NOT NULL, -- @, www, _dmarc
    record_type     CHAR(10)                 NOT NULL, -- A, AAAA, NS, ...
    rcode           INT,                               -- Success code for the requested record.
    crawl_timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    ok              BOOLEAN,
    problem         TEXT
);

-- New table response will take over dns_crawl_result's Json data.
CREATE TABLE dns_crawler.response
(
    id          SERIAL PRIMARY KEY,
    record_data TEXT   NOT NULL, -- 94.126.48.90, s1.named.be., ...
    ttl         INT,             -- time-to-live
    request_id  INT
);

ALTER TABLE dns_crawler.response -- Adding foreign key constraint.
    ADD CONSTRAINT dns_response_request_id_fk FOREIGN KEY (request_id) REFERENCES dns_crawler.request(id);

-- Adjusting dns_crawl_result_geo_ips
ALTER TABLE dns_crawler.dns_crawl_result_geo_ips
    RENAME TO response_geo_ips;

-- Adding a response_id value to geo ip first.
-- After data transfer is complete, dns_crawl_result_id (which is currently the FK) will be removed.
-- Then response_id will be altered to be the new FK.
ALTER TABLE dns_crawler.response_geo_ips
    ADD COLUMN response_id INT;

-- Adding ip_version to response_geo_ips to remove record_type later. (record_type is currently A / AAAA in stead of ip_version 4 / 6)
ALTER TABLE dns_crawler.response_geo_ips
    ADD COLUMN ip_version INT; -- A == 4, AAAA == 6

-- DO
-- $BODY$
--     DECLARE
--         _record_type         TEXT; -- underscored to solve ambiguous naming issues.
--         record_type_value    TEXT;
--         record_values_bundle JSONB;
--         prefix_values_bundle JSONB;
--
--         rec RECORD;
--
--         request_id      INT;
--         visit_id        UUID;
--         ok              BOOLEAN;
--         domain_name     VARCHAR(128);
--         prefix          VARCHAR(63);
--         crawl_timestamp TIMESTAMP WITH TIME ZONE;
--         problem         TEXT;
--
--         last_request_id     INT;
--         last_response_id    INT;
--     BEGIN
--         -- Initial loop, going through all records.
--         FOR rec IN SELECT * FROM dns_crawler.dns_crawl_result
--             LOOP
--                 request_id =        rec.id;
--                 visit_id =          rec.visit_id;
--                 ok =                rec.ok;
--                 domain_name =       rec.domain_name;
--                 crawl_timestamp =   rec.crawl_timestamp;
--                 problem =           rec.problem;
--
--                 -- Second loop, catching @, www, ... Prefixes.
--
--                 IF problem = 'no data in Nominet'
--                 THEN
--                     INSERT INTO dns_crawler.request
--                     (VISIT_ID, DOMAIN_NAME, PREFIX, RECORD_TYPE, RCODE, CRAWL_TIMESTAMP, OK, PROBLEM)
--                     VALUES
--                         (visit_id, domain_name, '@', 'A', null, crawl_timestamp, ok, problem);
--                 ELSIF problem = 'nxdomain'
--                 THEN
--                     INSERT INTO dns_crawler.request
--                     (VISIT_ID, DOMAIN_NAME, PREFIX, RECORD_TYPE, RCODE, CRAWL_TIMESTAMP, OK, PROBLEM)
--                     VALUES
--                         (visit_id, domain_name, '@', 'A', 3, crawl_timestamp, ok, 'NXDOMAIN');
--                 ELSIF problem = 'SERVFAIL'
--                 THEN
--                     INSERT INTO dns_crawler.request
--                     (VISIT_ID, DOMAIN_NAME, PREFIX, RECORD_TYPE, RCODE, CRAWL_TIMESTAMP, OK, PROBLEM)
--                     VALUES
--                         (visit_id, domain_name, '@', 'A', 2, crawl_timestamp, ok, problem);
--                 ELSIF problem = 'timed out'
--                 THEN
--                     INSERT INTO dns_crawler.request
--                     (VISIT_ID, DOMAIN_NAME, PREFIX, RECORD_TYPE, RCODE, CRAWL_TIMESTAMP, OK, PROBLEM)
--                     VALUES
--                         (visit_id, domain_name, '@', 'A', null, crawl_timestamp, ok, problem);
--                 ELSE
--                     FOR prefix, prefix_values_bundle IN SELECT * FROM jsonb_each(rec.all_records)
--                         LOOP
--
--                             -- Catching A, MX, CAA, SOA, ... Records. (Json contains a "records" object, which we access with the -> operator)
--                             FOR _record_type, record_values_bundle IN SELECT * FROM jsonb_each(prefix_values_bundle->'records')
--                                 LOOP
--
--                                 -- Inserting values from dns_crawler_result into request.
--                                     INSERT INTO dns_crawler.request
--                                     (VISIT_ID, DOMAIN_NAME, PREFIX, RECORD_TYPE, RCODE, CRAWL_TIMESTAMP, OK, PROBLEM)
--                                     VALUES
--                                         (visit_id, domain_name, prefix, _record_type, 0, crawl_timestamp, ok, problem)
--                                     RETURNING id INTO last_request_id;
--                                     -- Catching the last ID that was created from sequence.
--
--                                     -- Final loop - unpacking A, MX, CAA, ... Record types.
--                                     IF record_values_bundle IS NOT NULL AND jsonb_array_length(record_values_bundle) > 0
--                                     THEN
--
--                                         FOR record_type_value IN SELECT * FROM jsonb_array_elements_text(record_values_bundle)
--                                             LOOP
--
--                                             -- Inserting unpacked JSONB into the new table.
--                                             -- last_request_id is the ID of the last request row that was created.
--
--                                                 INSERT INTO dns_crawler.response
--                                                 (record_data, ttl, request_id)
--                                                 VALUES
--                                                     (record_type_value, NULL, last_request_id)
--                                                 RETURNING id into last_response_id;
--
--                                                 IF _record_type = 'A' OR _record_type = 'AAAA'
--                                                 THEN
--
--                                                     UPDATE dns_crawler.response_geo_ips ip
--                                                     SET response_id = last_response_id
--                                                     WHERE ip.dns_crawl_result_id = request_id AND ip.record_type = _record_type;
--
--                                                 END IF;
--                                             END LOOP;
--                                     END IF;
--                                 END LOOP;
--                         END LOOP;
--                 end if;
--             END LOOP;
--     END
-- $BODY$ LANGUAGE plpgsql;

ALTER TABLE dns_crawler.response_geo_ips
DROP CONSTRAINT geo_ip_dns_crawl_result_id_fk;

ALTER TABLE dns_crawler.response_geo_ips
    ADD CONSTRAINT geo_ip_response_id_fk FOREIGN KEY (response_id) REFERENCES dns_crawler.response(id);

-- Removing deprecated columns.
ALTER TABLE dns_crawler.response_geo_ips
DROP COLUMN dns_crawl_result_id;

ALTER TABLE dns_crawler.response_geo_ips
    ALTER COLUMN ip_version SET NOT NULL;

ALTER TABLE IF EXISTS dns_crawler.response_geo_ips
DROP COLUMN record_type;

-- Add primary key to response_geo_ips
ALTER TABLE dns_crawler.response_geo_ips
    ADD COLUMN IF NOT EXISTS id SERIAL PRIMARY KEY;

-- Destroy dns_crawl_result table
DROP TABLE IF EXISTS dns_crawler.dns_crawl_result;