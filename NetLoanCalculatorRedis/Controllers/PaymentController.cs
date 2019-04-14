using System;
using NetLoanCalculator.Models;
using NetLoanCalculator.Services;
using Microsoft.AspNetCore.Mvc;
using Steeltoe.Extensions.Configuration.CloudFoundry;
using Microsoft.Extensions.Options;

namespace NetLoanCalculator.Controllers
{
    [Route("/[controller]")]
    [ApiController]
    public class PaymentController
    {
        private IHitCountService _HitCountService;
        private PaymentCalculator _PaymentCalculator;
        private CloudFoundryApplicationOptions _ApplicationOptions;
        public PaymentController(IHitCountService HitCountService, PaymentCalculator PaymentCalculator, IOptions<CloudFoundryApplicationOptions> ApplicationOptions) {
            _HitCountService = HitCountService;
            _PaymentCalculator = PaymentCalculator;
            _ApplicationOptions = ApplicationOptions.Value;
        }

        [HttpGet]
        public ActionResult<CalculatedPayment> calculatePayment(double Amount, double Rate, int Years)
        {
            return new CalculatedPayment {
                Amount = Amount,
                Rate = Rate,
                Years = Years,
                Count = _HitCountService.GetAndIncrement(),
                Instance = _ApplicationOptions.InstanceIndex.ToString(),
                Payment = _PaymentCalculator.Calculate(Amount, Rate, Years)
            };
        }
    }
}
