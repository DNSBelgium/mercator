// Check data is not null or an empty object.
export const checkObjectIsFalsy = (data) => {
    // https://stackoverflow.com/questions/679915/how-do-i-test-for-an-empty-javascript-object
    if (data && Object.keys(data).length === 0 && Object.getPrototypeOf(data) === Object.prototype) {
        return true;
    }
    if (data === null) return true;
    if (data.length === 0) return true;
    return false;
}

// Decide whether to render the value of the boolean or "loading".
// Used for detailsCards when data is being delayed.
export const renderDataBoolean = (bool) => {
    if(bool === undefined) {
        return (<td>Loading</td>);
    }
    return (<td>{bool.toString()}</td>);
}



/*
    The following code is for handling errors from backend responses. (WIP)
*/

export const handleExResponse = (response) => {

    switch(response.status) {
        // Add status case as they come up.

        case 404: // Status Code 404 === Not_Found
            return caseFourOFour();

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
        <div className="error-div" id='Error-Case-500-Div'>
            <h2>An error has occurred.</h2>
            <p>Please try again later.</p>
        </div>
    );
}

// Return of case 404.
const caseFourOFour = () => {
    const domainName = localStorage.getItem('search') ? localStorage.getItem('search') : '';
    return (
        <div className="error-div" id='Error-Case-404-Div'>
            <h2>An error has occurred.</h2>
            <p>Domain {domainName} was not yet crawled or does not exist.</p>
        </div>
    );
}