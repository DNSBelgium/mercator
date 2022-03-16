import {Button, Form, FormControl, InputGroup} from "react-bootstrap";
import React, {useState} from "react";
import TimelineDomainName from "./TimelineDomainName";

const SearchField = () => {
    const [domainName, setDomainName] = useState(undefined);
    // const [state, setState] = useState({isComponentVisible: false})

    // reference to input of user (domain name)
    let textInput = React.createRef();

    // TODO understand what Khava has done here ..

    // useEffect(() => {
    //     const blockTime = localStorage.getItem('blockTime')
    //     if (blockTime !== null) {
    //         if (+new Date() >= parseInt(blockTime, 0)) {
    //             localStorage.removeItem('blockTime')
    //             localStorage.removeItem('SelectedOption')
    //         } else {
    //             const oldValue = localStorage.getItem('SelectedOption')
    //             if (oldValue) {
    //                 setDomainName(oldValue);
    //                 setState({isComponentVisible: true});
    //             }
    //         }
    //     }
    // }, [])

    // save input after submit in localstorage for +-15 min
    const search = evt => {
        evt.preventDefault()
        setDomainName(textInput.current.value.toLowerCase().trim())
        // setState({isComponentVisible: !state.isComponentVisible})

        // localStorage.setItem('blockTime', +new Date() + (200 * 5000))
        // localStorage.setItem('SelectedOption', textInput.current.value.toLowerCase().trim())

        // setTimeout(() => {
        //     setState({isComponentVisible: true});
        // }, 300);
    }

    return (
        <>
            <div className="searchfield">
                <Form className='form' onSubmit={search}>
                    <InputGroup className="input-group">
                        <label id="input-label">Domain name</label>
                        <FormControl id="input-domainname" ref={textInput}/>
                        <Button id="input-button" type="submit">Search</Button>
                    </InputGroup>
                </Form>
            </div>
            <div>
                <TimelineDomainName domainName={domainName} />
            </div>
        </>
    )
}

export default SearchField;
