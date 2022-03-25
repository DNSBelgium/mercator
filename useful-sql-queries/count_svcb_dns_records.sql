SELECT count(*), json_array_length(result.all_records::json->'@'->'records'->'SVCB') as number_svcb_record
FROM dns_crawler.dns_crawl_result result,
     dispatcher.dispatcher_event_labels labels
where result.visit_id = labels.visit_id
group by number_svcb_record