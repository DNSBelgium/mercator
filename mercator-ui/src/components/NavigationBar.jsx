import {Button, Form, FormControl, InputGroup} from "react-bootstrap";
import React, {useState} from "react";

import api from "../services/api";
import TimelineDomainName from "./timelineCards/TimelineDomainName";

function NavigationBar() {
    const [validated, setValidated] = useState(false); // Hook to validate input field

    let textInput = React.createRef();

    async function search(event) {
        event.preventDefault();

        setValidated(true);

        if(textInput.current.value.trim().length === 0) {
            return;
        }

        let domainName = textInput.current.value.toLowerCase().trim()
        await api.get(`/dispatcherEvents/search/findDispatcherEventByDomainName?domainName=${domainName}`)
            .then((resp) => {
                if (resp.status === 200) {
                    localStorage.setItem("search", domainName)
                    window.location.href = '/';
                }
            })
            .catch((ex) => {
                console.log(ex.response);
            })
    }

    return (
        <>
            <div className="searchfield">
                <Form noValidate validated={validated} className='form' onSubmit={search}>
                    <Form.Group className="input-group">
                        <Form.Label id="input-label">Domain name</Form.Label>
                        <FormControl 
                            id="input-domainname"
                            required
                            type="text"
                            placeholder="Enter full domain name"
                            ref={textInput}
                        />
                        <Button id="input-button" type="submit">Search</Button>
                    </Form.Group>
                </Form>
            </div>
        </>
    )
}

export default NavigationBar;