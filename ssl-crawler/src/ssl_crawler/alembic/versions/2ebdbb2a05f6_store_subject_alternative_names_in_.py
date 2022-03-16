"""Store Subject Alternative Names in certificate

Revision ID: 2ebdbb2a05f6
Revises: 6e9f2e96e17b
Create Date: 2022-02-21 16:37:20.473001

"""
from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision = '2ebdbb2a05f6'
down_revision = '6e9f2e96e17b'
branch_labels = None
depends_on = None


def upgrade():
    op.execute("""alter table certificate add subject_alt_names jsonb""")


def downgrade():
    pass
