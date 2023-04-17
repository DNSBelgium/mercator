import {useEffect, useState} from "react";
import api from "../../services/api";
import {Card, Col, Row, Table} from "react-bootstrap";
import {checkObjectIsFalsy} from "../../services/Util";
import moment from "moment";

require('moment-precise-range-plugin');

const TLSCard = (props) => {

    const visitId = props.visitId;

    const [crawlResult, setCrawlResult] = useState([]);
    const [fullScan, setFullScan] = useState({});
    const [certificates, setCertificatesChain] = useState({});
    const [openCertificateChain, setOpenCertificateChain] = useState(false);
    const [errorMessages, setErrorMessages] = useState([])

    useEffect(() => {
        const handlerData = async () => {
            let certificateResponse;
            let crawlResultResponse;
            let fullScanResponse;
            let fullScans = {};
            let certificateChain = {};

            setErrorMessages([]);

            try {
                crawlResultResponse = await api.get(`/crawlResultEntities/search/findByVisitId?visitId=${visitId}`);
                crawlResultResponse = crawlResultResponse.data._embedded.crawlResultEntities;
                if (crawlResultResponse.length === 0) throw "Error: 404 crawlResultResponse is empty";
            } catch (e) {
                handleError(e, "crawl");
            }

            for (let i = 0; i < crawlResultResponse.length; i++) {
                try {
                    fullScanResponse = await api.get(`/fullScanEntities/search/findByCrawlResultId?crawlResultId=${crawlResultResponse[i].id}`);
                    fullScans[crawlResultResponse[i].id] = fullScanResponse.data;
                } catch (e) {
                    handleError(e, "full scan")
                }

                try {
                    let chain = [];
                    certificateResponse = await api.get(`/certificateEntities/search/findByCrawlResultId?crawlResultId=${crawlResultResponse[i].id}`);
                    // Convert array of objects to object with id as key: https://stackoverflow.com/a/44325124
                    certificateResponse = certificateResponse.data;
                    chain.push(certificateResponse);
                    let fingerprintForSearch = certificateResponse.signedBySha256;
                    while (fingerprintForSearch != null) {
                        let cert = await api.get(`/certificateEntities/search/findBySha256fingerprint?fingerprint=${fingerprintForSearch}`);
                        fingerprintForSearch = cert.data.signedBySha256;
                        chain.push(cert.data);
                    }
                    certificateChain[crawlResultResponse[i].id] = chain;
                } catch (e) {
                    handleError(e, "certificate")
                }
            }

            setCrawlResult(crawlResultResponse.length === 0 ? null : crawlResultResponse);
            setFullScan(fullScans === {} ? null : fullScans);
            setCertificatesChain(certificateChain === {} ? null : certificateChain);
        };
        handlerData();
    }, [visitId])

    const serialNumberToHexString = (serialNumber) => {
        //Converts serial number to int to be able to convert to hex, then adds : after every 2nd char
        const string = parseInt(serialNumber).toString(16).replace(/(.{2})/g, "$1\:");
        if (string.charAt(string.length - 1) === ":") {
            return string.slice(0, -1);
        }
        return string;
    }

    const dateIsBefore = (date1, date2) => {
        const firstDate = new Date(date1);
        const secondDate = new Date(date2);
        return firstDate < secondDate;
    }

    const differenceBetweenTwoDates = (date1, date2) => {
        const firstDate = new moment(date1);
        const secondDate = new moment(date2);
        //https://github.com/codebox/moment-precise-range
        const diff = moment.preciseDiff(firstDate, secondDate, true);
        let string = "";
        if (diff.years > 0) {
            if (diff.years === 1) {
                string += (diff.years + " year ");
            } else {
                string += (diff.years + " years ");
            }
        }
        if (diff.months > 0) {
            string += (diff.months + " months ");
        }
        if (diff.days > 0) {
            string += (diff.days + " days ");
        }
        return string;
    }

    const handleError = (e, position) => {
        let errorMessage;
        if ((e.toString()).includes("404")) {
            errorMessage = "No " + position + " data was found for this visit.";
        } else if ((e.toString()).includes("500")) {
            errorMessage = "Something went wrong while trying to contact the back-end.";
        } else {
            errorMessage = "Something unexpected went wrong.";
        }
        setErrorMessages(errorMessages => [...errorMessages, errorMessage]);
        console.log(e);
    }

    const csvToP = (csv) => {
        if (csv == null) {
            return;
        }
        let array = csv.split(",");
        return <div>{array.map((x, i) => {
            return <p key={i}>{x}</p>
        })}</div>;
    }

    const renderXCheck = (bool) => {
        return bool ?
            //https://icons.getbootstrap.com/icons/x/
            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" fill="currentColor"
                 className="bi bi-x" viewBox="0 0 16 16">
                <path
                    d="M4.646 4.646a.5.5 0 0 1 .708 0L8 7.293l2.646-2.647a.5.5 0 0 1 .708.708L8.707 8l2.647 2.646a.5.5 0 0 1-.708.708L8 8.707l-2.646 2.647a.5.5 0 0 1-.708-.708L7.293 8 4.646 5.354a.5.5 0 0 1 0-.708z"/>
            </svg>
            :
            //https://icons.getbootstrap.com/icons/check/
            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" fill="currentColor"
                 className="bi bi-check" viewBox="0 0 16 16">
                <path
                    d="M10.97 4.97a.75.75 0 0 1 1.07 1.05l-3.99 4.99a.75.75 0 0 1-1.08.02L4.324 8.384a.75.75 0 1 1 1.06-1.06l2.094 2.093 3.473-4.425a.267.267 0 0 1 .02-.022z"/>
            </svg>
    }

    const renderCertificate = (type, certificate, crawlResult) => {
        return (
            <Table size="sm" borderless className={type === "additional" ? "certificates-chain" : ""}>
                <tbody>
                <tr>
                    <th>Subject</th>
                    <td>{csvToP(certificate.subject)}</td>
                    <td>
                        {/*https://icons.getbootstrap.com/icons/clipboard-fill/*/}
                        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16"
                             fill="#2a6485" className="copy-btn"
                             viewBox="0 0 16 16"
                             onClick={() => {
                                 navigator.clipboard.writeText(certificate.subject)
                             }}>
                            <path fillRule="evenodd"
                                  d="M10 1.5a.5.5 0 0 0-.5-.5h-3a.5.5 0 0 0-.5.5v1a.5.5 0 0 0 .5.5h3a.5.5 0 0 0 .5-.5v-1Zm-5 0A1.5 1.5 0 0 1 6.5 0h3A1.5 1.5 0 0 1 11 1.5v1A1.5 1.5 0 0 1 9.5 4h-3A1.5 1.5 0 0 1 5 2.5v-1Zm-2 0h1v1A2.5 2.5 0 0 0 6.5 5h3A2.5 2.5 0 0 0 12 2.5v-1h1a2 2 0 0 1 2 2V14a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V3.5a2 2 0 0 1 2-2Z"/>
                        </svg>
                    </td>
                </tr>
                <tr>
                    <th>Alternate names</th>
                    <td>
                        {certificate.subjectAltNames != null ?
                            certificate.subjectAltNames.map((altName, i) => {
                                if (i === 0) {
                                    return altName;
                                }
                                return ", " + altName
                            })
                            : ""}
                    </td>
                </tr>
                {type === "leaf" ?
                    <tr>
                        <th>Trusted</th>
                        <td>{crawlResult.chainTrustedByJavaPlatform ? "Yes" : "No"}</td>
                    </tr>
                    : ""}
                <tr>
                    <th>Version</th>
                    <td>{certificate.version}</td>
                </tr>
                <tr>
                    <th>Serial number</th>
                    <td>{serialNumberToHexString(certificate.serialNumber)}</td>
                </tr>
                {type === "leaf" ?
                    <tr>
                        <th>Valid from</th>
                        <td className={crawlResult.certificateTooSoon ? "defined-error" : ""}>
                            {moment(certificate.notBefore).format("DD/MM/YYYY HH:mm:ss")}
                            {renderXCheck(crawlResult.certificateTooSoon)}
                        </td>
                    </tr>
                    :
                    <tr>
                        <th>Valid from</th>
                        <td className={dateIsBefore(crawlResult.crawlTimestamp, certificate.notBefore) ? "defined-error" : ""}>
                            {moment(certificate.notBefore).format("DD/MM/YYYY HH:mm:ss")}
                            {renderXCheck(dateIsBefore(crawlResult.crawlTimestamp, certificate.notBefore))}
                        </td>
                    </tr>
                }
                {type === "leaf" ?
                    <tr>
                        <th>Valid until</th>
                        <td className={crawlResult.certificateExpired ? "defined-error" : ""}>
                            {moment(certificate.notAfter).format("DD/MM/YYYY HH:mm:ss")}
                            {renderXCheck(crawlResult.certificateExpired)}
                        </td>
                    </tr>
                    :
                    <tr>
                        <th>Valid until</th>
                        <td className={dateIsBefore(certificate.notAfter, crawlResult.crawlTimestamp) ? "defined-error" : ""}>
                            {moment(certificate.notAfter).format("DD/MM/YYYY HH:mm:ss")}
                            {renderXCheck(dateIsBefore(certificate.notAfter, crawlResult.crawlTimestamp))}
                        </td>
                    </tr>
                }
                <tr>
                    <th>Length validity period</th>
                    <td>{differenceBetweenTwoDates(certificate.notBefore, certificate.notAfter)}</td>
                </tr>
                <tr>
                    <th>Key</th>
                    <td>{certificate.publicKeySchema + "(" + certificate.publicKeyLength + " bit)"}</td>
                </tr>
                <tr>
                    <th>Signature algorithm</th>
                    <td>{certificate.signatureHashAlgorithm}</td>
                </tr>
                <tr>
                    <th>Issuer</th>
                    <td>{csvToP(certificate.issuer)}</td>
                </tr>
                </tbody>
            </Table>
        );
    }

    const renderHTML = () => {
        if (checkObjectIsFalsy(crawlResult)) {
            return (
                <>
                    <Row>
                        <Col className='mt-4'>
                            <Card>
                                <Card.Header as="h2" className="h5">Tls crawler</Card.Header>
                                <Card.Body>
                                    {errorMessages.length !== 0 ? errorMessages.map(e =>
                                        <p className={"defined-error"}>{e}</p>) : "Something unexpected went wrong."}
                                </Card.Body>
                            </Card>
                        </Col>
                    </Row>
                </>
            )
        }
        return crawlResult.map((result) => {
            if (checkObjectIsFalsy(fullScan)) {
                return null;
            }
            if (checkObjectIsFalsy(certificates)) {
                return null;
            }
            let scan = fullScan[result.id];
            let certs = certificates[result.id];
            return (
                <>
                    <Row>
                        <Col className='mt-4'>
                            <Card>
                                <Card.Header as="h2" className="h5">TLS crawler</Card.Header>
                                <Card.Body>
                                    <div className="tls-table">
                                        <Table size="sm" borderless>
                                            <tbody>
                                            <tr>
                                                <th>Host name</th>
                                                <td>{result.hostName}</td>
                                            </tr>
                                            <tr>
                                                <th>Crawl timestamp</th>
                                                <td>{moment(result.crawlTimestamp).format("DD/MM/YYYY HH:mm:ss")}</td>
                                            </tr>
                                            <tr>
                                                <th>Host name matches certificate</th>
                                                <td>{result.hostNameMatchesCertificate ? "Yes" : "No"}</td>
                                            </tr>
                                            <tr>
                                                <th>Full scan timestamp</th>
                                                <td>{moment(scan.crawlTimestamp).format("DD/MM/YYYY HH:mm:ss")}</td>
                                            </tr>
                                            <tr>
                                                <th>Full scan ip</th>
                                                <td>{scan.ip}</td>
                                            </tr>
                                            <tr>
                                                <th>Full scan server name</th>
                                                <td>{scan.serverName}</td>
                                            </tr>
                                            <tr>
                                                <th>Full scan connect ok</th>
                                                <td>{scan.connectOk ? "Yes" : "No"}</td>
                                            </tr>
                                            </tbody>
                                        </Table>
                                        <h4 className="mb-1 text-md-left">Protocols</h4>
                                        <Table size="sm" borderless>
                                            <thead>
                                            <tr>
                                                <th>Protocol</th>
                                                <th>Supported</th>
                                                <th>Selected cipher</th>
                                                <th>Error</th>
                                                <th>Duration</th>
                                            </tr>
                                            </thead>
                                            <tbody className="text-left">
                                            <tr>
                                                <td>SSL 2.0</td>
                                                <td className={scan.supportSsl_2_0 ? "font-weight-bold" : ""}>{scan.supportSsl_2_0 ? "Yes" : "No"}</td>
                                                <td></td>
                                                <td>{scan.errorSsl_2_0}</td>
                                                <td className={"duration"}>{scan.millis_ssl_2_0} ms</td>
                                            </tr>
                                            <tr>
                                                <td>SSL 3.0</td>
                                                <td className={scan.supportSsl_3_0 ? "font-weight-bold" : ""}>{scan.supportSsl_3_0 ? "Yes" : "No"}</td>
                                                <td>{scan.selectedCipherSsl_3_0}</td>
                                                <td>{scan.errorSsl_3_0}</td>
                                                <td className={"duration"}>{scan.millis_ssl_3_0} ms</td>
                                            </tr>
                                            <tr>
                                                <td>TLS 1.0</td>
                                                <td className={scan.supportTls_1_0 ? "font-weight-bold" : ""}>{scan.supportTls_1_0 ? "Yes" : "No"}</td>
                                                <td>{scan.selectedCipherTls_1_0}</td>
                                                <td>{scan.errorTls_1_0}</td>
                                                <td className={"duration"}>{scan.millis_tls_1_0} ms</td>
                                            </tr>
                                            <tr>
                                                <td>TLS 1.1</td>
                                                <td className={scan.supportTls_1_1 ? "font-weight-bold" : ""}>{scan.supportTls_1_1 ? "Yes" : "No"}</td>
                                                <td>{scan.selectedCipherTls_1_1}</td>
                                                <td>{scan.errorTls_1_1}</td>
                                                <td className={"duration"}>{scan.millis_tls_1_1} ms</td>
                                            </tr>
                                            <tr>
                                                <td>TLS 1.2</td>
                                                <td className={scan.supportTls_1_2 ? "font-weight-bold" : ""}>{scan.supportTls_1_2 ? "Yes" : "No"}</td>
                                                <td>{scan.selectedCipherTls_1_2}</td>
                                                <td>{scan.errorTls_1_2}</td>
                                                <td className={"duration"}>{scan.millis_tls_1_2} ms</td>
                                            </tr>
                                            <tr>
                                                <td>TLS 1.3</td>
                                                <td className={scan.supportTls_1_3 ? "font-weight-bold" : ""}>{scan.supportTls_1_3 ? "Yes" : "No"}</td>
                                                <td>{scan.selectedCipherTls_1_3}</td>
                                                <td>{scan.errorTls_1_3}</td>
                                                <td className={"duration"}>{scan.millis_tls_1_3} ms</td>
                                            </tr>
                                            <tr>
                                                <th>Total duration</th>
                                                <td></td>
                                                <td></td>
                                                <td></td>
                                                <td className="duration">{scan.totalDurationInMs} ms</td>
                                            </tr>
                                            </tbody>
                                        </Table>
                                    </div>
                                    <h4 className="mb-1 pt-3 text-md-left">Certificate chain</h4>
                                    {certs === undefined ? "No certificates were found for this crawl" :
                                        <div className="pt-3 tls-table">
                                            {renderCertificate("leaf", certs[0], result)}
                                            {certs.length > 1 ? <button
                                                onClick={() => setOpenCertificateChain(openCertificateChain => !openCertificateChain)}
                                                className={"more-info"}>Additional certificates</button> : ""}
                                            {(certs.length > 1 && openCertificateChain) ? certs.map((cert, i) => {
                                                if (i === 0) {
                                                    return null;
                                                }
                                                return renderCertificate("additional", cert, result);
                                            }) : ""}
                                        </div>
                                    }
                                </Card.Body>
                            </Card>
                        </Col>
                    </Row>
                </>

            )
        })
    }

    return (
        <>
            {
                renderHTML()
            }
        </>
    );

}

export default TLSCard;