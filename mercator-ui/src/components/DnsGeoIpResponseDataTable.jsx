export default function DnsGeoIpResponseDataTable({geoIpResponse, first}) {
    const colSpanGeoiIplessResponse = 5;

    function primaryRow(geoIp) {
        if (!geoIp) {
            return (
                <>
                    <td colSpan={colSpanGeoiIplessResponse} className="empty"></td>
                </>
            )
        }
        return (
            <>
                <td>
                    {geoIp.country || ""}
                </td>
                <td>
                    {geoIp.asn || ""}
                </td>
                <td>
                    {geoIp.asnOrganisation || ""}
                </td>
                <td>
                    {geoIp.ip || ""}
                </td>
                <td>
                    {geoIp.ipVersion || ""}
                </td>
            </>)
    }

    function secondaryRow(geoIp) {
        if (!geoIp) {
            return (
                <>
                    <td colSpan={colSpanGeoiIplessResponse} className="empty"></td>
                </>
            )
        }
        return (
            <>
                <tr>
                    <td>
                        {geoIp.country || ""}
                    </td>
                    <td>
                        {geoIp.asn || ""}
                    </td>
                    <td>
                        {geoIp.asnOrganisation || ""}
                    </td>
                    <td>
                        {geoIp.ip || ""}
                    </td>
                    <td>
                        {geoIp.ipVersion || ""}
                    </td>
                </tr>
            </>)
    }

    return <>{first ? primaryRow(geoIpResponse) : secondaryRow(geoIpResponse)}</>;
}
