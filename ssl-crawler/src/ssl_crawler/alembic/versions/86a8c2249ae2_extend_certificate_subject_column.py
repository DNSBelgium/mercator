"""Extend certificate.subject column

Revision ID: 86a8c2249ae2
Revises: 2ebdbb2a05f6
Create Date: 2022-03-03 10:24:26.269689

"""
from alembic import op


# revision identifiers, used by Alembic.
revision = '86a8c2249ae2'
down_revision = '2ebdbb2a05f6'
branch_labels = None
depends_on = None


def upgrade():
    op.execute("""alter table certificate alter subject type varchar(500)""")


def downgrade():
    pass
