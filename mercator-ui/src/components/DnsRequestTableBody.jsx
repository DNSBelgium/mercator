import DnsResponseDataTable from "./DnsResponseTableBody";

export default function DnsRequestDataTable({request, requestIndex}) {
    return (
        <>
            {request.responses.map((response, responseIndex) => (
                <DnsResponseDataTable request={request} response={response} responseIndex={responseIndex}
                                      key={request.id + responseIndex}/>
            ))}
            {request.responses.length === 0 && (
                <DnsResponseDataTable request={request} response={null} key={request.id}/>
            )}
        </>
    );
}