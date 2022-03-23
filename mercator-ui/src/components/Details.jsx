import {useState} from "react";
import {Button} from "react-bootstrap";
import ScrollableAnchor from 'react-scrollable-anchor';
import "../App.css";
import DispatcherCard from "./detailsCards/DispatcherCard";
import DNSCard from "./detailsCards/DNSCard";
import ContentCrawlCard from "./detailsCards/ContentCrawlCard";
import HTMLCard from "./detailsCards/HTMLCard";
import SMTPCard from "./detailsCards/SMTPCard";
import VATCard from "./detailsCards/VATCard";
import SSLCard from "./detailsCards/SSLCard";

function Details(props) {
    const visitId = props.match.params.visitId; // Fetch visit_id from url

    //used for the "Open all" button
    const [metricsJSONButtonOn, metricsJSONButtonSetOn] = useState(false);
    const [serverButtonOn, serverButtonSetOn] = useState(false);
    const [technologiesButtonOn, technologiesButtonSetOn] = useState(false);
    const [urlsButtonOn, urlsButtonSetOn] = useState(false);
    const [recordsButtonOn, recordsButtonSetOn] = useState(false);
    const [externalHostsButtonOn, externalHostsButtonSetOn] = useState(false);
    const [bodyTextButtonOn, bodyTextButtonSetOn] = useState(false);
    const [htmlstructButtonOn, htmlstructButtonSetOn] = useState(false);
    const [facebookLinksButtonOn, facebookLinksButtonSetOn] = useState(false);
    const [twitterLinksButtonOn, twitterLinksButtonSetOn] = useState(false);
    const [linkedinLinksButtonOn, linkedinLinksButtonSetOn] = useState(false);
    const [youtubeLinksButtonOn, youtubeLinksButtonSetOn] = useState(false);
    const [vimeoLinksButtonOn, vimeoLinksButtonSetOn] = useState(false);
    const [visitedUrlsVatButtonOn, visitedUrlsVatButtonSetOn] = useState(false);
    const [vatValuesButtonOn, vatValuesButtonSetOn] = useState(false);

    const [leafCertificateButtonOn, leafCertificateButtonSetOn] = useState(false);
    const [trustStoresButtonOn, trustStoresButtonSetOn] = useState(false);

    //used for the "Open All" button
    let allOpen = metricsJSONButtonOn && serverButtonOn && technologiesButtonOn && urlsButtonOn && recordsButtonOn && externalHostsButtonOn && bodyTextButtonOn && htmlstructButtonOn;
    const toggle = () => {
        metricsJSONButtonSetOn(!allOpen);
        serverButtonSetOn(!allOpen);
        technologiesButtonSetOn(!allOpen);
        urlsButtonSetOn(!allOpen);
        recordsButtonSetOn(!allOpen);
        externalHostsButtonSetOn(!allOpen);
        bodyTextButtonSetOn(!allOpen);
        htmlstructButtonSetOn(!allOpen);
        facebookLinksButtonSetOn(!allOpen);
        twitterLinksButtonSetOn(!allOpen);
        linkedinLinksButtonSetOn(!allOpen);
        youtubeLinksButtonSetOn(!allOpen);
        vimeoLinksButtonSetOn(!allOpen);
        visitedUrlsVatButtonSetOn(!allOpen);
        vatValuesButtonSetOn(!allOpen);
        leafCertificateButtonSetOn(!allOpen);
        trustStoresButtonSetOn(!allOpen);
    }

    return (
        <div>
            <ScrollableAnchor id={'content-top'}>
                <div className='p-3'>
                    <a className='mr-3' href='#content-card'>Content crawl</a>
                    <a className='mr-3' href='#dns-card'>DNS crawl</a>
                    <a className='mr-3' href='#smtp-card'>SMTP crawl</a>
                    <a className='mr-3' href='#html-card'>HTML features</a>
                    <a className='mr-3' href='#vat-card'>VAT crawl</a>
                    <a className='mr-3' href='#ssl-card'>SSL crawl</a>
                </div>
            </ScrollableAnchor>
            <div>
                <DispatcherCard visitId={visitId} />
                <Button variant="link" className="openall-button" onClick={toggle}>{allOpen ? "Close All" : "Open All"}</Button>

                <ScrollableAnchor id={'content-card'}>
                    <div><ContentCrawlCard openMetrics={metricsJSONButtonOn} setOpenMetrics={metricsJSONButtonSetOn} openTechnologies={technologiesButtonOn} setOpenTechnologies={technologiesButtonSetOn} openUrls={urlsButtonOn} setOpenUrls={urlsButtonSetOn} visitId={visitId}/></div>
                </ScrollableAnchor>

                <ScrollableAnchor id={'dns-card'}>
                    <div><DNSCard openRecords={recordsButtonOn} setOpenRecords={recordsButtonSetOn} visitId={visitId}/></div>
                </ScrollableAnchor>

                <ScrollableAnchor id={'smtp-card'}>
                    <div><SMTPCard openServer={serverButtonOn} setOpenServer={serverButtonSetOn} visitId={visitId}/></div>
                </ScrollableAnchor>

                <ScrollableAnchor id={'html-card'}>
                    <div><HTMLCard openBodyTextButton={bodyTextButtonOn} setOpenBodyTextButton={bodyTextButtonSetOn} openHtmlstructButton={htmlstructButtonOn} setOpenHtmlstructButton={htmlstructButtonSetOn}  openExternalHosts={externalHostsButtonOn} setOpenExternalHosts={externalHostsButtonSetOn} openFacebookLinks={facebookLinksButtonOn} setOpenFacebookLinks={facebookLinksButtonSetOn} openTwitterLinks={twitterLinksButtonOn} setOpenTwitterLinks={twitterLinksButtonSetOn} openLinkedinLinks={linkedinLinksButtonOn} setOpenLinkedinLinks={linkedinLinksButtonSetOn} openYoutubeLinks={youtubeLinksButtonOn} setOpenYoutubeLinks={youtubeLinksButtonSetOn} openVimeoLinks={vimeoLinksButtonOn} setOpenVimeoLinks={vimeoLinksButtonSetOn} visitId={visitId} /></div>
                </ScrollableAnchor>

                <ScrollableAnchor id={'vat-card'}>
                    <div><VATCard openVisitedUrlsVat={visitedUrlsVatButtonOn} setOpenVisitedUrlsVat={visitedUrlsVatButtonSetOn} openVatValues={vatValuesButtonOn} setOpenVatValues={vatValuesButtonSetOn} visitId={visitId}/></div>
                </ScrollableAnchor>

                <ScrollableAnchor id={'ssl-card'}>
                    <div><SSLCard openLeafCertificate={leafCertificateButtonOn} setOpenLeafCertificate={leafCertificateButtonSetOn} openTrustStores={trustStoresButtonOn} setOpenTrustStores={trustStoresButtonSetOn} visitId={visitId}/></div>
                </ScrollableAnchor>

            </div>
            <div className='p-3'>
                <a href='#content-top'>Go to Top</a>
            </div>
        </div>
    )
}

export default Details;
