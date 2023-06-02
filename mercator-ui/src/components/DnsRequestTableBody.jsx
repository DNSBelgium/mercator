import DnsResponseDataTable from "./DnsResponseTableBody";

// export default function DnsRequestDataTable({request, index}) {
//     const req = request
//     return (
//         <>
//             {request.responses.map((item, index) => (
//                 // only with code 0 passes due to having a response
//                 <DnsResponseDataTable request={req} response={item} first={index}/>
//             ))}
//         </>
//     );
// }
export default function DnsRequestDataTable({request, index}) {
    return (
        <>
            {request.responses.map((item, index) => (
                <DnsResponseDataTable request={request} response={item} first={index}/>
            ))}
            {request !== null && request.responses.length === 0 && (
                <DnsResponseDataTable request={request} response={null} first={0}/>
            )}
        </>
    );
}