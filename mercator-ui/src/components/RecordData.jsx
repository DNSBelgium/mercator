export default function RecordData({responseKey, recordData, rowSpan}) {
    return (
        <td className="record-data" key={responseKey} rowSpan={rowSpan}>
            <div className="record-data-container">
                <div className="record-data-content">
                    {recordData}
                </div>
                <div className="record-data-copy-btn">
                    <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16"
                         fill="#2a6485" className="copy-btn"
                         viewBox="0 0 16 16"
                         onClick={() => {
                             navigator.clipboard.writeText(recordData)
                         }}>
                        <path fillRule="evenodd"
                              d="M10 1.5a.5.5 0 0 0-.5-.5h-3a.5.5 0 0 0-.5.5v1a.5.5 0 0 0 .5.5h3a.5.5 0 0 0 .5-.5v-1Zm-5 0A1.5 1.5 0 0 1 6.5 0h3A1.5 1.5 0 0 1 11 1.5v1A1.5 1.5 0 0 1 9.5 4h-3A1.5 1.5 0 0 1 5 2.5v-1Zm-2 0h1v1A2.5 2.5 0 0 0 6.5 5h3A2.5 2.5 0 0 0 12 2.5v-1h1a2 2 0 0 1 2 2V14a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V3.5a2 2 0 0 1 2-2Z"/>
                    </svg>
                </div>
            </div>
        </td>
    )
}