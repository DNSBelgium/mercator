import {Accordion, Button, Card, Col, Row, Table} from "react-bootstrap";
import BorderWrapper from "react-border-wrapper";
import moment from "moment";
import React, {useEffect, useState} from "react";
import api from "../../services/api";

const SMTPCard = (props) => {

    const visitId = props.visitId

    const [data, setData] = useState({});

    // api SMTP
    useEffect(() => {
        const handlerData = async () => {
            let response;
            try {
                response = await api.get(`/smtpCrawlResults/search/findByVisitId?visitId=${visitId}`);
            } catch (e) {
                console.log(e)
            }
            setData(response === undefined ? null : response.data);
        };
        handlerData();
    }, [])

    const {openServer, setOpenServer} = props;
    const topElement = <p className='top-element'>SMTP crawl</p>
    if (!data || data === {}) {
        return (
            <>
                <Row>
                    <Col className='mt-4'>
                        <BorderWrapper borderWidth="3px" borderRadius="0px" innerPadding="30px" topElement={topElement}
                                       topPosition={0.07} topOffset="15px" topGap="15px">
                            <p> no data for this visit</p>
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
                    <div className="smtp-table">
                        <Table size="sm" borderless>
                            <tbody className="text-left">
                            <tr>
                                <th scope="row">Id</th>
                                <td>{data.id}</td>
                            </tr>
                            <tr>
                                <th scope="row">Crawl timestamp</th>
                                <td>{data.crawlTimestamp ? moment(data.crawlTimestamp).format("YYYY-MM-DD HH:mm:ss") : ''}</td>
                            </tr>
                            <tr>
                                <th scope="row">Server</th>
                                <td>
                                    {data.servers ?
                                        data.servers.length ?
                                            <Accordion>
                                                <Accordion.Toggle as={Button} id="button-servers" variant="link"
                                                                  eventKey="3"
                                                                  onClick={() => setOpenServer(!openServer)}>More
                                                    info</Accordion.Toggle>
                                                <Accordion.Collapse eventKey="3" in={openServer}>
                                                    <Card.Body>
                                                        {data.servers ? data.servers.map((item, index) => (
                                                            <Card className="mb-3">
                                                                <ul className="mt-3">
                                                                    <li key={index.toString()}>
                                                                        <span
                                                                            className="font-weight-bold">Hostname: </span><span> {item.hostName}</span>
                                                                        <li><span
                                                                            className="font-weight-bold">Priority: </span><span>{item.priority}</span>
                                                                        </li>
                                                                        {item.hosts ? item.hosts.length ?
                                                                                <Accordion>
                                                                                    <li><Accordion.Toggle as={Button}
                                                                                                          id="button-hosts"
                                                                                                          variant="link"
                                                                                                          eventKey="1"
                                                                                                          className="font-weight-bold">Hosts</Accordion.Toggle>
                                                                                    </li>
                                                                                    <Accordion.Collapse eventKey="1">
                                                                                        <ul>{item.hosts ? item.hosts.map((itemHost, index) => (
                                                                                            <li key={index.toString()}>
                                                                                                <span
                                                                                                    className="font-weight-bold">Ip: </span><span>{itemHost.ip}</span>
                                                                                                <li><span
                                                                                                    className="font-weight-bold">Asn: </span><span>{itemHost.asn}</span>
                                                                                                </li>
                                                                                                <li><span
                                                                                                    className="font-weight-bold">Country: </span><span>{itemHost.country}</span>
                                                                                                </li>
                                                                                                <li><span
                                                                                                    className="font-weight-bold">AsnOrganisation: </span><span>{itemHost.asnOrganisation}</span>
                                                                                                </li>
                                                                                                <li><span
                                                                                                    className="font-weight-bold">Banner: </span><span>{itemHost.banner}</span>
                                                                                                </li>
                                                                                                <li><span
                                                                                                    className="font-weight-bold">ConnectOK: </span><span>{itemHost.connectOK}</span>
                                                                                                </li>
                                                                                                <li><span
                                                                                                    className="font-weight-bold">ConnectReplyCode: </span><span>{itemHost.connectReplyCode}</span>
                                                                                                </li>
                                                                                                {itemHost.supportedExtensions ? itemHost.supportedExtensions.length ?
                                                                                                        <Accordion>
                                                                                                            <li>
                                                                                                                <Accordion.Toggle
                                                                                                                    as={Button}
                                                                                                                    id="button-extensions"
                                                                                                                    variant="link"
                                                                                                                    eventKey="1"
                                                                                                                    className="font-weight-bold">Extensions</Accordion.Toggle>
                                                                                                            </li>
                                                                                                            <Accordion.Collapse
                                                                                                                eventKey="1">
                                                                                                                <ul>{itemHost.supportedExtensions.map((itemExtensions, index) => (
                                                                                                                    <li key={index.toString()}>{itemExtensions}</li>
                                                                                                                ))}
                                                                                                                </ul>
                                                                                                            </Accordion.Collapse>
                                                                                                        </Accordion>
                                                                                                        : ''
                                                                                                    : ''}
                                                                                                <li><span
                                                                                                    className="font-weight-bold">Ipversion: {itemHost.ipVersion}</span>
                                                                                                </li>
                                                                                                <li><span
                                                                                                    className="font-weight-bold">StartLsOk: {itemHost.startTlsOk}</span>
                                                                                                </li>
                                                                                                <li><span
                                                                                                    className="font-weight-bold">StarTLsReplyCode: {itemHost.startTlsReplyCode}</span>
                                                                                                </li>
                                                                                                <li><span
                                                                                                    className="font-weight-bold">ErrorMessage: {itemHost.err}</span>
                                                                                                </li>
                                                                                                <li><span
                                                                                                    className="font-weight-bold">ConnectionTime: {itemHost.connectionTimeMs}</span>
                                                                                                </li>
                                                                                                <li><span
                                                                                                    className="font-weight-bold">Software: {item.software}</span>
                                                                                                </li>
                                                                                                <li><span
                                                                                                    className="font-weight-bold">SoftwareVersion: {item.softwareVersion}</span>
                                                                                                </li>
                                                                                            </li>
                                                                                        )) : ''}
                                                                                        </ul>
                                                                                    </Accordion.Collapse>
                                                                                </Accordion>
                                                                                : ''
                                                                            : ''}
                                                                    </li>
                                                                </ul>
                                                            </Card>
                                                        )) : ""}
                                                    </Card.Body>
                                                </Accordion.Collapse>
                                            </Accordion>
                                            : ""
                                        : ""}
                                </td>
                            </tr>
                            <tr>
                                <th scope="row">Crawl status</th>
                                <td>{data.crawlStatus}</td>
                            </tr>
                            </tbody>
                        </Table>
                    </div>
                </BorderWrapper>
            </Col>
        </Row>
    )
}

export default SMTPCard;
