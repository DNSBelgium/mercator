import {useEffect, useState} from "react";
import {Accordion, Button, Card, Col, Row, Table} from "react-bootstrap";
import BorderWrapper from "react-border-wrapper";
import moment from "moment";
import api from "../../services/api";

const HTMLCard = (props) => {

    const visitId = props.visitId

    const [data, setData] = useState([]);

    useEffect(() => {
        const handlerData = async () => {

            const url = `htmlFeatureses/search/findByVisitId?visitId=${visitId}`;
            await api.get(url)
                .then((resp) => {
                    if(resp.status === 200) {
                        setData(resp === undefined ? null : resp.data._embedded.htmlFeatureses);
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

    const topElement = <p className='top-element'>HTML features</p>
    if (!data.length || data.length === 0) {
        return (
            <>
                <Row>
                    <Col className='mt-4'>
                        <BorderWrapper borderWidth="3px" borderRadius="0px" innerPadding="30px"
                                       topElement={topElement}
                                       topPosition={0.07} topOffset="15px" topGap="15px">
                            <p>no data for this visit</p>
                        </BorderWrapper>
                    </Col>
                </Row>
            </>
        )
    }
    return (
        <>
            {data.map(data => (
                <Row key={data.id}>
                    <Col className='mt-4'>
                        <BorderWrapper borderWidth="3px" borderRadius="0px" innerPadding="30px"
                                       topElement={topElement}
                                       topPosition={0.07} topOffset="15px" topGap="15px">
                            <div className="html-table">
                                <Table size="sm" borderless>
                                    <tbody className="text-left">
                                    <tr>
                                        <th scope="row">Id</th>
                                        <td>{data.id}</td>
                                    </tr>
                                    <tr>
                                        <th scope="row">Crawl timestamp</th>
                                        <td>{moment(data.crawlTimestamp).format("YYYY-MM-DD HH:mm:ss")}</td>
                                    </tr>
                                    <tr>
                                        <th scope="row">#image</th>
                                        <td>{data.nb_imgs}</td>
                                    </tr>
                                    <tr>
                                        <th scope="row">#internal links</th>
                                        <td>{data.nb_links_int}</td>
                                    </tr>
                                    <tr>
                                        <th scope="row">#external links</th>
                                        <td>{data.nb_links_ext}</td>
                                    </tr>
                                    <tr>
                                        <th scope="row">#telephone links</th>
                                        <td>{data.nb_links_tel}</td>
                                    </tr>
                                    <tr>
                                        <th scope="row">#mailto links</th>
                                        <td>{data.nb_links_email}</td>
                                    </tr>
                                    <tr>
                                        <th scope="row">#text input fields</th>
                                        <td>{data.nb_input_txt}</td>
                                    </tr>
                                    <tr>
                                        <th scope="row">#buttons</th>
                                        <td>{data.nb_button}</td>
                                    </tr>
                                    <tr>
                                        <th scope="row">#meta descriptions</th>
                                        <td>{data.nb_meta_desc}</td>
                                    </tr>
                                    <tr>
                                        <th scope="row">#meta keywords</th>
                                        <td>{data.nb_meta_keyw}</td>
                                    </tr>
                                    <tr>
                                        <th scope="row">#numerical strings</th>
                                        <td>{data.nb_numerical_strings}</td>
                                    </tr>
                                    <tr>
                                        <th scope="row">#html elements</th>
                                        <td>{data.nb_numerical_strings}</td>
                                    </tr>
                                    <tr>
                                        <th scope="row">#words</th>
                                        <td>{data.nb_words}</td>
                                    </tr>
                                    <tr>
                                        <th scope="row">Title</th>
                                        <td>{data.title}</td>
                                    </tr>
                                    <tr>
                                        <th scope="row">Htmlstruct</th>
                                        <td className="htmlStruct">
                                            <Accordion>
                                                <Accordion.Toggle as={Button} id="button-htmlStruct" variant="link"
                                                                  eventKey="3"
                                                                  onClick={() => setOpenHtmlstructButton(!openHtmlstructButton)}>More
                                                    info</Accordion.Toggle>
                                                <Accordion.Collapse eventKey="3" in={openHtmlstructButton}>
                                                    <Card.Body>{data.htmlstruct}</Card.Body>
                                                </Accordion.Collapse>
                                            </Accordion>
                                        </td>
                                    </tr>
                                    <tr>
                                        <th scope="row">Body text</th>
                                        <td>
                                            <Accordion>
                                                <Accordion.Toggle as={Button} id="button-body-text" variant="link"
                                                                  eventKey="3"
                                                                  onClick={() => setOpenBodyTextButton(!openBodyTextButton)}>More
                                                    info</Accordion.Toggle>
                                                <Accordion.Collapse eventKey="3" in={openBodyTextButton}>
                                                    <Card.Body>{data.body_text}</Card.Body>
                                                </Accordion.Collapse>
                                            </Accordion>
                                        </td>
                                    </tr>
                                    <tr>
                                        <th scope="row">Meta text</th>
                                        <td>{data.meta_text}</td>
                                    </tr>
                                    <tr>
                                        <th scope="row">URL</th>
                                        <td>{data.url}</td>
                                    </tr>
                                    <tr>
                                        <th scope="row">Title truncated</th>
                                        <td>{data.title_truncated ? 'true' : 'false'}</td>
                                    </tr>
                                    <tr>
                                        <th scope="row">Nb letters</th>
                                        <td>{data.nb_letters}</td>
                                    </tr>
                                    <tr>
                                        <th scope="row">Nb distinct hosts in urls</th>
                                        <td>{data.nb_distinct_hosts_in_urls}</td>
                                    </tr>
                                    <tr>
                                        <th scope="row">External hosts</th>
                                        <td>
                                            {data.external_hosts ? data.external_hosts.length ?
                                                    <Accordion>
                                                        <Accordion.Toggle as={Button} id="button-external-hosts"
                                                                          variant="link" eventKey="3"
                                                                          onClick={() => setOpenExternalHosts(!openExternalHosts)}>More
                                                            info</Accordion.Toggle>
                                                        <Accordion.Collapse eventKey="3" in={openExternalHosts}>
                                                            <ul className="mt-2">
                                                                {data.external_hosts.map(data => (
                                                                    <li>{data}</li>
                                                                ))}
                                                            </ul>
                                                        </Accordion.Collapse>
                                                    </Accordion>
                                                    : ''
                                                : ''}
                                        </td>
                                    </tr>
                                    <tr>
                                        <th scope="row">#Facebook links
                                            {data.nb_facebook_shallow_links || data.nb_facebook_deep_links ?
                                                " (shallow / deep)" : ""}</th>
                                        <td>{data.nb_facebook_shallow_links != null || data.nb_facebook_deep_links != null ?
                                            data.nb_facebook_shallow_links + " / " + data.nb_facebook_deep_links : ""}


                                            {data.facebook_links && data.facebook_links.length ?
                                                <Accordion>
                                                    <Accordion.Toggle as={Button} className="toggle-button"
                                                                      id="button-facebook-links"
                                                                      variant="link" eventKey="3"
                                                                      onClick={() => setOpenFacebookLinks(!openFacebookLinks)}>Facebook
                                                        links</Accordion.Toggle>
                                                    <Accordion.Collapse eventKey="3" in={openFacebookLinks}>
                                                        <ul className="mt-2">
                                                            {data.facebook_links.map(data => (
                                                                <li>{data}</li>
                                                            ))}
                                                        </ul>
                                                    </Accordion.Collapse>
                                                </Accordion>
                                                : ''}

                                        </td>
                                    </tr>
                                    <tr>
                                        <th scope="row">#Twitter links
                                            {data.nb_twitter_shallow_links || data.nb_twitter_deep_links ?
                                                " (shallow / deep)" : ""}</th>
                                        <td>{data.nb_twitter_shallow_links != null || data.nb_twitter_deep_links != null ?
                                            data.nb_twitter_shallow_links + " / " + data.nb_twitter_deep_links : ""}

                                            {data.twitter_links && data.twitter_links.length ?
                                                <Accordion>
                                                    <Accordion.Toggle as={Button} className="toggle-button"
                                                                      id="button-twitter-links"
                                                                      variant="link" eventKey="3"
                                                                      onClick={() => setOpenTwitterLinks(!openTwitterLinks)}>Twitter
                                                        links</Accordion.Toggle>
                                                    <Accordion.Collapse eventKey="3" in={openTwitterLinks}>
                                                        <ul className="mt-2">
                                                            {data.twitter_links.map(data => (
                                                                <li>{data}</li>
                                                            ))}
                                                        </ul>
                                                    </Accordion.Collapse>
                                                </Accordion>
                                                : ''}

                                        </td>
                                    </tr>
                                    <tr>
                                        <th scope="row">#Linkedin links
                                            {data.nb_linkedin_shallow_links || data.nb_linkedin_deep_links ?
                                                " (shallow / deep)" : ""}</th>
                                        <td>{data.nb_linkedin_shallow_links != null || data.nb_linkedin_deep_links != null ?
                                            data.nb_linkedin_shallow_links + " / " + data.nb_linkedin_deep_links : ""}


                                            {data.linkedin_links && data.linkedin_links.length ?
                                                <Accordion>
                                                    <Accordion.Toggle as={Button} className="toggle-button"
                                                                      id="button-linkedin-links"
                                                                      variant="link" eventKey="3"
                                                                      onClick={() => setOpenLinkedinLinks(!openLinkedinLinks)}>Linkedin
                                                        links</Accordion.Toggle>
                                                    <Accordion.Collapse eventKey="3" in={openLinkedinLinks}>
                                                        <ul className="mt-2">
                                                            {data.linkedin_links.map(data => (
                                                                <li>{data}</li>
                                                            ))}
                                                        </ul>
                                                    </Accordion.Collapse>
                                                </Accordion>
                                                : ''}

                                        </td>
                                    </tr>
                                    <tr>
                                        <th scope="row">#YouTube links
                                            {data.nb_youtube_shallow_links || data.nb_youtube_deep_links ?
                                                " (shallow / deep)" : ""}</th>
                                        <td>{data.nb_youtube_shallow_links != null || data.nb_youtube_deep_links != null ?
                                            data.nb_youtube_shallow_links + " / " + data.nb_youtube_deep_links : ""}


                                            {data.youtube_links && data.youtube_links.length ?
                                                <Accordion>
                                                    <Accordion.Toggle as={Button} className="toggle-button"
                                                                      id="button-youtube-links"
                                                                      variant="link" eventKey="3"
                                                                      onClick={() => setOpenYoutubeLinks(!openYoutubeLinks)}>YouTube
                                                        links</Accordion.Toggle>
                                                    <Accordion.Collapse eventKey="3" in={openYoutubeLinks}>
                                                        <ul className="mt-2">
                                                            {data.youtube_links.map(data => (
                                                                <li>{data}</li>
                                                            ))}
                                                        </ul>
                                                    </Accordion.Collapse>
                                                </Accordion>
                                                : ''}

                                        </td>
                                    </tr>
                                    <tr>
                                        <th scope="row">#Vimeo links
                                            {data.nb_vimeo_shallow_links || data.nb_vimeo_deep_links ?
                                                " (shallow / deep)" : ""}</th>
                                        <td>{data.nb_vimeo_shallow_links != null || data.nb_vimeo_deep_links != null ?
                                            data.nb_vimeo_shallow_links + " / " + data.nb_vimeo_deep_links : ""}


                                            {data.vimeo_links && data.vimeo_links.length ?
                                                <Accordion>
                                                    <Accordion.Toggle as={Button} className="toggle-button"
                                                                      id="button-vimeo-links"
                                                                      variant="link" eventKey="3"
                                                                      onClick={() => setOpenVimeoLinks(!openVimeoLinks)}>Vimeo
                                                        links</Accordion.Toggle>
                                                    <Accordion.Collapse eventKey="3" in={openVimeoLinks}>
                                                        <ul className="mt-2">
                                                            {data.vimeo_links.map(data => (
                                                                <li>{data}</li>
                                                            ))}
                                                        </ul>
                                                    </Accordion.Collapse>
                                                </Accordion>
                                                : ''}

                                        </td>
                                    </tr>
                                    <tr>
                                        <th scope="row">#occurrences of currency names</th>
                                        <td>{data.nb_currency_names}</td>
                                    </tr>
                                    <tr>
                                        <th scope="row">#distinct currency names</th>
                                        <td>{data.nb_distinct_currencies}</td>
                                    </tr>
                                    <tr>
                                        <th scope="row">#distinct words in title</th>
                                        <td>{data.nb_distinct_words_in_title}</td>
                                    </tr>
                                    <tr>
                                        <th scope="row">Edit distance between title and initial / final DN</th>
                                        <td>{data.distance_title_initial_dn != null || data.distance_title_final_dn != null ?
                                            data.distance_title_initial_dn + " / " + data.distance_title_final_dn : ""}</td>
                                    </tr>
                                    <tr>
                                        <th scope="row">Longest subsequence between title and initial / final DN</th>
                                        <td>{data.longest_subsequence_title_initial_dn != null || data.longest_subsequence_title_final_dn != null ?
                                            data.longest_subsequence_title_initial_dn + " / " + data.longest_subsequence_title_final_dn : ""}</td>
                                    </tr>
                                    <tr>
                                        <th scope="row">Fraction of words in title also in initial / final DN</th>
                                        <td>{data.fraction_words_title_initial_dn != null || data.fraction_words_title_final_dn != null ?
                                            data.fraction_words_title_initial_dn + " / " + data.fraction_words_title_final_dn : ""}</td>
                                    </tr>
                                    <tr>
                                        <th scope="row">Language</th>
                                        <td>{data.body_text_language != null || data.body_text_language_2 != null ?
                                            data.body_text_language + " (" + data.body_text_language_2 + ")" : ""}</td>
                                    </tr>


                                    </tbody>
                                </Table>
                            </div>
                        </BorderWrapper>
                    </Col>
                </Row>
            ))}
        </>
    )
}

export default HTMLCard;
