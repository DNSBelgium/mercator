import DnsGeoIpResponseDataTable from "./DnsGeoIpResponseDataTable";
import RecordData from "./RecordData";

export default function DnsResponseDataTable({request, response, responseIndex}) {
    const colSpanResponselessRequest = 7;

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
                totalRows = 1;
            }
        }
        return totalRows;
    }

    function dataSorting(req, resp, responseIndex) {
        if (!req) {
            return null;
        } else if (!resp) {
            return requestRowPrimary(req, resp);
        } else if (responseIndex === 0) {
            return requestRowPrimary(req, resp);
        } else {
            return secondaryRows(resp);
        }
    }

    function requestRowPrimary(req, resp) {
        return (
            <>
                <tr>
                    <td rowSpan={countRowsRequest(req)}>{req.prefix}</td>
                    <td rowSpan={countRowsRequest(req)}>
                        {req.rcode === 0 ? "Successful (0)" : req.problem + " (" + req.rcode + ")"}
                    </td>
                    <td rowSpan={countRowsRequest(req)}>{req.recordType}</td>
                    {resp !== null ? (
                        <>
                            <td rowSpan={countRowsResponse(resp)}>{resp.ttl || ""}</td>
                            <RecordData recordData={resp.recordData} rowSpan={countRowsResponse(resp)}/>
                            <DnsGeoIpResponseDataTable
                                geoIpResponse={resp.responseGeoIps[0]}
                                first={true}
                            />
                        </>
                    ) : (
                        <td colSpan={colSpanResponselessRequest} className="empty"></td>
                    )}
                </tr>
                {resp !== null &&
                    resp.responseGeoIps.slice(1).map((item, index) => (
                        <DnsGeoIpResponseDataTable
                            geoIpResponse={item}
                            first={false}
                            key={index}
                        />
                    ))}
            </>
        );
    }

    function secondaryRows(resp) {
        return (
            <>
                <tr>
                    <td rowSpan={countRowsResponse(resp)}>{resp.ttl || ""}</td>
                    <RecordData recordData={resp.recordData} rowSpan={countRowsResponse(resp)}/>
                    <DnsGeoIpResponseDataTable
                        geoIpResponse={resp.responseGeoIps[0]}
                        first={true}
                    />
                </tr>
                {resp.responseGeoIps.slice(1).map((item, index) => (
                    <DnsGeoIpResponseDataTable
                        geoIpResponse={item}
                        first={false}
                        key={index}
                    />
                ))}
            </>
        );
    }

    return <>{dataSorting(request, response, responseIndex)}</>;
}
