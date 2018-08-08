using System;

namespace NetLoanCalculator.Services
{
    public class PaymentCalculator
    {
        public Decimal Calculate(double Amount, double Rate, int Years)
        {
            if (Rate == 0.0)
            {
                return CalculateWithoutInterest(Amount, Years);
            }
            else
            {
                return CalculateWithInterest(Amount, Rate, Years);
            }
        }
    
        private Decimal CalculateWithInterest(double Amount, double Rate, int Years)
        {
            double monthlyRate = Rate / 100.0 / 12.0;
            int numberOfPayments = Years * 12;
            double payment = (monthlyRate * Amount) / (1.0 - Math.Pow(1.0 + monthlyRate, -numberOfPayments));
            return ToMoney(payment);
        }

        private Decimal CalculateWithoutInterest(double Amount, int Years)
        {
            int numberOfPayments = Years * 12;
            return ToMoney(Amount / numberOfPayments);
        }
    
        private Decimal ToMoney(double d)
        {
            Decimal bd = new Decimal(d);
            return Decimal.Round(bd, 2, MidpointRounding.AwayFromZero);
        }
    }
}