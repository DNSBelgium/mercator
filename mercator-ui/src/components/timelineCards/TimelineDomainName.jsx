import {Col, Row, Table, Button} from "react-bootstrap";
import {useEffect, useState} from 'react';
import {Link} from 'react-router-dom';
import api from "../../services/api";
import moment from "moment";
import { checkObjectIsFalsy, handleExResponse } from "../../services/Util";

const TimelineDomainName = (props) => {
    const domainName = props.search; // Received from App.jsx search hook.

    const [data, setData] = useState([]); // Used to hold data from GET request.
    const [processing, setProcessing] = useState(true); // Used for holding HTML rendering if processing === true.
    const [exception, setException] = useState(null); // Used for handling GET request exception responses.

    const [showImages, setShowImages] = useState(false); // Hook to decide whether images should be shown in the table.

    useEffect(() => { // Triggers upon initial render and every time domainName changes.

        // Wrapping the inside of this hook with an async function so we can await the backend data with a Promise.
        // The actual useEffect hooks should not be executed asynchronously, but the inside can be whatever.
        async function executeHook() {
            setProcessing(true);
            setException(null);

            if (checkObjectIsFalsy(domainName)) {
                setProcessing(false);
                return;
            }

            const url = `/find-visits/${domainName}?page=${props.page}` // backend location: mercator-api/.../search/SearchController
            await api.get(url)
                .then((resp) => {
                    if(resp.status === 200) {
                        
                        setData(resp.data);
                    }
                })
                .catch((ex) => {
                    setException(ex.response); // TODO: Add 400 / 404?
                });

            setProcessing(false);
        }

        executeHook();

        // Reset image hooks for new searches.
        setShowImages(false);
    }, [domainName, props.page]);

    // Returns a boolean as a green V or red X.
    const booleanToCheckmark = (bool) => {
        return(bool ? 
                <p style={{color: "green", marginTop: "15px"}}>&#10003;</p> : 
                <p style={{color: "red", marginTop: "15px"}}>&#10005;</p>
            );
    }

    // Render the previous, numberical and next buttons.
    // When browsing to a new page, the image row should be hidden again.
    const renderPagingButtons = () => {
        let btnArray = [];
        let btn;

        if(data.hasPrevious) {
            btn =   <button 
                        key='prev'
                        className="paging-btn mr-1"
                        onClick={() => {
                            setShowImages(false);
                            props.setPage(props.page - 1) 
                        }}
                    >
                        prev
                    </button>

            btnArray.push(btn);
        }

        for(let i = 0; i < data.amountOfPages; i++) {
            let className;

            // The follow if-else is for giving the current page's button an underline.
            if(props.page === i) {
                className = "current-page paging-btn mr-1";
            }
            else {
                className = "paging-btn mr-1";
            }

            btn =   <button 
                        key={i} 
                        className={className}
                        onClick={() => {
                            setShowImages(false);
                            props.setPage(i)
                        }}
                    >
                        {i + 1}
                    </button>
            btnArray.push(btn);
        }

        if(data.hasNext) {
            btn =   <button 
                        key='next'
                        className="paging-btn"
                        onClick={() => {
                            setShowImages(false);
                            props.setPage(props.page + 1)
                        }}
                    >
                        next
                    </button>

            btnArray.push(btn);
        }

        return(
            <>
                { btnArray }
            </>
        );
    }

    const renderScreenshotsButton = () => {
        if (!showImages) {
            return (
                <Button 
                    id="show-images-btn"
                    className="mt-3"
                    onClick={() => setShowImages(state => !state)}
                >
                    Show screenshots
                </Button>
            );
        }
        return (
            <Button 
                id="show-images-btn"
                className="mt-3"
                onClick={() => setShowImages(state => !state)}
            >
                Hide screenshots
            </Button>
        );
    }

    // Handle showing / hiding of images when "Show / Hide screenshots" is clicked.
    const handleImages = (item) => {
         // URL for development / local environment.
        const DEV_URL = window._env_.REACT_APP_MUPPETS_HOST;
        const LOCAL_URL = 'http://localhost:4566/mercator-muppets';

        if (item.screenshotKey !== null) {
            return (
                <td>
                    <img
                        className="timeline-image"
                        src={`${LOCAL_URL}/${item.screenshotKey}`}
                        alt={`Thumbnail of ${item.visitId}`}
                    >
                    </img>
                </td>
            )
        }
        return (
            <td>
                <p>No image found</p>
            </td>
        );
    }

    // Rendering HTML on a JS Function base, so we can define logic.
    const renderHtml = () => {
        if(exception !== null) {
            return( handleExResponse(exception) ); // HandleError.jsx handles the exception.
        }

        if(domainName === null) {
            return(
                <h5 className="ml-3 mt-3">Enter a search to begin.</h5>
            );
        }

        if(processing) {
            return (
                <div className="ml-3 mt-3" alt="Div when processing is true.">
                    <h2>Searching ...</h2>
                </div>
            );
        }

        if(checkObjectIsFalsy(data)) {
            return (
                <h5 className="ml-3 mt-3">Apologies, something went wrong.</h5>
            );
        }

        return (
            <div id="TDN-Div" alt="Div when search is finished and data has been returned.">
                <Row>
                    <Col className='mt-4'>
                        <div>
                            <h1 className="mb-4">{domainName}</h1>
                            <p>Number of records: { data.amountOfRecords }</p>
                        </div>
                        {
                            renderScreenshotsButton()
                        }
                        <div className="mt-3">
                            <Table id="timeline-table" bordered hover size="sm">
                                <thead>
                                    <tr>
                                        <th>Crawl time</th>
                                        <th>Status<br/> Content crawl</th>
                                        <th>Status<br/> DNS crawl</th>
                                        <th>Status<br/> SMTP crawl</th>
                                        <th>Status<br/> Wappalyzer</th>
                                        <th>Visit Id</th>
                                        {
                                            showImages && ( // If showImages == true, render
                                                <th>Image</th>
                                            )
                                        }
                                    </tr>
                                </thead>
                                <tbody>
                                    { data.dtos.map((item, index) => (
                                        <tr key={index}>
                                            <td>
                                                <Link 
                                                    to={{pathname: `/details/${item.visitId}`}}
                                                >
                                                    { 
                                                        item.requestTimeStamp ? 
                                                            moment(item.requestTimeStamp).format("YYYY/MM/DD HH:mm:ss") :
                                                            '' 
                                                    }
                                                </Link>
                                            </td>
                                            <td>
                                                { booleanToCheckmark(item.crawlStatus.muppets) }
                                            </td>
                                            <td>
                                                { booleanToCheckmark(item.crawlStatus.dns) }
                                            </td>
                                            <td>
                                                { booleanToCheckmark(item.crawlStatus.smtp) }
                                            </td>
                                            <td>
                                                { booleanToCheckmark(item.crawlStatus.wappalyzer) }
                                            </td>
                                            <td>
                                                <Button 
                                                    id='Copy-Id-Btn'
                                                    onClick={() => navigator.clipboard.writeText(item.visitId)}
                                                >
                                                    Copy Visit Id
                                                </Button>
                                            </td>
                                            {
                                                showImages && ( // If showImages == true, execute
                                                    handleImages(item)
                                                )
                                            }
                                        </tr>
                                    ))}
                                </tbody>
                            </Table>
                        </div>
                    </Col>
                </Row>
                <div id="Paging-Div">
                    <button // First page btn
                        className="paging-btn mr-1"
                        onClick={() => {
                            setShowImages(false);
                            props.setPage(0);
                        }}
                    >
                        &#8676;
                    </button>

                    { // Next, numerical and previous buttons
                        renderPagingButtons() 
                    }

                    <button // Last page btn
                        className="paging-btn ml-1"
                        onClick={() => {
                            setShowImages(false);
                            props.setPage(data.amountOfPages - 1);
                        }}
                    >
                        &#8677;
                    </button>
                </div>
            </div>
        );
    }

    // Return of this file's HTML.
    return (
        <>
            {
                renderHtml()
            }
        </>
    )
}

export default TimelineDomainName;