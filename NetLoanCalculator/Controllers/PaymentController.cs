using System;
using NetLoanCalculator.Models;
using NetLoanCalculator.Services;
using Microsoft.AspNetCore.Mvc;

namespace NetLoanCalculator.Controllers
{
    [Route("/[controller]")]
    [ApiController]
    public class PaymentController
    {
        private IHitCountService _HitCountService;
        private PaymentCalculator _PaymentCalculator;

        public PaymentController(IHitCountService HitCountService, PaymentCalculator PaymentCalculator) {
            _HitCountService = HitCountService;
            _PaymentCalculator = PaymentCalculator;
        }

        [HttpGet]
        public ActionResult<CalculatedPayment> calculatePayment(double Amount, double Rate, int Years)
        {
            return new CalculatedPayment {
                Amount = Amount,
                Rate = Rate,
                Years = Years,
                Count = _HitCountService.GetAndIncrement(),
                Instance = Environment.GetEnvironmentVariable("CF_INSTANCE_INDEX"),
                Payment = _PaymentCalculator.Calculate(Amount, Rate, Years)
            };
        }
    }
}