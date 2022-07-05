import {Card, Col, Row, Table} from "react-bootstrap";
import moment from "moment";
import {useEffect, useState} from "react";
import api from "../../services/api";
import { checkObjectIsFalsy } from "../../services/Util";

const SMTPCard = (props) => {

    const visitId = props.visitId

    const [data, setData] = useState({});
    const [openHosts, setOpenHosts] = useState(false);
    const [openExtension, setOpenExtension] = useState([]);

    // api SMTP
    useEffect(() => {
        const handlerData = async () => {

            const url = `/smtpCrawlResults/search/findFirstByVisitId?visitId=${visitId}`;
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

    // Handle open/close 'Extensions' click.
    const handleOpenExtension = (index) => {
        // Destructuring the renderTech array, requesting the index and flipping the boolean.
        const bool = openExtension[index];

        setOpenExtension({
            ...openExtension,
            [index]: !bool
        });
    }
    
    // Render data.servers[index].hosts[index].supportedExtensions
    const renderExtensions = (extensions, index) => { // Inside ul element
        if(!extensions || !extensions.length) {
            return (
                ""
            );
        }
        return (
            <>
                <button 
                    className='more-info'
                    onClick={() => handleOpenExtension(index)} // Toggle openHosts boolean
                > 
                    Extensions
                </button>

                {
                    openExtension[index] && ( // if openHosts[index] === true, render
                        <ul>
                            {
                                extensions.map((data, index) => {
                                    return (
                                        <li key={index}>
                                            { data }
                                        </li>
                                    )
                                })
                            }
                        </ul>
                    )
                }
            </>
        );
    }

    // Render data.servers[index].hosts
    const renderItemHosts = (hosts) => { // Inside li element
        if(!hosts || !hosts.length) {
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
                                hosts.map((data, index) => {
                                    return(
                                        <div key={index} id='SMTP-Hosts-Card'>
                                            <li>
                                                <span className="font-weight-bold">
                                                    Ip:&nbsp;
                                                </span>
                                                { data.ip }
                                            </li>

                                            <li>
                                                <span className="font-weight-bold">
                                                    Asn:&nbsp;
                                                </span>
                                                { data.asn }
                                            </li>

                                            <li>
                                                <span className="font-weight-bold">
                                                    Country:&nbsp;
                                                </span>
                                                { data.country }
                                            </li>

                                            <li>
                                                <span className="font-weight-bold">
                                                    AsnOrganisation:&nbsp;
                                                </span>
                                                { data.asnOrganisation }
                                            </li>

                                            <li>
                                                <span className="font-weight-bold">
                                                    Banner:&nbsp;
                                                </span>
                                                { data.banner }
                                            </li>

                                            <li>
                                                <span className="font-weight-bold">
                                                    ConnectOK:&nbsp;
                                                </span>
                                                { data.connectOK }
                                            </li>

                                            <li>
                                                <span className="font-weight-bold">
                                                    ConnectReplyCode:&nbsp;
                                                </span>
                                                { data.connectReplyCode }
                                            </li>

                                            {
                                                renderExtensions(data.supportedExtensions, index)
                                            }
                                            
                                            <li>
                                                <span className="font-weight-bold">
                                                    Ipversion:&nbsp;
                                                </span>
                                                { data.ipVersion }
                                            </li>

                                            <li>
                                                <span className="font-weight-bold">
                                                    StartLsOk:&nbsp; 
                                                </span>
                                                { data.startTlsOk }
                                            </li>

                                            <li>
                                                <span className="font-weight-bold">
                                                    StarTLsReplyCode:
                                                </span>
                                                { data.startTlsReplyCode }
                                            </li>

                                            <li>
                                                <span className="font-weight-bold">
                                                    ErrorMessage:&nbsp;
                                                </span>
                                                { data.err }
                                            </li>

                                            <li>
                                                <span className="font-weight-bold">
                                                    ConnectionTime:&nbsp;
                                                </span>
                                                { data.connectionTimeMs }
                                            </li>

                                            <li>
                                                <span className="font-weight-bold">
                                                    Software:&nbsp;
                                                </span>
                                                { data.software }
                                            </li>

                                            <li className="mb-3">
                                                <span className="font-weight-bold">
                                                    SoftwareVersion:&nbsp;
                                                </span>
                                                { data.softwareVersion }
                                            </li>
                                            
                                        </div>
                                    )
                                })
                            }
                        </ul>
                    )
                }
            </>
        );
    } 

    // Render data.servers
    const renderDataServers = (servers) => { // Inside td element 
        if(!servers || !servers.length) {
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
                        <>
                            {
                                servers.map((item, index) => {
                                    return(
                                        <ul className="mt-3" key={index}>
                                            <li>
                                                <span className="font-weight-bold">
                                                    Hostname:&nbsp;
                                                </span>
                                                { item.hostName }
                                            </li>
            
                                            <li>
                                                <span className="font-weight-bold">
                                                    Priority:&nbsp;
                                                </span>
                                                { item.priority }
                                            </li>
            
                                            <li>
                                                { renderItemHosts(item.hosts) }
                                            </li>
            
                                        </ul>
                                    )
                                })
                            }
                        </>
                    )
                }
            </>
        );
    }

    // Writing HTML on a function base so we can define logic more easily.
    const renderHTML = () => {
        if(checkObjectIsFalsy(data)) {
            return (
                <Card.Body>No data for this visit.</Card.Body>
            );
        }

        return (
            <div className="smtp-table">
                <Table size='sm' borderless>
                    <tbody className="text-left">
                        <tr>
                            <th>
                                Crawl Timestamp
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
                            <th scope="row">
                                Crawl status
                            </th>
                            <td>
                                { data.crawlStatus }
                            </td>
                        </tr>
                        
                        <tr>
                            <th scope='row'>
                                Server
                            </th>
                            <td>
                                { renderDataServers(data.servers) }
                            </td>
                        </tr>
                    </tbody>
                </Table>
            </div>
        );
    }

    // This file's HTML return.
    return (
        <Row>
            <Col className='mt-4'>
                <Card>
                    <Card.Header as="h2" className="h5">Email crawl</Card.Header>
                    { renderHTML() }
                </Card>
            </Col>
        </Row>
    );
}

export default SMTPCard;
