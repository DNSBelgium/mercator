import {Col, Row, Table} from "react-bootstrap";
import {useEffect, useState} from 'react';
import {Link} from 'react-router-dom';
import api from "../../services/api";
import moment from "moment";
import { handleExResponse } from "../../services/Util";

const TimelineDomainName = () => {

    const domainName = localStorage.getItem("search"); // Received from NavigationBar.jsx textInput Ref hook.

    const [data, setData] = useState([]); // Used to hold data from GET request.
    // const [status, setStatus] = useState([]); // Used for statusMap which contains the muppets, dns, smtp and wappalyzer booleans.
    const [processing, setProcessing] = useState(true); // Used for holding HTML rendering if processing === true.
    const [exception, setException] = useState(null); // Used for handling GET request exception responses.

    // Function to sort rows by crawl date desc
    // function sortVisits(a, b) {
    //     return Date.parse(b.requestTimestamp) - Date.parse(a.requestTimestamp);
    // }

    useEffect(() => { // Triggers upon initial render and every time domainName changes.

        // Wrapping the inside of this hook with an async function so we can await the backend data with a Promise.
        // The actual useEffect hooks should not be executed asynchronously, but the inside can be whatever.
        async function executeHook() {
            setProcessing(true);
            setException(null);

            if (!domainName) {
                setProcessing(false);
                return;
            }

            // const url = `/dispatcherEvents/search/findDispatcherEventByDomainName?domainName=${domainName}`;
            const url = `/find-visits/${domainName}` // backend location: mercator-api/.../search/SearchController
            await api.get(url)
                .then((resp) => {
                    console.log(resp);
                    if(resp.status === 200) {
                        console.log(resp.data);

                        setData(resp.data);
                    }
                })
                .catch((ex) => {
                    setException(ex.response); // TODO: Add 400 / 404?
                });
        
            // setStatus([]); // Clear status state for a new domain

            setProcessing(false);
        }

        executeHook();
    }, [domainName]);

    // useEffect(() => { // Triggers upon initial render and every time the data hook changes.
    //     data.forEach(async (item, i, arr) => {

    //         // Get status from the backend for each visitId in the data hook.
    //         await api.get(`/status/${item.visitId}`)
    //             .then((resp) => {
    //                 if(resp.status === 200) {
    //                     setStatus(array => [...array, resp.data])
    //                 }
    //             })
    //             .catch((ex) => {
    //                 setException(ex.response);
    //             });
    //     });
    // }, [data])

    // let statusMap = status.reduce((obj, data) => ({...obj, [data.visit_id]: data}), {})

    // Fill HTML Table's 'Content crawl'.
    // const fillMuppets = (visitId) => {
    //     if(statusMap[visitId] === undefined) {
    //         return (<p style={{marginTop: "15px"}}>Loading</p>);
    //     }

    //     let check = visitId in statusMap ? statusMap[visitId].muppets : false;
        
    //     return(check ? 
    //                 <p style={{color: "green", marginTop: "15px"}}>&#10003;</p> : 
    //                 <p style={{color: "red", marginTop: "15px"}}>&#10005;</p>
    //     );
    // }

    // Fill HTML Table's 'DNS crawl'.
    // const fillDns = (visitId) => {
    //     if(statusMap[visitId] === undefined) {
    //         return (<p style={{marginTop: "15px"}}>Loading</p>);
    //     }

    //     let check = visitId in statusMap ? statusMap[visitId].dns : false
        
    //     return(check ? 
    //                 <p style={{color: "green", marginTop: "15px"}}>&#10003;</p> : 
    //                 <p style={{color: "red", marginTop: "15px"}}>&#10005;</p>
    //     );
    // }

    // Fill HTML Table's 'SMTP crawl'.
    // const fillSmtp = (visitId) => {
    //     if(statusMap[visitId] === undefined) {
    //         return (<p style={{marginTop: "15px"}}>Loading</p>);
    //     }

    //     let check = visitId in statusMap ? statusMap[visitId].smtp : false
        
    //     return(check ? 
    //                 <p style={{color: "green", marginTop: "15px"}}>&#10003;</p> : 
    //                 <p style={{color: "red", marginTop: "15px"}}>&#10005;</p>
    //     );
    // }

    // Fill HTML Table's 'Wappalyzer'.
    // const fillWappalyzer = (visitId) => {
    //     if(statusMap[visitId] === undefined) {
    //         return (<p style={{marginTop: "15px"}}>Loading</p>);
    //     }

    //     let check = visitId in statusMap ? statusMap[visitId].wappalyzer : false;
        
    //     return(check ? 
    //                 <p style={{color: "green", marginTop: "15px"}}>&#10003;</p> : 
    //                 <p style={{color: "red", marginTop: "15px"}}>&#10005;</p>
    //     );
    // }

    // Returns a boolean as a green V or red X.
    const booleanToCheckmark = (bool) => {
        return(bool ? 
                <p style={{color: "green", marginTop: "15px"}}>&#10003;</p> : 
                <p style={{color: "red", marginTop: "15px"}}>&#10005;</p>
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

        // if (data.length === 0) {
        //     return (
        //         <h5 className="ml-3 mt-3">
        //             Domain '{domainName}' was not yet crawled or does not exist.
        //         </h5>
        //     );
        // }

        return (
            <div id="Search-Data-Div" alt="Div when search is finished and data has been returned.">
                <Row>
                    <Col className='mt-4'>
                        <div>
                            <h1 className="mt-5 mb-4">{domainName}</h1>
                        </div>
                        <div className="mt-5">
                            <Table className="table-timeline" bordered hover size="sm">
                                <thead className="header-timeline-table">
                                    <tr>
                                        <th>Visit id</th>
                                        <th>Crawl time</th>
                                        <th>Final url</th>
                                        <th>Status<br/> Content crawl</th>
                                        <th>Status<br/> DNS crawl</th>
                                        <th>Status<br/> SMTP crawl</th>
                                        <th>Status<br/> Wappalyzer</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    { data.map((data, index) => (
                                        <tr key={index}>
                                            <td>
                                                <Link to={{pathname: `/details/${data.visitId}`}}>{data.visitId}</Link>
                                            </td>
                                            <td>
                                                { data.requestTimeStamp ? moment(data.requestTimeStamp).format("YYYY/MM/DD HH:mm:ss") : '' }
                                            </td>
                                            <td>
                                                { data.finalUrl }
                                            </td>
                                            <td>
                                                { booleanToCheckmark(data.crawlStatus.muppets) }
                                            </td>
                                            <td>
                                                { booleanToCheckmark(data.crawlStatus.dns) }
                                            </td>
                                            <td>
                                                { booleanToCheckmark(data.crawlStatus.smtp) }
                                            </td>
                                            <td>
                                                { booleanToCheckmark(data.crawlStatus.wappalyzer) }
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </Table>
                        </div>
                    </Col>
                </Row>
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
