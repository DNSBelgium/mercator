import {useEffect, useState} from "react";
import {Accordion, Button, Card, Col, Row, Table} from "react-bootstrap";
import api from "../../services/api";
import moment from "moment";
import { checkObjectIsFalsy, renderDataBoolean } from "../../services/Util";

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
                trustStoreResponse = trustStoreResponse.data._embedded.trustStores.reduce((obj, item) => (obj[item.id] = item /*, obj*/), {});
            } catch (e) {
                console.log(e);
            }

            try {
                certificateResponse = await api.get(`/certificates/search/findRelatedToSslCrawlResult?visitId=${visitId}`);
                // Convert array of objects to object with id as key: https://stackoverflow.com/a/44325124
                certificateResponse = certificateResponse.data._embedded.certificates.reduce((obj, item) => (obj[item.id] = item /*, obj*/), {});
            } catch (e) {
                console.log(e);
            }

            try {
                countCipherSuitesResponse = await api.get(`/countCipherSuitesResults/search/findNumberOfSupportedCipherSuitesByVisitId?visitId=${visitId}`);
                // Convert array of objects to object with protocol as key: https://stackoverflow.com/a/44325124
                countCipherSuitesResponse = countCipherSuitesResponse.data._embedded.countCipherSuitesResults.reduce((obj, item) => (obj[item.protocol] = item /*, obj*/), {});
            } catch (e) {
                console.log(e);
            }

            

            setTrustStores(trustStoreResponse === undefined ? null : trustStoreResponse);

            setCertificates(certificateResponse === undefined ? null : certificateResponse);

            setCountCipherSuites(countCipherSuitesResponse === undefined ? null : countCipherSuitesResponse);


        };
        handlerData();
    }, [visitId])

    // data from props
    const {
        openLeafCertificate,
        setOpenLeafCertificate,
        openTrustStores,
        setOpenTrustStores,
    } = props;

    // Render crawlResult.supportSsl_<version number> / crawlResult.supportTls_<version number>
    const renderSlSupport = (sl, cipherSuites) => { // Inside td element
        if (sl === null || sl === '') {
            return '';
        }

        if (sl) { // supportSsl / supportTsl true or false?

            if (cipherSuites) { // Does it have cipherSuites?
                return `true (${cipherSuites.count} cipher suites supported)`;
            }

            return 'true';
        }
        return 'false';
    }

    // Writing HTML on a function base so we can define logic more easily.
    // Rewriting old html (WIP, pauzed)
    const renderHTML = () => {

        const render = () => {
            if (checkObjectIsFalsy(crawlResult)) {
                return (
                    <p>No data for this visit.</p>
                );
            }

            return (
                <div className="vat-table">
                    <Table size='sm' borderless>
                        <tbody className="text-left">
                            <tr>
                                <th scope="row">
                                    Id
                                </th>
                                <td>
                                    { crawlResult.id}
                                </td>
                            </tr>

                            <tr>
                                <th scope="row">
                                    Crawl timestamp
                                </th>
                                <td>
                                    { // Ternary
                                        crawlResult.crawlTimestamp ? 
                                            moment(crawlResult.crawlTimestamp).format("DD/MM/YYYY HH:mm:ss") : 
                                            ''
                                    }
                                </td>
                            </tr>

                            <tr>
                                <th scope="row">
                                    Ok
                                </th>
                                { 
                                    renderDataBoolean(crawlResult.ok) // td element
                                }
                            </tr>

                            <tr>
                                <th scope="row">
                                    Problem
                                </th>
                                <td>
                                    { crawlResult.problem}
                                </td>
                            </tr>

                            <tr>
                                <th scope="row">
                                    IP address
                                </th>
                                <td>
                                    { crawlResult.ipAddress}
                                </td>
                            </tr>

                            <tr>
                                <th scope="row">
                                    # certificates deployed
                                </th>
                                <td>
                                    { crawlResult.nbCertificateDeployed}
                                </td>
                            </tr>

                            <tr>
                                <th scope="row">
                                    Support SSL 2.0
                                </th>
                                <td>
                                    { renderSlSupport(crawlResult.supportSsl_2_0, countCipherSuites['SSL_2_0']) }
                                </td>
                            </tr>
                        </tbody>
                    </Table>
                </div>
            );
        }
        
        return (
            <>
                <Row>
                    <Col className='mt-4'>
                        <Card>
                            <Card.Header></Card.Header>
                            {
                                render()
                            }
                        </Card>
                    </Col>
                </Row>
            </>
        );
    }

    // Old HTML (rewriting HTML - WIP, pauzed)
    const oldHTML = () => {
        if (checkObjectIsFalsy(crawlResult)) {
            return (
                <>
                    <Row>
                        <Col className='mt-4'>
                            <Card>
                                <Card.Header as="h2" className="h5">SSL crawler</Card.Header>
                                <Card.Body>no data for this visit</Card.Body>
                            </Card>
                        </Col>
                    </Row>
                </>
            )
        }
    
        return (
            <Row>
                <Col className='mt-4'>
                    <Card>
                        <Card.Header as="h2" className="h5">SSL crawler</Card.Header>
                        <Card.Body>
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
                                    { 
                                        renderDataBoolean(crawlResult.ok) 
                                    }
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
                                    <td>
                                        { renderSlSupport(crawlResult.supportSsl_2_0, countCipherSuites["SSL_2_0"]) }
                                    </td>
                                </tr>
                                <tr>
                                    <th scope="row">Support SSL 3.0</th>
                                    <td>
                                        { renderSlSupport(crawlResult.supportSsl_3_0, countCipherSuites["SSL_3_0"]) }
                                    </td>
                                </tr>
                                <tr>
                                    <th scope="row">Support TLS 1.0</th>
                                    <td>
                                        { renderSlSupport(crawlResult.supportTls_1_0, countCipherSuites["TLS_1_0"]) }
                                    </td>
                                </tr>
                                <tr>
                                    <th scope="row">Support TLS 1.1</th>
                                    <td>
                                        { renderSlSupport(crawlResult.supportTls_1_1, countCipherSuites["TLS_1_1"]) }
                                    </td>
                                </tr>
                                <tr>
                                    <th scope="row">Support TLS 1.2</th>
                                    <td>
                                        { renderSlSupport(crawlResult.supportTls_1_2, countCipherSuites["TLS_1_2"]) }
                                    </td>
                                </tr>
                                <tr>
                                    <th scope="row">Support TLS 1.3</th>
                                    <td>
                                        { renderSlSupport(crawlResult.supportTls_1_3, countCipherSuites["TLS_1_3"]) }
                                    </td>
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
                                                                    <Accordion.Toggle as={Button} className="toggle-button more-info"
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
                                                            <Accordion.Toggle as={Button} className="toggle-button more-info"
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
                        </Card.Body>
                    </Card>
                </Col>
            </Row>
        )
    }

    // This file's HTML return.
    return (
        <>
            {
                oldHTML()
            }
        </>
    );
}


export default SSLCard;
