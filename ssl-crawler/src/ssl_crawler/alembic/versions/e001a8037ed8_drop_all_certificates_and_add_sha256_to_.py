"""drop all certificates and add sha256 to fix serial number not unique

Revision ID: e001a8037ed8
Revises: e3b9c157df1f
Create Date: 2022-01-03 13:06:22.788080

"""
from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision = 'e001a8037ed8'
down_revision = 'e3b9c157df1f'
branch_labels = None
depends_on = None


def upgrade():
    op.execute("""
alter table certificate_deployment drop column leaf_certificate_id;
delete from certificate;

alter table certificate drop column signed_by;
alter table certificate drop column id;

alter table certificate add column sha256_fingerprint varchar(256) primary key;
alter table certificate add column signed_by_sha256 varchar(256) references certificate;

alter table certificate_deployment add column leaf_certificate_sha256 varchar(256) references certificate;

alter table certificate drop constraint if exists certificate_serial_number_key;
    """)


def downgrade():
    pass
