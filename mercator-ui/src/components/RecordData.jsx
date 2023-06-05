export default function RecordData({recordData, rowSpan}) {
    console.log(recordData)
    return (
        <td rowSpan={rowSpan}>
            {recordData}
        </td>
    )
}