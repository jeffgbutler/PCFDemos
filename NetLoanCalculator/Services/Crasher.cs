using System;
using System.Threading.Tasks;

namespace NetLoanCalculator.Services
{
    public class Crasher
    {
        public void CrashIt()
        {
            Task.Run(async delegate
            {
                await Task.Delay(2000);
                Environment.Exit(22);
            });
        }
    }
}