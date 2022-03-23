import React, {useEffect, useState} from "react";
import {Accordion, Button, Col, Row, Table} from "react-bootstrap";
import BorderWrapper from "react-border-wrapper";
import moment from "moment";
import api from "../../services/api";
import { renderDataBoolean } from "../../services/Util";

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
    }, [visitId])

    const {openRecords, setOpenRecords} = props;
    const topElement = <p className='top-element'> DNS crawl </p>
    if (data === {}) {
        return (
            <>
                <Row>
                    <Col className='mt-4'>
                        <BorderWrapper borderWidth="3px" borderRadius="0px" innerPadding="30px" topElement={topElement}
                                       topPosition={0.07} topOffset="15px" topGap="15px">
                            <p>no data for this visit</p>
                        </BorderWrapper>
                    </Col>
                </Row>
            </>
        )
    }

    return (
        <Row>
            <Col className='mt-4'>
                <BorderWrapper borderWidth="3px" borderRadius="0px" innerPadding="30px" topElement={topElement}
                               topPosition={0.07} topOffset="15px" topGap="15px">
                    <div className="dns-table">
                        <Table size="sm" borderless>
                            <tbody className="text-left">
                            <tr>
                                <th scope="row">Id</th>
                                <td>{data.id}</td>
                            </tr>
                            <tr>
                                <th scope="row">Ok</th>
                                { renderDataBoolean(data.ok) }
                            </tr>
                            <tr>
                                <th scope="row">All records</th>
                                <td>
                                    <Accordion>
                                        <Accordion.Toggle as={Button} id="button-records" variant="link"
                                                          eventKey="3" onClick={() => setOpenRecords(!openRecords)}>More
                                            info</Accordion.Toggle>
                                        <Accordion.Collapse eventKey="3" in={openRecords}>
                                            <ul className="dns-records ">
                                                {data.allRecords ?
                                                    <li><span className="font-weight-bold">@: records</span>
                                                        <ul>
                                                            {data.allRecords && data.allRecords['@'] ? (
                                                                Object.entries(data.allRecords['@'].records).map(([key, value]) => (
                                                                    <li key={key}>
                                                                                <span
                                                                                    className="font-weight-bold">{key}: </span>
                                                                        {value.map((data, index) => (
                                                                            <ul key={index.toString()}>
                                                                                <li>{data}</li>
                                                                            </ul>
                                                                        ))}
                                                                    </li>))
                                                            ) : ''}
                                                        </ul>
                                                    </li>
                                                    : ''}
                                                {data.allRecords && data.allRecords.www ?
                                                    <li><span className="font-weight-bold">www: records</span>
                                                        <ul>
                                                            {data.allRecords ? (
                                                                Object.entries(data.allRecords.www.records).map(([key, value]) => (
                                                                    <li key={key}><span
                                                                        className="font-weight-bold">{key}: </span>
                                                                        {value.map((data, index) => (
                                                                            <ul key={index.toString()}>
                                                                                <li>{data}</li>
                                                                            </ul>
                                                                        ))}
                                                                    </li>))
                                                            ) : ''}
                                                        </ul>
                                                    </li>
                                                    : ''}
                                            </ul>
                                        </Accordion.Collapse>
                                    </Accordion>
                                </td>
                            </tr>
                            <tr>
                                <th scope="row">Problem</th>
                                <td className="problem-dns">{data.problem}</td>
                            </tr>
                            <tr>
                                <th scope="row">Crawl timestamp</th>
                                <td>{data.crawlTimestamp ? moment(data.crawlTimestamp).format("DD/MM/YYYY HH:mm:ss") : ''}</td>
                            </tr>
                            </tbody>
                        </Table>
                        {data.geoIps ? (data.geoIps.map((item, index) => (
                                <Row key={index}>
                                    <Col>
                                        <h5 className="mt-3 text-left">GEO IPs</h5>
                                        <Table size="sm" borderless>
                                            <tbody className="text-md-left">
                                            <tr>
                                                <th scope="row">Asn</th>
                                                <td>{item.asn}</td>
                                            </tr>
                                            <tr>
                                                <th scope="row">Country</th>
                                                <td>{item.country}</td>
                                            </tr>
                                            <tr>
                                                <th scope="row">Ip</th>
                                                <td>{item.ip}</td>
                                            </tr>
                                            <tr>
                                                <th scope="row">Record type</th>
                                                <td>{item.recordType}</td>
                                            </tr>
                                            <tr>
                                                <th scope="row">Asn organisation</th>
                                                <td>{item.asnOrganisation}</td>
                                            </tr>
                                            </tbody>
                                        </Table>
                                    </Col>
                                </Row>
                            ))
                        ) : ""}
                    </div>
                </BorderWrapper>
            </Col>
        </Row>
    )
}

export default DNSCard;
