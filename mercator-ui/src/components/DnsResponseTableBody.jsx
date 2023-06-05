import DnsGeoIpResponseDataTable from "./DnsGeoIpResponseDataTable";
import RecordData from "./RecordData";

export default function DnsResponseDataTable({request, response, first}) {
    function countRowsResponse(response) {
        if (!response || !response.responseGeoIps || response.responseGeoIps.length === 0) {
            return 1;
        }
        return response.responseGeoIps.length;
    }

    function countRowsRequest(request) {
        let totalRows = 0;
        if (request && request.responses) {
            if (request.responses.length > 0) {
                request.responses.forEach((item) => {
                    if (item && item.responseGeoIps && item.responseGeoIps.length === 0) {
                        totalRows++;
                    }
                    if (item && item.responseGeoIps) {
                        totalRows += item.responseGeoIps.length;
                    }
                });
            } else {
                totalRows = 1
            }
        }
        return totalRows;
    }

    function firstrow(req, resp) {
        return (
            <>
                <tr>
                    <td rowSpan={countRowsRequest(req)}>
                        {req.prefix}
                    </td>
                    <td rowSpan={countRowsRequest(req)}>
                        {req.rcode === 0 ? "Successful (0)" : req.problem + " ( " + req.rcode + " )"}
                    </td>
                    {/*change with component*/}
                    <td rowSpan={countRowsRequest(req)}>
                        {req.recordType}
                    </td>
                    {resp !== null && (
                        <>
                            <td rowSpan={countRowsResponse(resp)}>
                                {resp.ttl || ""}
                            </td>
                            <RecordData recordData={resp.recordData} rowSpan={countRowsResponse(resp)}/>
                            <DnsGeoIpResponseDataTable geoIpResponse={resp.responseGeoIps[0]} first={true}/>
                        </>
                    )}
                </tr>
                {resp !== null && (
                    resp.responseGeoIps.map((item, index) => (
                        <>
                            {index > 0 &&
                                <DnsGeoIpResponseDataTable geoIpResponse={item} first={false}/>
                            }
                        </>
                    ))
                )}
            </>)
    }

    function otherrow(resp) {
        return (
            <>
                <tr>
                    <td rowSpan={countRowsResponse(resp)}>
                        {resp.ttl || ""}
                    </td>
                    <td rowSpan={countRowsResponse(resp)}>
                        {resp.recordData || ""}
                    </td>
                    <DnsGeoIpResponseDataTable geoIpResponse={resp.responseGeoIps[0]} first={true}/>
                </tr>
                {resp.responseGeoIps.map((item, index) => (
                    <>
                        {index > 0 &&
                            <DnsGeoIpResponseDataTable geoIpResponse={item} first={false}/>
                        }
                    </>
                ))}
            </>)
    }

    return <>
        {request !== null ? (
            response === null ? firstrow(request, response) : (first === 0 ? firstrow(request, response) : otherrow(response))
        ) : null}
    </>;
}

