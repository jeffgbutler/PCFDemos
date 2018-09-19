namespace NetLoanCalculator.Services
{
    public class MemoryHitCountService : IHitCountService
    {
        private long HitCount = 0;
        public long GetAndIncrement()
        {
            return ++HitCount;
        }

        public void Reset()
        {
            HitCount = 0;
        }
    }
}