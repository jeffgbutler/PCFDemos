new Vue({
  el: "#app",
  data: {
    started: false,
    paymentHistory: [],
    errorHistory: [],
    baseURL: "http://localhost:8080"
  },
  computed: {
    startDisabled: function() {
      return this.started || this.baseURL === "";
    },
    hasPaymentHistory: function() {
      return this.paymentHistory.length > 0;
    },
    hasErrors: function() {
      return this.errorHistory.length > 0;
    }
  },
  methods: {
    start: function() {
      this.started = true;
      this.scheduleUpdate();
    },
    stop: function() {
      this.started = false;
    },
    clear: function() {
      this.paymentHistory = [];
      this.errorHistory = [];
    },
    scheduleUpdate: function() {
      setTimeout(() => this.getPayment(), 1000);
    },
    addError: function(request, err) {
      this.errorHistory.unshift({
        request: request,
        message: err.message,
        timestamp: new Date().toLocaleString(),
      });
      this.errorHistory = this.errorHistory.slice(0, 10);
    },
    addPayment: function(data) {
      this.paymentHistory.unshift({
        amount: data.amount,
        rate: data.rate,
        years: data.years,
        payment: data.payment,
        timestamp: new Date().toLocaleString(),
        instance: data.instance,
        count: data.count
      });
      this.paymentHistory = this.paymentHistory.slice(0, 5);
    },
    getPayment: function() {
      let rate = Math.floor(Math.random() * 7); // random integer 0..6
      let amount = 100000.0 + Math.random() * 700000.00; // random between 100,000.00 and 700,000.00
      amount = Math.floor(amount * 100.0) / 100.0; // force 2 decimal digits
      let years = 30;
  
      fetch(`${this.baseURL}/payment?amount=${amount}&rate=${rate}&years=${years}`, {method: 'GET'})
      .then((res) => res.json())
      .then((data) => {
        this.addPayment(data);
      })
      .catch((err) => {
        this.addError(`Details: a=${amount}, r=${rate}, y=${years}`, err);
      })
      .then(() => {
        if (this.started) {
          this.scheduleUpdate();
        }
      });
    },
    crashIt: function() {
      fetch(`${this.baseURL}/crash`, {method: 'GET'})
      .catch((err) => {
        this.addError('Crash Request', err);
      });
    },
    resetCount() {
      fetch(`${this.baseURL}/resetCount`, {method: 'GET'})
      .catch((err) => {
        this.addError('Reset Count Request', err);
      });
    }
  }
});
