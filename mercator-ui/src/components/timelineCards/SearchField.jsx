// import {Button, Form, FormControl, InputGroup} from "react-bootstrap";
// import React, {useState} from "react";
// import TimelineDomainName from "./TimelineDomainName";

// const SearchField = () => {
//     const [domainName, setDomainName] = useState(undefined);
//     const [validated, setValidated] = useState(false); // Hook to validate input field

//     // const [state, setState] = useState({isComponentVisible: false})

//     // reference to input of user (domain name)
//     let textInput = React.createRef();

//     // TODO understand what Khava has done here ..

//     // useEffect(() => {
//     //     const blockTime = localStorage.getItem('blockTime')
//     //     if (blockTime !== null) {
//     //         if (+new Date() >= parseInt(blockTime, 0)) {
//     //             localStorage.removeItem('blockTime')
//     //             localStorage.removeItem('SelectedOption')
//     //         } else {
//     //             const oldValue = localStorage.getItem('SelectedOption')
//     //             if (oldValue) {
//     //                 setDomainName(oldValue);
//     //                 setState({isComponentVisible: true});
//     //             }
//     //         }
//     //     }
//     // }, [])

//     // save input after submit in localstorage for +-15 min
//     async function search(event) {
//         event.preventDefault()
//         await setDomainName(textInput.current.value.toLowerCase().trim())
//         // setState({isComponentVisible: !state.isComponentVisible})

//         // localStorage.setItem('blockTime', +new Date() + (200 * 5000))
//         // localStorage.setItem('SelectedOption', textInput.current.value.toLowerCase().trim())

//         // setTimeout(() => {
//         //     setState({isComponentVisible: true});
//         // }, 300);
        
//         // handleSearchBorder();

//         setValidated(true);
//     }

//     return (
//         <>
//             <div className="searchfield">
//                 <Form noValidate validated={validated} className='form' onSubmit={search}>
//                     <Form.Group className="input-group" controlId="validationForSearch">
//                         <Form.Label id="input-label">Domain name</Form.Label>
//                         <FormControl 
//                             id="input-domainname"
//                             required
//                             type="text"
//                             placeholder="Enter full domain name"
//                             ref={textInput}
//                         />
//                         <Button id="input-button" type="submit">Search</Button>
//                     </Form.Group>
//                 </Form>
//             </div>
//             <div>
//                 <TimelineDomainName domainName={domainName} />
//             </div>
//         </>
//     )
// }

// export default SearchField;
