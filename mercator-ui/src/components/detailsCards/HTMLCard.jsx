import {useEffect, useState} from "react";
import {Card, Col, Row, Table} from "react-bootstrap";
import BorderWrapper from "react-border-wrapper";
import moment from "moment";
import api from "../../services/api";
import { renderDataBoolean } from "../../services/Util";

const HTMLCard = (props) => {

    const visitId = props.visitId

    const [data, setData] = useState([]);

    useEffect(() => {
        const handlerData = async () => {

            const url = `htmlFeatureses/search/findByVisitId?visitId=${visitId}`;
            await api.get(url)
                .then((resp) => {
                    if(resp.status === 200) {
                        setData(resp.data === undefined ? null : resp.data._embedded.htmlFeatureses);
                    }
                })
                .catch((ex) => {
                    console.log(ex);
                });
        };
        handlerData();
    }, [visitId]);

    //data from props
    const {
        openExternalHosts,
        setOpenExternalHosts,
        openFacebookLinks,
        setOpenFacebookLinks,
        openTwitterLinks,
        setOpenTwitterLinks,
        openLinkedinLinks,
        setOpenLinkedinLinks,
        openYoutubeLinks,
        setOpenYoutubeLinks,
        openVimeoLinks,
        setOpenVimeoLinks,
        openBodyTextButton,
        setOpenBodyTextButton,
        openHtmlstructButton,
        setOpenHtmlstructButton
    } = props;

    const topElement = <p className='top-element'>HTML features</p> // Used for BorderWrapper title

    // Render data[index].<media>_links
    const renderLinks = (links, setBool, bool) => { // Inside td element
        if(!links || !links.length) {
            return ('');
        }
        return (
            <>
                <button 
                    className='more-info'
                    onClick={() => setBool(bool => !bool)} // Toggle openFacebookLinks boolean
                > 
                    Show links
                </button>

                {
                    bool && ( // if openFacebookLinks === true, render
                        <ul className="mt-2">
                            {
                                links.map((item, index) => {
                                    return (
                                        <li key={index}>
                                            { item }
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

    // Render data[index].nb_<media>_shallow_links and data[index].nb_<media>_deep_links
    const renderAmntOfLinks = (shallow, deep) => { // Inside td element
        if(!shallow && !deep) {
            return "0 / 0"
        }

        return (
                `shallow: ${shallow} / deep: ${deep}`
        );
    }

    // Render data[index].external_hosts
    const renderExternalHosts = (hosts) => { // Inside td element
        if(!hosts || !hosts.length) {
            return "";
        }

        return (
            <>
                <button 
                    className='more-info'
                    onClick={() => setOpenExternalHosts(openExternalHosts => !openExternalHosts)} // Toggle openExternalHosts boolean
                > 
                    More info
                </button>

                {
                    openExternalHosts && ( // if openExternalHosts === true, render
                        <ul>
                            {
                                hosts.map((item, index) => {
                                    return (
                                        <li key={index}>
                                            { item }
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

    // Render data[index].body_text
    const renderBodyText = (text) => { // Inside td element
        if(!text || text === "") {
            return "";
        }

        return (
            <>
                <button 
                    className='more-info'
                    onClick={() => setOpenBodyTextButton(openBodyTextButton => !openBodyTextButton)} // Toggle openBodyTextButton boolean
                > 
                    More info
                </button>

                {
                    openBodyTextButton && ( // if openBodyTextButton === true, render
                        <Card.Body>
                            { text }
                        </Card.Body>
                    )
                }
            </>
        );
    }

    // Render data[index].htmlStruct
    const renderHtmlStruct = (struct) => { // Inside td element
        if(!struct || struct === "") {
            return "";
        }

        return (
            <>
                <button 
                    className='more-info'
                    onClick={() => setOpenHtmlstructButton(openHtmlstructButton => !openHtmlstructButton)} // Toggle openHtmlstructButton boolean
                > 
                    More info
                </button>

                {
                    openHtmlstructButton && ( // if openHtmlstructButton === true, render
                        <Card.Body>
                            { struct }
                        </Card.Body>
                    )
                }
            </>
        );
    }

    // Writing HTML on a function base so we can define logic more easily.
    const renderHTML = () => {
        if(!data.length || data.length === 0) {
            return (
                <p>No data for this visit.</p>
            );
        }

        return (
            <>
                {
                    data.map((item, index) => {
                        return (
                            <div className="html-table" key={index}>
                                <Table size='sm' borderless>
                                    <tbody className="text-left">
                                        <tr>
                                            <th scope='row'>
                                                Id
                                            </th>
                                            <td>
                                                { item.id }
                                            </td>
                                        </tr>

                                        <tr>
                                            <th scope='row'>
                                                Crawl timestamp
                                            </th>
                                            <td>
                                                { moment(item.crawlTimestamp).format("YYYY-MM-DD HH:mm:ss") }
                                            </td>
                                        </tr>

                                        <tr>
                                            <th scope='row'>
                                                # images
                                            </th>
                                            <td>
                                                { item.nb_imgs }
                                            </td>
                                        </tr>

                                        <tr>
                                            <th scope='row'>
                                                # internal links
                                            </th>
                                            <td>
                                                { item.nb_links_int }
                                            </td>
                                        </tr>

                                        <tr>
                                            <th scope='row'>
                                                # external links
                                            </th>
                                            <td>
                                                { item.nb_links_ext }
                                            </td>
                                        </tr>

                                        <tr>
                                            <th scope='row'>
                                                # telephone links
                                            </th>
                                            <td>
                                                { item.nb_links_tel }
                                            </td>
                                        </tr>

                                        <tr>
                                            <th scope='row'>
                                                # email links
                                            </th>
                                            <td>
                                                { item.nb_links_email }
                                            </td>
                                        </tr>

                                        <tr>
                                            <th scope='row'>
                                                # text input fields
                                            </th>
                                            <td>
                                                { item.nb_input_txt }
                                            </td>
                                        </tr>

                                        <tr>
                                            <th scope='row'>
                                                # buttons
                                            </th>
                                            <td>
                                                { item.nb_button }
                                            </td>
                                        </tr>

                                        <tr>
                                            <th scope='row'>
                                                # meta descriptions
                                            </th>
                                            <td>
                                                { item.nb_meta_desc }
                                            </td>
                                        </tr>

                                        <tr>
                                            <th scope='row'>
                                                # meta keywords
                                            </th>
                                            <td>
                                                { item.nb_meta_keyw }
                                            </td>
                                        </tr>

                                        <tr>
                                            <th scope='row'>
                                                # numerical strings
                                            </th>
                                            <td>
                                                { item.nb_numerical_strings }
                                            </td>
                                        </tr>

                                        <tr>
                                            <th scope='row'>
                                                # html elements
                                            </th>
                                            <td>
                                                { item.nb_tags }
                                            </td>
                                        </tr>

                                        <tr>
                                            <th scope='row'>
                                                # words
                                            </th>
                                            <td>
                                                { item.nb_words }
                                            </td>
                                        </tr>

                                        <tr>
                                            <th scope='row'>
                                                Title
                                            </th>
                                            <td>
                                                { item.title }
                                            </td>
                                        </tr>

                                        <tr>
                                            <th scope='row'>
                                                Html struct
                                            </th>
                                            <td className="html-struct">
                                                { renderHtmlStruct(item.htmlstruct) }
                                            </td>
                                        </tr>

                                        <tr>
                                            <th scope='row'>
                                                Body text
                                            </th>
                                            <td className="body-text">
                                                { renderBodyText(item.body_text) }
                                            </td>
                                        </tr>

                                        <tr>
                                            <th scope='row'>
                                                Meta text
                                            </th>
                                            <td>
                                                { item.meta_text }
                                            </td>
                                        </tr>

                                        <tr>
                                            <th scope='row'>
                                                URL
                                            </th>
                                            <td>
                                                { item.url }
                                            </td>
                                        </tr>

                                        <tr>
                                            <th scope='row'>
                                                Title truncated
                                            </th>
                                            { 
                                                renderDataBoolean(item.title_truncated) // td element
                                            }
                                        </tr>

                                        <tr>
                                            <th scope='row'>
                                                # letters
                                            </th>
                                            <td>
                                                { item.nb_letters }
                                            </td>
                                        </tr>

                                        <tr>
                                            <th scope='row'>
                                                # distinct hosts in URL
                                            </th>
                                            <td>
                                                { item.nb_distinct_hosts_in_urls }
                                            </td>
                                        </tr>

                                        <tr>
                                            <th scope='row'>
                                                External hosts
                                            </th>
                                            <td>
                                                { renderExternalHosts(item.external_hosts) }
                                            </td>
                                        </tr>

                                        <tr>
                                            <th scope='row'>
                                                # Facebook links
                                            </th>
                                            <td>
                                                { renderAmntOfLinks(item.nb_facebook_shallow_links, item.nb_facebook_deep_links) }
                                                <br />
                                                { renderLinks(item.facebook_links, setOpenFacebookLinks, openFacebookLinks) }
                                            </td>
                                        </tr>

                                        <tr>
                                            <th scope='row'>
                                                # Twitter links
                                            </th>
                                            <td>
                                                { renderAmntOfLinks(item.nb_twitter_shallow_links, item.nb_twitter_deep_links) }
                                                <br />
                                                { renderLinks(item.twitter_links, setOpenTwitterLinks, openTwitterLinks) }
                                            </td>
                                        </tr>

                                        <tr>
                                            <th scope='row'>
                                                # LinkedIn links
                                            </th>
                                            <td>
                                                { renderAmntOfLinks(item.nb_linkedin_shallow_links, item.nb_linkedin_deep_links) }
                                                <br />
                                                { renderLinks(item.linkedin_links, setOpenLinkedinLinks, openLinkedinLinks) }
                                            </td>
                                        </tr>

                                        <tr>
                                            <th scope='row'>
                                                # YouTube links
                                            </th>
                                            <td>
                                                { renderAmntOfLinks(item.nb_youtube_shallow_links, item.nb_youtube_deep_links) }
                                                <br />
                                                { renderLinks(item.youtube_links, setOpenYoutubeLinks, openYoutubeLinks) }
                                            </td>
                                        </tr>

                                        <tr>
                                            <th scope='row'>
                                                # Vimeo links
                                            </th>
                                            <td>
                                                { renderAmntOfLinks(item.nb_vimeo_shallow_links, item.nb_vimeo_deep_links) }
                                                <br />
                                                { renderLinks(item.vimeo_links, setOpenVimeoLinks, openVimeoLinks) }
                                            </td>
                                        </tr>

                                        <tr>
                                            <th scope='row'>
                                                # occurences of currency names
                                            </th>
                                            <td>
                                                { item.nb_currency_names }
                                            </td>
                                        </tr>

                                        <tr>
                                            <th scope='row'>
                                                # distinct currency names
                                            </th>
                                            <td>
                                                { item.nb_distinct_currencies }
                                            </td>
                                        </tr>

                                        <tr>
                                            <th scope='row'>
                                                # distinct words in title
                                            </th>
                                            <td>
                                                { item.nb_distinct_words_in_title }
                                            </td>
                                        </tr>

                                        <tr>
                                            <th scope='row'>
                                                Edit distance between title and initial / final DN
                                            </th>
                                            <td>
                                                { // Ternary
                                                    item.distance_title_initial_dn !== null || item.distance_title_final_dn !== null ?
                                                        item.distance_title_initial_dn + " / " + item.distance_title_final_dn : 
                                                        ""
                                                }
                                            </td>
                                        </tr>

                                        <tr>
                                            <th scope="row">
                                                Longest subsequence between title and initial / final DN
                                            </th>
                                            <td>
                                                { // Ternary
                                                    item.longest_subsequence_title_initial_dn !== null || item.longest_subsequence_title_final_dn !== null ?
                                                        item.longest_subsequence_title_initial_dn + " / " + item.longest_subsequence_title_final_dn : 
                                                        ""
                                                }
                                            </td>
                                        </tr>

                                        <tr>
                                            <th scope="row">
                                                Fraction of words in title also in initial / final DN
                                            </th>
                                            <td>
                                                { // Ternary
                                                    item.fraction_words_title_initial_dn != null || item.fraction_words_title_final_dn != null ?
                                                        item.fraction_words_title_initial_dn + " / " + item.fraction_words_title_final_dn : 
                                                        ""
                                                }
                                            </td>
                                        </tr>

                                        <tr>
                                            <th scope="row">
                                                Language
                                            </th>
                                            <td>
                                                { // Ternary
                                                    item.body_text_language != null || item.body_text_language_2 != null ?
                                                        item.body_text_language + " (" + item.body_text_language_2 + ")" : 
                                                        ""
                                                }
                                            </td>
                                        </tr>

                                    </tbody>
                                </Table>
                            </div>
                        )
                    })
                }
            </>
        );
    }
    
    // This file's HTML return.
    return (
        <Row>
            <Col className='mt-4'>
                <BorderWrapper 
                    borderWidth="3px" 
                    borderRadius="0px" 
                    innerPadding="30px"
                    topElement={topElement}
                    topPosition={0.07} 
                    topOffset="15px" 
                    topGap="15px"
                >
                    {
                        renderHTML()
                    }
                </BorderWrapper>
            </Col>
        </Row>
    );
}

export default HTMLCard;
