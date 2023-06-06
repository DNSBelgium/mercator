import DnsResponseDataTable from "./DnsResponseTableBody";

export default function DnsRequestDataTable({request, requestIndex}) {
    // make function req.id + request index for key
    console.log(request.id.toString() + "_" + requestIndex.toString(), request)

    return (
        <>
            {request.responses.map((response, responseIndex) => (
                <DnsResponseDataTable request={request} response={response} responseIndex={responseIndex}
                                      key={request.id.toString() + "_" + requestIndex.toString()}/>
            ))}
            {request.responses.length === 0 && (
                <DnsResponseDataTable request={request} response={null} key={request.id + "." + requestIndex}/>
            )}
        </>
    );
}