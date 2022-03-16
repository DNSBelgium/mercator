## Dependencies

* react
* react-dom
* react-router-dom
* react-strips
* bootstrap
* react-bootstrap
* react-scrollable-anchor
* react-border-wrapper
* typescript
* axios
* moment
* web-vitals

## Running

Run `npm install` to install all dependencies.
Run `npm start` to run the app locally.

Mercator-ui runs on port 8090.
If something is already running on port 8090, you will see it in the terminal, and can run the app on another port, Run `y`.

### Timeline for one domain name

Timeline for one domain name is the start page, you need a domain name that you can write in the search bar and click on the button.
You can also navigate to "Details of one visit" page clicking on one of the visit id row.

### Details of one visit

You need a visit id for the URL in your browser like this to get the details of one visit:
 http://localhost:8090/details/32d6f1f0-3a95-4374-9273-c88445eb7f97

##Code
###Components of "Details of one visit"
**Details.jsx** => gets data from all the APIs and send it to the other components "props". 
Also contains the *"Open All"* button which opens all the buttons in each component.
* **ContentCrawlCard.jsx** => returns *"Content Crawl"* + **"Wappalyzer.jsx"**
* **DispatcherCard.jsx** => returns *Domain name*, *Visit id* and *Labels*
* **DNSCard.jsx** =>  returns *"DNS Crawl"*
* **HTMLCard.jsx** =>  returns *"HTML features"*
* **SMTPCard.jsx** =>  returns *"SMTP Crawl"*
* **Wappalyzer.jsx** =>  returns *"Wappalyzer"*

###Components of "Timeline for one domain name"
**Start.jsx** => return *"SearchField.jsx"*
* **SearchField.jsx** => gets input of user (domain name), save it in the localStorage (otherwise after click on link on the visit id, the data will disappear).
* **TimelineDomainName.jsx** => gets data from APIs and returns table
**!!!data of the columns "Status" is dummy data, the API of "api/status" is not working correctly!!!**
















