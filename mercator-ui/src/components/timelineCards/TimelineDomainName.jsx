import {Col, Row, Table} from "react-bootstrap";
import React, {useEffect, useState} from 'react';
import {Link} from 'react-router-dom';
import api from "../../services/api";
import moment from "moment";

const TimelineDomainName = (props) => {

    const domainName = props.domainName;

    const [data, setData] = useState([]);
    const [status, setStatus] = useState([]);
    const [processing, setProcessing] = useState(true);

    // Function to sort rows by crawl date desc
    function sortVisits(a, b) {
        return Date.parse(b.requestTimestamp) - Date.parse(a.requestTimestamp);
    }

    useEffect(() => {
        setProcessing(true);

        if (!domainName) {
            setProcessing(processing => !processing);
            return;
        }

        const handlerData = async () => {
            let response;
            try {
                response = await api.get(`/dispatcherEvents/search/findDispatcherEventByDomainName?domainName=${domainName}`);
            } catch (e) {
                console.log(e)
            }
            let data = response.data._embedded.dispatcherEvents;

            data.sort(sortVisits);

            setData(data);
            setStatus([]); // Clear status state for a new domain
            setProcessing(processing => !processing);
        };
        handlerData();
    }, [domainName])

    useEffect(() => {
        data.forEach(async (item, i, arr) => {
            let response;
            try {
                response = await api.get(`/status/${item.visitId}`);
            } catch (e) {
                console.log(e)
            }
            setStatus(array => [...array, response.data])
        });
    }, [data])

    if (!domainName || processing === true) return null;

    if (data.length === 0) return (
        <>
            This domain was not yet crawled or does not exist
        </>
    );

    let statusMap = status.reduce((obj, data) => ({...obj, [data.visit_id]: data}), {})

    return (
        <>
            <Row>
                <Col className='mt-4'>
                    <div>
                        <h1 className="mt-5 mb-4">{domainName}</h1>
                    </div>
                    <div className="mt-5">
                        <Table className="table-timeline" bordered hover size="sm">
                            <thead className="header-timeline-table">
                            <tr>
                                <th>Visit id</th>
                                <th>Crawl time</th>
                                <th>Final url</th>
                                <th>Status<br/> content crawl</th>
                                <th>Status<br/> DNS crawl</th>
                                <th>Status<br/> SMTP crawl</th>
                                <th>Status<br/> Wappalyzer</th>
                            </tr>
                            </thead>
                            <tbody>
                            {data.map(data => (
                                <tr key={data.visitId}>
                                    <td>
                                        <Link to={{pathname: `/details/${data.visitId}`}}>{data.visitId}</Link>
                                    </td>
                                    <td>{data.requestTimestamp ? moment(data.requestTimestamp).format("YYYY/MM/DD HH:mm:ss") : ''}</td>
                                    <td>{data.domainName}</td>
                                    <td>
                                        <input readOnly type="checkbox"
                                               checked={data.visitId in statusMap ? statusMap[data.visitId].muppets : false}/>
                                    </td>
                                    <td>
                                        <input readOnly type="checkbox"
                                               checked={data.visitId in statusMap ? statusMap[data.visitId].dns : false}/>
                                    </td>
                                    <td>
                                        <input readOnly type="checkbox"
                                               checked={data.visitId in statusMap ? statusMap[data.visitId].smtp : false}/>
                                    </td>
                                    <td>
                                        <input readOnly type="checkbox"
                                               checked={data.visitId in statusMap ? statusMap[data.visitId].wappalyzer : false}/>
                                    </td>
                                </tr>
                            ))}
                            </tbody>
                        </Table>
                    </div>
                </Col>
            </Row>
        </>
    )
}

export default TimelineDomainName;
