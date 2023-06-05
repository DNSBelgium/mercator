import DnsGeoIpResponseDataTable from "./DnsGeoIpResponseDataTable";
import RecordData from "./RecordData";

export default function DnsResponseDataTable({request, requestIndex, response, responseIndex}) {
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

    const responseKey = requestIndex + "." + responseIndex;

    function firstrow(req, resp) {
        return (
            <>
                <tr>
                    <td key={requestIndex} rowSpan={countRowsRequest(req)}>
                        {req.prefix}
                    </td>
                    <td key={requestIndex} rowSpan={countRowsRequest(req)}>
                        {req.rcode === 0 ? "Successful (0)" : req.problem + " (" + req.rcode + ")"}
                    </td>
                    {/*change with component*/}
                    <td key={requestIndex} rowSpan={countRowsRequest(req)}>
                        {req.recordType}
                    </td>
                    {resp !== null && (
                        <>
                            <td key={responseKey} rowSpan={countRowsResponse(resp)}>
                                {resp.ttl || ""}
                            </td>
                            <RecordData responseKey={responseKey} recordData={resp.recordData}
                                        rowSpan={countRowsResponse(resp)}/>
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
                    <td key={responseKey} rowSpan={countRowsResponse(resp)}>
                        {resp.ttl || ""}
                    </td>
                    <RecordData responseKey={responseKey} recordData={resp.recordData}
                                rowSpan={countRowsResponse(resp)}/>
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
            response === null ? firstrow(request, response) : (responseIndex === 0 ? firstrow(request, response) : otherrow(response))
        ) : null}
    </>;
}

