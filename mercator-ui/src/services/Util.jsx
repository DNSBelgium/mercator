export const renderDataBoolean = (bool) => {
    if(bool === undefined) {
        return (<td>Loading</td>);
    }
    return (<td>{bool.toString()}</td>);
}

/*
    The following code is for handling errors from backend responses.
*/

export const handleExResponse = (response) => {

    switch(response.status) {
        // Add status case as they come up.

        case 500: // Status Code 500+ is for server issues.
        case 501:
        case 502:
        case 503:
            return caseFiveHundredRange();

        default:
            return(<p>Apologies, something went wrong.</p>);
    }
}

// Return of case 500-503.
const caseFiveHundredRange = () => {
    return (
        <div id='Error-Case-500-Div'>
            <h2>An error has occurred.</h2>
            <p>Please try again later.</p>
        </div>
    );
}