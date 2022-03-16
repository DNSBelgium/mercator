FROM python:3.9-slim@sha256:9fbdd2a1863384390d42ed34b454e81469bf4ac0572369105f780c10bc0d60cb

WORKDIR /usr/src/app

COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt

COPY src/ .

RUN apt-get update \
    && apt-get install -y libssl1.1 openssl \
    && rm -rf /var/lib/apt/lists/*

CMD [ "python", "ssl_crawler/main.py" ]
