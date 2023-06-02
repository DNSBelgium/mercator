import DnsGeoIpResponseDataTable from "./DnsGeoIpResponseDataTable";

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
            request.responses.forEach((item) => {
                if (item && item.responseGeoIps && item.responseGeoIps.length === 0) {
                    totalRows++;
                }
                if (item && item.responseGeoIps) {
                    totalRows += item.responseGeoIps.length;
                }
            });
        }
        return totalRows;
    }

    function firstrow(resp) {
        // const renderPrefix = shouldRenderPrefix(request);
        // console.log(renderPrefix)
        return (
            <>
                <tr>

                    <td rowSpan={countRowsRequest(request)}>
                        {request.prefix}
                    </td>
                    <td rowSpan={countRowsRequest(request)}>
                        {request.problem} ({request.rcode})
                    </td>
                    <td rowSpan={countRowsRequest(request)}>
                        {request.recordType}
                    </td>
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
    return <>{response ? ((first === 0) ? firstrow(response) : otherrow(response)) : null}</>;
}

