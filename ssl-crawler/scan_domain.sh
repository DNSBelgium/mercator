domain_name=${1:-"www.dnsbelgium.be"}

echo "domain_name: [${domain_name}]"

FILENAME="ssl_scan.${domain_name}.json"

let start=$(date "+%s")
python -m sslyze ${domain_name} --json_out=${FILENAME}

let end=$(date "+%s")

echo "start: $start"
echo "end:   $end"

let seconds=end-start
echo "seconds: ${seconds}"

echo "Results saved in ${FILENAME}"