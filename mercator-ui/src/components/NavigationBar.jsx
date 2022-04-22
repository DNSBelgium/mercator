import {Button, Form, FormControl} from "react-bootstrap";
import {useState, useRef} from "react";

function NavigationBar() {
    const [validated, setValidated] = useState(false); // Hook to validate input field
    const [urlOrId, setUrlOrId] = useState(false); // Hook to define searching by URL or VisitId

    let textInput = useRef(); // Hook to hold the input field's value.

    // Handle 'search' click.
    const search = (event) => {
        event.preventDefault();

        setValidated(true); //TODO: UI Vaidation isn't quite right yet.

        let domainName = textInput.current.value.toLowerCase().trim();

        if (!urlOrId) { // false (default) === URL search
            
            if(textInput.current.value.trim().length === 0) {
                return;
            }
            
            localStorage.setItem("search", domainName);
            window.location.href = '/';
        }

        else { // true === VisitId search
            window.location.href = '/details/' + domainName;    
        }
        
    }

    // This function changes the input field to search for a URL or navigate directly to a visitId
    const changeSearchField = () => {
        if (!urlOrId) { // false (default) === URL search
            return (
                <>
                    <Form.Label id="navbar-input-label">
                        Domain name
                    </Form.Label>

                    <FormControl 
                        id="NavBar-Input"
                        required
                        type="text"
                        placeholder="Enter full domain name"
                        ref={textInput}
                    />
                    <Button 
                        id="input-button" 
                        type="submit"
                    >
                        Search
                    </Button>
                </>
            );
        } // true === VisitId search
        return (
            <>
                <Form.Label id="navbar-input-label">
                    Visit Id
                </Form.Label>

                <FormControl 
                    id="NavBar-Input"
                    required
                    type="text"
                    placeholder="Enter exact visit Id"
                    ref={textInput}
                />
                <Button 
                    id="input-button"
                    type="submit"
                >
                    Go
                </Button>
            </>
        );
    }

    // This function changes the button depending on which searchfield is active.
    // Also handles the boolean that defines which one should be active.
    const changeBtn = () => {
        if (!urlOrId) {
            return (
                <Button 
                    id='Url-Or-Id-Btn'
                    variant="secondary"
                    size='sm'
                    onClick={() => setUrlOrId(true)}
                >
                    Change to find by visit Id
                </Button>
            );
        }
        return (
            <Button 
                id='Url-Or-Id-Btn'
                variant="secondary"
                size='sm'
                onClick={() => setUrlOrId(false)}
            >
                Change to search by URL
            </Button>
        );
    }

    // This file's HTML return.
    return (
        <>
            <div className="searchfield" id='NavBar-Div'>
                <Form noValidate validated={validated} id='NavBar-Form' onSubmit={search}>
                    <Form.Group className="input-group">
                        {
                            changeSearchField()
                        }
                    </Form.Group>
                    {
                        changeBtn()
                    }
                </Form>
            </div>
        </>
    )
}

export default NavigationBar;