import {useEffect, useState} from "react";
import {Card, Col, Row, Table} from "react-bootstrap";
import moment from "moment";
import api from "../../services/api";
import { checkObjectIsFalsy } from "../../services/Util";

const VATCard = (props) => {
    const visitId = props.visitId

    const [data, setData] = useState(null);

    useEffect(() => {

        const fetchData = async () => {
            const url = `/vatCrawlResults/search/findByVisitId?visitId=${visitId}`;
            await api.get(url)
                .then((resp) => {
                    if(resp.status === 200) {
                        setData(resp.data);
                    }
                })
                .catch((ex) => {
                    console.log(ex);
                });
        };
        
        fetchData();
    }, [visitId])    

    // data from props
    const {
        openVisitedUrlsVat,
        setOpenVisitedUrlsVat,
        openVatValues,
        setOpenVatValues,
    } = props;

    // Render data.vatValues
    const renderVAT = () => { // Inside td element
        if(checkObjectIsFalsy(data.vatValues)) {
            return <>No VAT found</>;
        }
        if(data.vatValues.length === 1) {
            const vat = data.vatValues[0].substring(2);
            const link = "https://kbopub.economie.fgov.be/kbopub/zoeknummerform.html?nummer=" + vat + "&actionLu=Zoek";

            return (
                <a href={link} target="_blank">{data.vatValues[0]}</a>
            );
        }
        return (
            <>
                <button 
                    className='more-info'
                    onClick={() => setOpenVatValues(openVatValues => !openVatValues)} // Toggle openVatValues boolean
                > 
                    More info
                </button>

                {
                    openVatValues && ( // if openVatValues === true, render
                        <ul className="no-bullet mt-1 pl-0">
                            { 
                                data.vatValues.map((data, index) => {
                                    const link = "https://kbopub.economie.fgov.be/kbopub/zoeknummerform.html?nummer=" + data.substring(2) + "&actionLu=Zoek";
                                    return (
                                        <li key={index}>
                                            <a href={link} target="_blank">{data}</a>
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

    // Render data.visitedUrls
    const renderFollowedUrls = () => { // Inside td element
        if(checkObjectIsFalsy(data.visitedUrls)) {
            return (
                ''
            );
        }

        return (
            <>
                <button 
                    className='more-info'
                    onClick={() => setOpenVisitedUrlsVat(openVisitedUrlsVat => !openVisitedUrlsVat)} // Toggle openVisitedUrlsVat boolean
                > 
                    More info
                </button>      

                {
                    openVisitedUrlsVat && ( // if openVisitedUrlsVat === true, render
                        <ul className="mt-2 no-bullet pl-0">
                            { 
                                data.visitedUrls.map((data, index) => {
                                    return(
                                        <li key={index} className="mt-1">
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

    // Writing HTML on a function base so we can define logic more easily.
    const renderHTML = () => {
        if(checkObjectIsFalsy(data)) {
            return (
                <p>No data for this visit.</p>
            )
        }

        return (
            <div className="vat-table">
                <Table 
                    size='sm'
                    borderless
                >
                    <tbody>

                        <tr>
                            <th scope="row">
                                Id
                            </th>
                            <td>
                                { data.id }
                            </td>
                        </tr>

                        <tr>
                            <th scope="row">
                                Crawl timestamp
                            </th>
                            <td>
                                { // Ternary
                                    data.crawlStarted ? 
                                        moment(data.crawlStarted).format("DD/MM/YYYY HH:mm:ss") : 
                                        '' 
                                }
                            </td>
                        </tr>

                        <tr>
                            <th scope="row">
                                Crawl duration
                            </th>
                            <td>
                                { // Ternary
                                    data.crawlStarted && data.crawlFinished ?
                                        moment.duration(moment(data.crawlFinished).diff(moment(data.crawlStarted))).milliseconds() + ' ms' : 
                                        '' 
                                }
                            </td>
                        </tr>

                        <tr>
                            <th scope="row">
                                VAT
                            </th>
                            <td>
                                { renderVAT() }
                            </td>
                        </tr>

                        <tr>
                            <th scope="row">
                                URL
                            </th>
                            <td>
                                { data.startUrl }
                            </td>
                        </tr>

                        <tr>
                            <th scope="row">
                                Matching URL
                            </th>
                            <td>
                                { data.matchingUrl }
                            </td>
                        </tr>

                        <tr>
                            <th scope="row">
                                # URLs followed
                            </th>
                            <td>
                                { data.visitedUrls ? data.visitedUrls.length : '' }
                            </td>
                        </tr>

                        <tr>
                            <th scope="row">
                                URLs followed
                            </th>
                            <td>
                                { renderFollowedUrls() }
                            </td>
                        </tr>

                    </tbody>
                </Table>
            </div>
        )
    }

    // This file's HTML return.
    return (
        <Row>
            <Col className='mt-4'>
                <Card>
                    <Card.Header as="h2" className="h5">VAT crawl</Card.Header>
                    <Card.Body>
                    {
                        renderHTML()
                    }
                    </Card.Body>
                </Card>
            </Col>
        </Row>
    );
}


export default VATCard;