import {Col, Row} from "react-bootstrap";
import {useEffect, useState} from "react";
import api from "../../services/api";
import { checkObjectIsFalsy } from "../../services/Util";

function DispatcherCard(props) {

    const visitId = props.visitId
    const [data, setData] = useState(null);

    useEffect(() => {
        const handlerData = async () => {

            const url = `/dispatcherEvents/${visitId}`;

            await api.get(url)
                .then((resp) => {
                    if(resp.status === 200) {
                        setData(resp === undefined ? null : resp.data);
                    }
                })
                .catch((ex) => {
                    console.log(ex);
                });
        };

        handlerData();
    }, [visitId]);

    if (checkObjectIsFalsy(data)) {
        return (
            <>
                <Row className="mb-4">
                    <Col className='mt-4'>
                        <h4>no data for this visit</h4>
                    </Col>
                </Row>
            </>
        )
    } else {
        let listlabels = 'No labels';
        if (!checkObjectIsFalsy(data.labels)) {
            listlabels = data.labels.map((label, index) => <span key={index.toString()}>{label}</span>);
        }
        return (
            <>     
                <div className="mb-4 mt-2 ml-1">
                    <h1>{ data.domainName }</h1>

                    <Row>
                        <Col id='visit-id' md="auto">Visit id:</Col>
                        <Col md="auto">{data.visitId}</Col>
                    </Row>

                    <Row>
                        <Col id='labels' md="auto">Labels:</Col>
                        <Col md="auto">{listlabels}</Col>
                    </Row>
                </div>
            </>
        )
    }
}

export default DispatcherCard;
