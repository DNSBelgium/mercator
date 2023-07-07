import {Card, Col, Row, Table} from "react-bootstrap";
import moment from "moment";
import {useEffect, useState} from "react";
import api from "../../services/api";
import { checkObjectIsFalsy } from "../../services/Util";

const SMTPCard = (props) => {

    const visitId = props.visitId

    const [visit, setVisit] = useState({});
    const [host, setHost] = useState([]);
    const [conversation, setConversation] = useState({});
    const [openHosts, setOpenHosts] = useState(false);
    const [openExtension, setOpenExtension] = useState([]);

    // api SMTP
    useEffect(() => {
        const handlerData = async () => {
            const smtpVisitUrl = `/smtpVisits/search/findByVisitId?visitId=${visitId}`;
            await api.get(smtpVisitUrl)
                .then((resp) => {
                    if(resp.status === 200) {
                        setVisit(resp.data);
                    }
                });

            const smtpHostUrl = `/smtpHosts/search/findByVisitVisitId?visit_id=${visitId}`;
            await api.get(smtpHostUrl)
                .then(async (resp) => {
                    if (resp.status === 200) {
                        let hosts = resp.data._embedded.smtpHosts;
                        let conversations = {};
                        setHost(hosts);
                        for (let i = 0; i < hosts.length; i++) {
                            const smtpConversationUrl = `/smtpConversations/search/findByHostId?host_id=${hosts[i].id}`;
                            await api.get(smtpConversationUrl)
                                .then((resp) => {
                                    if (resp.status === 200) {
                                        conversations[hosts[i].id] = resp.data;
                                    }
                                });
                        }
                        setConversation(resp === undefined ? null : conversations);
                    }
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
                                extensions.map((visit, index) => {
                                    return (
                                        <li key={index}>
                                            { visit }
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
    const renderConversations = (conversation) => { // Inside li element
        if(checkObjectIsFalsy(conversation)) {
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
                    Conversation
                </button>

                {
                    openHosts && ( // if openHosts === true, render
                        <ul>

                                        <div id='SMTP-Hosts-Card'>
                                            <li>
                                                <span className="font-weight-bold">
                                                    Ip:&nbsp;
                                                </span>
                                                { conversation.ip }
                                            </li>

                                            <li>
                                                <span className="font-weight-bold">
                                                    Asn:&nbsp;
                                                </span>
                                                { conversation.asn }
                                            </li>

                                            <li>
                                                <span className="font-weight-bold">
                                                    Country:&nbsp;
                                                </span>
                                                { conversation.country }
                                            </li>

                                            <li>
                                                <span className="font-weight-bold">
                                                    AsnOrganisation:&nbsp;
                                                </span>
                                                { conversation.asnOrganisation }
                                            </li>

                                            <li>
                                                <span className="font-weight-bold">
                                                    Banner:&nbsp;
                                                </span>
                                                { conversation.banner }
                                            </li>

                                            <li>
                                                <span className="font-weight-bold">
                                                    ConnectOK:&nbsp;
                                                </span>
                                                { conversation.connectOK ? "Yes" : "No"}
                                            </li>

                                            <li>
                                                <span className="font-weight-bold">
                                                    ConnectReplyCode:&nbsp;
                                                </span>
                                                { conversation.connectReplyCode }
                                            </li>

                                            {
                                                renderExtensions(conversation.supportedExtensions)
                                            }

                                            <li>
                                                <span className="font-weight-bold">
                                                    Ipversion:&nbsp;
                                                </span>
                                                { conversation.ipVersion }
                                            </li>

                                            <li>
                                                <span className="font-weight-bold">
                                                    StartLsOk:&nbsp;
                                                </span>
                                                { conversation.startTlsOk ? "Yes" : "No"}
                                            </li>

                                            <li>
                                                <span className="font-weight-bold">
                                                    StarTLsReplyCode:
                                                </span>
                                                { conversation.startTlsReplyCode }
                                            </li>

                                            <li>
                                                <span className="font-weight-bold">
                                                    ErrorMessage:&nbsp;
                                                </span>
                                                { conversation.errorMessage }
                                            </li>

                                            <li>
                                                <span className="font-weight-bold">
                                                    Error:&nbsp;
                                                </span>
                                                { conversation.error }
                                            </li>

                                            <li>
                                                <span className="font-weight-bold">
                                                    ConnectionTime:&nbsp;
                                                </span>
                                                { conversation.connectionTimeMs } ms
                                            </li>

                                            <li>
                                                <span className="font-weight-bold">
                                                    Software:&nbsp;
                                                </span>
                                                { conversation.software }
                                            </li>

                                            <li className="mb-3">
                                                <span className="font-weight-bold">
                                                    SoftwareVersion:&nbsp;
                                                </span>
                                                { conversation.softwareVersion }
                                            </li>

                                        </div>

                        </ul>
                    )
                }
            </>
        );
    } 

    // Render data.servers
    const renderHosts = (servers) => { // Inside td element
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
                                                    From mx:&nbsp;
                                                </span>
                                                { item.fromMx ? "Yes" : "No" }
                                            </li>
            
                                            <li>
                                                <span className="font-weight-bold">
                                                    Priority:&nbsp;
                                                </span>
                                                { item.priority }
                                            </li>
            
                                            <li>
                                                { renderConversations(conversation[item.id]) }
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
        if(checkObjectIsFalsy(visit)) {
            return (
                <>No data for this visit.</>
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
                                visit.timestamp ?
                                    moment(visit.timestamp).format("YYYY-MM-DD HH:mm:ss") :
                                    '' 
                            }
                            </td>
                        </tr>

                        <tr>
                            <th scope="row">
                                Crawl status
                            </th>
                            <td>
                                { visit.crawlStatus }
                            </td>
                        </tr>
                        
                        <tr>
                            <th scope='row'>
                                Hosts
                            </th>
                            <td>
                                { renderHosts(host) }
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
                    <Card.Body>{ renderHTML() }</Card.Body>
                </Card>
            </Col>
        </Row>
    );
}

export default SMTPCard;
