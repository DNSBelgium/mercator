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

            // const url = `/dnsCrawlResults/search/findByVisitId?visitId=${visitId}`;
            const url = `dns-crawler?visitId=${visitId}`;
            await api.get(url)
                .then((resp) => {
                    console.log(resp);
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

    // Render 'Record Data' from data.prefixAndData.values.recordDataAndTtl
    const renderRecordDataAndTtl = (value) => { // Inside li element
        return (
            Object.entries(value).map(([ip, ttl]) => {
                return (
                    <ul key={ip}>
                        <li className="mb-2">
                            { ip }
                            <br/>
                            TTL: { ttl }
                        </li>
                    </ul>
                );
            })
        );
    }

    // Render the values of a 'prefix' from data.prefixAndData.values
    const renderValues = (values) => { // Inside ul element
        const elementsToReturn = [];
        
        for(let i = 0; i < values.length; i++) {
            let element =   <li key={i}
                                className="mb-2"
                            >
                                <strong>{values[i].recordType}</strong>
                                <br/>
                                rcode: {values[i].rcode}
                                <br/>
                                Record data: { renderRecordDataAndTtl(values[i].recordDataAndTtl) }
                            </li>
                
            elementsToReturn.push(element);
        }
        return elementsToReturn;
    }

    // Render the prefix and it's corresponding data from data.prefixAndData
    const renderPrefixAndData = () => { // Inside ul element
        return (
            Object.entries(data.prefixAndData).map(([prefix, value]) => {
                return (
                    <ul key={prefix}>
                        <li>
                            <strong>{ prefix } records:</strong>

                            <ul>
                                { renderValues(value) }
                            </ul>
                        </li>
                    </ul>
                );
            })
        );
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
                                <th scope="row">
                                    Problem
                                </th>
                                <td className="defined-error">
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
                                <th scope='row'>
                                    All Records
                                </th>
                                <td>
                                    {
                                        !checkObjectIsFalsy(data.prefixAndData) && ( // Don't render 'More Info' button if there are is no data.
                                            <button 
                                                className="more-info"
                                                onClick={() => setOpenRecords(openRecords => !openRecords)} // Toggle openRecords boolean
                                            > 
                                                More info
                                            </button>
                                        )
                                    }

                                    {
                                        openRecords && ( // if openRecords === true, render
                                            <ul>
                                                { renderPrefixAndData() }
                                            </ul>
                                        )
                                    }
                                </td>
                            </tr>
                        </tbody>
                    </Table>

                    {
                        data.geoIps && ( // if data.geoIps exists, render
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
                                                                    IP version
                                                                </th>
                                                                <td>
                                                                    { item.ipVersion }
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
