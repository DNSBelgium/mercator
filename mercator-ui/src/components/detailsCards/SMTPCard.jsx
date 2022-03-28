import {Accordion, Button, Card, Col, Row, Table} from "react-bootstrap";
import BorderWrapper from "react-border-wrapper";
import moment from "moment";
import {useEffect, useState} from "react";
import api from "../../services/api";
import { checkObjectIsFalsy } from "../../services/Util";

const SMTPCard = (props) => {

    const visitId = props.visitId

    const [data, setData] = useState({});
    const [openHosts, setOpenHosts] = useState(false);

    // api SMTP
    useEffect(() => {
        const handlerData = async () => {

            const url = `/smtpCrawlResults/search/findByVisitId?visitId=${visitId}`;
            await api.get(url)
                .then((resp) => {
                    if(resp.status === 200) {
                        setData(resp === undefined ? null : resp.data);
                    }
                })
                .catch((ex) => {
                    console.log(ex);
                });
        };
        
        handlerData();
    }, [visitId])

    const {openServer, setOpenServer} = props;
    const topElement = <p className='top-element'>SMTP crawl</p>

    data.servers ? 
        data.servers.length ?

            item.hosts ? 
                item.hosts.length ? 

                itemHost.supportedExtensions ? 
                    itemHost.supportedExtensions.length ? 
                    STUFF : 
                    "" : 
                    "" : 
            "" : 
            "" : 
        "" : 
        ""

    

    

    if(data.servers) {
        if(data.servers.length) {

            if(item.hosts) {
                if(item.hosts.length) {
                    
                    if(itemHost.supportedExtensions) {
                        if(itemHost.supportedExtensions.length) {
                            STUFF
                        }
                        else {
                            ""
                        }
                    }
                    else {
                        ""
                    }

                }
                else {
                    ""
                }
            }
            else {
                ""
            }

        }
        else {
            ""
        }
    }
    else {
        ""
    }

    const renderItemHosts = () => { // Inside li element
        if(!item.hosts || !item.hosts.length) {
            return (
                ""
            );
        }
        return (
            <>
                <button 
                    className='more-info'
                    onClick={() => setOpenHosts(openHosts => !openHosts)} // Toggle openHosts boolean
                > 
                    Hosts
                </button>

                {
                    openHosts && ( // if openHosts === true, render
                        <ul>
                            {
                                item.hosts.map((item, index) => {
                                    <div key={index}>
                                        <li>
                                            <span className="font-weight-bold">
                                                Ip:&nbsp;
                                            </span>
                                            { item.ip }
                                        </li>

                                        <li>
                                            <span className="font-weight-bold">
                                                Asn:&nbsp;
                                            </span>
                                            { item.asn }
                                        </li>

                                        <li>
                                            <span className="font-weight-bold">
                                                Country:&nbsp;
                                            </span>
                                            { item.country }
                                        </li>

                                        <li>
                                            <span className="font-weight-bold">
                                                AsnOrganisation:&nbsp;
                                            </span>
                                            { item.asnOrganisation }
                                        </li>

                                        <li>
                                            <span className="font-weight-bold">
                                                Banner:&nbsp;
                                            </span>
                                            { item.banner }
                                        </li>

                                        <li>
                                            <span className="font-weight-bold">
                                                ConnectOK:&nbsp;
                                            </span>
                                            { item.connectOK }
                                        </li>

                                        <li>
                                            <span className="font-weight-bold">
                                                ConnectReplyCode:&nbsp;
                                            </span>
                                            { item.connectReplyCode }
                                        </li>

                                    </div>
                                })
                            }
                        </ul>
                    )
                }
            </>
        );
    }

    const renderDataServers = () => { // Inside Card.Body element
        return (
            <>
                {
                    data.servers.map((item, index) => {
                        <Card key={index}>
                            <ul className="mt-3">
                                <li>
                                    <span className="font-weight-bold">
                                        Hostname: 
                                    </span>
                                    { item.hostName }
                                </li>

                                <li>
                                    <span className="font-weight-bold">
                                        Priority: 
                                    </span>
                                    { item.priority }
                                </li>

                                <li>
                                    { renderItemHosts() }
                                </li>
                            </ul>
                        </Card>
                    })
                }
            </>
        );
    }

    const renderServer = () => { // Inside td element 
        if(!data.servers || !data.servers.length) {
            return (
                ""
            );
        }
        return (
            <>
                <button 
                    className='more-info'
                    onClick={() => setOpenServer(openServer => !openServer)} // Toggle openServer boolean
                > 
                    More info
                </button>

                {
                    openServer && ( // if openServer === true, render
                        <Card.Body>
                            {
                                renderDataServers()
                            }
                        </Card.Body>
                    )
                }
            </>
        );
    }

    const renderHTML = () => {

        const render = () => {
            if(checkObjectIsFalsy(data)) {
                return (
                    <p>No data for this visit.</p>
                );
            }

            return (
                <div className="smtp-table">
                    <Table size='sm' borderless>
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
                                <th scope='row'>
                                    Crawl timestamp
                                </th>
                                <td>
                                    { // Ternary
                                        data.crawlTimestamp ? 
                                            moment(data.crawlTimestamp).format("YYYY-MM-DD HH:mm:ss") : 
                                            '' 
                                    }
                                </td>
                            </tr>

                            <tr>
                                <th scope='row'>
                                    Server
                                </th>
                                <td>
                                    { renderServer() }
                                </td>
                            </tr>

                            <tr>
                                <th scope='row'>

                                </th>
                                <td>
                                    
                                </td>
                            </tr>

                        </tbody>
                    </Table>
                </div>
            );
        }

        return (
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
                            topGap="15px"
                        >
                            { 
                                render() 
                            }
                        </BorderWrapper>
                    </Col>
                </Row>
            </>
        );
    }

    const renderOldHTML = () => {
        if (checkObjectIsFalsy(data)) {
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
                                                                <Card className="mb-3" key={index}>
                                                                    <ul className="mt-3">
                                                                        <li>
                                                                            <span className="font-weight-bold">Hostname: </span>{item.hostName}
                                                                        </li>
                                                                        <li>
                                                                            <span className="font-weight-bold">Priority: </span><span>{item.priority}</span>
                                                                        </li>
                                                                        { item.hosts ? item.hosts.length ?
                                                                                <Accordion>
                                                                                    <li>
                                                                                        <Accordion.Toggle as={Button}
                                                                                            id="button-hosts"
                                                                                            variant="link"
                                                                                            eventKey="1"
                                                                                            className="font-weight-bold"
                                                                                        >
                                                                                            Hosts
                                                                                        </Accordion.Toggle>
                                                                                    </li>
                                                                                    <Accordion.Collapse eventKey="1">
                                                                                        <ul>{item.hosts ? item.hosts.map((itemHost, index) => (
                                                                                            <div key={index}>
                                                                                                <li>
                                                                                                    <span className="font-weight-bold">Ip: </span><span>{itemHost.ip}</span>
                                                                                                </li>
    
                                                                                                <li>
                                                                                                    <span className="font-weight-bold">Asn: </span><span>{itemHost.asn}</span>
                                                                                                </li>
    
                                                                                                <li>
                                                                                                    <span className="font-weight-bold">Country: </span><span>{itemHost.country}</span>
                                                                                                </li>
    
                                                                                                <li>
                                                                                                    <span className="font-weight-bold">AsnOrganisation: </span><span>{itemHost.asnOrganisation}</span>
                                                                                                </li>
    
                                                                                                <li>
                                                                                                    <span className="font-weight-bold">Banner: </span><span>{itemHost.banner}</span>
                                                                                                </li>
    
                                                                                                <li>
                                                                                                    <span className="font-weight-bold">ConnectOK: </span><span>{itemHost.connectOK}</span>
                                                                                                </li>
    
                                                                                                <li>
                                                                                                    <span className="font-weight-bold">ConnectReplyCode: </span><span>{itemHost.connectReplyCode}</span>
                                                                                                </li>
                                                                                                {itemHost.supportedExtensions ? itemHost.supportedExtensions.length ?
                                                                                                        <Accordion>
                                                                                                            <div>
                                                                                                                <Accordion.Toggle
                                                                                                                    as={Button}
                                                                                                                    id="button-extensions"
                                                                                                                    variant="link"
                                                                                                                    eventKey="1"
                                                                                                                    className="font-weight-bold">Extensions</Accordion.Toggle>
                                                                                                            </div>
                                                                                                            <Accordion.Collapse
                                                                                                                eventKey="1">
                                                                                                                <ul>{itemHost.supportedExtensions.map((itemExtensions, index) => (
                                                                                                                    <li key={index}>{itemExtensions}</li>
                                                                                                                ))}
                                                                                                                </ul>
                                                                                                            </Accordion.Collapse>
                                                                                                        </Accordion>
                                                                                                        : ''
                                                                                                    : ''}
                                                                                                <li>
                                                                                                    <span className="font-weight-bold">Ipversion: {itemHost.ipVersion}</span>
                                                                                                </li>
    
                                                                                                <li>
                                                                                                    <span className="font-weight-bold">StartLsOk: {itemHost.startTlsOk}</span>
                                                                                                </li>
    
                                                                                                <li>
                                                                                                    <span className="font-weight-bold">StarTLsReplyCode: {itemHost.startTlsReplyCode}</span>
                                                                                                </li>
    
                                                                                                <li>
                                                                                                    <span className="font-weight-bold">ErrorMessage: {itemHost.err}</span>
                                                                                                </li>
    
                                                                                                <li>
                                                                                                    <span className="font-weight-bold">ConnectionTime: {itemHost.connectionTimeMs}</span>
                                                                                                </li>
    
                                                                                                <li>
                                                                                                    <span className="font-weight-bold">Software: {item.software}</span>
                                                                                                </li>
    
                                                                                                <li className="mb-3">
                                                                                                    <span className="font-weight-bold">SoftwareVersion: {item.softwareVersion}</span>
                                                                                                </li>
                                                                                            </div>
                                                                                        )) : ''}
                                                                                        </ul>
                                                                                    </Accordion.Collapse>
                                                                                </Accordion>
                                                                                : ''
                                                                            : ''}
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
    
    return (
        <>
            {
                // renderHTML()
                renderOldHTML()
            }
        </>
    );
}

export default SMTPCard;
