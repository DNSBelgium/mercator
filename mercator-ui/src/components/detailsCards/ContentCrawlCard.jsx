import BorderWrapper from 'react-border-wrapper'
import Wappalyzer from "./Wappalyzer";
import moment from "moment";
import {Row, Col, Table, Accordion, Button} from "react-bootstrap";
import React, {useEffect, useState} from "react";
import api from "../../services/api";

const ContentCard = (props) => {

    const visitId = props.visitId

    const [data, setData] = useState([]);

    useEffect(() => {
        const handlerData = async () => {
            let response;
            try {
                response = await api.get(`/contentCrawlResults/search/findByVisitId?visitId=${visitId}`);
            } catch (e) {
                console.log(e)
            }
            setData(response === undefined ? null : response.data._embedded.contentCrawlResults);
        };
        handlerData();
    }, [])

    const {openMetrics, setOpenMetrics, openTechnologies, setOpenTechnologies, openUrls, setOpenUrls} = props;
    const prefix = window._env_.REACT_APP_MUPPETS_HOST + "/" || '';
    const topElement = <p className='top-element'>Content crawl</p>
    if (!data || data.length === 0) {
        return (
            <>
                <Row>
                    <Col className='mt-4'>
                        <BorderWrapper borderWidth="3px" borderRadius="0px" innerPadding="30px"
                                       topElement={topElement}
                                       topPosition={0.07} topOffset="15px" topGap="15px">
                            <p>no data for this visit</p>
                        </BorderWrapper>
                    </Col>
                </Row>
            </>
        )
    } else {
        return (
            <>
                {data.map(data => (
                    <Row key={data.visitId}>
                        <Col className='mt-4'>
                            <BorderWrapper borderWidth="3px" borderRadius="0px" innerPadding="30px"
                                           topElement={topElement}
                                           topPosition={0.07} topOffset="15px" topGap="15px">
                                <div className="content-table">
                                    <Table size="sm" borderless>
                                        <tbody className="text-left">
                                        <tr>
                                            <th scope="row">Id</th>
                                            <td>{data.id}</td>
                                        </tr>
                                        <tr>
                                            <th scope="row">Metrics Json</th>
                                            <td>{data.metricsJson ?
                                                <Accordion>
                                                    <Accordion.Toggle as={Button} id="button-metricsJson"
                                                                      variant="link" eventKey="3"
                                                                      onClick={() => setOpenMetrics(!openMetrics)}>More
                                                        info</Accordion.Toggle>
                                                    <Accordion.Collapse eventKey="3" in={openMetrics}>
                                                        <ul className="mb-2 mt-2"> {data.metricsJson ? Object.entries(JSON.parse(data.metricsJson)).map(item => (
                                                            <li key={item}><span
                                                                className="font-weight-bold">{item[0]}:</span>
                                                                <span>{item[1]}</span></li>
                                                        )) : ''}
                                                        </ul>
                                                    </Accordion.Collapse>
                                                </Accordion>
                                                : ''}
                                            </td>
                                        </tr>
                                        <tr>
                                            <th scope="row">URL</th>
                                            <td>{data.url}</td>
                                        </tr>
                                        <tr>
                                            <th scope="row">Ok</th>
                                            <td>{data.ok ? 'true' : 'false'}</td>
                                        </tr>
                                        <tr>
                                            <th scope="row">Problem</th>
                                            <td className="problem-content">{data.problem}</td>
                                        </tr>
                                        <tr>
                                            <th scope="row">Final Url</th>
                                            <td>{data.finalUrl}</td>
                                        </tr>
                                        <tr>
                                            <th scope="row">HTML length</th>
                                            <td>{data.htmlLength}</td>
                                        </tr>
                                        <tr>
                                            <th scope="row">Crawl Timestamp</th>
                                            <td>{data.crawlTimestamp ? moment(data.crawlTimestamp).format("DD/MM/YYYY HH:mm:ss") : ''}</td>
                                        </tr>
                                        <tr>
                                            <th scope="row">Crawled from</th>
                                            <td>ipv4: {data.ipv4}, ipv6: {data.ipv6}</td>
                                        </tr>
                                        <tr>
                                            <th scope="row">Browser version</th>
                                            <td>{data.browserVersion}</td>
                                        </tr>
                                        </tbody>
                                    </Table>
                                    <div className="mb-4 mt-4 ml-4">
                                        <a href="#" className="mr-5 ml-5"
                                           onClick={() => window.open(prefix + data.screenshotKey)}>Screenshot</a>
                                        <a href="#" className="mr-5 ml-5"
                                           onClick={() => window.open(prefix + data.htmlKey)}>HTML</a>
                                        <a href="#" className="ml-5"
                                           onClick={() => window.open(prefix + data.harKey)}>Har</a>
                                    </div>

                                    <Wappalyzer visitId={visitId} openTechnologies={openTechnologies}
                                                setOpenTechnologies={setOpenTechnologies} openUrls={openUrls}
                                                setOpenUrls={setOpenUrls}/>

                                </div>
                            </BorderWrapper>
                        </Col>
                    </Row>
                ))}
            </>
        )
    }
}

export default ContentCard;
