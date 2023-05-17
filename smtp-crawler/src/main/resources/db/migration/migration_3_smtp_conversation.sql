delete from smtp_crawler.smtp_conversation;

insert into smtp_crawler.smtp_conversation
(ip, asn, country, asn_organisation, banner, connect_ok, connect_reply_code, ip_version, start_tls_ok, start_tls_reply_code, error_message, error, connection_time_ms, software, software_version, timestamp, extensions)
with error_message_converted as (
    select ip
        , asn
        , country
        , asn_organisation
        , banner
        , connect_ok
        , connect_reply_code
        , ip_version
        , start_tls_ok
        , start_tls_reply_code
        , case
              when error_message is null then null
              when error_message like 'connection timed out: %' then 'Connection timed out'
              when error_message like 'NotAfter: %' then 'NotAfter'
              when error_message like '% Timed out waiting for a response to%' then 'Timed out waiting for a response'
              when error_message like 'Received fatal alert:%' then 'Received fatal alert'
              when error_message like 'not an SSL/TLS record: %' then 'Not an SSL/TLS record'
              when error_message like 'Received invalid line: %' then 'Received invalid line'
              when error_message like 'The size of the handshake message %' then 'Handshake message size exceeds maximum'
              when error_message like 'Usage constraint TLSServer check failed: %' then 'Usage constraint TLSServer check failed'
              else
                  error_message
          end as error_message
        , connection_time_ms
        , software
        , software_version
        , timestamp
        , extensions
    from smtp_crawler.per_ip_per_day
    where rownr = 1
)
select
    ip
     , asn
     , country
     , asn_organisation
     , banner
     , connect_ok
     , connect_reply_code
     , ip_version
     , start_tls_ok
     , start_tls_reply_code
     , error_message
     , case
           when error_message is null
               then null
           when error_message = 'Connection timed out' or
                error_message = 'Timed out waiting for a response'
               then 'TIME_OUT'
           when error_message = 'Connection reset by peer' or
                error_message = 'Connection refused' or
                error_message = 'Connection reset'
               then 'CONNECTION_ERROR'
           when error_message = 'conversation with loopback address skipped' or
                error_message = 'conversation with site local address skipped' or
                error_message = 'conversation with IPv6 SMTP host skipped' or
                error_message = 'conversation with IPv4 SMTP host skipped'
               then 'SKIPPED'
           when error_message = 'NotAfter' or
                error_message = 'Not an SSL/TLS record' or
                error_message = 'Usage constraint TLSServer check failed' or
                error_message = 'Empty issuer DN not allowed in X509Certificates' or
                error_message = 'Handshake message size exceeds maximum' or
                error_message like 'handshake timed out after %' or
                error_message = 'unable to find valid certification path to requested target' or
                error_message like 'The server selected protocol version % is not accepted by client preferences %' or
                error_message = 'no more data allowed for version 1 certificate' or
                error_message like 'X.509 Certificate is incomplete:%'
               then 'TLS_ERROR'
           when error_message = 'No route to host' or
                error_message = 'Network is unreachable' or
                error_message = 'Host is unreachable' or
                error_message = 'Network unreachable'
               then  'HOST_UNREACHABLE'
           when error_message = 'ClosedChannelException' or
                error_message = 'channel was closed while waiting for response'
               then 'CHANNEL_CLOSED'
           else
               'OTHER'
    end
     , connection_time_ms
     , software
     , software_version
     , timestamp
     , extensions
from error_message_converted
;