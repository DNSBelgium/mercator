import { useRef, useState } from "react";
import { useNavigate } from 'react-router-dom';

import { Button } from "react-bootstrap";

import api from '../services/api';
import { checkObjectIsFalsy } from "../services/Util";

const ClusterValidator = () => {
    const navigate = useNavigate();

    const visitIdRef = useRef(null);

    const [data, setData] = useState([]);

    const handleSubmit = async () => {
        let input = visitIdRef.current.value.toLowerCase();

        const url = `/cluster?visitIds=${input}`;
        await api.get(url)
                .then((resp) => {
                    if(resp.status === 200) {
                        
                        setData(resp.data);
                    }
                })
                .catch((ex) => {
                    console.log(ex.response);
                });
    }

    const renderImages = () => {
        // URL for development / local environment.
        const DEV_URL = window._env_.REACT_APP_MUPPETS_HOST;
        const LOCAL_URL = 'http://localhost:4566/mercator-muppets';

        if(!checkObjectIsFalsy(data)) {
            return (
                <div id="flex-div">
                    {
                        data.map((item, index) => {
                            return (
                                <div 
                                    key={index}
                                    id="flex-item"
                                    onClick={() => navigate('/details/' + item.visitId)}
                                >
                                    <img
                                        className="timeline-image"
                                        src={`${LOCAL_URL}/${item.screenshotKey}`}
                                        alt={`Thumbnail of ${item.visitId}`}
                                    >
                                    </img>
                                    <p>{item.domainName}</p>
                                </div>
                                
                            )
                        })
                    }
                </div>
            );
        }
    }

    return (
        <div id="cluster-validator-div">
            <h5>Enter up to 50 VisitId's.</h5>
            <p>VisitId's can separated by a new line, comma and/or space.</p>

            <textarea
                id="cluster-textarea"
                placeholder={"adcx4899-79sx-6790-489o-a42ba489a6f7, abcd4899-79sx-6790-489o-a42ba489a6f7\nabcd4899-79sx-6790-489o-a42ba489a6f7"}
                ref={visitIdRef}
            >
            </textarea>

            <br/>
            <Button onClick={() => handleSubmit()}>Submit</Button>
            <br/>

            {
                renderImages()
            }
        </div>
    );
}

export default ClusterValidator;