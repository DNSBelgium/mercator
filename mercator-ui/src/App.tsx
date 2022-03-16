import React from 'react';
import {BrowserRouter as Router, Switch, Route} from 'react-router-dom';
import Details from './components/Details';
import Start from './components/Start'
import './App.css';
import 'bootstrap/dist/css/bootstrap.min.css';

function App() {
  return (
      <div className="App">
        <Router>
          <>
              <Switch>
              <Route exact path="/details/:visitId" component={Details}></Route>
              <Route path="/" component={Start}></Route>
              </Switch>
          </>
        </Router>
      </div>
  );
}

export default App;
