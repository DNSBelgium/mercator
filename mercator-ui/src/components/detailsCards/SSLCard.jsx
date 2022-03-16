import React, {useEffect, useState} from "react";
import {Accordion, Button, Col, Row, Table} from "react-bootstrap";
import BorderWrapper from "react-border-wrapper";
import api from "../../services/api";
import moment from "moment";

const SSLCard = (props) => {

    const visitId = props.visitId

    const [crawlResult, setCrawlResult] = useState({});
    const [trustStores, setTrustStores] = useState({});
    const [certificates, setCertificates] = useState({});
    const [countCipherSuites, setCountCipherSuites] = useState({});

    // api SSL
    useEffect(() => {
        const handlerData = async () => {
            let crawlResultResponse;
            let trustStoreResponse;
            let certificateResponse;
            let countCipherSuitesResponse;

            try {
                crawlResultResponse = await api.get(`/sslCrawlResults/search/findOneByVisitId?visitId=${visitId}`);
            } catch (e) {
                console.log(e);
            }

            try {
                trustStoreResponse = await api.get(`/trustStores/search/findRelatedToSslCrawlResult?visitId=${visitId}`);
                // Convert array of objects to object with id as key: https://stackoverflow.com/a/44325124
                trustStoreResponse = trustStoreResponse.data._embedded.trustStores.reduce((obj, item) => (obj[item.id] = item, obj), {});
            } catch (e) {
                console.log(e);
            }

            try {
                certificateResponse = await api.get(`/certificates/search/findRelatedToSslCrawlResult?visitId=${visitId}`);
                // Convert array of objects to object with id as key: https://stackoverflow.com/a/44325124
                certificateResponse = certificateResponse.data._embedded.certificates.reduce((obj, item) => (obj[item.id] = item, obj), {});
            } catch (e) {
                console.log(e);
            }

            try {
                countCipherSuitesResponse = await api.get(`/countCipherSuitesResults/search/findNumberOfSupportedCipherSuitesByVisitId?visitId=${visitId}`);
                // Convert array of objects to object with protocol as key: https://stackoverflow.com/a/44325124
                countCipherSuitesResponse = countCipherSuitesResponse.data._embedded.countCipherSuitesResults.reduce((obj, item) => (obj[item.protocol] = item, obj), {});
            } catch (e) {
                console.log(e);
            }

            setCrawlResult(crawlResultResponse === undefined ? null : crawlResultResponse.data);

            setTrustStores(trustStoreResponse === undefined ? null : trustStoreResponse);

            setCertificates(certificateResponse === undefined ? null : certificateResponse);

            setCountCipherSuites(countCipherSuitesResponse === undefined ? null : countCipherSuitesResponse);


        };
        handlerData();
    }, [])

    // data from props
    const {
        openLeafCertificate,
        setOpenLeafCertificate,
        openTrustStores,
        setOpenTrustStores,
    } = props;

    const topElement = <p className='top-element'>SSL Crawl</p>
    if (!crawlResult || crawlResult === {}) {
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
                    <div className="vat-table">
                        <Table size="sm" borderless>
                            <tbody className="text-left">
                            <tr>
                                <th scope="row">Id</th>
                                <td>{crawlResult.id}</td>
                            </tr>
                            <tr>
                                <th scope="row">Crawl timestamp</th>
                                <td>{crawlResult.crawlTimestamp ? moment(crawlResult.crawlTimestamp).format("DD/MM/YYYY HH:mm:ss") : ''}</td>
                            </tr>
                            <tr>
                                <th scope="row">OK</th>
                                <td>{crawlResult.ok ? 'true' : 'false'}</td>
                            </tr>
                            <tr>
                                <th scope="row">Problem</th>
                                <td>{crawlResult.problem}</td>
                            </tr>
                            <tr>
                                <th scope="row">IP address</th>
                                <td>{crawlResult.ipAddress}</td>
                            </tr>
                            <tr>
                                <th scope="row">#certificates deployed</th>
                                <td>{crawlResult.nbCertificateDeployed}</td>
                            </tr>
                            <tr>
                                <th scope="row">Support SSL 2.0</th>
                                <td>{crawlResult.supportSsl_2_0 == null ? '' : crawlResult.supportSsl_2_0 ?
                                    ('true' + (countCipherSuites["SSL_2_0"] !== undefined ? ' (' + countCipherSuites["SSL_2_0"].count + ' cipher suites supported)' : ''))
                                    : 'false'}</td>
                            </tr>
                            <tr>
                                <th scope="row">Support SSL 3.0</th>
                                <td>{crawlResult.supportSsl_3_0 == null ? '' : crawlResult.supportSsl_3_0 ?
                                    ('true' + (countCipherSuites["SSL_3_0"] !== undefined ? ' (' + countCipherSuites["SSL_3_0"].count + ' cipher suites supported)' : ''))
                                    : 'false'}</td>
                            </tr>
                            <tr>
                                <th scope="row">Support TLS 1.0</th>
                                <td>{crawlResult.supportTls_1_0 == null ? '' : crawlResult.supportTls_1_0 ?
                                    ('true' + (countCipherSuites["TLS_1_0"] !== undefined ? ' (' + countCipherSuites["TLS_1_0"].count + ' cipher suites supported)' : ''))
                                    : 'false'}</td>
                            </tr>
                            <tr>
                                <th scope="row">Support TLS 1.1</th>
                                <td>{crawlResult.supportTls_1_1 == null ? '' : crawlResult.supportTls_1_1 ?
                                    ('true' + (countCipherSuites["TLS_1_1"] !== undefined ? ' (' + countCipherSuites["TLS_1_1"].count + ' cipher suites supported)' : ''))
                                    : 'false'}</td>
                            </tr>
                            <tr>
                                <th scope="row">Support TLS 1.2</th>
                                <td>{crawlResult.supportTls_1_2 == null ? '' : crawlResult.supportTls_1_2 ?
                                    ('true' + (countCipherSuites["TLS_1_2"] !== undefined ? ' (' + countCipherSuites["TLS_1_2"].count + ' cipher suites supported)' : ''))
                                    : 'false'}</td>
                            </tr>
                            <tr>
                                <th scope="row">Support TLS 1.3</th>
                                <td>{crawlResult.supportTls_1_3 == null ? '' : crawlResult.supportTls_1_3 ?
                                    ('true' + (countCipherSuites["TLS_1_3"] !== undefined ? ' (' + countCipherSuites["TLS_1_3"].count + ' cipher suites supported)' : ''))
                                    : 'false'}</td>
                            </tr>
                            <tr>
                                <th scope="row">Support ECDH key exchange</th>
                                <td>{crawlResult.supportEcdhKeyExchange == null ? '' : crawlResult.supportEcdhKeyExchange ? 'true' : 'false'}</td>
                            </tr>


                            </tbody>
                        </Table>

                        {crawlResult.certificateDeployments ? (crawlResult.certificateDeployments.map((item, index) => (
                                <Row>
                                    <Col key={index.toString()}>
                                        <h5 className="mt-3 text-left">Certificate deployment</h5>
                                        <Table size="sm" borderless>
                                            <tbody className="text-md-left">
                                            <tr>
                                                <th scope="row">Id</th>
                                                <td>{item.id}</td>
                                            </tr>
                                            <tr>
                                                <th scope="row">Length of received chain</th>
                                                <td>{item.lengthReceivedCertificateChain}</td>
                                            </tr>
                                            <tr>
                                                <th scope="row">Received chain has valid order</th>
                                                <td>{item.receivedChainHasValidOrder == null ? '' : item.receivedChainHasValidOrder ? 'true' : 'false'}</td>
                                            </tr>
                                            <tr>
                                                <th scope="row">Received chain contains anchor</th>
                                                <td>{item.receivedChainContainsAnchorCertificate == null ? '' : item.receivedChainContainsAnchorCertificate ? 'true' : 'false'}</td>
                                            </tr>
                                            <tr>
                                                <th scope="row">Verified chain has legacy Symantec anchor</th>
                                                <td>{item.verifiedChainHasLegacySymantecAnchor == null ? '' : item.verifiedChainHasLegacySymantecAnchor ? 'true' : 'false'}</td>
                                            </tr>
                                            <tr>
                                                <th scope="row">Verified chain has sha-1 signature</th>
                                                <td>{item.verifiedChainHasSha1Signature == null ? '' : item.verifiedChainHasSha1Signature ? 'true' : 'false'}</td>
                                            </tr>
                                            <tr>
                                                <th scope="row">Leaf subject matches hostname</th>
                                                <td>{item.leafCertificateSubjectMatchesHostname == null ? '' : item.leafCertificateSubjectMatchesHostname ? 'true' : 'false'}</td>
                                            </tr>
                                            <tr>
                                                <th scope="row">Leaf has must staple extension</th>
                                                <td>{item.leafCertificateHasMustStapleExtension == null ? '' : item.leafCertificateHasMustStapleExtension ? 'true' : 'false'}</td>
                                            </tr>
                                            <tr>
                                                <th scope="row">OCSP response is trusted</th>
                                                <td>{item.ocspResponseIsTrusted == null ? '' : item.ocspResponseIsTrusted ? 'true' : 'false'}</td>
                                            </tr>
                                            <tr>
                                                <th scope="row">Leaf is extended validation</th>
                                                <td>{item.leafCertificateIsEv == null ? '' : item.leafCertificateIsEv ? 'true' : 'false'}</td>
                                            </tr>

                                            <tr>
                                                <th scope="row">Leaf certificate</th>
                                                <td>


                                                    <div>
                                                        {certificates[item.leafCertificateId] !== undefined &&

                                                            <Accordion>
                                                                <Accordion.Toggle as={Button} className="toggle-button"
                                                                                  id="button-vat-values"
                                                                                  variant="link" eventKey="3"
                                                                                  onClick={() => setOpenLeafCertificate(!openLeafCertificate)}>More
                                                                    info
                                                                </Accordion.Toggle>
                                                                <Accordion.Collapse eventKey="3"
                                                                                    in={openLeafCertificate}>
                                                                    <table>
                                                                        <tr>
                                                                            <th className='nested-table-header'>Id</th>
                                                                            <td className='nested-table-data'>
                                                                                {certificates[item.leafCertificateId].id}</td>
                                                                        </tr>
                                                                        <tr>
                                                                            <th className='nested-table-header'>Subject</th>
                                                                            <td className='nested-table-data'>
                                                                                {certificates[item.leafCertificateId].subject}</td>
                                                                        </tr>
                                                                        <tr>
                                                                            <th className='nested-table-header'>Issuer</th>
                                                                            <td className='nested-table-data'>
                                                                                {certificates[item.leafCertificateId].issuer}</td>
                                                                        </tr>
                                                                        <tr>
                                                                            <th className='nested-table-header'>Not
                                                                                before
                                                                            </th>
                                                                            <td className='nested-table-data'>
                                                                                {(certificates[item.leafCertificateId].notBefore ?
                                                                                    moment(certificates[item.leafCertificateId].notBefore).format("DD/MM/YYYY HH:mm") : '')}</td>
                                                                        </tr>
                                                                        <tr>
                                                                            <th className='nested-table-header'>Not
                                                                                after
                                                                            </th>
                                                                            <td className='nested-table-data'>
                                                                                {(certificates[item.leafCertificateId].notAfter ?
                                                                                    moment(certificates[item.leafCertificateId].notAfter).format("DD/MM/YYYY HH:mm") : '')}</td>
                                                                        </tr>
                                                                        <tr>
                                                                            <th className='nested-table-header'>Public
                                                                                key
                                                                            </th>
                                                                            <td className='nested-table-data'>
                                                                                {certificates[item.leafCertificateId].publicKeySchema + ' ' + certificates[item.leafCertificateId].publicKeyLength}</td>
                                                                        </tr>

                                                                        <tr>
                                                                            <th className='nested-table-header'>Serial
                                                                                number
                                                                            </th>
                                                                            <td className='nested-table-data'>
                                                                                {certificates[item.leafCertificateId].serialNumber}</td>
                                                                        </tr>

                                                                        <tr>
                                                                            <th className='nested-table-header'>Signature
                                                                                hash
                                                                            </th>
                                                                            <td className='nested-table-data'>
                                                                                {certificates[item.leafCertificateId].signatureHashAlgorithm}</td>
                                                                        </tr>
                                                                        <tr>
                                                                            <th className='nested-table-header'>Version</th>
                                                                            <td className='nested-table-data'>
                                                                                {certificates[item.leafCertificateId].version}</td>
                                                                        </tr>

                                                                    </table>
                                                                </Accordion.Collapse>
                                                            </Accordion>

                                                        }
                                                    </div>


                                                </td>
                                            </tr>

                                            <tr>
                                                <th scope="row">Validity against trust stores</th>

                                                <td>
                                                    <Accordion>
                                                        <Accordion.Toggle as={Button} className="toggle-button"
                                                                          id="button-vat-values"
                                                                          variant="link" eventKey="3"
                                                                          onClick={() => setOpenTrustStores(!openTrustStores)}>More info
                                                        </Accordion.Toggle>
                                                        <Accordion.Collapse eventKey="3" in={openTrustStores}>
                                                            <div>

                                                                {item.checksAgainstTrustStores ? (item.checksAgainstTrustStores.map((trustStoreCheck, index) => (
                                                                        <div>
                                                                            {trustStores[trustStoreCheck.trustStoreId] !== undefined &&
                                                                                <div>{(trustStoreCheck.valid ? 'valid: ' : 'not valid: ') +
                                                                                    trustStores[trustStoreCheck.trustStoreId].name +
                                                                                    " (" + trustStores[trustStoreCheck.trustStoreId].version + ")"}<br/>
                                                                                </div>}
                                                                        </div>
                                                                    ))
                                                                ) : ""}


                                                            </div>
                                                        </Accordion.Collapse>
                                                    </Accordion>
                                                </td>


                                            </tr>


                                            </tbody>
                                        </Table>
                                    </Col>
                                </Row>
                            ))
                        ) : ""}

                    </div>
                </BorderWrapper>
            </Col>
        </Row>
    )
}


export default SSLCard;
