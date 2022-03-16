message=$(sed -e "s/\${domain}/$1/" snap-command.json)

queue="mercator-muppets-input"

aws sqs send-message --queue-url $(aws sqs get-queue-url --queue-name ${queue} | jq -r .QueueUrl) --message-body "$message"
