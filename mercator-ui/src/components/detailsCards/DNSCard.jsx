import {useEffect, useState} from "react";
import {Col, Row, Table} from "react-bootstrap";
import BorderWrapper from "react-border-wrapper";
import moment from "moment";
import api from "../../services/api";
import { checkObjectIsFalsy, renderDataBoolean } from "../../services/Util";

const DNSCard = (props) => {

    const visitId = props.visitId

    const [data, setData] = useState({});

    useEffect(() => {
        const handlerData = async () => {

            const url = `/requests/search/findByVisitId?visitId=${visitId}`;
            await api.get(url)
                .then((resp) => {
                    if(resp.status === 200) {
                        setData(resp.data._embedded.requests);
                    }
                })
                .catch((ex) => {
                    console.log(ex);
                })            
        };
        
        handlerData();
    }, [visitId]);

    const {openRecords, setOpenRecords} = props;
    const topElement = <p className='top-element'> DNS crawl </p> // BorderWrapper title

    // Handles rendering of data[x].responses[x].responseGeoIps list.
    const renderGeoIps = (geoIpList) => { // Inside li element.
        if(!checkObjectIsFalsy(geoIpList)) {
            return(
                <div className="geo-ip-data">
                    {
                        geoIpList.map((geoIp, index) => {
                            return (
                                <p key={index}>
                                    IPv: { geoIp.ipVersion } <br/>
                                    Country: { geoIp.country } <br/>
                                    ASN: { geoIp.asn } <br/>
                                    ASN Organisation: { geoIp.asnOrganisation }
                                </p>
                            )
                        })
                    }
                </div>
            );
        }
    }

    // Define some logic for incoming data[x]'s response and responseGeoIps data.
    const renderResponses = (request) => { // Inside ul element.
        return(
            <>
                {
                    request.responses.map((response, index) => {
                        return(
                            <li key={index} className="mb-1">
                                { response.recordData } <br/>
                                TTL: { response.ttl }
                                {
                                    renderGeoIps(response.responseGeoIps) 
                                } 
                            </li>
                        )
                    })
                }
            </>
        );
    }

    // Handles rendering of a prefix and its corresponding recordTypes and recordData from data[x].
    const renderDataPerPrefix = (prefix) => { // Inside section element.
        return(
            data.map((request, index) => {
                return(
                    <div key={index}>
                        {
                            request.prefix === prefix && request.responses.length >= 1 && (
                                <div className="prefix-data-div">
                                    <p>
                                        { request.recordType }
                                    </p>
                                    
                                    <ul className="mb-3"> 
                                        { renderResponses(request) }
                                    </ul>
                                </div>
                            )
                        }
                    </div>
                );
            })
        );
    }

    // Handles creating separate divs per unique prefix from data[x].prefix
    const renderRecords = () => { // Inside div element
        let distinctPrefixes = [];
        for(let i = 0; i < data.length; i++) {

            // If a new prefix is found and it has responses then add it to the array.
            // This is to 'filter out' empty data.
            if(!distinctPrefixes.includes(data[i].prefix) && data[i].responses.length >= 1) {
                    distinctPrefixes.push(data[i].prefix);
            }
        }

        return(
            distinctPrefixes.map((prefix, index) => {
                return(
                    <div 
                        key={index} 
                        className="ml-3"
                        id="prefix-div"
                    >
                        <span>{ prefix }</span>

                        <section>
                            { renderDataPerPrefix(prefix) }
                        </section>
                    </div>
                );
            })
        );
    }

    const checkDataHasResponses = (data) => {
        for(let i = 0; i < data.length; i++) {
            if (data[i].responses.length >= 1) return true;
        }
        return false;
    }

    // Writing HTML on a function base so we can define logic more easily.
    const renderHTML = () => {

        const render = () => {
            if(checkObjectIsFalsy(data)) {
                return (<p>No data for this visit.</p>)
            }

            return (
                <div className="dns-table">
                    <Table
                        size="sm"
                        borderless
                    >
                        <tbody className="text-left">
                            
                            <tr>
                                <th scope="row">
                                    rcode
                                </th>
                                <td>
                                    { data[0].rcode }
                                </td>
                            </tr>

                            <tr>
                                <th scope="row">
                                    OK
                                </th>
                                {
                                    renderDataBoolean(data[0].ok) // td element
                                }
                            </tr>                            

                            <tr>
                                <th scope="row">
                                    Problem
                                </th>
                                <td className="defined-error">
                                    { data[0].problem }
                                </td>
                            </tr>

                            <tr>
                                <th scope="row">
                                    Crawl timestamp
                                </th>
                                <td>
                                    { data[0].crawlTimestamp ? moment(data[0].crawlTimestamp).format("DD/MM/YYYY HH:mm:ss") : '' }
                                </td>
                            </tr>

                            <tr>
                                <th scope='row'>
                                    Record data and Geo IP's
                                </th>
                                <td>
                                    {
                                        checkDataHasResponses(data) && ( // Don't render 'More Info' button if there are is no response data.
                                            <button 
                                                className="more-info"
                                                onClick={() => setOpenRecords(openRecords => !openRecords)} // Toggle openRecords boolean
                                            > 
                                                More info
                                            </button>
                                        )
                                    }
                                </td>
                            </tr>
                        </tbody>
                    </Table>

                    {
                        openRecords && ( // if openRecords === true, render
                            <div>
                                { renderRecords() }
                            </div>
                        )
                    }
                </div>
            );
        }

        return(
            <>
                <Row>
                    <Col className='mt-4'>
                        <BorderWrapper
                            borderWidth="3px" 
                            borderRadius="0px"
                            innerPadding="30px" 
                            topElement={topElement}
                            topPosition={0.07} 
                            topOffset="15px" 
                            topGap="15px">

                            { 
                                render() 
                            }
                        </BorderWrapper>
                    </Col>
                </Row>
            </>
        );
    }

    // This file's HTML return.
    return (
        <>
            {
                renderHTML()
            }
        </>
    );
}

export default DNSCard;
