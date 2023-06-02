export default function RecordData({recordType, rowSpan}) {
    return (
        <td rowSpan={rowSpan}>
            {recordType}
        </td>
    )
}