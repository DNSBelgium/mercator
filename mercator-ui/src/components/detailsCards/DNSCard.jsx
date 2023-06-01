import {useEffect, useState} from "react";
import {Card, Col, Row, Table} from "react-bootstrap";
import moment from "moment";
import api from "../../services/api";
import {checkObjectIsFalsy, renderDataBoolean} from "../../services/Util";
import DnsRequestDataTable from "../DnsRequestTableBody";

const DNSCard = (props) => {
    const visitId = props.visitId

    const [data, setData] = useState({});

    useEffect(() => {

        const fetchData = async () => {
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
        
        fetchData();
    }, [visitId]);

    const {openRecords, setOpenRecords} = props;

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
                            <li key={index}>
                                { response.recordData }
                                <br/>
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

        if(checkObjectIsFalsy(data)) {
            return (<Card.Body>No data for this visit.</Card.Body>)
        }

        return (
            <Card.Body className="dns-table">

                <Table
                    size="sm"
                >

                    <thead>
                    <tr>
                        <th>
                            prefix
                        </th>
                        <th>
                            result
                        </th>
                        <th>
                            record type
                        </th>
                        <th>
                            ttl
                        </th>
                        <th>
                            record data
                        </th>
                        <th>
                            country
                        </th>
                        <th>
                            adn
                        </th>
                        <th>
                            adn organisation
                        </th>
                        <th>
                            ip
                        </th>
                        <th>
                            ip version
                        </th>
                    </tr>
                    </thead>
                    <tbody>
                    {data.map((item, index) => (
                        <DnsRequestDataTable request={item} index={index}>
                        </DnsRequestDataTable>
                    ))}
                    </tbody>
                </Table>

            </Card.Body>
        );
    }

    // This file's HTML return.
    return (
        <Row>
            <Col className='mt-4'>
                <Card className="card">
                    <Card.Header as="h2" className="h5">DNS crawl</Card.Header>
                    {
                        renderHTML()
                    }
                </Card>
            </Col>
        </Row>
    );
}

export default DNSCard;
