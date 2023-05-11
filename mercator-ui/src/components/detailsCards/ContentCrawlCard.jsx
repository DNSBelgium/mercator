import Wappalyzer from "./Wappalyzer";
import moment from "moment";
import {Row, Col, Table, Card} from "react-bootstrap";
import {useEffect, useState} from "react";
import api from "../../services/api";
import PopupComponent from "../Warning";
import {differenceBetweenTwoDates, renderDataBoolean} from '../../services/Util';
// import DOMPurify from 'dompurify';

//js enum for visibility state
export const VisibiltyState = Object.freeze({
    None: 'false',
    Screenshot: 'Screenshot',
    WarningPopup: 'WarningPopup',
})


const ContentCrawlCard = (props) => {
    const visitId = props.visitId

    const [visibility, setVisible] = useState(VisibiltyState.None)
    const [data, setData] = useState([]);

    function toggleVisibilityWarningMessage() {
        setVisible(VisibiltyState.None)
    }

    function toggleVisibility(state) {
        if (visibility === state) {
            setVisible(VisibiltyState.None)
        } else {
            setVisible(state)
        }
    }

    async function fetchRawHtml(htmlKey) {
        const DEV_URL = window._env_.REACT_APP_MUPPETS_HOST;
        const fetchHtml = async () => {
            try {
                if (htmlKey !== null) {
                    const response = await fetch(`${DEV_URL}/${htmlKey}`, {
                        mode: 'cors',
                        headers: {
                            'Accept': 'text/plain',
                            'Content-Type': 'text/plain',
                        }
                    });
                    const htmlContent = await response.text();
                    return htmlContent
                }
            } catch (error) {
                console.error(error);
            }
        };

        const html = await fetchHtml()

        const pre = document.createElement("pre")

        const encodedHtml = encodeURIComponent(html);
        const newWindow = window.open(`data:text/plain;charset=utf-8,${encodedHtml}`);

        newWindow.document.title = "Raw HTML";
        newWindow.document.body.append(pre)

        pre.id = "generatedPreForHtml";
        pre.innerHTML = html;
        pre.textContent = html;
    }

    const showScreenshot = (item) => {
        // URL for development / local environment.
        const DEV_URL = window._env_.REACT_APP_MUPPETS_HOST;
        const LOCAL_URL = 'http://localhost:4566/mercator-muppets';

        if (item.screenshotKey !== null) {
            return (
                <img
                    id="screenshotPreview"
                    src={`${DEV_URL}/${item.screenshotKey}`}
                    alt={`Thumbnail of ${item.visitId}`}
                >
                </img>
            )
        }
        return (
            <p>No image found</p>
        );
    }

    useEffect(() => {
        const handlerData = async () => {

            const url = `/contentCrawlResults/search/findByVisitId?visitId=${visitId}`;
            await api.get(url)
                .then((resp) => {
                    if (resp.status === 200) {
                        setData(resp === undefined ? null : resp.data._embedded.contentCrawlResults);
                    }
                })
                .catch((ex) => {
                    console.log(ex);
                });
        };

        handlerData();
    }, [visitId]);

    // Variables for HTML
    const {openMetrics, setOpenMetrics, openTechnologies, setOpenTechnologies, openUrls, setOpenUrls} = props; // Used deciding open/close of Accordions.
    const prefix = window._env_.REACT_APP_MUPPETS_HOST + "/" || '';
    // const prefix = "http://localhost:4566/mercator-muppets/"
    const title = <Card.Header as="h2" className="h5">Content crawl</Card.Header>;

    // Writing HTML on a function base so we can define logic more easily.
    const renderHTML = () => {

        if (!data.length || data.length === 0) {
            return (
                <Row>
                    <Col className='mt-4'>
                        <Card>
                            {title}
                            <Card.Body>No data for this visit</Card.Body>
                        </Card>
                    </Col>
                </Row>
            )
        }

        return (
            <>
                {
                    data.map((data) => {
                        return (
                            <Row key={data.visitId}>
                                <Col className='mt-4'>
                                    <Card>
                                        {title}
                                        <Card.Body className='content-table'>

                                            <Table size='sm' borderless>
                                                <tbody className='text-left'>

                                                <tr>
                                                    <th scope='row'>
                                                        Id
                                                    </th>
                                                    <td>
                                                        {data.id}
                                                    </td>
                                                </tr>

                                                <tr>
                                                    <th scope='row'>
                                                        Metrics Json
                                                    </th>
                                                    <td>
                                                        {
                                                            data.metricsJson ? // metricsJson exists? render, else empty string.
                                                                <>
                                                                    <button
                                                                        className='more-info'
                                                                        onClick={() => setOpenMetrics(openMetrics => !openMetrics)} // Toggle openMetrics boolean
                                                                    >
                                                                        More info
                                                                    </button>

                                                                    {   // if openMetrics === true, render
                                                                        openMetrics && (
                                                                            <div id='metrics-json-content'>
                                                                                <ul className="mb-2 mt-2">
                                                                                    {
                                                                                        Object.entries(JSON.parse(data.metricsJson)).map((item, index) => {
                                                                                            return (
                                                                                                <li key={index}>
                                                                                                        <span
                                                                                                            className='font-weight-bold'
                                                                                                        >
                                                                                                            {item[0]}:
                                                                                                        </span>
                                                                                                    <span> {item[1]}</span>
                                                                                                </li>
                                                                                            )
                                                                                        })
                                                                                    }
                                                                                </ul>
                                                                            </div>
                                                                        )
                                                                    }
                                                                </> :
                                                                "" // metricsJson exists? render, else empty string.
                                                        }
                                                    </td>
                                                </tr>
                                                <tr>
                                                    <th scope="row">URL</th>
                                                    <td>{data.url}</td>
                                                </tr>
                                                <tr>
                                                    <th scope="row">Ok</th>
                                                    {renderDataBoolean(data.ok)}
                                                </tr>
                                                <tr>
                                                    <th scope="row">Problem</th>
                                                    <td className="defined-error">{data.problem}</td>
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
                                                    <th scope="row">Retries</th>
                                                    <td>{data.retries}</td>
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

                                                <button
                                                    id="previewScreenshotBTN"
                                                    className="mr-5 ml-5 content-card-link-button"
                                                    onClick={() => toggleVisibility(VisibiltyState.Screenshot)}
                                                >
                                                    Screenshot preview
                                                </button>

                                                <button
                                                    className="mr-5 ml-5 content-card-link-button"
                                                    onClick={() => window.open(prefix + data.screenshotKey)}
                                                >
                                                    Screenshot
                                                </button>

                                                <button
                                                    id="previewRawHtmlBTN"
                                                    className="mr-5 ml-5 content-card-link-button"
                                                    onClick={() => fetchRawHtml(data.htmlKey)}
                                                >
                                                    HTML raw data
                                                </button>

                                                <button
                                                    className="mr-5 ml-5 content-card-link-button"
                                                    onClick={() => toggleVisibility(VisibiltyState.WarningPopup)}
                                                >
                                                    HTML
                                                </button>

                                                <button
                                                    className="ml-5 content-card-link-button"
                                                    onClick={() => window.open(prefix + data.harKey)}
                                                >
                                                    Har
                                                </button>

                                            </div>

                                            <div id="previewWrapper">
                                                {
                                                    visibility === VisibiltyState.Screenshot && (
                                                        showScreenshot(data)
                                                    )
                                                }
                                                {
                                                    visibility === VisibiltyState.WarningPopup && (
                                                        <PopupComponent item={data}
                                                                        onClickCancel={toggleVisibilityWarningMessage}/>
                                                    )
                                                }
                                            </div>

                                            <div id='wappalyzer'>
                                                <Wappalyzer
                                                    visitId={visitId}
                                                    openTechnologies={openTechnologies}
                                                    setOpenTechnologies={setOpenTechnologies}
                                                    openUrls={openUrls}
                                                    setOpenUrls={setOpenUrls}
                                                />
                                            </div>

                                        </Card.Body>
                                    </Card>
                                </Col>
                            </Row>
                        );
                    })
                }
            </>
        );
    }


    // This file's HTML return.
    return (
        <>
            { renderHTML() }
        </>
    );
}

export default ContentCrawlCard;
