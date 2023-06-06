import {useEffect, useState} from "react";
import {Card, Col, Row, Table} from "react-bootstrap";
import api from "../../services/api";
import {checkObjectIsFalsy} from "../../services/Util";
import DnsRequestDataTable from "../DnsRequestTableBody";
import moment from "moment";

const DNSCard = (props) => {
    const visitId = props.visitId

    const [data, setData] = useState({});
    console.log(data)

    useEffect(() => {

        const fetchData = async () => {
            const url = `/requests/search/findByVisitId?visitId=${visitId}`;
            await api.get(url)
                .then((resp) => {
                    if(resp.status === 200) {
                        setData(resp.data._embedded.requests);
                    }
                })
                .catch((ex) => {
                    console.log(ex);
                })
        };

        fetchData();
    }, [visitId]);


    // Writing HTML on a function base so we can define logic more easily.
    const renderHTML = () => {

        if(checkObjectIsFalsy(data)) {
            return (<Card.Body>No data for this visit.</Card.Body>)
        }

        return (
            <Card.Body className="dns-table">
                <br/>
                <h5>Crawl time stamp
                    : {data[0].crawlTimestamp ? moment(data[0].crawlTimestamp).format("DD/MM/YYYY HH:mm:ss") : ''}</h5>
                <br/>
                <Table
                    size="sm"
                >
                    <thead className="dns-table-head">
                    <tr>
                        <th>
                            prefix
                        </th>
                        <th>
                            result
                        </th>
                        <th>
                            record type
                        </th>
                        <th>
                            ttl
                        </th>
                        <th>
                            record data
                        </th>
                        <th>
                            country
                        </th>
                        <th>
                            adn
                        </th>
                        <th>
                            adn organisation
                        </th>
                        <th>
                            ip
                        </th>
                        <th>
                            ip version
                        </th>
                    </tr>
                    </thead>
                    <tbody>
                    {/*{console.log(data)}*/}
                    {/*sort data first by prefix then by Rcode*/}
                    {/*check alternative */}
                    {Object.values(data).sort((a, b) => {
                        if (a.prefix < b.prefix) {
                            return -1;
                        }
                        if (a.prefix > b.prefix) {
                            return 1;
                        }
                        return a.rcode - b.rcode;
                    }).map((request, index) => (
                        <DnsRequestDataTable request={request} requestIndex={index} key={request.id}/>
                    ))}

                    {/*{data.map((request, index) => (*/}
                    {/*    <DnsRequestDataTable request={request} requestIndex={index}>*/}
                    {/*    </DnsRequestDataTable>*/}
                    {/*))}*/}
                    </tbody>
                </Table>
            </Card.Body>
        );
    }

    // This file's HTML return.
    return (
        <Row>
            <Col className='mt-4'>
                <Card className="card">
                    <Card.Header as="h2" className="h5">DNS crawl</Card.Header>
                    {
                        renderHTML()
                    }
                </Card>
            </Col>
        </Row>
    );
}

export default DNSCard;
