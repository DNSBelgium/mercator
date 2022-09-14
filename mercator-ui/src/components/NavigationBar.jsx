import { useState, useRef } from "react";
import { useNavigate } from 'react-router-dom';
import { Button, ButtonGroup, Dropdown, Form, FormControl, ButtonToolbar, Container } from "react-bootstrap";

function NavigationBar() {
    const navigate = useNavigate();

    const [recentDashboards, setRecentDashboards] = useState(null);
    const [validated, setValidated] = useState(false); // Hook to validate input field.
    const [searchType, setSearchType] = useState("domain"); // Hook to define searching by URL, visitId, ...

    let textInput = useRef(); // Hook to hold the input field's value.

    // Handle 'search' click.
    const search = (event) => {
        event.preventDefault();
        setValidated(true); //TODO: UI Validation isn't quite right yet.
        let input = textInput.current.value.toLowerCase().trim();

        if(textInput.current.value.trim().length === 0) {
            return;
        }

        switch (searchType) {
            case "visitId":
                navigate('/details/' + input);
                break;

            case "domain":
                navigate(`${input}/1`);
                break;

            default:
                return;
        }
    }

    // This function changes the search functionality depending on which one is selected.
    const changeSearchField = () => { // Inside Form.Group element
        let btnText;
        let searchPlaceholder;
        let searchBtnText;

        switch(searchType) {
            case "visitId":
                btnText = "Visit Id";
                searchPlaceholder = "Enter exact visit Id";
                searchBtnText = "Go";
                break;

            case "domain":
            default:
                btnText = "Domain Name";
                searchPlaceholder = "Enter domain name";
                searchBtnText = "Search";
                break;
        }

        return (
            <div id="form-container">
                <Dropdown>
                    <Dropdown.Toggle id="dropdown-btn">
                        { btnText }
                    </Dropdown.Toggle>
                    
                    <Dropdown.Menu>
                        <Dropdown.Item onClick={() => setSearchType("domain")}>Domain Name</Dropdown.Item>
                        <Dropdown.Item onClick={() => setSearchType("visitId")}>Visit Id</Dropdown.Item>
                    </Dropdown.Menu>
                </Dropdown>

                <FormControl
                    id="navbar-input"
                    required
                    type="text"
                    placeholder={searchPlaceholder}
                    ref={textInput}
                />

                <Button 
                    id="submit-btn"
                    type="submit"
                >
                    { searchBtnText }
                </Button>
            </div>
        );
    }

    // This file's HTML return.
    return (
        <>
            <div className="searchfield" id='NavBar-Div'>
                <Form noValidate validated={validated} onSubmit={search}>
                    <Form.Group className="input-group">
                        {
                            changeSearchField()
                        }
                    </Form.Group>
                </Form>
                <Container fluid>
                <ButtonToolbar>
                    <ButtonGroup>
                        <Button
                            id="cluster-link"
                            onClick={() => navigate("/cluster")}
                        >
                            Cluster Validation
                        </Button>
                    </ButtonGroup>
                </ButtonToolbar></Container>
            </div>
        </>
    )
}

export default NavigationBar;