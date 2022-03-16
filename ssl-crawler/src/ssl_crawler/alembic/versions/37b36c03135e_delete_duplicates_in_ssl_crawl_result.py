"""delete duplicates in ssl_crawl_result

Revision ID: 37b36c03135e
Revises: e001a8037ed8
Create Date: 2022-01-10 17:18:00.078775

"""
from alembic import op

# revision identifiers, used by Alembic.
revision = '37b36c03135e'
down_revision = 'e001a8037ed8'
branch_labels = None
depends_on = None


def upgrade():
    op.execute("""
create table ssl_duplicates
as
with data as
         (
             select id
                  , visit_id
                  , ok
                  , lag(id) over (partition by visit_id)                     as prev_id
                  , lag(ok) over (partition by visit_id)                     as prev_ok
                  , lead(id) over (partition by visit_id)                    as next_id
                  , count(1) over (partition by visit_id)                    as count
                  , rank() over (partition by visit_id order by ok desc, id) as rank
             from ssl_crawl_result r
         )
select *
from data
where rank > 1
;
    """)

    op.execute("""
delete
from check_against_trust_store
where certificate_deployment_id
          in (
          select cd.id
          from ssl_duplicates dup
                   join certificate_deployment cd on cd.ssl_crawl_result_id = dup.id
      )
;
    """)

    op.execute("""
delete
from cipher_suite_support
where ssl_crawl_result_id in (
    select id
    from ssl_duplicates
);
    """)

    op.execute("""
delete
from curve_support
where ssl_crawl_result_id in (
    select id
    from ssl_duplicates
);
    """)

    op.execute("""
delete
from certificate_deployment
where ssl_crawl_result_id in (
    select id
    from ssl_duplicates
);
    """)

    op.execute("""
delete
from ssl_crawl_result
where id in (
    select id
    from ssl_duplicates
);
    """)

    op.execute("""
drop table ssl_duplicates;
    """)

def downgrade():
    pass
