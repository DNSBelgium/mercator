"""add hostname and constraints to ssl_crawl_result

Revision ID: 78c0fb91ac1f
Revises: 37b36c03135e
Create Date: 2022-01-10 17:23:52.555437

"""
from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision = '78c0fb91ac1f'
down_revision = '37b36c03135e'
branch_labels = None
depends_on = None


def upgrade():
    op.execute("""
alter table ssl_crawl_result
add column hostname varchar(256);
    """)

    op.execute("""
update ssl_crawl_result
set hostname = domain_name;
    """)

    op.execute("""
alter table ssl_crawl_result
add unique (visit_id, hostname);
    """)

    op.execute("""
alter table ssl_crawl_result
alter column hostname set not null; 
    """)


def downgrade():
    pass
