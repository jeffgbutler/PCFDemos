namespace NetLoanCalculator.Models
{
    public class CalculatedPayment
    {
        public double Amount {get; set;}
        public double Rate {get; set;}
        public int Years {get; set;}
        public decimal Payment {get; set;}
        public string Instance {get; set;}
        public long Count {get; set;}
    }
}