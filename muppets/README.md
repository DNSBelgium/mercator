# Muppets - module for taking snapshots using puppeteer and driven by SQS

## Build and run locally
```
npm i
npm run build
npm start
```

### Docker
build: `docker build -t dnsbelgium/mercator/muppets .`

run locally (after getting IAM credentials):
```
cat << EOF > docker_env
AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID}
AWS_SECRET_ACCESS_KEY=${AWS_SECRET_ACCESS_KEY}
AWS_SESSION_TOKEN=${AWS_SESSION_TOKEN}
EOF

docker run --env-file docker_env --cap-add=SYS_ADMIN dnsbelgium/mercator/muppets
```

## Usage
You can either use  `./push-to-sqs.sh domain.be` to push a message (change the SQS queue if needed)
or manually push a message:

* Log in to AWS console
* Go to SQS console and find the correct queue
* Queue Actions | Send a Message
* paste the contents of snap-command.json
* click "Send Message"

=> muppets should receive the message, make a screenshot, upload the files to S3 
and write a message in the output SQS queue
