import {useEffect, useState} from "react";
import api from "../../services/api";
import {Accordion, Button, Card, Col, Row, Table, Icon} from "react-bootstrap";
import {checkObjectIsFalsy} from "../../services/Util";

const TLSCard = (props) => {

    const visitId = props.visitId;

    const [certificates, setCertificates] = useState({});
    const [crawlResult, setCrawlResult] = useState({});
    const [fullScan, setFullScan] = useState({});
    const [chain, setCertificatesChain] = useState(null);
    const [openCertificateChain, setOpenCertificateChain] = useState(false);

    useEffect(() => {
        const handlerData = async () => {
            let certificateResponse;
            let crawlResultResponse;
            let fullScanResponse;
            let certificateChain;

            try {
                crawlResultResponse = await api.get(`/crawlResultEntities/search/findByVisitId?visitId=${visitId}`);
            } catch (e) {
                console.log(e);
            }

            try {
                fullScanResponse = await api.get(`/fullScanEntities/search/findRelatedToCrawlResult?visitId=${visitId}`);
                // Convert array of objects to object with id as key: https://stackoverflow.com/a/44325124
                fullScanResponse = fullScanResponse.data._embedded.fullScanEntities.reduce((obj, item) => (obj[item.id] = item /*, obj*/), {});
            } catch (e) {
                console.log(e);
            }

            try {
                certificateResponse = await api.get(`/certificateEntities/search/findRelatedToCrawlResult?visitId=${visitId}`);
                // Convert array of objects to object with id as key: https://stackoverflow.com/a/44325124
                certificateResponse = certificateResponse.data._embedded.certificateEntities.reduce((obj, item) => (obj[item.id] = item /*, obj*/), {});
                let fingerprintForSearch = certificateResponse.signedBySha256;
                while (fingerprintForSearch != null){
                    if (certificateChain === undefined){
                        certificateChain = [];
                    }
                    let cert = await api.get(`/certificateEntities/search/findBySha256fingerprint?fingerprint=${fingerprintForSearch}`);
                    fingerprintForSearch = cert.data.signedBySha256;
                    certificateChain.push(cert.data);
                }
            } catch (e) {
                console.log(e);
            }

            setCrawlResult(crawlResultResponse === undefined ? null : crawlResultResponse.data);
            setCertificates(certificateResponse === undefined ? null : certificateResponse);
            setFullScan(fullScanResponse === undefined ? null : fullScanResponse);
            setCertificatesChain(certificateChain === undefined ? null : certificateChain);
        };
        handlerData();
    }, [visitId])

    const serialNumberToHexString = (serialNumber) => {
        //Converts serial number to int to be able to convert to hex, then adds : after every 2nd char
        const string = parseInt(serialNumber).toString(16).replace(/(.{2})/g,"$1\:");
        if (string.charAt(string.length -1) === ":"){
            return string.slice(0, -1);
        }
        return string;
    }

    const dateIsBefore = (date1, date2) => {
        const firstDate = new Date(date1);
        const secondDate = new Date(date2);
        return firstDate < secondDate;
    }

    const renderHTML = () => {
        if (checkObjectIsFalsy(crawlResult)) {
            return (
                <>
                    <Row>
                        <Col className='mt-4'>
                            <Card>
                                <Card.Header as="h2" className="h5">Tls crawler</Card.Header>
                                <Card.Body>No data for this visit</Card.Body>
                            </Card>
                        </Col>
                    </Row>
                </>
            )
        }
        return (
            <>
                <Row>
                    <Col className='mt-4'>
                        <Card>
                            <Card.Header as="h2" className="h5">TLS crawler</Card.Header>
                            <Card.Body>
                                <div className="vat-table">
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
                                            <td className={fullScan.supportSsl_2_0 ? "font-weight-bold" : ""}>{fullScan.supportSsl_2_0 ? "Yes" : "No"}</td>
                                            <td></td>
                                            <td>{fullScan.errorSsl_2_0}</td>
                                            <td>{fullScan.millis_ssl_2_0}</td>
                                        </tr>
                                        <tr>
                                            <td>SSL 3.0</td>
                                            <td className={fullScan.supportSsl_3_0 ? "font-weight-bold" : ""}>{fullScan.supportSsl_3_0 ? "Yes" : "No"}</td>
                                            <td>{fullScan.selectedCipherSsl_3_0}</td>
                                            <td>{fullScan.errorSsl_3_0}</td>
                                            <td>{fullScan.millis_ssl_3_0}</td>
                                        </tr>
                                        <tr>
                                            <td>TLS 1.0</td>
                                            <td className={fullScan.supportTls_1_0 ? "font-weight-bold" : ""}>{fullScan.supportTls_1_0 ? "Yes" : "No"}</td>
                                            <td>{fullScan.selectedCipherTls_1_0}</td>
                                            <td>{fullScan.errorTls_1_0}</td>
                                            <td>{fullScan.millis_tls_1_0}</td>
                                        </tr>
                                        <tr>
                                            <td>TLS 1.1</td>
                                            <td className={fullScan.supportTls_1_1 ? "font-weight-bold" : ""}>{fullScan.supportTls_1_1 ? "Yes" : "No"}</td>
                                            <td>{fullScan.selectedCipherTls_1_1}</td>
                                            <td>{fullScan.errorTls_1_1}</td>
                                            <td>{fullScan.millis_tls_1_1}</td>
                                        </tr>
                                        <tr>
                                            <td>TLS 1.2</td>
                                            <td className={fullScan.supportTls_1_2 ? "font-weight-bold" : ""}>{fullScan.supportTls_1_2 ? "Yes" : "No"}</td>
                                            <td>{fullScan.selectedCipherTls_1_2}</td>
                                            <td>{fullScan.errorTls_1_2}</td>
                                            <td>{fullScan.millis_tls_1_2}</td>
                                        </tr>

                                        <tr>
                                            <td>TLS 1.3</td>
                                            <td className={fullScan.supportTls_1_3 ? "font-weight-bold" : ""}>{fullScan.supportTls_1_3 ? "Yes" : "No"}</td>
                                            <td>{fullScan.selectedCipherTls_1_3}</td>
                                            <td>{fullScan.errorTls_1_3}</td>
                                            <td>{fullScan.millis_tls_1_3}</td>
                                        </tr>
                                        </tbody>
                                    </Table>
                                </div>
                                <div className="pt-3 vat-table">
                                    <h4 className="mb-3 text-md-left">Certificate chain</h4>
                                    <Table size="sm" borderless>
                                        <tbody>
                                        <tr>
                                            <th>Subject</th>
                                            <td className={"truncate"}>{certificates.subject}</td>
                                            <td>
                                                {/*https://icons.getbootstrap.com/icons/clipboard-fill/*/}
                                                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16"
                                                     fill="#2a6485" className="copy-btn"
                                                     viewBox="0 0 16 16"
                                                     onClick={() => {
                                                         navigator.clipboard.writeText(certificates.subject)
                                                     }}>
                                                    <path fillRule="evenodd"
                                                          d="M10 1.5a.5.5 0 0 0-.5-.5h-3a.5.5 0 0 0-.5.5v1a.5.5 0 0 0 .5.5h3a.5.5 0 0 0 .5-.5v-1Zm-5 0A1.5 1.5 0 0 1 6.5 0h3A1.5 1.5 0 0 1 11 1.5v1A1.5 1.5 0 0 1 9.5 4h-3A1.5 1.5 0 0 1 5 2.5v-1Zm-2 0h1v1A2.5 2.5 0 0 0 6.5 5h3A2.5 2.5 0 0 0 12 2.5v-1h1a2 2 0 0 1 2 2V14a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V3.5a2 2 0 0 1 2-2Z"/>
                                                </svg>
                                            </td>
                                        </tr>
                                        <tr>
                                            <th>Alternate names</th>
                                            <td>
                                                {certificates.subjectAltNames != null ?
                                                    certificates.subjectAltNames.map((altName, i) => {
                                                        if (i === 0){
                                                            return altName;
                                                        }
                                                        return ", " + altName
                                                    })
                                                 : ""}
                                            </td>
                                        </tr>
                                        <tr>
                                            <th>Trusted</th>
                                            <td>{crawlResult.chainTrustedByJavaPlatform ? "Yes" : "No"}</td>
                                        </tr>
                                        <tr>
                                            <th>Version</th>
                                            <td>{certificates.version}</td>
                                        </tr>
                                        <tr>
                                            <th>Serial number</th>
                                            <td>{serialNumberToHexString(certificates.serialNumber)}</td>
                                        </tr>
                                        <tr>
                                            <th>Valid from</th>
                                            <td className={dateIsBefore(crawlResult.crawlTimestamp, certificates.notBefore) ? "defined-error" : ""}>{certificates.notBefore}</td>
                                        </tr>
                                        <tr>
                                            <th>Valid until</th>
                                            <td className={dateIsBefore(certificates.notAfter, crawlResult.crawlTimestamp) ? "defined-error" : ""}>{certificates.notAfter}</td>
                                        </tr>
                                        <tr>
                                            <th>Key</th>
                                            <td>{certificates.publicKeySchema + "(" + certificates.publicKeyLength + " bit)"}</td>
                                        </tr>
                                        <tr>
                                            <th>Signature algorithm</th>
                                            <td>{certificates.signatureHashAlgorithm}</td>
                                        </tr>
                                        <tr>
                                            <th>Issuer</th>
                                            <td className={"truncate"}>{certificates.issuer}</td>
                                        </tr>
                                        </tbody>
                                    </Table>
                                </div>
                                {chain != null ? <button onClick={() => setOpenCertificateChain(openCertificateChain => !openCertificateChain)}
                                                        className={"more-info"}>Additional certificates</button> : ""}
                                {(chain != null && openCertificateChain) ? chain.map((cert, index) => {
                                    return (
                                        <div className="pt-3 vat-table">
                                        <Table size="sm" borderless className={"certificates-chain"} key={index}>
                                            <tbody>
                                            <tr>
                                                <th>Subject</th>
                                                <td className={"truncate"}>{cert.subject}</td>
                                                <td>
                                                    {/*https://icons.getbootstrap.com/icons/clipboard-fill/*/}
                                                    <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16"
                                                         fill="#2a6485" className="copy-btn"
                                                         viewBox="0 0 16 16"
                                                         onClick={() => {
                                                             navigator.clipboard.writeText(cert.subject)
                                                         }}>
                                                        <path fillRule="evenodd"
                                                              d="M10 1.5a.5.5 0 0 0-.5-.5h-3a.5.5 0 0 0-.5.5v1a.5.5 0 0 0 .5.5h3a.5.5 0 0 0 .5-.5v-1Zm-5 0A1.5 1.5 0 0 1 6.5 0h3A1.5 1.5 0 0 1 11 1.5v1A1.5 1.5 0 0 1 9.5 4h-3A1.5 1.5 0 0 1 5 2.5v-1Zm-2 0h1v1A2.5 2.5 0 0 0 6.5 5h3A2.5 2.5 0 0 0 12 2.5v-1h1a2 2 0 0 1 2 2V14a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V3.5a2 2 0 0 1 2-2Z"/>
                                                    </svg>
                                                </td>
                                            </tr>
                                            <tr>
                                                <th>Alternate names</th>
                                                <td>
                                                    {cert.subjectAltNames != null ?
                                                        cert.subjectAltNames.map((altName, i) => {
                                                            if (i === 0){
                                                                return altName;
                                                            }
                                                            return ", " + altName
                                                        })
                                                        : ""}
                                                </td>
                                            </tr>
                                            <tr>
                                                <th>Version</th>
                                                <td>{cert.version}</td>
                                            </tr>
                                            <tr>
                                                <th>Serial number</th>
                                                <td>{serialNumberToHexString(cert.serialNumber)}</td>
                                            </tr>
                                            <tr>
                                                <th>Valid from</th>
                                                <td className={dateIsBefore(crawlResult.crawlTimestamp, cert.notBefore) ? "defined-error" : ""}>{cert.notBefore}</td>
                                            </tr>
                                            <tr>
                                                <th>Valid until</th>
                                                <td className={dateIsBefore(certificates.notAfter, crawlResult.crawlTimestamp) ? "defined-error" : ""}>{cert.notAfter}</td>
                                            </tr>
                                            <tr>
                                                <th>Key</th>
                                                <td>{cert.publicKeySchema + "(" + cert.publicKeyLength + " bit)"}</td>
                                            </tr>
                                            <tr>
                                                <th>Signature algorithm</th>
                                                <td>{cert.signatureHashAlgorithm}</td>
                                            </tr>
                                            <tr>
                                                <th>Issuer</th>
                                                <td className={"truncate"}>{cert.issuer}</td>
                                            </tr>
                                            </tbody>
                                        </Table>
                                        </div>
                                    )
                                }) : ""}
                            </Card.Body>
                        </Card>
                    </Col>
                </Row>
            </>
        )
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