

# report duckdb bug
create table t as select current_timestamp ct, epoch_ms(current_timestamp) ems, make_timestamp(epoch_ms(current_timestamp)*1000) mt, epoch_ms(epoch_ms(current_timestamp)) ct2;

set time zone 'UTC' 
ct = ct2 => True

set time zone 'Europe/Brussels'
ct = ct2 => False

# check test coverage
* now: 73% class, 70% method, 65% line

# VisitRequest Message format
* dns.enabled
* dns.prefixes
* dns.rrtypes
* web.enabled
* web.max_pages
* screenshot
  * always
  * if_single_page_app
  * if_html_changed
  * if_developed
  * if_suspicious
* vat.enabled
* tls.enabled
* tls.tls13.enabled
* tls.tls12.enabled
* tls.tls11.enabled
* tls.tls10.enabled
* tls.ssl2.enabled
* tls.ssl3.enabled

