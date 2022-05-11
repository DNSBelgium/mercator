import { useRef, useState } from "react";

import api from '../services/api';

const ClusterValidator = () => {
    const visitIdRef = useRef(null);
    const [data, setData] = useState([]);
    // fc8b5040-1659-47d7-851e-b98ce904d9f7

    const handleSubmit = async () => {
        let input = visitIdRef.current.value.toLowerCase().trim();
        console.log(input);

        let data = {
            sending: input
        }

        const url = "/cluster";

        // axios({
        //     method: 'post',
        //     url: url,
        //     data: input,
        //     responseType: 'text'
        // }).then((resp) => {
        //     console.log(resp);
        // })
        await api.post(url, data)
                .then((resp) => {
                    if(resp.status === 200) {
                        
                        setData(resp.data);
                    }
                })
                .catch((ex) => {
                    console.log(ex.response);
                });
    }

    return (
        <div id="cluster-validator-div">
            <textarea
                id="cluster-textarea"
                placeholder="Input up to 50 space or comma separated visit id's."
                ref={visitIdRef}
            >
            </textarea>

            <br/>
            <button onClick={() => handleSubmit()}>Submit</button>
        </div>
    );
}

export default ClusterValidator;