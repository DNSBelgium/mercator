create or replace function migrate_smtp_data(input jsonb, visit_id_var uuid, domain_name_var varchar, crawl_timestamp timestamp with time zone, crawl_status_var integer)
    returns integer
    language plpgsql
as
$$
declare
    input_counter int;
    hosts_counter int;
    hosts jsonb;
    host jsonb;
    id_var integer;
    error_message_var varchar;
    error_var varchar;
    num_conversations_var int;
BEGIN
    input_counter := 0;
    num_conversations_var := 0;

    insert into test.smtp_visit(visit_id, domain_name, timestamp, num_conversations, crawl_status)
    values (visit_id_var, domain_name_var, crawl_timestamp, 0, crawl_status_var);

    loop
        exit when input_counter = jsonb_array_length(input);
        hosts := (input -> input_counter) #>> '{hosts}';
        hosts_counter := 0;
        loop
            exit when hosts_counter = jsonb_array_length(hosts);
            host := (hosts -> hosts_counter);
            error_message_var := null;
            select c.id into id_var from test.smtp_conversation c where c.ip = host ->> 'ip' and c.timestamp > (crawl_timestamp - interval '1 day');
            if id_var is  null then
                if (host ->> 'errorMessage') is not null then
                    case
                        when host ->> 'errorMessage' like 'connection timed out: %' then error_message_var := 'Connection timed out';
                        when host ->> 'errorMessage' like 'NotAfter: %' then error_message_var := 'NotAfter';
                        when host ->> 'errorMessage' like '% Timed out waiting for a response to%' then error_message_var := 'Timed out waiting for a response';
                        when host ->> 'errorMessage' like 'Received fatal alert:%' then error_message_var := 'Received fatal alert';
                        when host ->> 'errorMessage' like 'not an SSL/TLS record: %' then error_message_var := 'Not an SSL/TLS record';
                        when host ->> 'errorMessage' like 'Received invalid line: %' then error_message_var := 'Received invalid line';
                        when host ->> 'errorMessage' like 'The size of the handshake message %' then error_message_var := 'Handshake message size exceeds maximum';
                        when host ->> 'errorMessage' like 'Usage constraint TLSServer check failed: %' then error_message_var := 'Usage constraint TLSServer check failed';
                        else
                            error_message_var := host ->> 'errorMessage';
                        end case;
                    case
                        when lower(error_message_var) like '%timed out%' then error_var := 'TIME_OUT';
                        when lower(error_message_var) like '%connection%' then error_var := 'CONNECTION_ERROR';
                        when lower(error_message_var) like '%skipped%' then error_var := 'SKIPPED';
                        when lower(error_message_var) like '%notafter%' or
                             lower(error_message_var) like '%tls%' or
                             lower(error_message_var) like '%ssl%' or
                             lower(error_message_var) like '%certificate%' or
                             lower(error_message_var) like '%handshake message%' or
                             lower(error_message_var) like '%unable to find valid certification path%'
                            then error_var := 'CERTIFICATE_ERROR';
                        when lower(error_message_var) like '%unreachable%' then error_var := 'HOST_UNREACHABLE';
                        when lower(error_message_var) like '%channel%' then error_var := 'CHANNEL_CLOSED';
                        else
                            error_var := 'OTHER';
                        end case;
                end if;
                insert into test.smtp_conversation(ip,
                                                   asn,
                                                   country,
                                                   asn_organisation,
                                                   banner,
                                                   connect_ok,
                                                   connect_reply_code,
                                                   ip_version,
                                                   start_tls_ok,
                                                   start_tls_reply_code,
                                                   error_message,
                                                   error,
                                                   connection_time_ms,
                                                   software,
                                                   software_version,
                                                   timestamp,
                                                   extensions)
                values (host ->> 'ip',
                        cast(host ->> 'asn' as bigint),
                        host ->> 'country',
                        host ->> 'asnOrganisation',
                        host ->> 'banner',
                        cast(host ->> 'connectOK' as boolean),
                        cast(host ->> 'connectReplyCode' as integer),
                        cast(host ->> 'ipVersion' as smallint),
                        cast(host ->> 'startTlsOk' as boolean),
                        cast(host ->> 'startTlsReplyCode' as integer),
                        error_message_var,
                        error_var,
                        cast(host ->> 'connectionTimeMs' as bigint),
                        host ->> 'software',
                        host ->> 'softwareVersion',
                        crawl_timestamp,
                        cast(host ->> 'supportedExtensions' as jsonb))
                returning smtp_conversation.id into id_var;
            end if;
            insert into test.smtp_host(visit_id,
                                       from_mx,
                                       host_name,
                                       priority,
                                       conversation,
                                       status)
            values (visit_id_var,
                    null,
                    (input -> input_counter) ->> 'hostName',
                    cast((input -> input_counter) ->> 'priority' as integer),
                    id_var,
                    null);

            num_conversations_var := num_conversations_var + 1;
            hosts_counter := hosts_counter + 1;
        end loop;

        input_counter := input_counter + 1;
    end loop;

    update test.smtp_visit set num_conversations = num_conversations_var where visit_id = visit_id_var;

    return id_var;
END;
$$;

select migrate_smtp_data(servers, visit_id, domain_name, crawl_timestamp, crawl_status), servers
from smtp_crawler.smtp_crawl_result;