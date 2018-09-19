using Microsoft.AspNetCore.Mvc;
using NetLoanCalculator.Services;

namespace NetLoanCalculator.Controllers
{
    [Route("/[controller]")]
    [ApiController]
    public class ResetCountController
    {
        private IHitCountService _HitCountService;

        public ResetCountController (IHitCountService HitCountService)
        {
            _HitCountService = HitCountService;
        }

        [HttpGet]
        public void ResetCount()
        {
            _HitCountService.Reset();
        }
    }
}