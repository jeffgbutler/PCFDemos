using NetLoanCalculator.Services;
using Microsoft.AspNetCore.Mvc;

namespace NetLoanCalculator.Controllers
{
    [Route("/[controller]")]
    [ApiController]
    public class CrashController
    {
        private Crasher _Crasher;

        public CrashController(Crasher Crasher)
        {
            _Crasher = Crasher;
        }

        [HttpGet]
        public void CrashIt()
        {
            _Crasher.CrashIt();
        }
    }
}