import {useEffect, useState} from "react";

const ShowPopup = (item) => {
    const DEV_URL = window._env_.REACT_APP_MUPPETS_HOST;
    const [showDanger, setShowDanger] = useState(false);

    if (item.htmlKey !== null) {
        return (
            <>
                <div id='warningComponent'>
                    <img src="../" alt="warning aign"/>
                    <h6>Do you want to open and render the html in a new tab ?</h6>
                    <div>
                        <button
                            id="acceptHtmlNewTab"
                            className="mr-5 ml-5 content-card-link-button"
                            onClick={() => window.open(DEV_URL + "/" + item.htmlKey)}
                        >
                            Yes
                        </button>

                        <button
                            id="declineHtmlNewTab"
                            className="mr-5 ml-5 content-card-link-button"
                            onClick={() => setShowDanger(state => !state)}
                        >
                            No
                        </button>
                    </div>
                </div>
            </>
        )
    }
    return (
        <p>No html found</p>
    );
}