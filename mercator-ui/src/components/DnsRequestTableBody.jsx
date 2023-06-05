import DnsResponseDataTable from "./DnsResponseTableBody";

export default function DnsRequestDataTable({request, requestIndex}) {
    return (
        <>
            {request.responses.map((item, responseIndex) => (
                <DnsResponseDataTable request={request} requestIndex={requestIndex} response={item}
                                      responseIndex={responseIndex}/>
            ))}
            {request.responses.length === 0 && (
                <DnsResponseDataTable request={request} requestIndex={requestIndex} response={null}/>
            )}
        </>
    );
}