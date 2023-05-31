import DnsResponseDataTable from "./DnsResponseTableBody";

export default function DnsRequestDataTable({request, index}) {

    return (
        <>
            <DnsResponseDataTable request={request} response={request.responses[0]} first={true}/>

            {request.responses.map((item, index) => (
                <>
                    {index > 0 &&
                        < DnsResponseDataTable response={item} first={false}/>
                    }
                </>
            ))}
        </>
    );
}