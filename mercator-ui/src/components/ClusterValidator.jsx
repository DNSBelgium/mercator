import { useRef, useState } from "react";
import { useNavigate } from 'react-router-dom';

import { Button } from "react-bootstrap";

import api from '../services/api';
import { checkObjectIsFalsy } from "../services/Util";

const ClusterValidator = (props) => {
    const navigate = useNavigate();
    const visitIdRef = useRef(null);

    const [exception, setException] = useState(null);

    // Checking if the input is greater than X to warn / alert user.
    const checkInput = (input) => {
        let trimmedInput = input.replace(/\n| , | ,|, |,| /g, "");
        let inputLength = trimmedInput.length;

        // Retaining this code in case muppets allows handling of more than 50 visitId's.
        // if (inputLength > 18000) { // 500 VisitId's
        //     alert(`${inputLength / 36} VisitId's have been given. Please input 500 or less.`);
        //     return false;
        // }
        // if (inputLength > 1800) { // 50 VisitId's
        //     return window.confirm(`${inputLength / 36} VisitId's have been given. Continue any way?`);
        // }
        if (inputLength > 1800) { // 500 VisitId's
            alert(`${inputLength / 36} VisitId's have been given. Please input 50 or less.`);
            return false;
        }
        return true;
    }

    // Handle clicking of the 'Submit' button.
    const handleSubmit = async () => {
        props.setClusterData([]);
        let input = visitIdRef.current.value.toLowerCase();

        if (!checkInput(input)) return;

        // Replacing new lines with "NEWLINE" for backend's readability.
        // \n would contain an invalid character for HTTP, and encoding with UTF-8 / 16 creates a whole new array of problems.
        // Not sure how to improve this.
        input = input.replace(/\n/g, "NEWLINE");

        let dataToSend = {
            data: input
        }

        const url = `/cluster`;
        await api.post(url, dataToSend)
            .then((resp) => {
                if(resp.status === 200) {
                    
                    props.setClusterData(resp.data);
                    setException(null);
                }
            })
            .catch((ex) => {
                console.log(ex.response);
                props.setClusterData([]);
                setException(ex.response);
            });
    }

    // Handle rendering of images after data has been received from the backend.
    const renderImages = () => {
        // URL for development / local environment.
        const DEV_URL = window._env_.REACT_APP_MUPPETS_HOST;
        const LOCAL_URL = 'http://localhost:4566/mercator-muppets';

        if(!checkObjectIsFalsy(props.clusterData)) {
            return (
                <div id="flex-div">
                    {
                        props.clusterData.map((item, index) => {
                            if(checkObjectIsFalsy(item.screenshotKey)) return null;
                            return (
                                <div
                                    key={index}
                                    id="flex-item"
                                    onClick={() => navigate('/details/' + item.visitId)}
                                >
                                    <img
                                        className="timeline-image"
                                        src={`${DEV_URL}/${item.screenshotKey}`}
                                        alt={`Thumbnail of ${ item.visitId }`}
                                    >
                                    </img>
                                    <p>{ item.domainName }</p>
                                </div>                        
                            )
                        })
                    }
                </div>
            );
        }

        if (!checkObjectIsFalsy(exception)) {
            return (
                <p className="mt-3">An error has occurred.</p>
            );
        }
    }

    // Handle rendering of faulty visitId's if there are any.
    const renderFaultyVisitIds = () => {
        let ids = []; // Creating array to hold jsx elements

        // If an item doesn't have a screenshotKey, add it as a 'faulty visitId' to the array.
        props.clusterData.map((item, index) => {
            if (checkObjectIsFalsy(item.screenshotKey)) {
                ids.push( // Adding this object to the array.
                    <> 
                        <p 
                            key={index}
                            onClick={() => navigate(`/details/${item.receivedVisitId}`)}
                        >
                            { item.receivedVisitId }
                        </p>
                    </>
                );
            }
            return null;
        })

        if (!checkObjectIsFalsy(ids)) { // If any faulty visitId's got added, render
            return (
                <>
                    <h5>Faulty VisitId's:</h5>
                    {
                        ids
                    }
                </>
            );
        }
    }

    // Return of this file's HTML.
    return (
        <div id="cluster-validator-div">
            <h5>Enter up to 50 VisitId's.</h5>
            <p>VisitId's can separated by a new line, comma and/or space.</p>

            <textarea
                id="cluster-textarea"
                placeholder={"qnax4899-79sx-6790-489o-a42ba489a6f7, qnax4899-79sx-6790-489o-a42ba489a6f7\nqnax4899-79sx-6790-489o-a42ba489a6f7"}
                ref={visitIdRef}
            >
            </textarea>

            <br/>
            <Button onClick={() => handleSubmit()}>Submit</Button>
            <br/>

            {
                renderImages()
            }
            <div id="faulty-visitIds">
                {
                    renderFaultyVisitIds()
                }
            </div>
        </div>
    );
}

export default ClusterValidator;