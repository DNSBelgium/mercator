export default function HtmlRenderWarning({onClickYes, onClickNo}) {
    return (
        <>
            <div id="warningWrapper">
                <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" fill="currentColor"
                     className="bi bi-exclamation-circle" viewBox="0 0 16 16">
                    <path d="M8 15A7 7 0 1 1 8 1a7 7 0 0 1 0 14zm0 1A8 8 0 1 0 8 0a8 8 0 0 0 0 16z"/>
                    <path
                        d="M7.002 11a1 1 0 1 1 2 0 1 1 0 0 1-2 0zM7.1 4.995a.905.905 0 1 1 1.8 0l-.35 3.507a.552.552 0 0 1-1.1 0L7.1 4.995z"/>
                </svg>
                <h6>Open and render HTML in a new tab?</h6>
                <p>This could be potentially dangerous. Check the raw HTML first.</p>
                <div>
                    <button
                        id="acceptHtmlNewTab"
                        className="mr-5 ml-5 btn btn-warning"
                        onClick={onClickYes}
                    >
                        Proceed anyway
                    </button>

                    <button
                        id="declineHtmlNewTab"
                        className="mr-5 ml-5 btn"
                        onClick={onClickNo}
                    >
                        Cancel
                    </button>
                </div>
            </div>
        </>
    )
}
