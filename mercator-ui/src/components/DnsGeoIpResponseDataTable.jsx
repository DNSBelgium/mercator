export default function DnsGeoIpResponseDataTable({geoIpResponse, first}) {
    function firstrow(geoIp) {
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

    return <>{geoIpResponse ? (first ? firstrow(geoIpResponse) : otherrow(geoIpResponse)) : null}</>;
}
