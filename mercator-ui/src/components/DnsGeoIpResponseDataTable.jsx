export default function DnsGeoIpResponseDataTable({geoIpResponse, first}) {
    const colSpanGeoiIplessResponse = 5;

    function firstrow(geoIp) {
        if (!geoIp) {
            return (
                <>
                    <td colSpan={colSpanGeoiIplessResponse}></td>
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

    function otherrow(geoIp) {
        if (!geoIp) {
            return (
                <>
                    <td colSpan={colSpanGeoiIplessResponse}></td>
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

    return <>{first ? firstrow(geoIpResponse) : otherrow(geoIpResponse)}</>;
}
