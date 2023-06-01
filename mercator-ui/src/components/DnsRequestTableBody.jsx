import DnsResponseDataTable from "./DnsResponseTableBody";

export default function DnsRequestDataTable({request, index}) {
    return (
        <>
            {request.responses.map((item, index) => (
                <DnsResponseDataTable request={request} response={item} first={index}/>
            ))}
        </>
    );
}
