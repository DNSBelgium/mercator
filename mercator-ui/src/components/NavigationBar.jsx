import {Button, Form, FormControl} from "react-bootstrap";
import {useState, useRef} from "react";

function NavigationBar() {
    const [validated, setValidated] = useState(false); // Hook to validate input field

    let textInput = useRef();

    const search = (event) => {
        event.preventDefault();

        setValidated(true); //TODO: UI Vaidation isn't quite right yet.

        if(textInput.current.value.trim().length === 0) {
            return;
        }

        let domainName = textInput.current.value.toLowerCase().trim();
        localStorage.setItem("search", domainName);
        window.location.href = '/';
    }

    return (
        <>
            <div className="searchfield" id='NavBar-Div'>
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