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
    const [imageData, setImageData] = useState([]); // Hook to hold images.

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
        setImageData([]); 
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
    // When browsing to a new page, the images hooks will get reset.
    const renderPagingButtons = () => {
        let btnArray = [];
        let btn;

        if(data.hasPrevious) {
            btn =   <button 
                        key='prev'
                        className="paging-btn mr-1"
                        onClick={() => {
                            setShowImages(false);
                            setImageData([]);
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
                            setImageData([]);
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
                            setImageData([]);
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

    // Get image urls from backend and set them in the imageData hook.
    // 2 URLs below are environment variables for develop and local. Set the used URL in the HTML's img src below.
    const DEV_URL = window._env_.REACT_APP_MUPPETS_HOST;
    const LOCAL_URL = window._env_.LOCAL_MUPPETS_HOST;
    const getAndShowImages = async () => {
        if(!showImages) {
            let screenshotVisitIds = [];
            for (let i = 0; i < data.dtos.length; i++) {
                screenshotVisitIds[i] = data.dtos[i].visitId;
            }
            
            const url = `/findScreenshotsByVisitIds?visitIdList=${screenshotVisitIds}`;
            await api.get(url)
                .then((resp) => {
                    console.log(resp);
                    if(resp.status === 200) {
                        setImageData(resp.data);
                    }
                })
                .catch((ex) => {
                    console.log(ex.response);
                });
        }

        await setShowImages(state => !state);
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
                        <button 
                            className="mt-3"
                            onClick={() => getAndShowImages()}
                        >
                            Show screenshots
                        </button>
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
                                                showImages && ( // If showImages == true, render
                                                    <td>
                                                        <img 
                                                            className="timeline-image"
                                                            src={`${LOCAL_URL}/${imageData[index]}`}
                                                        >
                                                        </img>
                                                    </td>
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
                            setImageData([]);
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
                            setImageData([]);
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