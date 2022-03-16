# SSL crawler module

## Setup

Create a new virtual environment with Python 3.9 and do a pip install of the requirements in `requirements.txt`

Below is shown how to create it via IntelliJ but you could also create the virtual environment using mkvirtualenv  

### IntelliJ  

#### To create the virtual environment via IntelliJ 

File | Project Structure | Platform Settings | SDKs  
Add Python SDK... | Virutalenv Environment  
* Select "New environment"
* Make sure to select a Python 3.9 interpreter as the Base interpreter  
* Do not inherit global site-package
* Save

#### To create a Run configuration via IntelliJ
Run | Edit configurations... | Add new run configuration | Python
* Give it a name  
* Script path: `mercator/ssl-crawler/src/ssl_crawler/main.py`  
* Make sure to choose the virtual environment previously created  
* Working directory: `mercator/ssl-crawler/src`  
* Save

If needed, activate the virtual environment: `source venv/bin/activate` 

Do `pip install -r requirements.txt`  

Run  

### Common pitfalls with IntelliJ

Here are some of the things that often got me searching around for a solution because of how IntelliJ deals with Python.

Make sure that 
* `mercator/ssl-crawler/src/ssl_crawler` is marked as *Sources Root* (blue folder icon)
* `mercator/ssl-crawler/src/test` is marked as *Test Sources Root* (green folder icon)
* _Add content roots to PYTHONPATH_ and _Add source roots to PYTHONPATH_ are both checked in your Run Configurations 

In your Run Configurations make sure that the following option is correctly set.  This will allow the tests to discover the modules.
* Working directory: `mercator/ssl-crawler/src`

You might want to add the following in the Environment variables of the Run Configurations. 
`SSL_CRAWLER_SQS_ENDPOINT` sets the endpoint for AWS SQS. `SSL_CRAWLER_ENV` modifies the format of the output to be 
human-readable when launching the application locally.  Set it to `local` when running the module and to `test` when running the tests. 
* `SSL_CRAWLER_SQS_ENDPOINT=http://localhost:4566;SSL_CRAWLER_ENV=local`

## Configure

To configure the module, either export the variables as environment variable or use the .env file in the src folder.
If the variable is defined in both places (.env file and environment variable), the environment variable will be used.   
These are the variables currently used
```env
SSL_CRAWLER_DB_USER=postgres
SSL_CRAWLER_DB_PASSWORD=password
SSL_CRAWLER_DB_HOST=localhost
SSL_CRAWLER_DB_PORT=55432
SSL_CRAWLER_DB_NAME=postgres
SSL_CRAWLER_DB_SCHEMA=ssl_crawler_test

SSL_CRAWLER_INPUT_QUEUE=mercator-ssl-crawler-input
SSL_CRAWLER_OUTPUT_QUEUE=mercator-ssl-crawler-output
SSL_CRAWLER_SQS_ENDPOINT=http://localhost:4566

# 10 is the maximum that AWS allows
SSL_CRAWLER_BATCH_SIZE=10

SSL_CRAWLER_MAX_WORKERS=25

AWS_REGION=eu-west-1
AWS_DEFAULT_REGION=eu-west-1

SERVER_PORT=8443
CERTIFICATE_FILE=server.pem

SSL_CRAWLER_ENV=local
```

## Modifying the database schema

To modify the database schema, make sure that the virtual environment is activated and run  
`alembic revision -m "describe the modification here"`  
This will create a new file under `src/ssl_crawler/alembic/version`.

Write the content of the function `upgrade()`.  
It is possible to write plain SQL or to use the sqlalchemy syntax.  
Example with a plain SQL
```py
def upgrade():
    op.execute(
        """
        ALTER TABLE table ADD COLUMN comment VARCHAR(255);
        """
    )
```

Alembic will upgrade the schema when the module is launched.
