using System.Web;
using Microsoft.Extensions.Configuration;

namespace NetLoanCalculatorRedis
{
    public static class RedisExtentions
    {
        private const string REDIS_URL = "vcap:services:azure-rediscache:0:credentials:redisUrl";

        public static IConfiguration DecodeAzureServiceBrokerRedisUrl(this IConfiguration configuration)
        {
            configuration[REDIS_URL] = HttpUtility.UrlDecode(configuration[REDIS_URL]);
            return configuration;
        }
    }
}
