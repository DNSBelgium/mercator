with groups as (
    select case
               when (dmarc_txt_record::text ~ 'v=DMARC1') then
                   case
                       when (dmarc_txt_record::text ~ 'p=none') then 'none'
                       when (dmarc_txt_record::text ~ 'p=quarantine') then 'quarantine'
                       when (dmarc_txt_record::text ~ 'p=reject') then 'reject'
                       else 'no_policy'
                       end
               else 'not_dmarc'
               end p,
           labels.labels
    from dns_crawler.dns_crawl_result result,
         dispatcher.dispatcher_event_labels labels
             cross join lateral jsonb_array_elements(all_records -> '_dmarc' ->'records'-> 'TXT') dmarc_txt_record
where all_records -
    > '_dmarc' is not null
  and all_records -
    > '_dmarc' -
    >'records'-
    > 'TXT' != '[]'::jsonb
  and result.visit_id = labels.visit_id
    )
select p, count(*)
from groups
group by p