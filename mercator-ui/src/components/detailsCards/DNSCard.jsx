import {useEffect, useState} from "react";
import {Col, Row, Table} from "react-bootstrap";
import BorderWrapper from "react-border-wrapper";
import moment from "moment";
import api from "../../services/api";
import { checkDataObject, renderDataBoolean } from "../../services/Util";

const DNSCard = (props) => {

    const visitId = props.visitId

    const [data, setData] = useState({});

    useEffect(() => {
        const handlerData = async () => {

            const url = `/dnsCrawlResults/search/findByVisitId?visitId=${visitId}`;
            await api.get(url)
                .then((resp) => {
                    if(resp.status === 200) {
                        setData(resp.data);
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

    // Render data.records and manage necessary logic.
    const renderRecords = () => {

        const renderAtRecords = () => { // Entering inside a ul element.
            if(data.allRecords['@']) {
                return (
                    <li>
                        <span className="font-weight-bold">
                            @ records:
                        </span>
                        {
                            Object.entries(data.allRecords['@'].records).map(([key, value]) => {
                                return(
                                    <ul key={key}>
                                        <li>
                                            <span className="font-weight-bold">
                                                { key }:&nbsp;
                                            </span>
                                            { 
                                                value.map((data, index) => {
                                                    return(
                                                        <ul key={index}>
                                                            <li>
                                                                { data }
                                                            </li>
                                                        </ul>
                                                    )
                                                })
                                            }
                                        </li>
                                    </ul>
                                )
                            })
                        }
                    </li>
                    
                );
            } 
        }

        const renderWwwRecords = () => {  // Entering inside a ul element.
            if(data.allRecords.www) {
                return(
                    <li className="mt-2">
                        <span className="font-weight-bold">
                            www records:
                        </span>
                        {
                            Object.entries(data.allRecords.www.records).map(([key, value]) => {
                                return (
                                    <ul key={key}>
                                        <li>
                                            <span className="font-weight-bold">
                                                { key }:&nbsp;
                                            </span>
                                            { 
                                                value.map((data, index) => {
                                                    return (
                                                        <ul key={index}>
                                                            <li>
                                                                { data }
                                                            </li>
                                                        </ul>
                                                    )
                                                })
                                            }
                                        </li>
                                    </ul>
                                );
                            })
                        }          
                    </li>
                );
            }
        }

        return (
            <>
                {
                    renderAtRecords()
                }
                {
                    renderWwwRecords()
                }
            </>
        )
    }

    // Writing HTML on a function base so we can define logic more easily.
    const renderHTML = () => {

        const render = () => {
            if(checkDataObject(data)) {
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
                                <th scope='row'>
                                    Id
                                </th>
                                <td>
                                    { data.id }
                                </td>
                            </tr>

                            <tr>
                                <th scope="row">
                                    OK
                                </th>
                                {
                                    renderDataBoolean(data.ok) // td element
                                }
                            </tr>

                            <tr>
                                <th scope='row'>
                                    All Records
                                </th>
                                <td>
                                    <button 
                                        className="more-info"
                                        onClick={() => setOpenRecords(openRecords => !openRecords)} // Toggle openRecords boolean
                                    > 
                                        More info
                                    </button>

                                    {
                                        openRecords && ( // if openRecords === true, render
                                            data.allRecords && ( // if data.allRecords exists, continue
                                                <ul className="dns-records">
                                                    { renderRecords() }
                                                </ul>
                                            )
                                        )
                                    }
                                </td>
                            </tr>

                            <tr>
                                <th scope="row">
                                    Problem
                                </th>
                                <td className="problem-dns">
                                    { data.problem }
                                </td>
                            </tr>

                            <tr>
                                <th scope="row">
                                    Crawl timestamp
                                </th>
                                <td>
                                    { data.crawlTimestamp ? moment(data.crawlTimestamp).format("DD/MM/YYYY HH:mm:ss") : '' }
                                </td>
                            </tr>

                            <tr>
                                <th scope="row">

                                </th>
                                <td>

                                </td>
                            </tr>
                        </tbody>
                    </Table>

                    {
                        data.geoIps && ( // if data.allRecords exists, render
                            <>
                                {
                                    data.geoIps.map((item, index) => {
                                        return(
                                            <Row key={index}>
                                                <Col>
                                                    <h5 className="mt-3 text-left">GEO IPs</h5>

                                                    <Table size="sm" borderless>
                                                        <tbody className="text-md-left">
                                                            <tr>
                                                                <th scope="row">
                                                                    Asn
                                                                </th>
                                                                <td>
                                                                    { item.asn }
                                                                </td>
                                                            </tr>

                                                            <tr>
                                                                <th scope="row">
                                                                    Country
                                                                </th>
                                                                <td>
                                                                    { item.country }
                                                                </td>
                                                            </tr>

                                                            <tr>
                                                                <th scope="row">
                                                                    Ip
                                                                </th>
                                                                <td>
                                                                    { item.ip }
                                                                </td>
                                                            </tr>

                                                            <tr>
                                                                <th scope="row">
                                                                    Record type
                                                                </th>
                                                                <td>
                                                                    { item.recordType }
                                                                </td>
                                                            </tr>
                                                            
                                                            <tr>
                                                                <th scope="row">
                                                                    Asn organisation
                                                                </th>
                                                                <td>
                                                                    { item.asnOrganisation }
                                                                </td>
                                                            </tr>
                                                        </tbody>
                                                    </Table>
                                                </Col>
                                            </Row>
                                        )
                                    })
                                }
                            </>
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
