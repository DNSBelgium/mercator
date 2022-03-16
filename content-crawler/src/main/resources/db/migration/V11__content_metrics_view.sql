create or replace view content_crawler.metrics
as
select r.*
     , cast(metrics_json::json ->> 'Nodes' as int) as Nodes
     , cast(metrics_json::json  ->> 'Documents' as int) as documents
     , cast(metrics_json::json ->> 'Frames' as int) as frames
     , cast(metrics_json::json ->> 'JSEventListeners' as int) as JSEventListeners
     , cast(metrics_json::json ->> 'Timestamp'  as float) as timestamp
     , cast(metrics_json::json ->> 'LayoutCount' as int) as LayoutCount
     , cast(metrics_json::json ->> 'RecalcStyleCount' as int) RecalcStyleCount
     , cast(metrics_json::json ->> 'RecalcStyleDuration' as float) RecalcStyleDuration
     , cast(metrics_json::json ->> 'ScriptDuration' as float) as ScriptDuration
     , cast(metrics_json::json ->> 'LayoutDuration' as float) as LayoutDuration
     , cast(metrics_json::json ->> 'TaskDuration' as float) as TaskDuration
     , cast(metrics_json::json ->> 'JSHeapUsedSize' as int) JSHeapUsedSize
     , cast(metrics_json::json ->> 'JSHeapTotalSize' as int) JSHeapTotalSize
from content_crawler.content_crawl_result r
;