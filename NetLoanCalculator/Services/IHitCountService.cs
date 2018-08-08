namespace NetLoanCalculator.Services
{
    public interface IHitCountService
    {
        long GetAndIncrement();
        void Reset();
    }
}