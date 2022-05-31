import {Col, Row, Table, Button} from "react-bootstrap";
import {useEffect, useState} from 'react';
import {Link, useNavigate, useParams} from 'react-router-dom';
import api from "../../services/api";
import moment from "moment";
import { checkObjectIsFalsy, handleExResponse } from "../../services/Util";

const TimelineDomainName = () => {
    const { domain } = useParams();
    let { page } = useParams(); // Fetch :id from url
    page = parseInt(page);

    const navigate = useNavigate();

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

            if (checkObjectIsFalsy(domain)) {
                setProcessing(false);
                return;
            }

            const url = `/find-visits/${domain}?page=${page - 1}` // backend location: mercator-api/.../search/SearchController
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
    }, [domain, page]);

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
                            navigate(`/${domain}/${page - 1}`);
                        }}
                    >
                        prev
                    </button>

            btnArray.push(btn);
        }

        for(let i = 0; i < data.amountOfPages; i++) {
            let className;

            // The follow if-else is for giving the current page's button an underline.
            if(i === page - 1) {
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
                            navigate(`/${domain}/${i + 1}`);
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
                            navigate(`/${domain}/${page + 1}`);
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
                        src={`${DEV_URL}/${item.screenshotKey}`}
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
    const renderHTML = () => {
        if(exception !== null) {
            if(!domain.includes(".be") && !domain.includes(".vlaanderen") && !domain.includes(".brussels")) {
                return(
                    <h5 className="ml-3 mt-3">Please enter a domain name with TLD (.be, .vlaanderen, .brussels).</h5>
                );
            }
            return( handleExResponse(exception) ); // HandleError.jsx handles the exception.
        }

        if(processing) {
            return (
                <div className="ml-3 mt-3" alt="Div when processing is true.">
                    <h2>Searching ...</h2>
                </div>
            );
        }

        if(checkObjectIsFalsy(data)) {
            if(!domain.includes(".be") && !domain.includes(".vlaanderen") && !domain.includes(".brussels")) {
                return(
                    <h5 className="ml-3 mt-3">Please enter a domain name with TLD (.be, .vlaanderen, .brussels).</h5>
                );
            }
            return (
                <h5 className="ml-3 mt-3">Apologies, something went wrong.</h5>
            );
        }

        return (
            <div id="TDN-Div" alt="Div when search is finished and data has been returned.">
                <Row>
                    <Col className='mt-4'>
                        <div>
                            <h1 className="mb-4">{domain}</h1>
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
                                        {/* <th>Crawl label</th> */}
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
                                            {/* <td> 
                                                { item.label } 
                                            </td> */}
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
                                                    data-id={item.visitId}
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
                            navigate(`/${domain}/1`);
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
                            navigate(`/${domain}/${data.amountOfPages}`);
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
                renderHTML()
            }
        </>
    )
}

export default TimelineDomainName;