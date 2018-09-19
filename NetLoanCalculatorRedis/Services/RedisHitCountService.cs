using StackExchange.Redis;

namespace NetLoanCalculator.Services
{
    public class RedisHitCountService : IHitCountService
    {
        private IConnectionMultiplexer _conn;
        public RedisHitCountService (IConnectionMultiplexer conn)
        {
            _conn = conn;
        }

        public long GetAndIncrement()
        {
            IDatabase db = _conn.GetDatabase();
            return db.StringIncrement("loan-calculator");
        }

        public void Reset()
        {
            IDatabase db = _conn.GetDatabase();
            db.StringSet("loan-calculator", 5000);
        }
    }
}