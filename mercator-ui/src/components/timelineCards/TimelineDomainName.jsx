import {Col, Row, Table} from "react-bootstrap";
import React, {useEffect, useState} from 'react';
import {Link} from 'react-router-dom';
import api from "../../services/api";
import moment from "moment";

const TimelineDomainName = (props) => {

    const domainName = localStorage.getItem("search"); // Received from SearchField.jsx textInput Ref hook.

    const [data, setData] = useState([]);
    const [status, setStatus] = useState([]);
    const [processing, setProcessing] = useState(true);

    // Function to sort rows by crawl date desc
    function sortVisits(a, b) {
        return Date.parse(b.requestTimestamp) - Date.parse(a.requestTimestamp);
    }

    useEffect(() => {
        setProcessing(true);

        if (!domainName) {
            setProcessing(false);
            return;
        }

        const handlerData = async () => {
            let response;
            try {
                response = await api.get(`/dispatcherEvents/search/findDispatcherEventByDomainName?domainName=${domainName}`);
            } catch (e) {
                console.log(e)
            }
            let data = response.data._embedded.dispatcherEvents;

            data.sort(sortVisits);

            setData(data);
            setStatus([]); // Clear status state for a new domain
            setProcessing(false);
        };
        handlerData();
    }, [domainName])

    useEffect(() => {
        data.forEach(async (item, i, arr) => {
            let response;
            try {
                response = await api.get(`/status/${item.visitId}`);
            } catch (e) {
                console.log(e)
            }
            setStatus(array => [...array, response.data])
        });
    }, [data])

    if (!domainName || processing === true) return null;

    if (data.length === 0) return (
        <>
            This domain was not yet crawled or does not exist.
        </>
    );

    let statusMap = status.reduce((obj, data) => ({...obj, [data.visit_id]: data}), {})

    // Fill HTML Table's 'Content crawl'.
    const fillMuppets = (visitId) => {
        if(statusMap[visitId] === undefined) {
            return (<p style={{marginTop: "15px"}}>Loading</p>);
        }

        let check = visitId in statusMap ? statusMap[visitId].muppets : false;
        
        return(check ? 
                    <p style={{color: "green", marginTop: "15px"}}>&#10003;</p> : 
                    <p style={{color: "red", marginTop: "15px"}}>&#10005;</p>
        );
    }

    // Fill HTML Table's 'DNS crawl'.
    const fillDns = (visitId) => {
        if(statusMap[visitId] === undefined) {
            return (<p style={{marginTop: "15px"}}>Loading</p>);
        }

        let check = visitId in statusMap ? statusMap[visitId].dns : false
        
        return(check ? 
                    <p style={{color: "green", marginTop: "15px"}}>&#10003;</p> : 
                    <p style={{color: "red", marginTop: "15px"}}>&#10005;</p>
        );
    }

    // Fill HTML Table's 'SMTP crawl'.
    const fillSmtp = (visitId) => {
        if(statusMap[visitId] === undefined) {
            return (<p style={{marginTop: "15px"}}>Loading</p>);
        }

        let check = visitId in statusMap ? statusMap[visitId].smtp : false
        
        return(check ? 
                    <p style={{color: "green", marginTop: "15px"}}>&#10003;</p> : 
                    <p style={{color: "red", marginTop: "15px"}}>&#10005;</p>
        );
    }

    // Fill HTML Table's 'Wappalyzer'.
    const fillWappalyzer = (visitId) => {
        if(statusMap[visitId] === undefined) {
            return (<p style={{marginTop: "15px"}}>Loading</p>);
        }

        let check = visitId in statusMap ? statusMap[visitId].wappalyzer : false;
        
        return(check ? 
                    <p style={{color: "green", marginTop: "15px"}}>&#10003;</p> : 
                    <p style={{color: "red", marginTop: "15px"}}>&#10005;</p>
        );
    }

    // Rendering HTML on a JS Function base, so we can define logic.
    const renderHtml = () => {
        if(processing === true) {
            return (
                <div id="Crawling-Div" alt="Div when processing is true.">
                    <h2 style={{marginTop: '2rem', marginLeft: '2rem'}}>Crawling ...</h2>
                </div>
            );
        }
        else {
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
                                        { data.map(data => (
                                            <tr key={data.visitId}>
                                                <td>
                                                    <Link to={{pathname: `/details/${data.visitId}`}}>{data.visitId}</Link>
                                                </td>
                                                <td>
                                                    { data.requestTimestamp ? moment(data.requestTimestamp).format("YYYY/MM/DD HH:mm:ss") : '' }
                                                </td>
                                                <td>
                                                    { data.domainName }
                                                </td>
                                                <td>
                                                    { fillMuppets(data.visitId) }
                                                    {/* <input readOnly type="checkbox"
                                                        checked={data.visitId in statusMap ? statusMap[data.visitId].muppets : false}/> */}
                                                </td>
                                                <td>
                                                    { fillDns(data.visitId) }
                                                    {/* <input readOnly type="checkbox"
                                                        checked={data.visitId in statusMap ? statusMap[data.visitId].dns : false}/> */}
                                                </td>
                                                <td>
                                                    { fillSmtp(data.visitId) }
                                                    {/* <input readOnly type="checkbox"
                                                        checked={data.visitId in statusMap ? statusMap[data.visitId].smtp : false}/> */}
                                                </td>
                                                <td>
                                                    { fillWappalyzer(data.visitId) }
                                                    {/* <input readOnly type="checkbox"
                                                        checked={data.visitId in statusMap ? statusMap[data.visitId].wappalyzer : false}/> */}
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
