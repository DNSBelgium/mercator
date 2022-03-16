"""Make booleans non-nullable

Revision ID: 6e9f2e96e17b
Revises: 78c0fb91ac1f
Create Date: 2022-02-21 11:18:17.378633

"""
from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision = '6e9f2e96e17b'
down_revision = '78c0fb91ac1f'
branch_labels = None
depends_on = None


def upgrade():
    op.execute("update ssl_crawl_result set support_tls_1_3 = false where support_tls_1_3 is null")
    op.execute("update ssl_crawl_result set support_tls_1_2 = false where support_tls_1_2 is null")
    op.execute("update ssl_crawl_result set support_tls_1_1 = false where support_tls_1_1 is null")
    op.execute("update ssl_crawl_result set support_tls_1_0 = false where support_tls_1_0 is null")
    op.execute("update ssl_crawl_result set support_ssl_2_0 = false where support_ssl_2_0 is null")
    op.execute("update ssl_crawl_result set support_ssl_3_0 = false where support_ssl_3_0 is null")
    op.execute("update ssl_crawl_result set support_ecdh_key_exchange = false where support_ecdh_key_exchange is null")

    op.execute("""alter table ssl_crawl_result alter column support_tls_1_3 set not null""")
    op.execute("""alter table ssl_crawl_result alter column support_tls_1_2 set not null""")
    op.execute("""alter table ssl_crawl_result alter column support_tls_1_1 set not null""")
    op.execute("""alter table ssl_crawl_result alter column support_tls_1_0 set not null""")
    op.execute("""alter table ssl_crawl_result alter column support_ssl_2_0 set not null""")
    op.execute("""alter table ssl_crawl_result alter column support_ssl_3_0 set not null""")
    op.execute("""alter table ssl_crawl_result alter column support_ecdh_key_exchange set not null""")

def downgrade():
    pass
