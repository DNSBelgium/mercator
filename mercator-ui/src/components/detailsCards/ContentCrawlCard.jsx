import Wappalyzer from "./Wappalyzer";
import moment from "moment";
import {Row, Col, Table, Card, Modal, Button} from "react-bootstrap";
import {useEffect, useState} from "react";
import api from "../../services/api";
import HtmlRenderWarning from "../HtmlRenderWarning";
import ContentCrawlLinkButton from "../ContentCrawlLinkButton";
import {differenceBetweenTwoDates, renderDataBoolean} from '../../services/Util';

//js enum for visibility state
export const VisibiltyState = Object.freeze({
    None: 'false',
    Screenshot: 'Screenshot',
    HtmlRenderWarning: 'WarningPopup',
})

const ContentCrawlCard = (props) => {
    const visitId = props.visitId
    const URL = window._env_.REACT_APP_MUPPETS_HOST;
    const [visibility, setVisible] = useState(VisibiltyState.None)
    const [data, setData] = useState([]);
    const [show, setShow] = useState(false);

    const handleClose = () => setShow(false);
    const handleShow = () => setShow(true);

    function openHtmlInNewTab(item) {
        window.open(URL + "/" + item.htmlKey)
    }

    function toggleVisibility(state) {
        if (visibility === state) {
            setVisible(VisibiltyState.None)
        } else {
            setVisible(state)
        }
    }

    async function fetchRawHtml(htmlKey) {
        const fetchHtml = async () => {
            try {
                if (htmlKey !== null) {
                    const response = await fetch(`${URL}/${htmlKey}`, {
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

        const newWindow = window.open();
        newWindow.document.title = "Raw HTML";
        newWindow.document.body.append(pre)

        pre.id = "generatedPreForHtml";
        pre.innerHTML = html;
        pre.textContent = html;
    }

    const showScreenshot = (item) => {
        if (item.screenshotKey !== null) {
            return (
                <img
                    id="screenshotPreview"
                    src={`${URL}/${item.screenshotKey}`}
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
                                            <div id="contentCrawlContentLinks" className="mb-4 mt-4 ml-4">
                                                <ContentCrawlLinkButton
                                                    id="previewScreenshotBTN"
                                                    text="View screenshot"
                                                    onClickHandler={() => {
                                                        toggleVisibility(VisibiltyState.Screenshot)
                                                    }}
                                                    hasIcon={false}
                                                />

                                                <ContentCrawlLinkButton
                                                    id="newTabScreenshotBTN"
                                                    text="Open screenshot"
                                                    onClickHandler={() => {
                                                        window.open(prefix + data.screenshotKey);
                                                        setVisible(VisibiltyState.None);
                                                    }}
                                                    hasIcon={true}
                                                />

                                                <ContentCrawlLinkButton
                                                    id="previewRawHtmlBTN"
                                                    text="Open raw html"
                                                    onClickHandler={() => {
                                                        fetchRawHtml(data.htmlKey);
                                                        setVisible(VisibiltyState.None)
                                                    }}
                                                    hasIcon={true}
                                                />

                                                <ContentCrawlLinkButton
                                                    id="openRenderWarning"
                                                    text=" View page"
                                                    onClickHandler={() => {
                                                        handleShow()
                                                    }}
                                                    hasIcon={false}
                                                />

                                                <ContentCrawlLinkButton
                                                    id="harContentNewTab"
                                                    text="Open har"
                                                    onClickHandler={() => {
                                                        window.open(prefix + data.harKey);
                                                        setVisible(VisibiltyState.None)
                                                    }}
                                                    hasIcon={true}
                                                />

                                            </div>

                                            <div id="previewWrapper">
                                                {
                                                    visibility === VisibiltyState.Screenshot && (
                                                        showScreenshot(data)
                                                    )
                                                }

                                                <Modal
                                                    show={show}
                                                    onHide={handleClose}
                                                    onShow={() => setVisible(VisibiltyState.None)}
                                                >
                                                    <Modal.Header closeButton>
                                                        <Modal.Title>Render the html in a new tab ?</Modal.Title>
                                                    </Modal.Header>
                                                    <Modal.Body>this could be harmful please check out the raw html
                                                        before proceeding </Modal.Body>
                                                    <Modal.Footer>
                                                        <Button variant="secondary" onClick={handleClose}>
                                                            No
                                                        </Button>
                                                        <Button variant="secondary"
                                                                onClick={() => {
                                                                    openHtmlInNewTab(data);
                                                                    handleClose()
                                                                }}>
                                                            Yes
                                                        </Button>
                                                    </Modal.Footer>
                                                </Modal>

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
