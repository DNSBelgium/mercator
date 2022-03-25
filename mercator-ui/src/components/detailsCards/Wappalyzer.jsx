import {Row, Col, Table} from "react-bootstrap";
import {useEffect, useState} from "react";
import api from "../../services/api";
import { checkDataObject, renderDataBoolean } from "../../services/Util";

const Wappalyzer = (props) => {

    const visitId = props.visitId
    const {openTechnologies, setOpenTechnologies, openUrls, setOpenUrls} = props;

    const [data, setData] = useState({});
    const [technologies, setTechnologies] = useState([]);
    const [renderTech, setRenderTech] = useState([]);

    useEffect(() => {
        const handlerData = async () => {

            const url = `/wappalyzerResults/${visitId}`;
            await api.get(url)
                .then((resp) => {
                    if(resp.status === 200) {
                        setData(resp === undefined ? null : resp.data);
                        setTechnologies(resp === undefined ? [] : resp.data.technologies);

                        // Set renderTech array with the same length as technologies array and initialize all with value: false.
                        // Used for showing / not showing technologies in the renderTechonlogies function.
                        setRenderTech(new Array(resp.data.technologies.length).fill(false)); 
                    }
                })
                .catch((ex) => {
                    console.log(ex);
                });            
        };
        handlerData();
    }, [visitId]);

    // Set the current renderTech[index] boolean to the opposite value of its current value.
    const handleTechClick = (index) => {
        const bool = renderTech[index];

        // Destructuring the renderTech array, requesting the index and flipping the boolean.
        setRenderTech({
            ...renderTech,
            [index]: !bool
        });
    }

    // Define logic for and render data.technologies
    const renderTechnologies = () => {
        
        if(!technologies || technologies.length === 0) {
            return(<td>{""}</td>)
        }

        return ( // Change these Accordions to displayed or undisplayed Divs. Look at ContentCrawlCard.jsx. WIP
            <td>
                <button 
                    className="more-info"
                    onClick={() => setOpenTechnologies(openTechnologies => !openTechnologies)} // Toggle openTechnologies boolean
                > 
                    More info
                </button>

                { 
                    openTechnologies && ( // if openTechnologies === true, render
                        <div id="technologies-list">
                            {
                                technologies.map((item, index) => {
                                    return(
                                        <div className="ml-3" key={index}>
                                            <button
                                                className="technologies-names-btns"
                                                onClick={() => handleTechClick(index)}
                                            >
                                                { item.name }
                                            </button>

                                            { 
                                                // The handleTechClick function sets booleans to true or false in the renderTech array.
                                                // Here we decide that if renderTech[index] === true then we render the technology its list of data.
                                                renderTech[index] && (
                                                    <ul key={index}>
                                                        <li>
                                                            <span className="font-weight-bold">Slug: </span>
                                                            { technologies[index].slug }
                                                        </li>
                                                        <li>
                                                            <span className="font-weight-bold">Name: </span>
                                                            { technologies[index].name }
                                                        </li>
                                                        <li>
                                                            <span className="font-weight-bold">Confidence: </span>
                                                            { technologies[index].confidence }
                                                        </li>
                                                        <li>
                                                            <span className="font-weight-bold">Version: </span>
                                                            { technologies[index].version }
                                                        </li>
                                                        <li>
                                                            <span className="font-weight-bold">Icon: </span>
                                                            { technologies[index].icon }
                                                        </li>
                                                        <li>
                                                            <span className="font-weight-bold">Website: </span>
                                                            { technologies[index].website }
                                                        </li>
                                                        <li>
                                                            <span className="font-weight-bold">Cpe: </span>
                                                            { technologies[index].cpe }
                                                        </li>
                                                        <li>
                                                            <span className="font-weight-bold">Categories: </span>
                                                        </li>
                                                        <ul>
                                                            {
                                                                technologies[index].categories.map((item, index) => {
                                                                    return (
                                                                        <div className="mb-2" key={index}>
                                                                            <li>
                                                                                <span className="font-weight-bold">Id: </span>
                                                                                { item.id }
                                                                            </li>
                                                                            <li>
                                                                                <span className="font-weight-bold">Slug: </span>
                                                                                { item.slug }
                                                                            </li>
                                                                            <li>
                                                                                <span className="font-weight-bold">Name: </span>
                                                                                { item.name }
                                                                            </li>
                                                                        </div>
                                                                    )
                                                                })
                                                            }
                                                        </ul>
                                                    </ul>
                                                )
                                            }
                                        </div>
                                    )
                                })
                            }
                        </div>
                    )
                }
            </td>
        );
    }

    // Define logic for and render data.urls
    const renderUrls = () => {
        if(!data.urls) {
            return(<td></td>);
        }
        return (
            <td>
                <button 
                    className="more-info"
                    onClick={() => setOpenUrls(openUrls => !openUrls)} // Toggle openUrls boolean
                > 
                    More info
                </button>

                {
                    openUrls && ( // if openUrls === true, render
                    <ul>
                        {
                            Object.entries(data.urls).map((item, index) => {
                                return (
                                    <li key={index}>
                                        <span className="font-weight-bold">
                                            { item[0] } 
                                        </span>

                                        <ul className="mb-2">
                                            {
                                                item[1] && (
                                                    Object.entries(item[1]).map(([key, value]) => {
                                                        return (
                                                            <li key={key}>
                                                                <span className="font-weight-bold">
                                                                    { key }:&nbsp;
                                                                </span>
                                                                { value }
                                                            </li>
                                                        );
                                                    })
                                                )
                                            }
                                        </ul>
                                    </li>
                                );
                            })
                        }   
                    </ul>
                    )
                }
            </td>
        );
    }

    // Writing HTML on a function base so we can define logic more easily.
    const renderHTML = () => {
        if (checkDataObject(data)) {
            return (
                <>
                    <Row>
                        <Col className='mt-2 mb-3'>
                            <span className="font-weight-bold">Wappalyzer: </span>no data for this visit
                        </Col>
                    </Row>
                </>
            )
        }
        return (
            <>
                <h4 className="mb-3 text-md-left">Wappalyzer</h4>
                <div className="card_content">
                    <Row>
                        <Col>
                            <Table 
                                size='sm'
                                borderless
                            >
                                <tbody className="text-left">
                                    <tr>
                                        <th scope="row">
                                            Url
                                        </th>
                                        <td>
                                            { data.url }
                                        </td>
                                    </tr>

                                    <tr>
                                        <th scope="row">
                                            Ok
                                        </th>
                                        { 
                                            renderDataBoolean(data.ok) // td element
                                        }
                                    </tr>

                                    <tr>
                                        <th scope='row'>
                                            Technologies
                                        </th>
                                        { 
                                            renderTechnologies(technologies) // td element
                                        }
                                    </tr>

                                    <tr>
                                        <th scope="row">
                                            Error
                                        </th>
                                        <td className="error-wappalyzer"> 
                                            { data.error }
                                        </td>
                                    </tr>

                                    <tr>
                                        <th scope='row'>
                                            Urls
                                        </th>
                                        {
                                            renderUrls(data.urls) // td element
                                        }
                                    </tr>
                                </tbody>
                            </Table>
                        </Col>
                    </Row>
                </div>
            </>
        );
    }

    // This file's HTML return.
    return (
        <>
            {
                renderHTML()
            }
        </>
    );
}

export default Wappalyzer;
