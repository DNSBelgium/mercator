import {Row, Col, Table, Accordion, Button} from "react-bootstrap";
import {useEffect, useState} from "react";
import api from "../../services/api";
import { renderDataBoolean } from "../../services/Util";

const Wappalyzer = (props) => {

    const visitId = props.visitId
    const {openTechnologies, setOpenTechnologies, openUrls, setOpenUrls} = props;

    const [data, setData] = useState({});

    useEffect(() => {
        const handlerData = async () => {
            let response;
            try {
                response = await api.get(`/wappalyzerResults/${visitId}`);
            } catch (e) {
                console.log(e)
            }
            setData(response === undefined ? null : response.data);
        };
        handlerData();
    }, [visitId]);

    if (!data) {
        return (
            <>
                <Row>
                    <Col className='mt-2 mb-3'>
                        <span className="font-weight-bold">Wappalyzer: </span><span>no data for this visit</span>
                    </Col>
                </Row>
            </>
        )
    } else {
        return (
            <>
                <h4 className="mb-3 text-md-left">Wappalyzer</h4>
                <div className="card_content">
                    <Row>
                        <Col>
                            <Table size="sm" borderless>
                                <tbody className="text-left">
                                <tr>
                                    <th scope="row">Url</th>
                                    <td>{data.url}</td>
                                </tr>
                                <tr>
                                    <th scope="row">Ok</th>
                                    { renderDataBoolean(data.ok) }
                                </tr>
                                <tr>
                                    <th scope="row">Technologies</th>
                                    <td>
                                        {data.technologies ? data.technologies.length ?
                                                <Accordion>
                                                    <Accordion.Toggle as={Button} id="button-technologies" variant="link"
                                                                      eventKey="3"
                                                                      onClick={() => setOpenTechnologies(!openTechnologies)}>More
                                                        info</Accordion.Toggle>
                                                    <Accordion.Collapse eventKey="3" in={openTechnologies}>
                                                        <div className="technologies-list">
                                                            {data.technologies.map((item, index) => (
                                                                <Accordion key={index}>
                                                                    <ul className="mb-2 mt-2">
                                                                        <li><Accordion.Toggle as={Button}
                                                                                              id="button-technologies-items"
                                                                                              size="sm" variant="link"
                                                                                              eventKey="1"
                                                                                              className="font-weight-bold">{item.name}</Accordion.Toggle>
                                                                        </li>
                                                                        <Accordion.Collapse eventKey="1">
                                                                            <ul>
                                                                                <div key={index}>
                                                                                    <span
                                                                                        className="font-weight-bold">slug: </span><span> {item.slug}</span>
                                                                                    <li><span
                                                                                        className="font-weight-bold">name: </span><span>{item.name}</span>
                                                                                    </li>
                                                                                    <li><span
                                                                                        className="font-weight-bold">confidence: </span><span>{item.confidence}</span>
                                                                                    </li>
                                                                                    <li><span
                                                                                        className="font-weight-bold">version: </span><span>{item.version}</span>
                                                                                    </li>
                                                                                    <li><span
                                                                                        className="font-weight-bold">icon: </span><span>{item.icon}</span>
                                                                                    </li>
                                                                                    <li><span
                                                                                        className="font-weight-bold">website: </span><span>{item.website}</span>
                                                                                    </li>
                                                                                    <li><span
                                                                                        className="font-weight-bold">cpe: </span><span>{item.cpe} </span>
                                                                                    </li>
                                                                                    <li><span
                                                                                        className="font-weight-bold">categories: </span>
                                                                                    </li>
                                                                                    <ul> {item.categories.map((itemHost, index) => (
                                                                                        <li className="list-categories"
                                                                                            key={index.toString()}>
                                                                                            <li><span
                                                                                                className="font-weight-bold">Id: </span><span>{itemHost.id}</span>
                                                                                            </li>
                                                                                            <li><span
                                                                                                className="font-weight-bold">slug: </span><span>{itemHost.slug}</span>
                                                                                            </li>
                                                                                            <li><span
                                                                                                className="font-weight-bold">name: </span><span>{itemHost.name}</span>
                                                                                            </li>
                                                                                            <br/>
                                                                                        </li>
                                                                                    ))}
                                                                                    </ul>
                                                                                </div>
                                                                            </ul>
                                                                        </Accordion.Collapse>
                                                                    </ul>
                                                                </Accordion>
                                                            ))}
                                                        </div>
                                                    </Accordion.Collapse>
                                                </Accordion>
                                                : ''
                                            : ''}
                                    </td>
                                </tr>
                                <tr>
                                    <th scope="row">Error</th>
                                    <td className="error-wappalyzer"> {data.error}</td>
                                </tr>
                                <tr>
                                    <th scope="row">Urls</th>
                                    <td>
                                        {data.urls ?
                                            <Accordion>
                                                <Accordion.Toggle as={Button} id="button-urls" variant="link"
                                                                  eventKey="3" onClick={() => setOpenUrls(!openUrls)}>More
                                                    info</Accordion.Toggle>
                                                <Accordion.Collapse eventKey="3" in={openUrls}>
                                                    <ul className="mb-2 mt-2">
                                                        {data.urls ? Object.entries(data.urls).map((item, index) => (
                                                            <li key={item}><span
                                                                className="font-weight-bold">{item[0]}</span>
                                                                <ul>
                                                                    {item[1] ? (
                                                                        Object.entries(item[1]).map(([key, value]) => (
                                                                            <li key={key}><span
                                                                                className="font-weight-bold">{key}: </span><span>{value}</span>
                                                                            </li>))
                                                                    ) : ''}
                                                                </ul>
                                                            </li>
                                                        )) : ''}
                                                    </ul>
                                                </Accordion.Collapse>
                                            </Accordion>
                                            : ''}
                                    </td>
                                </tr>
                                </tbody>
                            </Table>
                        </Col>
                    </Row>
                </div>
            </>
        )
    }
}

export default Wappalyzer;
