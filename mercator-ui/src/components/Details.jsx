import {useState} from "react";
import {Button} from "react-bootstrap";
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

    // Scroll to an element Id.
    // Original code idea from: https://stackoverflow.com/questions/61196420/react-navigation-that-will-smooth-scroll-to-section-of-the-page
    const scrollToElement = (elementId) => {
        let elementToGoTo = document.getElementById(elementId);
        elementToGoTo && elementToGoTo.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }

    return (
        <div id="Details-Div">

            <div className='p-3' id='Go-to-card-Div'>
                <button onClick={() => scrollToElement('content-card')}>
                    Content crawl
                </button>

                <button onClick={() => scrollToElement('dns-card')}>
                    DNS crawl
                </button>

                <button onClick={() => scrollToElement('smtp-card')}>
                    SMTP crawl
                </button>

                <button onClick={() => scrollToElement('html-card')}>
                    HTML features
                </button>

                <button onClick={() => scrollToElement('vat-card')}>
                    VAT crawl
                </button>

                <button onClick={() => scrollToElement('ssl-card')}>
                    SSL crawl
                </button>
            </div>

            <div>
                <div id='dispatcher-card'>
                    <DispatcherCard visitId={visitId} />
                </div>

                <button 
                    className="open-all-button" 
                    onClick={toggle}>
                        { 
                            allOpen ? "Close All" : "Open All" // Toggle text
                        }
                </button>
            
                <div id={'content-card'}>
                    <ContentCrawlCard 
                        openMetrics={metricsJSONButtonOn} 
                        setOpenMetrics={metricsJSONButtonSetOn} 
                        openTechnologies={technologiesButtonOn} 
                        setOpenTechnologies={technologiesButtonSetOn} 
                        openUrls={urlsButtonOn} setOpenUrls={urlsButtonSetOn} 
                        visitId={visitId}
                    />
                </div>

                <div id={'dns-card'}>
                    <DNSCard 
                        openRecords={recordsButtonOn} 
                        setOpenRecords={recordsButtonSetOn} 
                        visitId={visitId}
                    />
                </div>

                <div id={'smtp-card'}>
                    <SMTPCard 
                        openServer={serverButtonOn} 
                        setOpenServer={serverButtonSetOn} 
                        visitId={visitId}
                    />
                </div>

                <div id={'html-card'}>
                    <HTMLCard 
                        openBodyTextButton={bodyTextButtonOn} 
                        setOpenBodyTextButton={bodyTextButtonSetOn} 
                        openHtmlstructButton={htmlstructButtonOn} 
                        setOpenHtmlstructButton={htmlstructButtonSetOn}  
                        openExternalHosts={externalHostsButtonOn} 
                        setOpenExternalHosts={externalHostsButtonSetOn} 
                        openFacebookLinks={facebookLinksButtonOn} 
                        setOpenFacebookLinks={facebookLinksButtonSetOn} 
                        openTwitterLinks={twitterLinksButtonOn} 
                        setOpenTwitterLinks={twitterLinksButtonSetOn} 
                        openLinkedinLinks={linkedinLinksButtonOn} 
                        setOpenLinkedinLinks={linkedinLinksButtonSetOn} 
                        openYoutubeLinks={youtubeLinksButtonOn} 
                        setOpenYoutubeLinks={youtubeLinksButtonSetOn} 
                        openVimeoLinks={vimeoLinksButtonOn} 
                        setOpenVimeoLinks={vimeoLinksButtonSetOn} 
                        visitId={visitId} 
                    />
                </div>

                <div id={'vat-card'}>
                    <VATCard 
                        openVisitedUrlsVat={visitedUrlsVatButtonOn} 
                        setOpenVisitedUrlsVat={visitedUrlsVatButtonSetOn} 
                        openVatValues={vatValuesButtonOn} 
                        setOpenVatValues={vatValuesButtonSetOn} 
                        visitId={visitId}/>
                </div>

                <div id={'ssl-card'}>
                    <SSLCard 
                        openLeafCertificate={leafCertificateButtonOn} 
                        setOpenLeafCertificate={leafCertificateButtonSetOn} 
                        openTrustStores={trustStoresButtonOn} 
                        setOpenTrustStores={trustStoresButtonSetOn} 
                        visitId={visitId}
                    />
                </div>

            </div>

            <div className='p-3' id='Go-to-top-Div'>
                <button onClick={() => scrollToElement('NavBar-Div')} >
                    Go to top
                </button>

                <button onClick={() => window.location.href = '/'} >
                    Back to search
                </button>
            </div>
        </div>
    )
}

export default Details;
