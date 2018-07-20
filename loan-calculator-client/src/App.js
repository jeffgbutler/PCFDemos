import React, { Component } from 'react';
import './App.css';

class App extends Component {
  constructor(props) {
    super(props);
    this.state = {
      started: false,
      paymentHistory: [],
      errorHistory: [],
      baseURL: ""
    }
    this.updateURL = this.updateURL.bind(this);
  }

  start() {
    this.setState((prevState, props) => {
      return {
        started: true,
        paymentHistory: prevState.paymentHistory,
        errorHistory: prevState.errorHistory,
        baseURL: prevState.baseURL
      }
    });
    this.scheduleUpdate();
  }

  stop() {
    this.setState((prevState, props) => {
      return {
        started: false,
        paymentHistory: prevState.paymentHistory,
        errorHistory: prevState.errorHistory,
        baseURL: prevState.baseURL
      }
    });
  }

  clearHistory() {
    this.setState((prevState, props) => {
      return {
        started: prevState.started,
        paymentHistory: [],
        errorHistory: [],
        baseURL: prevState.baseURL
      }
    })
  }

  scheduleUpdate() {
    setTimeout(() => this.getPayment(), 1000);
  }

  updateURL(event) {
    const newURL = event.target.value;

    this.setState((prevState, props) => {
      return {
        started: prevState.started,
        paymentHistory: prevState.paymentHistory,
        errorHistory: prevState.errorHistory,
        baseURL: newURL
      }
    })
  }

  getPayment() {
    let rate = Math.floor(Math.random() * 7); // random integer 0..6
    let amount = 100000.0 + Math.random() * 600000.00; // random between 100,000.00 and 700,000.00
    amount = Math.floor(amount * 100.0) / 100.0; // force 2 decimal digits
    let years = 30;

    fetch(`https://${this.state.baseURL}/payment?amount=${amount}&rate=${rate}&years=${years}`, {method: 'GET'})
    .then((res) => res.json())
    .then((data) => {
      this.addPayment(data);
    })
    .catch((err) => {
      this.addError(`Details: a=${amount}, r=${rate}, y=${years}`, err);
    })
    .then(() => {
      if (this.state.started) {
        this.scheduleUpdate();
      }
    })
  }

  crashIt() {
    fetch(`https://${this.state.baseURL}/crash`, {method: 'GET'})
    .catch((err) => {
      this.addError('Crash Request', err);
    });
  }

  resetCount() {
    fetch(`https://${this.state.baseURL}/resetCount`, {method: 'GET'})
    .catch((err) => {
      this.addError('Reset Count Request', err);
    });
  }

  addError(request, err) {
    let history = this.state.errorHistory;

    history.unshift({
      request: request,
      message: err.message,
      timestamp: new Date().toLocaleString(),
    });

    history = history.slice(0, 15);
    this.setState((prevState, props) => {
      return {
        started: prevState.started,
        paymentHistory: prevState.paymentHistory,
        errorHistory: history,
        baseURL: prevState.baseURL
      }
    })
  }

  addPayment(data) {
    let history = this.state.paymentHistory;

    history.unshift({
      amount: data.amount,
      rate: data.rate,
      years: data.years,
      payment: data.payment,
      timestamp: new Date().toLocaleString(),
      instance: data.instance,
      count: data.count
    });

    history = history.slice(0, 15);
    this.setState((prevState, props) => {
      return {
        started: prevState.started,
        paymentHistory: history,
        errorHistory: prevState.errorHistory,
        baseURL: prevState.baseURL
      }
    })
  }

  render() {
    let paymentHistory = this.state.paymentHistory;
    let errorHistory = this.state.errorHistory;

    return (
      <div className="mainPage">
        <title>Loan Calculator Tester</title>
        <h1>Loan Calculator Tester</h1>
        <form>
          Base URL: <input id="urlBox" type="text" value={this.state.baseURL} onChange={this.updateURL}/>
        </form>
        <br/>
        <div>
          <button id="startButton" onClick={() => this.start()} disabled={this.state.started || this.state.baseURL === ""}>Start</button>
          &nbsp;
          <button id="stopButton" onClick={() => this.stop()} disabled={!this.state.started || this.state.baseURL === ""}>Stop</button>
          &nbsp;
          <button id="resetCountButton" onClick={() => this.resetCount()} disabled={this.state.baseURL === ""}>Reset Count</button>
          &nbsp;
          <button id="clearButton" onClick={() => this.clearHistory()}>Clear History</button>
          &nbsp;
          <button id="crashButton" onClick={() => this.crashIt()} disabled={this.state.baseURL === ""}>Crash It</button>
        </div>
        <div className="inline">
          <h2>Payment History</h2>
          <table border="1" cellSpacing="0" cellPadding="5">
            <thead>
              <tr>
                <th>Amount</th>
                <th>Rate</th>
                <th>Years</th>
                <th>Payment</th>
                <th>Timestamp</th>
                <th>PCF Instance</th>
                <th>Hit Count</th>
              </tr>
            </thead>
            <tbody>
              {paymentHistory.map((hist, index) => {
                return (
                  <tr key={index}>
                    <td>${hist.amount}</td>
                    <td>{hist.rate}%</td>
                    <td>{hist.years}</td>
                    <td>${hist.payment}</td>
                    <td>{hist.timestamp}</td>
                    <td>{hist.instance}</td>
                    <td>{hist.count}</td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
        <div className="inline">
          <h2>Error History</h2>
          <table border="1" cellSpacing="0" cellPadding="5">
            <thead>
              <tr>
                <th>Request</th>
                <th>Error</th>
                <th>Timestamp</th>
              </tr>
            </thead>
            <tbody>
            {errorHistory.map((hist, index) => {
                return (
                  <tr key={index}>
                    <td>{hist.request}</td>
                    <td>{hist.message}</td>
                    <td>{hist.timestamp}</td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>
    );
  }
}

export default App;
