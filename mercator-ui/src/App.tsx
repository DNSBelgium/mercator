import React from 'react';
import {BrowserRouter as Router, Switch, Route} from 'react-router-dom';

import Details from './components/Details';
import NavigationBar from './components/NavigationBar';

import './App.scss';
import 'bootstrap/dist/css/bootstrap.min.css';
import TimelineDomainName from './components/timelineCards/TimelineDomainName';

function App() {
  return (
      <div className="App">
        <NavigationBar />

        <Router>
            <Switch>
              <Route exact path="/details/:visitId" component={Details}></Route>
              <Route path="/" component={TimelineDomainName}></Route>
            </Switch>
        </Router>
      </div>
  );
}

export default App;
